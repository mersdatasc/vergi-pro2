import Foundation
import Security

protocol CaptureDraftStoring: Sendable {
    func load(organizationID: Int) throws -> CaptureDraftManifest
    func save(_ manifest: CaptureDraftManifest) throws
}

struct CaptureDraftManifest: Codable, Sendable {
    var version = 2
    let organizationID: Int
    var documents: [VaultDocument]
    var submissionID: UUID?
}

struct KeychainCaptureDraftStore: CaptureDraftStoring {
    private let service = "com.vergipro.mobile.capture-drafts"

    func load(organizationID: Int) throws -> CaptureDraftManifest {
        let query = baseQuery(organizationID: organizationID).merging([
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]) { _, new in new }
        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        if status == errSecItemNotFound { return .init(organizationID: organizationID, documents: [], submissionID: nil) }
        guard status == errSecSuccess, let data = result as? Data else { throw KeychainError(status: status) }
        if let manifest = try? JSONDecoder().decode(CaptureDraftManifest.self, from: data) {
            guard manifest.organizationID == organizationID else { throw APIError.forbidden }
            return manifest
        }
        let legacy = try JSONDecoder().decode([VaultDocument].self, from: data)
        return .init(organizationID: organizationID, documents: legacy, submissionID: nil)
    }

    func save(_ manifest: CaptureDraftManifest) throws {
        let query = baseQuery(organizationID: manifest.organizationID)
        SecItemDelete(query as CFDictionary)
        guard !manifest.documents.isEmpty else { return }
        var insert = query
        insert[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        insert[kSecValueData as String] = try JSONEncoder().encode(manifest)
        let status = SecItemAdd(insert as CFDictionary, nil)
        guard status == errSecSuccess else { throw KeychainError(status: status) }
    }

    private func baseQuery(organizationID: Int) -> [String: Any] {[
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: service,
        kSecAttrAccount as String: "organization-\(organizationID)-v1",
        kSecUseDataProtectionKeychain as String: true
    ]}
}
