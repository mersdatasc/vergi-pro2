import Foundation

enum AppEnvironment: String, Sendable {
    case development
    case staging
    case production
}

enum ConfigurationError: Error, Equatable, Sendable {
    case missingBaseURL
    case insecureBaseURL
    case invalidBaseURL
}

struct AppConfiguration: Sendable {
    let environment: AppEnvironment
    let apiBaseURL: URL

    static func load(bundle: Bundle = .main) throws -> AppConfiguration {
        let environment = AppEnvironment(rawValue: bundle.object(forInfoDictionaryKey: "VP_ENVIRONMENT") as? String ?? "") ?? .production
        var rawURL = (bundle.object(forInfoDictionaryKey: "VP_API_BASE_URL") as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .nilIfEmpty ?? "https://api.vergi.pro"
        
        // If an old local or invalid IP is cached/present, force fallback to production domain
        if rawURL.contains("192.168.") || rawURL.contains("10.0.2.2") || rawURL.contains("127.0.0.1") || rawURL.contains("localhost") {
            rawURL = "https://api.vergi.pro"
        }
        guard var components = URLComponents(string: rawURL) else {
            throw ConfigurationError.invalidBaseURL
        }
        if environment == .production {
            guard components.scheme?.lowercased() == "https" else {
                throw ConfigurationError.insecureBaseURL
            }
        }
        guard components.host != nil, components.user == nil, components.password == nil,
              components.query == nil, components.fragment == nil else {
            throw ConfigurationError.invalidBaseURL
        }
        components.path = "/api/"
        guard let url = components.url else { throw ConfigurationError.invalidBaseURL }
        return AppConfiguration(environment: environment, apiBaseURL: url)
    }
}

private extension String {
    var nilIfEmpty: String? {
        isEmpty ? nil : self
    }
}
