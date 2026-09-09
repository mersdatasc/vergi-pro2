package com.vergipro.mobile.feature.capture

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.vergipro.mobile.core.security.VaultDocument
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.KeyStore
import java.time.Instant
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface CaptureDraftStore {
    fun load(organizationId: Int): CaptureDraftManifest
    fun save(manifest: CaptureDraftManifest)
}

data class CaptureDraftManifest(
    val organizationId: Int,
    val documents: List<VaultDocument>,
    val submissionId: UUID? = null,
)

class EncryptedCaptureDraftStore(context: Context) : CaptureDraftStore {
    private val preferences = context.getSharedPreferences("secure_capture_drafts", Context.MODE_PRIVATE)
    private val alias = "vergipro_capture_manifest_key_v1"

    override fun load(organizationId: Int): CaptureDraftManifest {
        val ciphertext = preferences.getString("$organizationId.ciphertext", null) ?: return CaptureDraftManifest(organizationId, emptyList())
        val iv = preferences.getString("$organizationId.iv", null) ?: return CaptureDraftManifest(organizationId, emptyList())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        }
        return decode(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), organizationId)
    }

    override fun save(manifest: CaptureDraftManifest) {
        if (manifest.documents.isEmpty()) {
            check(preferences.edit().remove("${manifest.organizationId}.ciphertext").remove("${manifest.organizationId}.iv").commit())
            return
        }
        require(manifest.documents.all { it.organizationId == manifest.organizationId })
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(encode(manifest))
        check(preferences.edit()
            .putString("${manifest.organizationId}.ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString("${manifest.organizationId}.iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .commit()) { "Capture manifest could not be persisted" }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
            generateKey()
        }
    }

    private fun encode(manifest: CaptureDraftManifest) = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(2)
            output.writeBoolean(manifest.submissionId != null)
            manifest.submissionId?.let { output.writeUTF(it.toString()) }
            output.writeInt(manifest.documents.size)
            manifest.documents.forEach { document ->
                output.writeUTF(document.id.toString())
                output.writeInt(document.organizationId)
                output.writeUTF(document.originalFileName)
                output.writeUTF(document.mediaType)
                output.writeInt(document.byteCount)
                output.writeUTF(document.sha256)
                output.writeLong(document.createdAt.toEpochMilli())
            }
        }
        bytes.toByteArray()
    }

    private fun decode(data: ByteArray, expectedOrganizationId: Int) = DataInputStream(ByteArrayInputStream(data)).use { input ->
        val version = input.readInt()
        check(version in 1..2) { "Unsupported capture manifest version" }
        val submissionId = if (version >= 2 && input.readBoolean()) UUID.fromString(input.readUTF()) else null
        val documents = List(input.readInt().coerceIn(0, 100)) {
            val document = VaultDocument(
                id = UUID.fromString(input.readUTF()),
                organizationId = input.readInt(),
                originalFileName = input.readUTF(),
                mediaType = input.readUTF(),
                byteCount = input.readInt(),
                sha256 = input.readUTF(),
                createdAt = Instant.ofEpochMilli(input.readLong()),
            )
            check(document.organizationId == expectedOrganizationId) { "Cross-tenant capture manifest" }
            document
        }
        CaptureDraftManifest(expectedOrganizationId, documents, submissionId)
    }
}
