package com.vergipro.mobile.core.sync

import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

data class RemoteSubmissionStatus(
    val state: MutationState,
    val progress: Float? = null,
    val serverDocumentId: String? = null,
    val retryAfterSeconds: Long? = null,
)

fun interface SubmissionStatusTransport {
    suspend fun fetch(submissionId: UUID, organizationId: Int): RemoteSubmissionStatus
}

class SubmissionStatusPoller(
    private val queue: OfflineMutationQueue,
    private val transport: SubmissionStatusTransport,
) {
    suspend fun poll(submissionId: UUID, organizationId: Int) {
        var failures = 0
        while (true) {
            try {
                val remote = transport.fetch(submissionId, organizationId)
                queue.transition(submissionId, remote.state, remote.progress, remote.serverDocumentId)
                failures = 0
                if (remote.state.isTerminal()) return
                delay((remote.retryAfterSeconds ?: 3).coerceIn(1, 30) * 1000)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (state: SubmissionStateFailure) {
                throw state
            } catch (_: Exception) {
                failures++
                val seconds = min(2.0.pow(failures.toDouble()), 30.0) + Random.nextDouble(0.0, 1.0)
                delay((seconds * 1000).toLong())
            }
        }
    }
}

fun MutationState.isTerminal() = this == MutationState.NeedsVerification || this == MutationState.NeedsAttention || this == MutationState.Completed
