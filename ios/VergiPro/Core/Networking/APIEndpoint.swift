import Foundation

enum HTTPMethod: String { case get = "GET", post = "POST", patch = "PATCH", delete = "DELETE" }

struct APIEndpoint<Response: Decodable> {
    let path: String
    let method: HTTPMethod
    var query: [URLQueryItem] = []
    var body: Data?
    var requiresTenant = true
    var isMutation = false
}

struct OrganizationContext: Equatable, Sendable {
    let organizationID: Int
    let membershipID: Int
}

