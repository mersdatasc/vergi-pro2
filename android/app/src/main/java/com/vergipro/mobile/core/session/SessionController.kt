package com.vergipro.mobile.core.session

import com.vergipro.mobile.core.security.CredentialStore

enum class SessionState { SignedOut, Restoring, Active, Locked }

class SessionController(private val credentialStore: CredentialStore) {
    @Volatile private var accessToken: String? = null
    @Volatile var state: SessionState = SessionState.SignedOut
        private set

    fun currentAccessToken(): String? = accessToken

    @Synchronized
    fun establish(newAccessToken: String, refreshToken: String) {
        credentialStore.saveRefreshToken(refreshToken.encodeToByteArray())
        accessToken = newAccessToken
        state = SessionState.Active
    }

    @Synchronized
    fun beginRestore(): ByteArray? {
        state = SessionState.Restoring
        return credentialStore.readRefreshToken()
    }

    @Synchronized
    fun completeRestore(newAccessToken: String, rotatedRefreshToken: String) {
        establish(newAccessToken, rotatedRefreshToken)
    }

    @Synchronized
    fun lock() {
        accessToken = null
        state = SessionState.Locked
    }

    @Synchronized
    fun signOut() {
        accessToken = null
        credentialStore.deleteRefreshToken()
        state = SessionState.SignedOut
    }
}
