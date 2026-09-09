package com.vergipro.mobile.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class VaultDocument(
    val id: UUID,
    val organizationId: Int,
    val originalFileName: String,
    val mediaType: String,
    val byteCount: Int,
    val sha256: String,
    val createdAt: Instant,
)

class SecureDocumentVault(context: Context) {
    private val root = File(context.filesDir, "secure_documents").apply { mkdirs() }
    private val alias = "vergipro_document_vault_key_v1"

    fun store(plaintext: ByteArray, organizationId: Int, fileName: String, mediaType: String): VaultDocument {
        val id = UUID.randomUUID()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val folder = File(root, organizationId.toString()).apply { mkdirs() }
        val target = File(folder, "$id.vpd")
        val temporary = File(folder, "$id.tmp")
        temporary.outputStream().use { output ->
            output.write(cipher.iv.size)
            output.write(cipher.iv)
            output.write(cipher.doFinal(plaintext))
            output.flush()
            output.fd.sync()
        }
        check(temporary.renameTo(target)) { "Secure document could not be committed" }
        return VaultDocument(id, organizationId, fileName, mediaType, plaintext.size, plaintext.sha256(), Instant.now())
    }

    fun read(document: VaultDocument): ByteArray {
        val encrypted = File(File(root, document.organizationId.toString()), "${document.id}.vpd").readBytes()
        require(encrypted.isNotEmpty())
        val ivSize = encrypted[0].toInt() and 0xff
        require(ivSize in 12..16 && encrypted.size > ivSize + 1)
        val iv = encrypted.copyOfRange(1, ivSize + 1)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
        val plaintext = cipher.doFinal(encrypted.copyOfRange(ivSize + 1, encrypted.size))
        if (plaintext.sha256() != document.sha256) throw VaultFailure.IntegrityFailure
        return plaintext
    }

    fun delete(document: VaultDocument) {
        val file = File(File(root, document.organizationId.toString()), "${document.id}.vpd")
        check(!file.exists() || file.delete()) { "Secure document could not be deleted" }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build())
            generateKey()
        }
    }
}

sealed class VaultFailure : IllegalStateException() { data object IntegrityFailure : VaultFailure() }

internal fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this).joinToString("") { "%02x".format(it) }
