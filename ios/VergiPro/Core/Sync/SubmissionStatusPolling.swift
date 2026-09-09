import Foundation

struct RemoteSubmissionStatus: Sendable {
    let state: QueuedMutation.State
    let progress: Double?
    let serverDocumentID: String?
    let retryAfter: TimeInterval?
}

protocol SubmissionStatusTransport: Sendable {
    func fetch(submissionID: UUID, organizationID: Int) async throws -> RemoteSubmissionStatus
}

actor SubmissionStatusPoller {
    private let queue: OfflineMutationQueue
    private let transport: any SubmissionStatusTransport

    init(queue: OfflineMutationQueue, transport: any SubmissionStatusTransport) {
        self.queue = queue; self.transport = transport
    }

    func poll(submissionID: UUID, organizationID: Int) async throws {
        var transportFailures = 0
        while !Task.isCancelled {
            do {
                let remote = try await transport.fetch(submissionID: submissionID, organizationID: organizationID)
                try await queue.transition(id: submissionID, to: remote.state, progress: remote.progress, serverDocumentID: remote.serverDocumentID)
                transportFailures = 0
                if remote.state.isTerminal { return }
                try await Task.sleep(for: .seconds(min(max(remote.retryAfter ?? 3, 1), 30)))
            } catch is CancellationError { throw CancellationError() }
            catch is SubmissionStateError { throw SubmissionStateError.invalidTransition }
            catch {
                transportFailures += 1
                let seconds = min(pow(2, Double(transportFailures)), 30) + Double.random(in: 0...1)
                try await Task.sleep(for: .seconds(seconds))
            }
        }
    }
}

extension QueuedMutation.State {
    var isTerminal: Bool { self == .needsVerification || self == .needsAttention || self == .completed }
}
