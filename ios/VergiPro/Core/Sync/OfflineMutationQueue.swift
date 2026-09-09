import Foundation
import Security

enum OfflineOperation: String, Codable, Sendable {
    case uploadDocument, addComment, updateVerification, completeTask
}

struct QueuedMutation: Identifiable, Codable, Sendable {
    enum State: String, Codable, Sendable {
        case pending, running, retryScheduled, uploading, processing, needsVerification, needsAttention, completed
    }

    let id: UUID
    let organizationID: Int
    let aggregateID: String
    let operation: OfflineOperation
    let payload: Data
    let idempotencyKey: UUID
    var attemptCount: Int
    var nextAttemptAt: Date?
    var state: State
    var progress: Double?
    var serverDocumentID: String?

    init(id: UUID, organizationID: Int, aggregateID: String, operation: OfflineOperation, payload: Data, idempotencyKey: UUID, attemptCount: Int, nextAttemptAt: Date?, state: State, progress: Double? = nil, serverDocumentID: String? = nil) {
        self.id = id; self.organizationID = organizationID; self.aggregateID = aggregateID; self.operation = operation
        self.payload = payload; self.idempotencyKey = idempotencyKey; self.attemptCount = attemptCount
        self.nextAttemptAt = nextAttemptAt; self.state = state; self.progress = progress; self.serverDocumentID = serverDocumentID
    }

    enum CodingKeys: String, CodingKey { case id, organizationID, aggregateID, operation, payload, idempotencyKey, attemptCount, nextAttemptAt, state, progress, serverDocumentID }

    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        id = try values.decode(UUID.self, forKey: .id)
        organizationID = try values.decode(Int.self, forKey: .organizationID)
        aggregateID = try values.decode(String.self, forKey: .aggregateID)
        operation = try values.decode(OfflineOperation.self, forKey: .operation)
        payload = try values.decode(Data.self, forKey: .payload)
        idempotencyKey = try values.decode(UUID.self, forKey: .idempotencyKey)
        attemptCount = try values.decode(Int.self, forKey: .attemptCount)
        nextAttemptAt = try values.decodeIfPresent(Date.self, forKey: .nextAttemptAt)
        state = try values.decode(State.self, forKey: .state)
        progress = try values.decodeIfPresent(Double.self, forKey: .progress)
        serverDocumentID = try values.decodeIfPresent(String.self, forKey: .serverDocumentID)
    }
}

actor OfflineMutationQueue {
    private(set) var items: [QueuedMutation]
    private let store: any MutationQueueStore

    init(store: any MutationQueueStore = KeychainMutationQueueStore()) throws {
        self.store = store
        self.items = try store.load()
    }

    func enqueue(_ mutation: QueuedMutation) throws {
        guard !items.contains(where: { $0.idempotencyKey == mutation.idempotencyKey }) else { return }
        items.append(mutation)
        try persist()
    }

    func next(for organizationID: Int) -> QueuedMutation? {
        items.first { $0.organizationID == organizationID && ($0.state == .pending || $0.state == .retryScheduled) && ($0.nextAttemptAt ?? .distantPast) <= .now }
    }

    func markSucceeded(id: UUID) throws {
        items.removeAll { $0.id == id }
        try persist()
    }

    func markFailed(id: UUID, retryable: Bool) throws {
        guard let index = items.firstIndex(where: { $0.id == id }) else { return }
        items[index].attemptCount += 1
        if !retryable || items[index].attemptCount >= 8 {
            items[index].state = .needsAttention
        } else {
            let seconds = min(pow(2, Double(items[index].attemptCount)), 300) + Double.random(in: 0...3)
            items[index].nextAttemptAt = .now.addingTimeInterval(seconds)
            items[index].state = .retryScheduled
        }
        try persist()
    }

    func transition(id: UUID, to state: QueuedMutation.State, progress: Double? = nil, serverDocumentID: String? = nil) throws {
        guard let index = items.firstIndex(where: { $0.id == id }) else { throw SubmissionStateError.notFound }
        let current = items[index]
        guard current.state.canTransition(to: state) else { throw SubmissionStateError.invalidTransition }
        if let progress {
            guard (0...1).contains(progress), progress >= (current.progress ?? 0) else { throw SubmissionStateError.invalidProgress }
        }
        items[index].state = state
        items[index].progress = progress ?? current.progress
        items[index].serverDocumentID = serverDocumentID ?? current.serverDocumentID
        try persist()
    }

    func status(id: UUID) -> QueuedMutation? { items.first { $0.id == id } }

    func removeAll(for organizationID: Int) throws {
        items.removeAll { $0.organizationID == organizationID }
        try persist()
    }

    private func persist() throws {
        try store.save(items)
    }
}

private extension QueuedMutation.State {
    func canTransition(to next: Self) -> Bool {
        if self == next { return true }
        return switch (self, next) {
        case (.pending, .running), (.pending, .retryScheduled),
             (.retryScheduled, .running), (.running, .uploading),
             (.uploading, .uploading), (.uploading, .processing),
             (.processing, .needsVerification), (.processing, .completed),
             (.running, .needsAttention), (.uploading, .needsAttention),
             (.processing, .needsAttention), (.needsAttention, .retryScheduled): true
        default: false
        }
    }
}

enum SubmissionStateError: Error, Sendable { case notFound, invalidTransition, invalidProgress }

protocol MutationQueueStore: Sendable {
    func load() throws -> [QueuedMutation]
    func save(_ mutations: [QueuedMutation]) throws
}

struct KeychainMutationQueueStore: MutationQueueStore {
    private let service = "com.vergipro.mobile.offline-queue"
    private let account = "queue-v1"

    func load() throws -> [QueuedMutation] {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecUseDataProtectionKeychain as String: true,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        if status == errSecItemNotFound { return [] }
        guard status == errSecSuccess, let data = result as? Data else { throw KeychainError(status: status) }
        return try JSONDecoder().decode([QueuedMutation].self, from: data)
    }

    func save(_ mutations: [QueuedMutation]) throws {
        let data = try JSONEncoder().encode(mutations)
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecUseDataProtectionKeychain as String: true
        ]
        SecItemDelete(query as CFDictionary)
        guard !mutations.isEmpty else { return }
        var insert = query
        insert[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        insert[kSecValueData as String] = data
        let status = SecItemAdd(insert as CFDictionary, nil)
        guard status == errSecSuccess else { throw KeychainError(status: status) }
    }
}
