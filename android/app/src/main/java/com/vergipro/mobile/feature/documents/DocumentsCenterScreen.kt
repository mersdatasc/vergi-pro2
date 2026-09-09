package com.vergipro.mobile.feature.documents

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import com.vergipro.mobile.core.data.DocumentItem
import com.vergipro.mobile.core.designsystem.VPColor
import com.vergipro.mobile.core.designsystem.formattedTRY
import com.vergipro.mobile.core.designsystem.formattedTRYCompact
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

enum class DocFilterCategory(@StringRes val labelRes: Int) {
    ALL(com.vergipro.mobile.R.string.documents_filter_all),
    PENDING(com.vergipro.mobile.R.string.documents_filter_pending),
    EXPENSE(com.vergipro.mobile.R.string.documents_filter_expense),
    SALES(com.vergipro.mobile.R.string.documents_filter_sales),
    FUEL(com.vergipro.mobile.R.string.documents_filter_fuel),
    RECEIPT(com.vergipro.mobile.R.string.documents_filter_receipt)
}

@Composable
fun DocumentsCenterScreen(
    documents: List<DocumentItem> = emptyList(),
    accessToken: String? = null,
    apiBaseUri: URI,
    onStatusUpdate: (docId: Int, newStatus: String, onResult: (Boolean) -> Unit) -> Unit = { _, _, cb -> cb(false) },
    onQuickCategory: (docId: Int, category: String, plate: String?, isHarici: Boolean, onResult: (Boolean) -> Unit) -> Unit = { _, _, _, _, cb -> cb(false) },
    onBulkConfirm: (onResult: (Boolean) -> Unit) -> Unit = { cb -> cb(false) },
    onDeleteDocument: (docId: Int, onResult: (Boolean) -> Unit) -> Unit = { _, cb -> cb(false) },
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(DocFilterCategory.ALL) }
    var selectedDoc by remember { mutableStateOf<DocumentItem?>(null) }

    if (selectedDoc != null) {
        DocumentDetailSheet(
            document = selectedDoc!!,
            accessToken = accessToken,
            apiBaseUri = apiBaseUri,
            onBack = { selectedDoc = null },
            onQuickCategory = { cat, plate, isHarici ->
                val current = selectedDoc ?: return@DocumentDetailSheet
                onQuickCategory(current.id, cat, plate, isHarici) { success ->
                    if (success) selectedDoc = current.copy(category = cat, isHarici = isHarici, plate = plate ?: current.plate)
                    else Toast.makeText(context, context.getString(com.vergipro.mobile.R.string.documents_update_failed), Toast.LENGTH_SHORT).show()
                }
            },
            onApprove = {
                val current = selectedDoc ?: return@DocumentDetailSheet
                onStatusUpdate(current.id, "APPROVED") { success ->
                    if (success) selectedDoc = null
                    else Toast.makeText(context, context.getString(com.vergipro.mobile.R.string.documents_approve_failed), Toast.LENGTH_SHORT).show()
                }
            },
            onReject = {
                val current = selectedDoc ?: return@DocumentDetailSheet
                onStatusUpdate(current.id, "REJECTED") { success ->
                    if (success) selectedDoc = null
                    else Toast.makeText(context, context.getString(com.vergipro.mobile.R.string.documents_reject_failed), Toast.LENGTH_SHORT).show()
                }
            },
            onDelete = {
                val current = selectedDoc ?: return@DocumentDetailSheet
                onDeleteDocument(current.id) { success ->
                    if (success) selectedDoc = null
                    else Toast.makeText(context, context.getString(com.vergipro.mobile.R.string.documents_delete_failed), Toast.LENGTH_SHORT).show()
                }
            }
        )
        return
    }

    val filteredList = documents.filter { doc ->
        val q = query.trim().lowercase()
        val matchQuery = q.isEmpty() ||
                doc.supplierName.lowercase().contains(q) ||
                doc.customerName.lowercase().contains(q) ||
                doc.invoiceNo.lowercase().contains(q) ||
                doc.category.lowercase().contains(q) ||
                (doc.plate?.lowercase()?.contains(q) == true)

        val matchFilter = when (selectedFilter) {
            DocFilterCategory.ALL -> true
            DocFilterCategory.PENDING -> doc.status == "PENDING_REVIEW"
            DocFilterCategory.EXPENSE -> doc.docType == "EXPENSE"
            DocFilterCategory.SALES -> doc.docType == "SALES"
            DocFilterCategory.FUEL -> doc.docType == "FUEL"
            DocFilterCategory.RECEIPT -> doc.docType == "RECEIPT"
        }
        matchQuery && matchFilter
    }

    val pendingCount = documents.count { it.status == "PENDING_REVIEW" }
    val approvedCount = documents.count { it.status == "APPROVED" || it.status == "CONFIRMED" }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(com.vergipro.mobile.R.string.documents_center_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(com.vergipro.mobile.R.string.documents_center_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text(stringResource(com.vergipro.mobile.R.string.documents_search_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
        }

        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DocFilterCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedFilter == cat,
                        onClick = { selectedFilter = cat },
                        label = { Text(stringResource(cat.labelRes)) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(com.vergipro.mobile.R.string.documents_total), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${documents.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(stringResource(com.vergipro.mobile.R.string.documents_pending_review), style = MaterialTheme.typography.labelSmall, color = VPColor.Warning)
                        Text("$pendingCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VPColor.Warning)
                    }
                    Column {
                        Text(stringResource(com.vergipro.mobile.R.string.documents_approved), style = MaterialTheme.typography.labelSmall, color = VPColor.Success)
                        Text("$approvedCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VPColor.Success)
                    }
                }
            }
        }

        if (pendingCount > 0) {
            item {
                Button(
                    onClick = {
                        onBulkConfirm { success ->
                            val message = if (success) com.vergipro.mobile.R.string.documents_bulk_approved else com.vergipro.mobile.R.string.documents_bulk_failed
                            Toast.makeText(context, context.getString(message), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3F3F46),
                        contentColor = Color.White,
                    )
                ) {
                    Icon(Icons.Outlined.Bolt, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFFFBBF24))
                    Spacer(Modifier.width(8.dp))
                    Text("⚡ ${stringResource(com.vergipro.mobile.R.string.documents_approve_all, pendingCount)}", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(com.vergipro.mobile.R.string.documents_empty_search), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(filteredList, key = { it.id }) { doc ->
                DocumentCard(document = doc, onClick = { selectedDoc = doc })
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun DocumentCard(document: DocumentItem, onClick: () -> Unit) {
    val isSales = document.docType == "SALES"
    val isFuel = document.docType == "FUEL"
    val isPending = document.status == "PENDING_REVIEW"

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = when {
                    isSales -> Color(0xFF059669).copy(alpha = 0.12f)
                    isFuel -> Color(0xFF2563EB).copy(alpha = 0.12f)
                    else -> Color(0xFF64748B).copy(alpha = 0.12f)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when {
                            isSales -> Icons.Outlined.TrendingUp
                            isFuel -> Icons.Outlined.LocalGasStation
                            else -> Icons.Outlined.Receipt
                        },
                        contentDescription = null,
                        tint = when {
                            isSales -> Color(0xFF059669)
                            isFuel -> Color(0xFF2563EB)
                            else -> Color(0xFF64748B)
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isSales) document.customerName.ifBlank { stringResource(com.vergipro.mobile.R.string.documents_customer_invoice) } else document.supplierName.ifBlank { document.category },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = document.invoiceNo.ifBlank { stringResource(com.vergipro.mobile.R.string.documents_no_number) },
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = document.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (document.isDuplicate) {
                        Surface(color = VPColor.Warning.copy(alpha = 0.14f), shape = RoundedCornerShape(4.dp)) {
                            Text(stringResource(com.vergipro.mobile.R.string.documents_duplicate), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontSize = 10.sp, color = VPColor.Warning, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (document.isOutOfPeriod) {
                        Surface(color = VPColor.AccentBlue.copy(alpha = 0.14f), shape = RoundedCornerShape(4.dp)) {
                            Text(stringResource(com.vergipro.mobile.R.string.documents_period_difference), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontSize = 10.sp, color = VPColor.AccentBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (document.docType == "FUEL" || !document.plate.isNullOrBlank()) {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp)) {
                            Text("%30 KKEG", modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = document.totalAmount.formattedTRY(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Surface(
                    color = (if (isPending) VPColor.Warning else VPColor.Success).copy(alpha = 0.14f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = stringResource(if (isPending) com.vergipro.mobile.R.string.documents_pending_status else com.vergipro.mobile.R.string.documents_approved_status),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPending) VPColor.Warning else VPColor.Success,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentDetailSheet(
    document: DocumentItem,
    accessToken: String? = null,
    apiBaseUri: URI,
    onBack: () -> Unit,
    onQuickCategory: (category: String, plate: String?, isHarici: Boolean) -> Unit = { _, _, _ -> },
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDelete: () -> Unit,
) {
    val quickCategories = listOf(
        Triple(com.vergipro.mobile.R.string.documents_category_fuel, "Akaryakıt / Taşıt", false),
        Triple(com.vergipro.mobile.R.string.documents_category_meal, "Yemek / Temsil Ağırlama", false),
        Triple(com.vergipro.mobile.R.string.documents_category_cloud, "Sunucu / Bulut Hizmetleri", false),
        Triple(com.vergipro.mobile.R.string.documents_category_office, "Ofis / Kırtasiye", false),
        Triple(com.vergipro.mobile.R.string.documents_category_cargo, "Kargo / Lojistik", false),
        Triple(com.vergipro.mobile.R.string.documents_category_consulting, "Müşavirlik & Danışmanlık", false),
        Triple(com.vergipro.mobile.R.string.documents_category_external, "Harici Masraflar", true),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(com.vergipro.mobile.R.string.back))
                }
                Text(stringResource(com.vergipro.mobile.R.string.documents_detail_vat_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        if (document.isDuplicate) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VPColor.Warning.copy(alpha = 0.12f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Warning, contentDescription = null, tint = VPColor.Warning)
                        Column {
                            Text(stringResource(com.vergipro.mobile.R.string.documents_duplicate_warning), fontWeight = FontWeight.Bold, color = VPColor.Warning, fontSize = 13.sp)
                            Text(document.duplicateReason.ifBlank { stringResource(com.vergipro.mobile.R.string.documents_duplicate_default) }, fontSize = 11.sp, color = VPColor.Warning)
                        }
                    }
                }
            }
        }

        if (document.isOutOfPeriod) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VPColor.AccentBlue.copy(alpha = 0.12f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Warning, contentDescription = null, tint = VPColor.AccentBlue)
                        Column {
                            Text(stringResource(com.vergipro.mobile.R.string.documents_period_notice), fontWeight = FontWeight.Bold, color = VPColor.AccentBlue, fontSize = 13.sp)
                            Text(document.fiscalPeriodWarning.ifBlank { stringResource(com.vergipro.mobile.R.string.documents_period_default) }, fontSize = 11.sp, color = VPColor.AccentBlue)
                        }
                    }
                }
            }
        }

        // Quick Category Chips
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(com.vergipro.mobile.R.string.documents_quick_category), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickCategories.forEach { (labelRes, catName, isExt) ->
                            val isSelected = document.category == catName
                            FilterChip(
                                selected = isSelected,
                                onClick = { onQuickCategory(catName, document.plate, isExt) },
                                label = { Text(stringResource(labelRes), fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        document.supplierName.ifBlank { document.customerName }.ifBlank { stringResource(com.vergipro.mobile.R.string.documents_record) },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(stringResource(com.vergipro.mobile.R.string.documents_invoice_date, document.invoiceNo.ifBlank { "—" }, document.date.ifBlank { "—" }), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(VPColor.CardBorder))
                    Spacer(Modifier.height(4.dp))

                    DetailRow(stringResource(com.vergipro.mobile.R.string.documents_type), document.docType)
                    DetailRow(stringResource(com.vergipro.mobile.R.string.documents_category), document.category)
                    if (!document.plate.isNullOrBlank()) {
                        DetailRow(stringResource(com.vergipro.mobile.R.string.documents_vehicle_plate), document.plate ?: "")
                    }
                    if (document.supplierVkn.isNotBlank()) {
                        DetailRow(stringResource(com.vergipro.mobile.R.string.documents_supplier_vkn), document.supplierVkn)
                    }

                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(VPColor.CardBorder))
                    Spacer(Modifier.height(4.dp))

                    Text(stringResource(com.vergipro.mobile.R.string.documents_vat_breakdown), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (document.matrah20 > 0) DetailRow("%20 KDV Matrahı", document.matrah20.formattedTRY())
                    if (document.kdv20 > 0) DetailRow("%20 KDV Tutarı", document.kdv20.formattedTRY())
                    if (document.matrah10 > 0) DetailRow("%10 KDV Matrahı", document.matrah10.formattedTRY())
                    if (document.kdv10 > 0) DetailRow("%10 KDV Tutarı", document.kdv10.formattedTRY())
                    if (document.matrah1 > 0) DetailRow("%1 KDV Matrahı", document.matrah1.formattedTRY())
                    if (document.matrah0 > 0) DetailRow("%0 İstisna Matrahı", document.matrah0.formattedTRY())
                    if (document.totalKdv > 0) DetailRow("Toplam KDV", document.totalKdv.formattedTRY())

                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(VPColor.CardBorder))
                    Spacer(Modifier.height(4.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(com.vergipro.mobile.R.string.documents_grand_total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(document.formattedTotalAmount, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Document File / Image Preview
        item {
            val fileUrl = apiBaseUri.resolve("api/app/documents/${document.id}/file").toString()

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(com.vergipro.mobile.R.string.documents_file_preview), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(com.vergipro.mobile.R.string.documents_secure_connection), style = MaterialTheme.typography.labelSmall, color = VPColor.Success)
                    }
                    if (document.originalFilename.endsWith(".pdf", ignoreCase = true)) {
                        AuthenticatedPdfPreview(fileUrl = fileUrl, accessToken = accessToken)
                    } else {
                        coil.compose.AsyncImage(
                            model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(fileUrl)
                                .apply {
                                    accessToken?.takeIf(String::isNotBlank)?.let {
                                        addHeader("Authorization", "Bearer $it")
                                    }
                                }
                                .build(),
                            contentDescription = stringResource(com.vergipro.mobile.R.string.documents_image),
                            modifier = Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(com.vergipro.mobile.R.string.documents_approve), fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(com.vergipro.mobile.R.string.documents_reject))
                }
            }
        }

        item {
            Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626).copy(alpha = 0.12f), contentColor = Color(0xFFDC2626))
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(com.vergipro.mobile.R.string.documents_delete_permanently), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AuthenticatedPdfPreview(fileUrl: String, accessToken: String?) {
    val context = LocalContext.current
    var pages by remember(fileUrl) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var failed by remember(fileUrl) { mutableStateOf(false) }

    LaunchedEffect(fileUrl, accessToken) {
        failed = false
        pages = runCatching {
            withContext(Dispatchers.IO) {
                val connection = (URL(fileUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    accessToken?.takeIf(String::isNotBlank)?.let { setRequestProperty("Authorization", "Bearer $it") }
                }
                val bytes = connection.inputStream.use { it.readBytes() }
                connection.disconnect()
                val file = File.createTempFile("document-preview-", ".pdf", context.cacheDir).apply { writeBytes(bytes) }
                try {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                        PdfRenderer(descriptor).use { renderer ->
                            (0 until renderer.pageCount).map { index ->
                                renderer.openPage(index).use { page ->
                                    val width = 1200
                                    val height = (width.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                                        bitmap.eraseColor(android.graphics.Color.WHITE)
                                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    file.delete()
                }
            }
        }.getOrElse {
            failed = true
            emptyList()
        }
    }

    when {
        failed -> Text(stringResource(com.vergipro.mobile.R.string.documents_preview_failed), color = MaterialTheme.colorScheme.error)
        pages.isEmpty() -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
        else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            pages.forEachIndexed { index, bitmap ->
                Text(stringResource(com.vergipro.mobile.R.string.documents_pdf_page, index + 1, pages.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(com.vergipro.mobile.R.string.documents_pdf_page, index + 1, pages.size),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White),
                )
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
