import CryptoKit
import Foundation
import Security

struct VaultDocument: Codable, Identifiable, Sendable {
    let id: UUID
    let organizationID: Int
    let originalFileName: String
    let mediaType: String
    let byteCount: Int
    let sha256: String
    let createdAt: Date
}

actor SecureDocumentVault {
    private let root: URL
    private let keyAccount = "document-vault-key-v1"
    private let keyService = "com.vergipro.mobile.document-vault"

    init(fileManager: FileManager = .default) throws {
        let support = try fileManager.url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
        root = support.appending(path: "SecureDocuments", directoryHint: .isDirectory)
        try fileManager.createDirectory(at: root, withIntermediateDirectories: true, attributes: [.protectionKey: FileProtectionType.completeUntilFirstUserAuthentication])
    }

    func store(_ plaintext: Data, organizationID: Int, fileName: String, mediaType: String) throws -> VaultDocument {
        let id = UUID()
        let digest = SHA256.hash(data: plaintext).map { String(format: "%02x", $0) }.joined()
        let encrypted = try AES.GCM.seal(plaintext, using: symmetricKey()).combined!
        let folder = root.appending(path: String(organizationID), directoryHint: .isDirectory)
        try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true, attributes: [.protectionKey: FileProtectionType.completeUntilFirstUserAuthentication])
        let file = folder.appending(path: id.uuidString).appendingPathExtension("vpd")
        try encrypted.write(to: file, options: [.atomic, .completeFileProtectionUnlessOpen])
        return VaultDocument(id: id, organizationID: organizationID, originalFileName: fileName, mediaType: mediaType, byteCount: plaintext.count, sha256: digest, createdAt: .now)
    }

    func read(_ document: VaultDocument) throws -> Data {
        let file = root.appending(path: String(document.organizationID)).appending(path: document.id.uuidString).appendingPathExtension("vpd")
        let sealed = try AES.GCM.SealedBox(combined: Data(contentsOf: file))
        let plaintext = try AES.GCM.open(sealed, using: symmetricKey())
        let digest = SHA256.hash(data: plaintext).map { String(format: "%02x", $0) }.joined()
        guard digest == document.sha256 else { throw VaultError.integrityFailure }
        return plaintext
    }

    func delete(_ document: VaultDocument) throws {
        let file = root.appending(path: String(document.organizationID)).appending(path: document.id.uuidString).appendingPathExtension("vpd")
        if FileManager.default.fileExists(atPath: file.path) { try FileManager.default.removeItem(at: file) }
    }

    private func symmetricKey() throws -> SymmetricKey {
        let base: [String: Any] = [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: keyService, kSecAttrAccount as String: keyAccount, kSecUseDataProtectionKeychain as String: true]
        var query = base
        query[kSecReturnData as String] = true
        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        if status == errSecSuccess, let data = result as? Data { return SymmetricKey(data: data) }
        guard status == errSecItemNotFound else { throw KeychainError(status: status) }
        let key = SymmetricKey(size: .bits256)
        let data = key.withUnsafeBytes { Data($0) }
        var insert = base
        insert[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        insert[kSecValueData as String] = data
        let insertStatus = SecItemAdd(insert as CFDictionary, nil)
        guard insertStatus == errSecSuccess else { throw KeychainError(status: insertStatus) }
        return key
    }
}

enum VaultError: Error, Sendable { case integrityFailure }
