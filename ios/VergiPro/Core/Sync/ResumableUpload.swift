import CryptoKit
import Foundation

enum UploadState: String, Codable, Sendable { case local, queued, creatingSession, uploading, paused, completing, processing, needsAttention, completed }

struct ResumableUpload: Codable, Identifiable, Sendable {
    let id: UUID
    let organizationID: Int
    let document: VaultDocument
    let idempotencyKey: UUID
    var serverUploadID: String?
    var confirmedOffset: Int
    var state: UploadState
    var attemptCount: Int
}

struct UploadSession: Sendable { let id: String; let confirmedOffset: Int; let chunkSize: Int }

protocol UploadTransport: Sendable {
    func create(for upload: ResumableUpload) async throws -> UploadSession
    func append(_ data: Data, offset: Int, checksum: String, session: UploadSession, organizationID: Int) async throws -> Int
    func complete(session: UploadSession, upload: ResumableUpload) async throws
}

actor ResumableUploadCoordinator {
    private let vault: SecureDocumentVault
    private let transport: any UploadTransport

    init(vault: SecureDocumentVault, transport: any UploadTransport) { self.vault = vault; self.transport = transport }

    func run(_ original: ResumableUpload) async throws -> ResumableUpload {
        var upload = original
        let bytes = try await vault.read(upload.document)
        upload.state = .creatingSession
        let session = try await transport.create(for: upload)
        guard session.chunkSize > 0, session.confirmedOffset >= 0, session.confirmedOffset <= bytes.count else { throw UploadError.invalidServerOffset }
        upload.serverUploadID = session.id
        upload.confirmedOffset = session.confirmedOffset
        upload.state = .uploading
        while upload.confirmedOffset < bytes.count {
            try Task.checkCancellation()
            let end = min(upload.confirmedOffset + session.chunkSize, bytes.count)
            let chunk = bytes[upload.confirmedOffset..<end]
            let checksum = SHA256Hex.make(Data(chunk))
            let nextOffset = try await transport.append(Data(chunk), offset: upload.confirmedOffset, checksum: checksum, session: session, organizationID: upload.organizationID)
            guard nextOffset > upload.confirmedOffset, nextOffset <= bytes.count else { throw UploadError.invalidServerOffset }
            upload.confirmedOffset = nextOffset
        }
        upload.state = .completing
        try await transport.complete(session: session, upload: upload)
        upload.state = .processing
        return upload
    }
}

enum UploadError: Error, Sendable { case invalidServerOffset }

private enum SHA256Hex {
    static func make(_ data: Data) -> String {
        SHA256.hash(data: data).map { String(format: "%02x", $0) }.joined()
    }
}
