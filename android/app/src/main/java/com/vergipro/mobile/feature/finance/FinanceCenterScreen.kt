package com.vergipro.mobile.feature.finance

import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vergipro.mobile.core.data.FinancialSummary
import com.vergipro.mobile.core.data.ExportFile
import com.vergipro.mobile.core.data.FiscalPeriod
import com.vergipro.mobile.core.data.JournalPreviewData
import com.vergipro.mobile.core.data.MonthEndReminderStatus
import com.vergipro.mobile.core.data.TaxRadarData
import com.vergipro.mobile.core.data.TriggerReminderResult
import com.vergipro.mobile.core.designsystem.VPColor
import com.vergipro.mobile.core.designsystem.formattedTRY
import com.vergipro.mobile.core.designsystem.formattedTRYCompact
import com.vergipro.mobile.feature.home.MonthEndReminderCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class FinanceSection { OVERVIEW, CLOSING, EXPORTS }

@Composable
fun FinanceCenterScreen(
    summary: FinancialSummary? = null,
    taxRadarData: TaxRadarData? = null,
    reminderStatus: MonthEndReminderStatus? = null,
    organizationId: Int,
    periods: List<FiscalPeriod> = emptyList(),
    selectedPeriod: FiscalPeriod? = null,
    onPeriodSelected: (FiscalPeriod) -> Unit = {},
    onTriggerReminder: (suspend () -> TriggerReminderResult?)? = null,
    onFetchJournalPreview: (suspend () -> JournalPreviewData?)? = null,
    onDownloadExport: (suspend (type: String, format: String?) -> ExportFile?)? = null,
    onNavigateToCapture: (() -> Unit)? = null,
    initialSection: FinanceSection = FinanceSection.OVERVIEW,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(initialSection) {
        when (initialSection) {
            FinanceSection.OVERVIEW -> Unit
            FinanceSection.CLOSING -> listState.scrollToItem(2)
            FinanceSection.EXPORTS -> listState.scrollToItem(3)
        }
    }

    var showJournalPreviewDialog by remember { mutableStateOf(false) }
    var journalPreviewData by remember { mutableStateOf<JournalPreviewData?>(null) }
    var isLoadingPreview by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    var isDownloadingExport by remember { mutableStateOf(false) }
    var showPeriodMenu by remember { mutableStateOf(false) }

    suspend fun shareExport(file: ExportFile, title: String) {
        val safeName = file.filename.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val localFile = withContext(Dispatchers.IO) {
            val exportDirectory = File(context.cacheDir, "exports").apply { mkdirs() }
            File(exportDirectory, safeName).apply { writeBytes(file.bytes) }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", localFile)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = file.contentType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }

    fun downloadAndShare(type: String, format: String?, title: String) {
        if (isDownloadingExport) return
        scope.launch {
            isDownloadingExport = true
            exportError = null
            try {
                val file = onDownloadExport?.invoke(type, format)
                if (file == null) exportError = context.getString(com.vergipro.mobile.R.string.finance_download_failed)
                else shareExport(file, title)
            } catch (_: Exception) {
                exportError = context.getString(com.vergipro.mobile.R.string.finance_secure_download_failed)
            } finally {
                isDownloadingExport = false
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    stringResource(com.vergipro.mobile.R.string.finance_center_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = VPColor.TextPrimary
                )
                Text(
                    taxRadarData?.period?.takeIf { it.isNotBlank() } ?: summary?.period?.takeIf { it.isNotBlank() } ?: stringResource(com.vergipro.mobile.R.string.finance_current_period),
                    style = MaterialTheme.typography.bodyMedium,
                    color = VPColor.TextSecondary
                )
            }
                if (periods.isNotEmpty()) {
                    Box {
                        androidx.compose.material3.OutlinedButton(onClick = { showPeriodMenu = true }) {
                            Text(selectedPeriod?.label ?: stringResource(com.vergipro.mobile.R.string.finance_current_period))
                        }
                        androidx.compose.material3.DropdownMenu(expanded = showPeriodMenu, onDismissRequest = { showPeriodMenu = false }) {
                            periods.forEach { period ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(period.label) },
                                    onClick = { showPeriodMenu = false; onPeriodSelected(period) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. Canlı KDV & Vergi Radarı Card
        item {
            TaxRadarCard(radarData = taxRadarData)
        }

        // 2. Saha Masraf Toplama Radarı Card
        item {
            MonthEndReminderCard(
                reminderStatus = reminderStatus,
                onTriggerReminder = onTriggerReminder,
                onNavigateToCapture = onNavigateToCapture
            )
        }

        // 3. Mali Müşavir İhracat Masası
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Description, contentDescription = null, tint = VPColor.Brand, modifier = Modifier.size(20.dp))
                            Text(
                                stringResource(com.vergipro.mobile.R.string.finance_export_desk),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = VPColor.TextSecondary,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = VPColor.AccentBlue.copy(alpha = 0.12f)
                        ) {
                            Text(
                                "Luca & Zirve & TDHP",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = VPColor.AccentBlue,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Text(
                        stringResource(com.vergipro.mobile.R.string.finance_export_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = VPColor.TextSecondary
                    )

                    // Primary Luca & Zirve buttons
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(
                            onClick = {
                                downloadAndShare("luca", null, "Luca Muhasebe Fişi")
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = VPColor.Canvas),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Icon(Icons.Outlined.FileDownload, contentDescription = null, tint = VPColor.Brand, modifier = Modifier.size(18.dp))
                                    Icon(Icons.Outlined.Share, contentDescription = null, tint = VPColor.TextSecondary, modifier = Modifier.size(14.dp))
                                }
                                Text(stringResource(com.vergipro.mobile.R.string.finance_luca_receipt), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(stringResource(com.vergipro.mobile.R.string.finance_luca_detail), style = MaterialTheme.typography.labelSmall, color = VPColor.TextSecondary, fontSize = 10.sp)
                            }
                        }

                        Card(
                            onClick = {
                                downloadAndShare("zirve", "xml", "Zirve Müşavir XML")
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = VPColor.Canvas),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Icon(Icons.Outlined.Code, contentDescription = null, tint = VPColor.Success, modifier = Modifier.size(18.dp))
                                    Icon(Icons.Outlined.Share, contentDescription = null, tint = VPColor.TextSecondary, modifier = Modifier.size(14.dp))
                                }
                                Text(stringResource(com.vergipro.mobile.R.string.finance_zirve_xml), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(stringResource(com.vergipro.mobile.R.string.finance_zirve_detail), style = MaterialTheme.typography.labelSmall, color = VPColor.TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }

                    // Secondary Excel & Zip buttons
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(
                            onClick = {
                                downloadAndShare("excel", null, "5-Sayfalı Master Excel")
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = VPColor.Canvas),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Icon(Icons.Outlined.TableChart, contentDescription = null, tint = VPColor.AccentBlue, modifier = Modifier.size(18.dp))
                                    Icon(Icons.Outlined.Share, contentDescription = null, tint = VPColor.TextSecondary, modifier = Modifier.size(14.dp))
                                }
                                Text(stringResource(com.vergipro.mobile.R.string.finance_master_excel), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(stringResource(com.vergipro.mobile.R.string.finance_master_detail), style = MaterialTheme.typography.labelSmall, color = VPColor.TextSecondary, fontSize = 10.sp)
                            }
                        }

                        Card(
                            onClick = {
                                downloadAndShare("zip", null, "Evraklar ZIP Arşivi")
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = VPColor.Canvas),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Icon(Icons.Outlined.FolderZip, contentDescription = null, tint = VPColor.TextSecondary, modifier = Modifier.size(18.dp))
                                    Icon(Icons.Outlined.Share, contentDescription = null, tint = VPColor.TextSecondary, modifier = Modifier.size(14.dp))
                                }
                                Text(stringResource(com.vergipro.mobile.R.string.finance_documents_zip), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(stringResource(com.vergipro.mobile.R.string.finance_documents_zip_detail), style = MaterialTheme.typography.labelSmall, color = VPColor.TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }

                    // TDHP Live Preview Trigger Button
                    Surface(
                        onClick = {
                            scope.launch {
                                isLoadingPreview = true
                                if (onFetchJournalPreview != null) {
                                    journalPreviewData = onFetchJournalPreview()
                                }
                                isLoadingPreview = false
                                showJournalPreviewDialog = true
                            }
                        },
                        color = VPColor.Canvas,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = VPColor.AccentBlue, modifier = Modifier.size(20.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(com.vergipro.mobile.R.string.finance_journal_preview), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(stringResource(com.vergipro.mobile.R.string.finance_journal_preview_detail), style = MaterialTheme.typography.bodySmall, color = VPColor.TextSecondary, fontSize = 11.sp)
                            }
                            if (isLoadingPreview) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = VPColor.TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // 4. Key Metrics 2x2 Grid
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(com.vergipro.mobile.R.string.finance_total_sales_base), style = MaterialTheme.typography.labelSmall, color = VPColor.TextSecondary)
                        Text(
                            (taxRadarData?.sales?.total ?: (summary?.salesTotal ?: 0.0)).formattedTRYCompact(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(stringResource(com.vergipro.mobile.R.string.finance_invoice_count, taxRadarData?.sales?.count ?: 0), style = MaterialTheme.typography.labelSmall, color = VPColor.Brand)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(com.vergipro.mobile.R.string.finance_total_expense), style = MaterialTheme.typography.labelSmall, color = VPColor.TextSecondary)
                        Text(
                            (taxRadarData?.expenses?.total ?: (summary?.purchasesTotal ?: 0.0)).formattedTRYCompact(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(stringResource(com.vergipro.mobile.R.string.finance_document_count, taxRadarData?.expenses?.count ?: 0), style = MaterialTheme.typography.labelSmall, color = VPColor.Brand)
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(com.vergipro.mobile.R.string.finance_vat_payable), style = MaterialTheme.typography.labelSmall, color = VPColor.TextSecondary)
                        Text(
                            (taxRadarData?.vat?.balance ?: (summary?.vatBalance ?: 0.0)).formattedTRY(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = VPColor.Danger
                        )
                        Text(stringResource(com.vergipro.mobile.R.string.finance_vat_return), style = MaterialTheme.typography.labelSmall, color = VPColor.Danger)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(com.vergipro.mobile.R.string.finance_vehicle_nd_expense), style = MaterialTheme.typography.labelSmall, color = VPColor.TextSecondary)
                        Text(
                            (taxRadarData?.fuel?.totalKkeg ?: 0.0).formattedTRY(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = VPColor.Warning
                        )
                        Text(stringResource(com.vergipro.mobile.R.string.finance_income_tax_restriction), style = MaterialTheme.typography.labelSmall, color = VPColor.Warning)
                    }
                }
            }
        }

        // 5. GVK 40/1 Vehicle Restriction Detail Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = VPColor.Brand, modifier = Modifier.size(18.dp))
                            Text(stringResource(com.vergipro.mobile.R.string.finance_vehicle_restriction), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            (taxRadarData?.fuel?.total ?: 0.0).formattedTRY(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    HorizontalDivider(color = VPColor.CardBorder)

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(com.vergipro.mobile.R.string.finance_deductible_expense), style = MaterialTheme.typography.bodySmall, color = VPColor.TextSecondary)
                        Text(
                            (taxRadarData?.fuel?.matrah70 ?: 0.0).formattedTRY(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = VPColor.Success
                        )
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(com.vergipro.mobile.R.string.finance_non_deductible_expense), style = MaterialTheme.typography.bodySmall, color = VPColor.TextSecondary)
                        Text(
                            (taxRadarData?.fuel?.totalKkeg ?: 0.0).formattedTRY(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = VPColor.Danger
                        )
                    }

                    Text(
                        stringResource(com.vergipro.mobile.R.string.finance_vehicle_restriction_explanation),
                        style = MaterialTheme.typography.bodySmall,
                        color = VPColor.TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(32.dp))
        }
    }

    exportError?.let { message ->
        AlertDialog(
            onDismissRequest = { exportError = null },
            confirmButton = { TextButton(onClick = { exportError = null }) { Text(stringResource(com.vergipro.mobile.R.string.ok)) } },
            title = { Text(stringResource(com.vergipro.mobile.R.string.finance_export_failed_title)) },
            text = { Text(message) },
        )
    }

    // Journal Preview Dialog
    if (showJournalPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showJournalPreviewDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(com.vergipro.mobile.R.string.finance_live_journals), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Surface(
                        shape = CircleShape,
                        color = if (journalPreviewData?.isBalanced == true) VPColor.Success.copy(alpha = 0.15f) else VPColor.Danger.copy(alpha = 0.15f)
                    ) {
                        Text(
                            if (journalPreviewData?.isBalanced == true) stringResource(com.vergipro.mobile.R.string.finance_balanced) else stringResource(com.vergipro.mobile.R.string.finance_unbalanced),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (journalPreviewData?.isBalanced == true) VPColor.Success else VPColor.Danger
                        )
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val entries = journalPreviewData?.entries ?: emptyList()
                    if (entries.isEmpty()) {
                        item {
                            Text(stringResource(com.vergipro.mobile.R.string.finance_no_journal), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    items(entries) { entry ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = VPColor.Canvas),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(entry.voucherNo, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    Text(entry.date, fontSize = 11.sp, color = VPColor.TextSecondary)
                                }
                                Text(entry.description, fontSize = 11.sp, color = VPColor.TextSecondary)
                                HorizontalDivider(color = VPColor.CardBorder)
                                entry.lines.forEach { line ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${line.accountCode} ${line.accountName}", fontSize = 11.sp, modifier = Modifier.weight(1f))
                                        if (line.debit > 0) {
                                            Text("${line.debit.formattedTRY()} B", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        } else {
                                            Text("${line.credit.formattedTRY()} A", fontSize = 11.sp, color = VPColor.TextSecondary, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showJournalPreviewDialog = false }) {
                    Text(stringResource(com.vergipro.mobile.R.string.finance_close))
                }
            }
        )
    }
}
