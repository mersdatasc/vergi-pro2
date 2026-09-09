import Foundation

enum RepositoryEnvironment: Sendable {
    case production
    case preview

    static func validated(_ requested: RepositoryEnvironment) -> RepositoryEnvironment {
        #if DEBUG
        requested
        #else
        .production
        #endif
    }
}

protocol TenantScopedRepository: Sendable {
    associatedtype Value: Sendable

    func load(for organization: OrganizationContext) async throws -> Value
}

struct RepositoryContainer: Sendable {
    let environment: RepositoryEnvironment

    init(environment: RepositoryEnvironment) {
        self.environment = .validated(environment)
    }
}
