package com.vergipro.mobile.feature.capture

import com.vergipro.mobile.core.security.VaultDocument
import java.time.Instant

enum class CaptureQualitySignal { DocumentNotFound, MoveCloser, HoldSteady, LowLight, Glare, EdgeClipped, Ready }

data class CapturedPage(
    val id: String,
    val previewBytes: ByteArray,
    val vaultDocument: VaultDocument,
    val capturedAt: Instant = Instant.now(),
)

data class CaptureUiState(
    val pages: List<CapturedPage> = emptyList(),
    val qualitySignal: CaptureQualitySignal = CaptureQualitySignal.DocumentNotFound,
    val isCameraOpen: Boolean = false,
    val isCapturing: Boolean = false,
    val errorMessage: String? = null,
    val submissionId: String? = null,
) {
    val canContinue: Boolean get() = pages.isNotEmpty() && !isCapturing && submissionId == null
}
