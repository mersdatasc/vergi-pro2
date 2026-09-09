import Foundation
import SwiftUI
import UIKit

enum CaptureQualitySignal: String, CaseIterable, Sendable {
    case documentNotFound
    case moveCloser
    case holdSteady
    case lowLight
    case glare
    case edgeClipped
    case ready

    var symbol: String {
        switch self {
        case .ready: "checkmark.circle.fill"
        case .lowLight: "sun.min"
        case .glare: "sparkle"
        case .holdSteady: "hand.raised"
        case .moveCloser: "arrow.up.left.and.arrow.down.right"
        case .edgeClipped: "viewfinder"
        case .documentNotFound: "doc.viewfinder"
        }
    }
}

struct CapturedPage: Identifiable {
    let id: UUID
    let image: UIImage
    let capturedAt: Date
    let vaultDocument: VaultDocument
}

@MainActor
final class CaptureDraft: ObservableObject {
    @Published private(set) var pages: [CapturedPage] = []
    @Published var isScannerPresented = false
    @Published var qualitySignal: CaptureQualitySignal = .documentNotFound
    @Published private(set) var isPersisting = false
    @Published private(set) var hasRestored = false
    @Published private(set) var submissionID: UUID?
    private var organizationID: Int?
    private let vault: SecureDocumentVault?
    private let store: any CaptureDraftStoring

    init(store: any CaptureDraftStoring = KeychainCaptureDraftStore()) {
        vault = try? SecureDocumentVault()
        self.store = store
    }

    var canContinue: Bool { !pages.isEmpty && !isPersisting && submissionID == nil }

    func configure(organizationID: Int) async throws {
        guard self.organizationID != organizationID else { return }
        self.organizationID = organizationID
        pages = []
        submissionID = nil
        hasRestored = false
        try await restore()
    }

    func append(images: [UIImage]) async throws {
        guard submissionID == nil else { throw APIError.conflict }
        guard let organizationID else { throw APIError.invalidRequest }
        guard let vault else { throw CapturePersistenceError.vaultUnavailable }
        isPersisting = true
        defer { isPersisting = false }
        var persisted: [CapturedPage] = []
        for image in images {
            guard let data = image.jpegData(compressionQuality: 0.92) else { throw CapturePersistenceError.encodingFailed }
            let document = try await vault.store(data, organizationID: organizationID, fileName: "scan-\(UUID().uuidString).jpg", mediaType: "image/jpeg")
            persisted.append(CapturedPage(id: document.id, image: image, capturedAt: .now, vaultDocument: document))
        }
        do {
            try store.save(.init(organizationID: organizationID, documents: (pages + persisted).map(\.vaultDocument), submissionID: nil))
        } catch {
            for page in persisted { try? await vault.delete(page.vaultDocument) }
            throw error
        }
        pages.append(contentsOf: persisted)
        qualitySignal = pages.isEmpty ? .documentNotFound : .ready
    }

    func removePage(id: CapturedPage.ID) async {
        guard submissionID == nil else { return }
        guard let organizationID else { return }
        guard let page = pages.first(where: { $0.id == id }) else { return }
        let remaining = pages.filter { $0.id != id }
        guard (try? store.save(.init(organizationID: organizationID, documents: remaining.map(\.vaultDocument), submissionID: nil))) != nil else { return }
        try? await vault?.delete(page.vaultDocument)
        pages = remaining
        if pages.isEmpty { qualitySignal = .documentNotFound }
    }

    func reorder(fromOffsets: IndexSet, toOffset: Int) async {
        guard submissionID == nil else { return }
        guard let organizationID else { return }
        var reordered = pages
        reordered.move(fromOffsets: fromOffsets, toOffset: toOffset)
        guard (try? store.save(.init(organizationID: organizationID, documents: reordered.map(\.vaultDocument), submissionID: nil))) != nil else { return }
        pages = reordered
    }

    func restore() async throws {
        guard !hasRestored else { return }
        guard let organizationID else { throw APIError.invalidRequest }
        defer { hasRestored = true }
        guard let vault else { throw CapturePersistenceError.vaultUnavailable }
        let manifest = try store.load(organizationID: organizationID)
        let documents = manifest.documents
        var restored: [CapturedPage] = []
        for document in documents {
            let data = try await vault.read(document)
            guard let image = UIImage(data: data) else { throw CapturePersistenceError.encodingFailed }
            restored.append(.init(id: document.id, image: image, capturedAt: document.createdAt, vaultDocument: document))
        }
        pages = restored
        submissionID = manifest.submissionID
        qualitySignal = restored.isEmpty ? .documentNotFound : .ready
    }

    func beginSubmission() throws -> UUID {
        guard let organizationID else { throw APIError.invalidRequest }
        if let submissionID { return submissionID }
        let id = UUID()
        try store.save(.init(organizationID: organizationID, documents: pages.map(\.vaultDocument), submissionID: id))
        submissionID = id
        return id
    }

    func rollbackSubmission(_ id: UUID) {
        guard submissionID == id else { return }
        guard let organizationID else { return }
        guard (try? store.save(.init(organizationID: organizationID, documents: pages.map(\.vaultDocument), submissionID: nil))) != nil else { return }
        submissionID = nil
    }

    func completeUpload() async {
        guard let organizationID else { return }
        for page in pages { try? await vault?.delete(page.vaultDocument) }
        try? store.save(.init(organizationID: organizationID, documents: [], submissionID: nil))
        pages = []
        submissionID = nil
        qualitySignal = .documentNotFound
    }
}

enum CapturePersistenceError: Error { case vaultUnavailable, encodingFailed }
