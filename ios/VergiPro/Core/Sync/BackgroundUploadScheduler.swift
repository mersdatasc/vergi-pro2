import BackgroundTasks
import Foundation

enum CellularUploadPolicy: String, Codable, Sendable { case allowed, wifiOnly }

enum SyncIndicatorState: Equatable, Sendable {
    case hidden
    case offlineQueued(count: Int)
    case uploading(progress: Double)
    case needsAttention(count: Int)
}

@MainActor
final class SyncStatusController: ObservableObject {
    @Published private(set) var indicator: SyncIndicatorState = .hidden

    func update(queued: Int, activeProgress: Double?, needsAttention: Int, isOnline: Bool) {
        if needsAttention > 0 {
            indicator = .needsAttention(count: needsAttention)
        } else if let activeProgress {
            indicator = .uploading(progress: min(max(activeProgress, 0), 1))
        } else if queued > 0, !isOnline {
            indicator = .offlineQueued(count: queued)
        } else {
            indicator = .hidden
        }
    }
}

protocol BackgroundUploadRunning: Sendable {
    func processPendingUploads() async throws
}

final class BackgroundUploadScheduler: @unchecked Sendable {
    static let identifier = "com.vergipro.mobile.background-upload"
    private let runner: any BackgroundUploadRunning
    private let scheduler: BGTaskScheduler

    init(runner: any BackgroundUploadRunning, scheduler: BGTaskScheduler = .shared) {
        self.runner = runner
        self.scheduler = scheduler
    }

    func register() {
        scheduler.register(forTaskWithIdentifier: Self.identifier, using: nil) { [runner] task in
            guard let processingTask = task as? BGProcessingTask else {
                task.setTaskCompleted(success: false)
                return
            }
            let completion = BackgroundTaskCompletion(processingTask)
            let work = Task {
                do {
                    try await runner.processPendingUploads()
                    completion.finish(success: true)
                } catch is CancellationError {
                    completion.finish(success: false)
                } catch {
                    completion.finish(success: false)
                }
            }
            completion.setExpirationHandler { work.cancel() }
        }
    }

    func schedule(earliest: Date? = nil) throws {
        let request = BGProcessingTaskRequest(identifier: Self.identifier)
        request.requiresNetworkConnectivity = true
        request.requiresExternalPower = false
        request.earliestBeginDate = earliest
        try scheduler.submit(request)
    }

    func cancel() { scheduler.cancel(taskRequestWithIdentifier: Self.identifier) }
}

private final class BackgroundTaskCompletion: @unchecked Sendable {
    private let task: BGProcessingTask
    init(_ task: BGProcessingTask) { self.task = task }
    func finish(success: Bool) { task.setTaskCompleted(success: success) }
    func setExpirationHandler(_ handler: @escaping @Sendable () -> Void) { task.expirationHandler = handler }
}
