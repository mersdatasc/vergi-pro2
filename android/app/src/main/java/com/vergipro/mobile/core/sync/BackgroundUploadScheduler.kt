package com.vergipro.mobile.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.UUID

enum class CellularUploadPolicy { Allowed, WifiOnly }

sealed interface SyncIndicatorState {
    data object Hidden : SyncIndicatorState
    data class OfflineQueued(val count: Int) : SyncIndicatorState
    data class Uploading(val progress: Float) : SyncIndicatorState
    data class NeedsAttention(val count: Int) : SyncIndicatorState
}

fun syncIndicator(queued: Int, activeProgress: Float?, needsAttention: Int, isOnline: Boolean): SyncIndicatorState = when {
    needsAttention > 0 -> SyncIndicatorState.NeedsAttention(needsAttention)
    activeProgress != null -> SyncIndicatorState.Uploading(activeProgress.coerceIn(0f, 1f))
    queued > 0 && !isOnline -> SyncIndicatorState.OfflineQueued(queued)
    else -> SyncIndicatorState.Hidden
}

fun interface UploadWorkDelegate { suspend fun process(uploadId: UUID, organizationId: Int): Boolean }

object UploadWorkRuntime {
    @Volatile var delegate: UploadWorkDelegate? = null
}

class ResumableUploadWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val uploadId = inputData.getString(KEY_UPLOAD_ID)?.let(UUID::fromString) ?: return Result.failure()
        val organizationId = inputData.getInt(KEY_ORGANIZATION_ID, -1).takeIf { it >= 0 } ?: return Result.failure()
        val delegate = UploadWorkRuntime.delegate ?: return Result.retry()
        return if (delegate.process(uploadId, organizationId)) Result.success() else Result.retry()
    }

    companion object {
        const val KEY_UPLOAD_ID = "upload_id"
        const val KEY_ORGANIZATION_ID = "organization_id"
    }
}

class BackgroundUploadScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun schedule(uploadId: UUID, organizationId: Int, policy: CellularUploadPolicy) {
        val network = if (policy == CellularUploadPolicy.WifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(network)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()
        val request = OneTimeWorkRequestBuilder<ResumableUploadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(ResumableUploadWorker.KEY_UPLOAD_ID to uploadId.toString(), ResumableUploadWorker.KEY_ORGANIZATION_ID to organizationId))
            .addTag("organization:$organizationId")
            .build()
        workManager.enqueueUniqueWork("upload:$uploadId", ExistingWorkPolicy.KEEP, request)
    }

    fun cancel(uploadId: UUID) = workManager.cancelUniqueWork("upload:$uploadId")
    fun cancelOrganization(organizationId: Int) = workManager.cancelAllWorkByTag("organization:$organizationId")
}
