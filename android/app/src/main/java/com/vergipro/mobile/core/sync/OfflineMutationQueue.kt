package com.vergipro.mobile.core.sync

import java.time.Instant
import java.util.UUID
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

enum class OfflineOperation { UploadDocument, AddComment, UpdateVerification, CompleteTask }
enum class MutationState { Pending, Running, RetryScheduled, Uploading, Processing, NeedsVerification, NeedsAttention, Completed }

data class QueuedMutation(
    val id: UUID,
    val organizationId: Int,
    val aggregateId: String,
    val operation: OfflineOperation,
    val payload: ByteArray,
    val idempotencyKey: UUID,
    val attemptCount: Int = 0,
    val nextAttemptAt: Instant? = null,
    val state: MutationState = MutationState.Pending,
    val progress: Float? = null,
    val serverDocumentId: String? = null,
)

class OfflineMutationQueue(
    private val store: MutationQueueStore,
) {
    private val items = store.load().toMutableList()

    @Synchronized
    fun enqueue(mutation: QueuedMutation) {
        if (items.none { it.idempotencyKey == mutation.idempotencyKey }) {
            items += mutation
            persist()
        }
    }

    @Synchronized
    fun next(organizationId: Int, now: Instant = Instant.now()): QueuedMutation? =
        items.firstOrNull { it.organizationId == organizationId && it.state in setOf(MutationState.Pending, MutationState.RetryScheduled) && (it.nextAttemptAt == null || !it.nextAttemptAt.isAfter(now)) }

    @Synchronized
    fun markSucceeded(id: UUID) {
        items.removeAll { it.id == id }
        persist()
    }

    @Synchronized
    fun markFailed(id: UUID, retryable: Boolean) {
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) return
        val current = items[index]
        val attempts = current.attemptCount + 1
        items[index] = if (!retryable || attempts >= 8) {
            current.copy(attemptCount = attempts, state = MutationState.NeedsAttention)
        } else {
            val seconds = min(2.0.pow(attempts.toDouble()), 300.0) + Random.nextDouble(0.0, 3.0)
            current.copy(attemptCount = attempts, state = MutationState.RetryScheduled, nextAttemptAt = Instant.now().plusMillis((seconds * 1000).toLong()))
        }
        persist()
    }

    @Synchronized
    fun transition(id: UUID, state: MutationState, progress: Float? = null, serverDocumentId: String? = null) {
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) throw SubmissionStateFailure.NotFound
        val current = items[index]
        if (!current.state.canTransitionTo(state)) throw SubmissionStateFailure.InvalidTransition
        if (progress != null && (progress !in 0f..1f || progress < (current.progress ?: 0f))) throw SubmissionStateFailure.InvalidProgress
        items[index] = current.copy(state = state, progress = progress ?: current.progress, serverDocumentId = serverDocumentId ?: current.serverDocumentId)
        persist()
    }

    @Synchronized
    fun status(id: UUID): QueuedMutation? = items.firstOrNull { it.id == id }

    @Synchronized
    fun removeAll(organizationId: Int) {
        items.removeAll { it.organizationId == organizationId }
        persist()
    }

    private fun persist() = store.save(items)
}

private fun MutationState.canTransitionTo(next: MutationState): Boolean = this == next || when (this to next) {
    MutationState.Pending to MutationState.Running,
    MutationState.Pending to MutationState.RetryScheduled,
    MutationState.RetryScheduled to MutationState.Running,
    MutationState.Running to MutationState.Uploading,
    MutationState.Uploading to MutationState.Processing,
    MutationState.Processing to MutationState.NeedsVerification,
    MutationState.Processing to MutationState.Completed,
    MutationState.Running to MutationState.NeedsAttention,
    MutationState.Uploading to MutationState.NeedsAttention,
    MutationState.Processing to MutationState.NeedsAttention,
    MutationState.NeedsAttention to MutationState.RetryScheduled -> true
    else -> false
}

sealed class SubmissionStateFailure : IllegalStateException() {
    data object NotFound : SubmissionStateFailure()
    data object InvalidTransition : SubmissionStateFailure()
    data object InvalidProgress : SubmissionStateFailure()
}

interface MutationQueueStore {
    fun load(): List<QueuedMutation>
    fun save(mutations: List<QueuedMutation>)
}

class InMemoryMutationQueueStore : MutationQueueStore {
    private var mutations = emptyList<QueuedMutation>()
    override fun load(): List<QueuedMutation> = mutations
    override fun save(mutations: List<QueuedMutation>) { this.mutations = mutations.toList() }
}
