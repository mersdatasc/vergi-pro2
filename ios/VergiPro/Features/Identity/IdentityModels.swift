import Foundation
import SwiftUI

enum IdentityStage: Equatable, Sendable {
    case welcome
    case signIn
    case forgotPassword
    case resetPassword
    case registerEmail
    case registerCode
    case registerProfile
    case organizationSelection
    case authenticated
}

struct OrganizationSummary: Identifiable, Equatable, Sendable {
    let id: String
    let name: String
    let role: String
    let planTier: String?
    let subscriptionStatus: String?
    let maxUsers: Int?
    let maxMonthlyInvoices: Int?
    let monthlyProcessedCount: Int?
}

enum IdentityError: LocalizedError, Equatable, Sendable {
    case invalidCredentials
    case codeInvalid
    case codeCooldown
    case emailAlreadyRegistered
    case registrationDisabled
    case weakPassword
    case validationFailed
    case networkUnavailable
    case accountLocked
    case serviceUnavailable

    var errorDescription: String? {
        let isTR = Locale.current.language.languageCode?.identifier == "tr"
        switch self {
        case .invalidCredentials:
            return isTR ? "E-posta veya parola hatalı." : "Invalid email or password."
        case .codeInvalid:
            return isTR ? "Doğrulama kodu geçersiz veya süresi dolmuş." : "Verification code is invalid or expired."
        case .codeCooldown:
            return isTR ? "Yeni kod istemeden önce lütfen biraz bekleyin." : "Please wait before requesting a new code."
        case .emailAlreadyRegistered:
            return isTR ? "Bu e-posta adresiyle zaten bir hesap mevcut." : "An account with this email already exists."
        case .registrationDisabled:
            return isTR ? "Kayıt sistemi şu anda bakımdadır." : "Registration is currently disabled."
        case .weakPassword:
            return isTR ? "Parola en az 6 karakter olmalıdır." : "Password must be at least 6 characters."
        case .validationFailed:
            return isTR ? "Lütfen tüm alanları kurallara uygun doldurun." : "Please fill in all fields correctly."
        case .networkUnavailable:
            return isTR ? "İnternet bağlantısı kurulamadı." : "Internet connection unavailable."
        case .accountLocked:
            return isTR ? "Bu hesap geçici olarak kilitlenmiştir." : "This account is temporarily locked."
        case .serviceUnavailable:
            return isTR ? "Sunucuya bağlanılamadı. Lütfen tekrar deneyin." : "Service unavailable. Please try again."
        }
    }
}

private struct RequestCodePayload: Encodable {
    let email: String
    let locale: String
}

private struct RegisterPayload: Encodable {
    let email: String
    let full_name: String
    let company_name: String
    let password: String
}

private struct LoginPayload: Encodable {
    let email: String
    let password: String
}

private struct RefreshPayload: Encodable {
    let refresh_token: String
}

private struct ProfileResponse: Decodable {
    struct OrgInfo: Decodable {
        let id: Int
        let name: String
        let role: String?
        let planTier: String?
        let subscriptionStatus: String?
        let maxUsers: Int?
        let maxMonthlyInvoices: Int?
        let monthlyProcessedCount: Int?

        var summary: OrganizationSummary {
            .init(
                id: String(id),
                name: name,
                role: role ?? "OWNER",
                planTier: planTier,
                subscriptionStatus: subscriptionStatus,
                maxUsers: maxUsers,
                maxMonthlyInvoices: maxMonthlyInvoices,
                monthlyProcessedCount: monthlyProcessedCount
            )
        }
    }
    struct UserInfo: Decodable {
        let id: Int
        let email: String
        let fullName: String?
    }
    let user: UserInfo?
    let organizations: [OrgInfo]?
}

private struct TokenPair: Decodable {
    let accessToken: String?
    let refreshToken: String?
    let access_token: String?
    let refresh_token: String?
    let organizations: [ProfileResponse.OrgInfo]?

    var validAccess: String { accessToken ?? access_token ?? "" }
    var validRefresh: String { refreshToken ?? refresh_token ?? "" }
}

private struct IdentityErrorPayload: Decodable {
    let detail: String?
}

actor IdentityRepository {
    private let configuration: AppConfiguration
    private let sessionController: SessionController
    private let session: URLSession

    init(configuration: AppConfiguration, sessionController: SessionController, session: URLSession = .shared) {
        self.configuration = configuration
        self.sessionController = sessionController
        self.session = session
    }

    func requestPasswordReset(email: String) async throws {
        struct Payload: Encodable { let email: String }
        struct Response: Decodable { let success: Bool?; let message: String? }
        let _: Response = try await request(
            path: "auth/forgot-password",
            body: Payload(email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())
        )
    }

    func resetPassword(email: String, code: String, newPassword: String) async throws {
        struct Payload: Encodable { let email: String; let code: String; let new_password: String }
        struct Response: Decodable { let success: Bool?; let message: String? }
        let _: Response = try await request(
            path: "auth/reset-password",
            body: Payload(
                email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
                code: code.trimmingCharacters(in: .whitespacesAndNewlines),
                new_password: newPassword
            )
        )
    }

    func requestRegistrationCode(email: String, locale: String) async throws {
        let payload = RequestCodePayload(email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(), locale: locale)
        struct CodeResp: Decodable { let success: Bool? }
        let _: CodeResp = try await request(path: "auth/send-verification-code", body: payload)
    }

    func verifyRegistrationCode(email: String, code: String) async throws {
        struct VerifyPayload: Encodable { let email: String; let code: String }
        struct VerifyResponse: Decodable { let success: Bool?; let verified: Bool? }
        let payload = VerifyPayload(
            email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            code: code.trimmingCharacters(in: .whitespacesAndNewlines)
        )
        let _: VerifyResponse = try await request(path: "auth/verify-code", body: payload)
    }

    func register(
        email: String,
        code: String,
        displayName: String,
        password: String,
        organizationName: String,
        locale: String
    ) async throws -> [OrganizationSummary] {
        let payload = RegisterPayload(
            email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            full_name: displayName.trimmingCharacters(in: .whitespacesAndNewlines),
            company_name: organizationName.trimmingCharacters(in: .whitespacesAndNewlines),
            password: password
        )
        let pair: TokenPair = try await request(path: "auth/register", body: payload)
        guard !pair.validAccess.isEmpty, !pair.validRefresh.isEmpty else {
            throw IdentityError.serviceUnavailable
        }
        try await sessionController.establish(accessToken: pair.validAccess, refreshToken: pair.validRefresh)
        if let directOrgs = pair.organizations, !directOrgs.isEmpty {
            return directOrgs.map(\.summary)
        }
        let profile: ProfileResponse = try await authorizedRequest(path: "auth/me", accessToken: pair.validAccess)
        let organizations = profile.organizations?.map(\.summary) ?? []
        guard !organizations.isEmpty else { throw IdentityError.serviceUnavailable }
        return organizations
    }

    func signIn(email: String, password: String) async throws -> [OrganizationSummary] {
        let payload = LoginPayload(
            email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            password: password
        )
        let pair: TokenPair = try await request(path: "auth/login", body: payload)
        guard !pair.validAccess.isEmpty, !pair.validRefresh.isEmpty else {
            throw IdentityError.serviceUnavailable
        }
        try await sessionController.establish(accessToken: pair.validAccess, refreshToken: pair.validRefresh)
        
        if let directOrgs = pair.organizations, !directOrgs.isEmpty {
            return directOrgs.map(\.summary)
        }
        
        let profile: ProfileResponse = try await authorizedRequest(path: "auth/me", accessToken: pair.validAccess)
        let organizations = profile.organizations?.map(\.summary) ?? []
        guard !organizations.isEmpty else { throw IdentityError.serviceUnavailable }
        return organizations
    }

    func restore() async throws -> [OrganizationSummary]? {
        guard let data = try await sessionController.beginRestore(), let refreshToken = String(data: data, encoding: .utf8) else { return nil }
        let pair: TokenPair = try await request(path: "auth/refresh", body: RefreshPayload(refresh_token: refreshToken))
        guard !pair.validAccess.isEmpty, !pair.validRefresh.isEmpty else {
            throw IdentityError.serviceUnavailable
        }
        try await sessionController.completeRestore(accessToken: pair.validAccess, rotatedRefreshToken: pair.validRefresh)
        if let directOrgs = pair.organizations, !directOrgs.isEmpty {
            return directOrgs.map(\.summary)
        }
        let profile: ProfileResponse = try await authorizedRequest(path: "auth/me", accessToken: pair.validAccess)
        let organizations = profile.organizations?.map(\.summary) ?? []
        guard !organizations.isEmpty else { throw IdentityError.serviceUnavailable }
        return organizations
    }

    private func request<Response: Decodable, Body: Encodable>(path: String, body: Body) async throws -> Response {
        let url = configuration.apiBaseURL.appending(path: path)
        var req = URLRequest(url: url, timeoutInterval: 25)
        req.httpMethod = "POST"
        req.httpBody = try JSONEncoder().encode(body)
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        req.setValue(UUID().uuidString, forHTTPHeaderField: "X-Request-ID")
        return try await execute(req)
    }

    private func authorizedRequest<Response: Decodable>(path: String, accessToken: String) async throws -> Response {
        let url = configuration.apiBaseURL.appending(path: path)
        var req = URLRequest(url: url, timeoutInterval: 25)
        req.httpMethod = "GET"
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        req.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        req.setValue(UUID().uuidString, forHTTPHeaderField: "X-Request-ID")
        return try await execute(req)
    }

    private func execute<Response: Decodable>(_ request: URLRequest) async throws -> Response {
        do {
            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse else { throw IdentityError.serviceUnavailable }

            if (200...299).contains(http.statusCode) {
                return try JSONDecoder().decode(Response.self, from: data)
            }

            if http.statusCode == 401 { throw IdentityError.invalidCredentials }
            if http.statusCode == 403 { throw IdentityError.registrationDisabled }
            if http.statusCode == 409 { throw IdentityError.emailAlreadyRegistered }
            if http.statusCode == 429 { throw IdentityError.codeCooldown }

            let err = try? JSONDecoder().decode(IdentityErrorPayload.self, from: data)
            if err?.detail?.contains("kod") == true {
                throw IdentityError.codeInvalid
            }
            throw IdentityError.validationFailed
        } catch let err as IdentityError {
            throw err
        } catch {
            throw IdentityError.networkUnavailable
        }
    }
}

@MainActor
final class IdentityViewModel: ObservableObject {
    @Published var stage: IdentityStage = .welcome
    @Published var email = ""
    @Published var password = ""
    @Published var verificationCode = ""
    @Published var displayName = ""
    @Published var organizationName = ""
    @Published var acceptedTerms = false
    @Published var isSubmitting = false
    @Published var errorMessage: String?
    @Published var selectedOrganization: OrganizationSummary?
    @Published private(set) var organizations: [OrganizationSummary] = []
    @Published var resendCooldownRemaining = 0
    @Published private(set) var isRestoringSession = true
    @Published private(set) var isPortalZooming = false

    private var cooldownTask: Task<Void, Never>?
    private var didAttemptRestore = false
    private let repository: IdentityRepository?
    private var registrationLocale = "tr-TR"

    init() {
        if let configuration = try? AppConfiguration.load() {
            repository = IdentityRepository(
                configuration: configuration,
                sessionController: SessionController.shared
            )
        } else {
            repository = nil
        }
    }

    var isEmailValid: Bool { email.contains("@") && email.contains(".") && email.count >= 5 }
    var canSubmitSignIn: Bool { isEmailValid && password.count >= 6 && !isSubmitting }
    var canRequestCode: Bool { isEmailValid && !isSubmitting }
    var canSubmitCode: Bool { verificationCode.count == 6 && !isSubmitting }

    func applyLanguage(_ language: AppLanguage) {
        switch language {
        case .turkish:
            registrationLocale = "tr-TR"
        case .english:
            registrationLocale = "en-US"
        case .system:
            registrationLocale = Locale.autoupdatingCurrent.language.languageCode?.identifier == "tr" ? "tr-TR" : "en-US"
        }
    }

    func showWelcome() { errorMessage = nil; stage = .welcome }
    func showSignIn() { errorMessage = nil; stage = .signIn }
    func showForgotPassword() { password = ""; verificationCode = ""; errorMessage = nil; stage = .forgotPassword }
    func showRegister() { errorMessage = nil; stage = .registerEmail }

    func restoreSessionIfAvailable() async {
        guard !didAttemptRestore else { return }
        didAttemptRestore = true
        guard let repository else {
            isRestoringSession = false
            return
        }
        
        let start = Date()
        var targetStage: IdentityStage = .welcome
        var targetOrgs: [OrganizationSummary] = []
        var targetSelectedOrg: OrganizationSummary? = nil
        
        do {
            let restored = try await repository.restore()
            if let restored, !restored.isEmpty {
                targetOrgs = restored
                if restored.count == 1, let organization = restored.first {
                    targetSelectedOrg = organization
                    targetStage = .authenticated
                } else {
                    targetStage = .organizationSelection
                }
            } else {
                targetStage = .welcome
            }
        } catch {
            errorMessage = nil
            targetStage = .welcome
        }

        let elapsed = Date().timeIntervalSince(start)
        if elapsed < 0.85 {
            try? await Task.sleep(nanoseconds: UInt64((0.85 - elapsed) * 1_000_000_000))
        }

        // Set target destination state before triggering the portal zoom
        self.organizations = targetOrgs
        if let targetSelectedOrg {
            self.select(targetSelectedOrg)
        } else {
            self.stage = targetStage
        }

        // Trigger cinematic portal zoom
        withAnimation(.easeInOut(duration: 0.65)) {
            self.isPortalZooming = true
        }

        // Allow portal zoom to complete seamlessly before releasing overlay
        try? await Task.sleep(nanoseconds: 650_000_000)
        self.isRestoringSession = false
    }

    func signIn() async {
        guard canSubmitSignIn, let repository else { return }
        await perform {
            self.organizations = try await repository.signIn(email: self.email, password: self.password)
            self.advanceAfterAuthentication()
        }
    }

    func requestPasswordReset() async {
        guard canRequestCode, let repository else { return }
        await perform {
            try await repository.requestPasswordReset(email: self.email)
            self.stage = .resetPassword
        }
    }

    func completePasswordReset() async {
        guard verificationCode.count == 6, password.count >= 6, let repository else { return }
        await perform {
            try await repository.resetPassword(email: self.email, code: self.verificationCode, newPassword: self.password)
            self.verificationCode = ""
            self.password = ""
            self.stage = .signIn
            self.errorMessage = String(localized: "identity.reset.success")
        }
    }

    func requestCode() async {
        guard canRequestCode, let repository else { return }
        await perform {
            try await repository.requestRegistrationCode(email: self.email, locale: self.registrationLocale)
            self.startCooldownTimer()
            self.stage = .registerCode
        }
    }

    func resendCode() async {
        guard resendCooldownRemaining == 0 else { return }
        await requestCode()
    }

    func verifyCodeAndProceed() async {
        guard canSubmitCode, let repository else { return }
        await perform {
            try await repository.verifyRegistrationCode(email: self.email, code: self.verificationCode)
            self.stage = .registerProfile
        }
    }

    func validateProfileInput() -> String? {
        if displayName.trimmingCharacters(in: .whitespacesAndNewlines).count < 2 { return "Lütfen adınızı ve soyadınızı girin." }
        if organizationName.trimmingCharacters(in: .whitespacesAndNewlines).count < 2 { return "Lütfen şirket veya işletme adınızı girin." }
        if password.count < 12 { return "Parola en az 12 karakter olmalıdır." }
        if !acceptedTerms { return "Kullanım Şartları ve Gizlilik Politikasını onaylamalısınız." }
        return nil
    }

    func completeRegistration() async {
        if let validationError = validateProfileInput() { errorMessage = validationError; return }
        guard let repository else { return }
        await perform {
            self.organizations = try await repository.register(
                email: self.email,
                code: self.verificationCode,
                displayName: self.displayName,
                password: self.password,
                organizationName: self.organizationName,
                locale: self.registrationLocale
            )
            self.advanceAfterAuthentication()
        }
    }

    func select(_ organization: OrganizationSummary) {
        selectedOrganization = organization
        stage = .authenticated
    }

    func signOut() async {
        try? await SessionController.shared.signOut()
        cooldownTask?.cancel()
        organizations = []
        selectedOrganization = nil
        password = ""
        verificationCode = ""
        errorMessage = nil
        stage = .welcome
    }

    private func advanceAfterAuthentication() {
        guard !organizations.isEmpty else {
            errorMessage = IdentityError.serviceUnavailable.localizedDescription
            return
        }
        if organizations.count == 1, let organization = organizations.first { select(organization) }
        else { stage = .organizationSelection }
    }

    private func perform(_ operation: @escaping () async throws -> Void) async {
        isSubmitting = true
        errorMessage = nil
        defer { isSubmitting = false }
        do { try await operation() }
        catch let error as IdentityError { errorMessage = error.localizedDescription }
        catch { errorMessage = IdentityError.serviceUnavailable.localizedDescription }
    }

    private func startCooldownTimer() {
        resendCooldownRemaining = 60
        cooldownTask?.cancel()
        cooldownTask = Task { @MainActor [weak self] in
            while let self, self.resendCooldownRemaining > 0 {
                try? await Task.sleep(for: .seconds(1))
                if Task.isCancelled { break }
                self.resendCooldownRemaining -= 1
            }
        }
    }
}
