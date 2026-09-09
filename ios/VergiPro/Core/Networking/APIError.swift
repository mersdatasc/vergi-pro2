import Foundation

enum APIError: Error, Equatable, Sendable {
    case invalidRequest
    case networkUnavailable
    case authenticationRequired
    case forbidden
    case validation(requestID: String?)
    case conflict
    case rateLimited(retryAfter: TimeInterval?)
    case server(requestID: String?)
    case maintenance
    case decoding
}

struct APIErrorEnvelope: Decodable {
    let code: String
    let messageKey: String?
    let requestID: String?
    let retryAfter: TimeInterval?

    enum CodingKeys: String, CodingKey {
        case code
        case messageKey = "message_key"
        case requestID = "request_id"
        case retryAfter = "retry_after"
    }
}
