import SwiftUI

@main
struct VergiProApp: App {
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            IdentityFlowView()
                .environmentObject(appState)
                .preferredColorScheme(appState.preferredColorScheme)
                .environment(\.locale, appState.locale)
        }
    }
}

@MainActor
final class AppState: ObservableObject {
    @Published var selectedDestination: AppDestination = .today
    @Published var theme: AppTheme {
        didSet { UserDefaults.standard.set(theme.rawValue, forKey: Self.themeKey) }
    }
    @Published var currentOrganization: OrganizationSummary?
    @Published var availableOrganizations: [OrganizationSummary] = []
    @Published var language: AppLanguage {
        didSet { UserDefaults.standard.set(language.rawValue, forKey: Self.languageKey) }
    }

    private static let languageKey = "appLanguage"
    private static let themeKey = "appTheme"

    init() {
        let storedTheme = UserDefaults.standard.string(forKey: Self.themeKey)
        theme = AppTheme(rawValue: storedTheme ?? "") ?? .system
        let storedLanguage = UserDefaults.standard.string(forKey: Self.languageKey)
        language = AppLanguage(rawValue: storedLanguage ?? "") ?? .system
    }

    var locale: Locale {
        switch language {
        case .system: .autoupdatingCurrent
        case .turkish: Locale(identifier: "tr_TR")
        case .english: Locale(identifier: "en_US")
        }
    }

    var preferredColorScheme: ColorScheme? {
        switch theme {
        case .system: nil
        case .porcelain: .light
        case .obsidian: .dark
        case .contrast: nil
        }
    }
}

enum AppTheme: String, CaseIterable, Sendable {
    case system, porcelain, obsidian, contrast
}

enum AppLanguage: String, CaseIterable, Sendable {
    case system
    case turkish = "tr"
    case english = "en"
}
