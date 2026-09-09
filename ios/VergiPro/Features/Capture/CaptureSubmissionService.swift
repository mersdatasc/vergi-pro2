import Foundation

struct CaptureSubmissionPayload: Codable, Sendable {
    let submissionID: UUID
    let organizationID: Int
    let createdAt: Date
    let pages: [VaultDocument]
}

actor CaptureSubmissionService {
    private let queue: OfflineMutationQueue

    init(queue: OfflineMutationQueue) { self.queue = queue }

    func prepare(pages: [CapturedPage], organizationID: Int, submissionID: UUID) async throws -> UUID {
        guard !pages.isEmpty, pages.allSatisfy({ $0.vaultDocument.organizationID == organizationID }) else {
            throw APIError.invalidRequest
        }
        let payload = CaptureSubmissionPayload(submissionID: submissionID, organizationID: organizationID, createdAt: .now, pages: pages.map(\.vaultDocument))
        let mutation = QueuedMutation(
            id: submissionID, organizationID: organizationID, aggregateID: submissionID.uuidString,
            operation: .uploadDocument, payload: try JSONEncoder().encode(payload), idempotencyKey: submissionID,
            attemptCount: 0, nextAttemptAt: nil, state: .pending
        )
        try await queue.enqueue(mutation)
        return submissionID
    }

    func status(submissionID: UUID) async -> QueuedMutation? { await queue.status(id: submissionID) }
}
