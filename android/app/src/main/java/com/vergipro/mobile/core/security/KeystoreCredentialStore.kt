package com.vergipro.mobile.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface CredentialStore {
    fun saveRefreshToken(token: ByteArray)
    fun readRefreshToken(): ByteArray?
    fun deleteRefreshToken()
}

class KeystoreCredentialStore(context: Context) : CredentialStore {
    private val preferences = context.getSharedPreferences("secure_session_ciphertext", Context.MODE_PRIVATE)
    private val alias = "vergipro_refresh_token_key_v1"

    override fun saveRefreshToken(token: ByteArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        preferences.edit()
            .putString("ciphertext", Base64.encodeToString(cipher.doFinal(token), Base64.NO_WRAP))
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    override fun readRefreshToken(): ByteArray? {
        val ciphertext = preferences.getString("ciphertext", null) ?: return null
        val iv = preferences.getString("iv", null) ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        return cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP))
    }

    override fun deleteRefreshToken() {
        preferences.edit().clear().apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}

