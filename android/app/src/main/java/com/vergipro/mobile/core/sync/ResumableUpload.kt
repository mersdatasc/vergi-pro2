package com.vergipro.mobile.core.sync

import com.vergipro.mobile.core.security.SecureDocumentVault
import com.vergipro.mobile.core.security.VaultDocument
import com.vergipro.mobile.core.security.sha256
import java.util.UUID

enum class UploadState { Local, Queued, CreatingSession, Uploading, Paused, Completing, Processing, NeedsAttention, Completed }

data class ResumableUpload(
    val id: UUID,
    val organizationId: Int,
    val document: VaultDocument,
    val idempotencyKey: UUID,
    val serverUploadId: String? = null,
    val confirmedOffset: Int = 0,
    val state: UploadState = UploadState.Local,
    val attemptCount: Int = 0,
)

data class UploadSession(val id: String, val confirmedOffset: Int, val chunkSize: Int)

interface UploadTransport {
    suspend fun create(upload: ResumableUpload): UploadSession
    suspend fun append(data: ByteArray, offset: Int, checksum: String, session: UploadSession, organizationId: Int): Int
    suspend fun complete(session: UploadSession, upload: ResumableUpload)
}

class ResumableUploadCoordinator(
    private val vault: SecureDocumentVault,
    private val transport: UploadTransport,
) {
    suspend fun run(original: ResumableUpload): ResumableUpload {
        var upload = original.copy(state = UploadState.CreatingSession)
        val bytes = vault.read(upload.document)
        val session = transport.create(upload)
        require(session.chunkSize > 0 && session.confirmedOffset in 0..bytes.size) { "Server returned an invalid upload session" }
        upload = upload.copy(serverUploadId = session.id, confirmedOffset = session.confirmedOffset, state = UploadState.Uploading)
        while (upload.confirmedOffset < bytes.size) {
            val end = minOf(upload.confirmedOffset + session.chunkSize, bytes.size)
            val chunk = bytes.copyOfRange(upload.confirmedOffset, end)
            val nextOffset = transport.append(chunk, upload.confirmedOffset, chunk.sha256(), session, upload.organizationId)
            require(nextOffset in (upload.confirmedOffset + 1)..bytes.size) { "Server returned an invalid upload offset" }
            upload = upload.copy(confirmedOffset = nextOffset)
        }
        upload = upload.copy(state = UploadState.Completing)
        transport.complete(session, upload)
        return upload.copy(state = UploadState.Processing)
    }
}
