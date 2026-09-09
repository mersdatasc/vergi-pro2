package com.vergipro.mobile.feature.capture

import com.vergipro.mobile.core.sync.OfflineMutationQueue
import com.vergipro.mobile.core.sync.OfflineOperation
import com.vergipro.mobile.core.sync.QueuedMutation
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.time.Instant
import java.util.UUID

class CaptureSubmissionService(private val queue: OfflineMutationQueue) {
    fun prepare(pages: List<CapturedPage>, organizationId: Int, submissionId: UUID): UUID {
        require(pages.isNotEmpty() && pages.all { it.vaultDocument.organizationId == organizationId })
        val payload = ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(1)
                output.writeUTF(submissionId.toString())
                output.writeInt(organizationId)
                output.writeLong(Instant.now().toEpochMilli())
                output.writeInt(pages.size)
                pages.forEach { output.writeUTF(it.vaultDocument.id.toString()) }
            }
            bytes.toByteArray()
        }
        queue.enqueue(QueuedMutation(submissionId, organizationId, submissionId.toString(), OfflineOperation.UploadDocument, payload, submissionId))
        return submissionId
    }

    fun status(submissionId: UUID): QueuedMutation? = queue.status(submissionId)
}
