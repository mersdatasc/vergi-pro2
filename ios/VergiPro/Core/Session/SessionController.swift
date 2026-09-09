import Foundation

actor SessionController {
    static let shared = SessionController(credentialStore: KeychainCredentialStore())
    enum State: Equatable { case signedOut, restoring, active, locked }

    private(set) var state: State = .signedOut
    private var accessToken: String?
    private let credentialStore: CredentialStore
    private let accountKey = "primary-device-session"

    init(credentialStore: CredentialStore) {
        self.credentialStore = credentialStore
    }

    func currentAccessToken() -> String? { accessToken }

    func establish(accessToken: String, refreshToken: String) throws {
        try credentialStore.saveRefreshToken(Data(refreshToken.utf8), account: accountKey)
        self.accessToken = accessToken
        state = .active
    }

    func beginRestore() throws -> Data? {
        state = .restoring
        return try credentialStore.readRefreshToken(account: accountKey)
    }

    func completeRestore(accessToken: String, rotatedRefreshToken: String) throws {
        try establish(accessToken: accessToken, refreshToken: rotatedRefreshToken)
    }

    func lock() {
        accessToken = nil
        state = .locked
    }

    func signOut() throws {
        accessToken = nil
        try credentialStore.deleteRefreshToken(account: accountKey)
        state = .signedOut
    }
}
