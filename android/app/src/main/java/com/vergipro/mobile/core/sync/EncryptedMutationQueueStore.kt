package com.vergipro.mobile.core.sync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
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

class EncryptedMutationQueueStore(context: Context) : MutationQueueStore {
    private val preferences = context.getSharedPreferences("secure_offline_queue", Context.MODE_PRIVATE)
    private val alias = "vergipro_offline_queue_key_v1"

    override fun load(): List<QueuedMutation> {
        val ciphertext = preferences.getString("ciphertext", null) ?: return emptyList()
        val iv = preferences.getString("iv", null) ?: return emptyList()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        return decode(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)))
    }

    override fun save(mutations: List<QueuedMutation>) {
        if (mutations.isEmpty()) {
            preferences.edit().clear().commit()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(encode(mutations))
        check(preferences.edit()
            .putString("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .commit()) { "Offline queue could not be persisted" }
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

    private fun encode(mutations: List<QueuedMutation>): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(2)
            output.writeInt(mutations.size)
            mutations.forEach { item ->
                output.writeUTF(item.id.toString())
                output.writeInt(item.organizationId)
                output.writeUTF(item.aggregateId)
                output.writeUTF(item.operation.name)
                output.writeInt(item.payload.size)
                output.write(item.payload)
                output.writeUTF(item.idempotencyKey.toString())
                output.writeInt(item.attemptCount)
                output.writeLong(item.nextAttemptAt?.toEpochMilli() ?: -1L)
                output.writeUTF(item.state.name)
                output.writeBoolean(item.progress != null)
                item.progress?.let(output::writeFloat)
                output.writeBoolean(item.serverDocumentId != null)
                item.serverDocumentId?.let(output::writeUTF)
            }
        }
        bytes.toByteArray()
    }

    private fun decode(data: ByteArray): List<QueuedMutation> = DataInputStream(ByteArrayInputStream(data)).use { input ->
        val version = input.readInt()
        check(version in 1..2) { "Unsupported offline queue version" }
        List(input.readInt().coerceIn(0, 10_000)) {
            val id = UUID.fromString(input.readUTF())
            val organizationId = input.readInt()
            val aggregateId = input.readUTF()
            val operation = OfflineOperation.valueOf(input.readUTF())
            val payloadSize = input.readInt().also { require(it in 0..1_048_576) }
            val payload = ByteArray(payloadSize).also(input::readFully)
            val idempotencyKey = UUID.fromString(input.readUTF())
            val attempts = input.readInt()
            val nextAttempt = input.readLong().takeIf { it >= 0 }?.let(Instant::ofEpochMilli)
            val state = MutationState.valueOf(input.readUTF())
            val progress = if (version >= 2 && input.readBoolean()) input.readFloat() else null
            val serverDocumentId = if (version >= 2 && input.readBoolean()) input.readUTF() else null
            QueuedMutation(id, organizationId, aggregateId, operation, payload, idempotencyKey, attempts, nextAttempt, state, progress, serverDocumentId)
        }
    }
}
