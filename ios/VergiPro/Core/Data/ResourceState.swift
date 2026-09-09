import Foundation

enum ResourceState<Value: Sendable>: Sendable {
    case idle
    case loading(previous: Value?)
    case content(Value, isStale: Bool)
    case empty
    case failure(APIError, previous: Value?)
}

struct Page<Value: Sendable>: Sendable {
    let items: [Value]
    let nextCursor: String?
}
