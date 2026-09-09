package com.vergipro.mobile.feature.documents

import java.time.Instant

enum class DocumentCenterStatus { NeedsAttention, AwaitingAccountant, Ready, Archived }
enum class SavedDocumentView { All, NeedsAttention, AwaitingAccountant, ReadyForClosing }

data class DocumentListItem(
    val id: Int,
    val supplier: String,
    val invoiceNumber: String,
    val date: Instant,
    val formattedAmount: String,
    val status: DocumentCenterStatus,
    val attentionReason: Int?,
    val confidence: Float,
)

val previewDocuments = listOf(
    DocumentListItem(101, "Petrol ve Enerji A.Ş.", "GIB2026000088921", Instant.now(), "₺3.420,00", DocumentCenterStatus.NeedsAttention, com.vergipro.mobile.R.string.documents_reason_low_confidence, .79f),
    DocumentListItem(102, "Cloud Server EMEA B.V.", "GIB2026000045123", Instant.now().minusSeconds(86_400), "₺22.200,00", DocumentCenterStatus.Ready, null, .97f),
    DocumentListItem(103, "Gurme Restoran Ltd.", "FIS-2026-00441", Instant.now().minusSeconds(172_800), "₺1.595,00", DocumentCenterStatus.AwaitingAccountant, com.vergipro.mobile.R.string.documents_reason_clarification, .76f),
    DocumentListItem(104, "Global Telekom A.Ş.", "GIB2026000000101", Instant.now().minusSeconds(259_200), "₺288.000,00", DocumentCenterStatus.Ready, null, .99f),
)

