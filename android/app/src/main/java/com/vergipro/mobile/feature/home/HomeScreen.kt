package com.vergipro.mobile.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vergipro.mobile.core.data.DashboardData
import com.vergipro.mobile.core.data.AppNotification
import com.vergipro.mobile.core.data.DashboardMetrics
import com.vergipro.mobile.core.data.MonthEndReminderStatus
import com.vergipro.mobile.core.data.QuotaData
import com.vergipro.mobile.core.data.TaxRadarData
import com.vergipro.mobile.core.data.TenantOrg
import com.vergipro.mobile.core.data.TriggerReminderResult
import com.vergipro.mobile.core.designsystem.VPColor
import com.vergipro.mobile.core.designsystem.formattedTRY
import com.vergipro.mobile.core.designsystem.formattedTRYCompact
import com.vergipro.mobile.feature.finance.TaxRadarCard
import kotlin.math.abs

data class HomeActionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color,
)

@Composable
fun HomeScreen(
    companyName: String = "Şirketiniz",
    dashboardData: DashboardData? = null,
    taxRadarData: TaxRadarData? = null,
    reminderStatus: MonthEndReminderStatus? = null,
    notifications: List<AppNotification> = emptyList(),
    onTriggerReminder: (suspend () -> TriggerReminderResult?)? = null,
    onNavigateToCapture: () -> Unit = {},
    onNavigateToDocuments: () -> Unit = {},
    onNavigateToFinance: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showNotifications by remember { mutableStateOf(false) }
    val org = dashboardData?.organization ?: TenantOrg(0, companyName)
    val metrics = dashboardData?.metrics ?: DashboardMetrics(
        salesTotal = taxRadarData?.sales?.total ?: 0.0,
        expenseTotal = taxRadarData?.expenses?.total ?: 0.0,
        vatCalculated = taxRadarData?.sales?.kdvTotal ?: 0.0,
        vatDeductible = taxRadarData?.expenses?.kdvTotal ?: 0.0,
        vatBalance = taxRadarData?.vat?.balance ?: 0.0,
    )
    val quota = dashboardData?.quota ?: QuotaData(monthlyLimit = 0, planTier = "")

    val vatBal = taxRadarData?.vat?.balance ?: metrics.vatBalance
    val isVatPayable = vatBal > 0

    val actionItems = listOf(
        HomeActionItem(
            "pending",
            "Onay Bekleyen ${metrics.pendingCount} Belge",
            "Yönetici onayı bekleyen fiş ve faturaları inceleyin.",
            Icons.Outlined.PendingActions,
            VPColor.Warning
        ),
        HomeActionItem(
            "vat",
            "KDV Durumu: ${abs(vatBal).formattedTRY()}",
            if (isVatPayable) "Bu dönem ödenecek KDV farkı (GVK 40/1 sonrası)" else "Sonraki döneme devreden KDV fazlası",
            Icons.Outlined.AccountBalance,
            if (isVatPayable) VPColor.Danger else VPColor.Success
        ),
        HomeActionItem(
            "deadline",
            taxRadarData?.period?.ifBlank { "Güncel dönem" } ?: "Güncel dönem",
            "Ön muhasebe ve SMMM Luca/Zirve ihracat hazırlığı sürüyor.",
            Icons.Outlined.CalendarMonth,
            VPColor.SecondaryText
        ),
    )

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Executive Top Header
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Row 1: Brand Mark & Executive Notifications
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.vergipro.mobile.core.designsystem.VPWordmark(
                        iconSize = 30.dp,
                        fontSize = 20.sp,
                    )

                    Surface(
                        onClick = { showNotifications = true },
                        shape = RoundedCornerShape(12.dp),
                        color = VPColor.Surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, VPColor.CardBorder),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = "Bildirimler",
                                tint = VPColor.TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            if (notifications.any { !it.isRead }) {
                                Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(7.dp).background(VPColor.Danger, CircleShape))
                            }
                        }
                    }
                }

                // Row 2: Company Overview & Regime Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            stringResource(com.vergipro.mobile.R.string.home_overview),
                            style = MaterialTheme.typography.labelSmall,
                            color = VPColor.SecondaryText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            org.name,
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                            fontWeight = FontWeight.Bold,
                            color = VPColor.TextPrimary
                        )
                        if (org.vkn.isNotBlank()) {
                            Text(
                                "VKN: ${org.vkn} · ${org.taxOffice}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = VPColor.SecondaryText,
                            )
                        }
                    }
                    if (org.taxRegime.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VPColor.AccentBlue.copy(alpha = 0.12f),
                        ) {
                            Text(
                                org.taxRegime,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = VPColor.AccentBlue,
                            )
                        }
                    }
                }
            }
        }

        // 4 Clean Key Metrics Tiles
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Ciro
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = VPColor.Surface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(VPColor.CardBorder)),
                        onClick = onNavigateToFinance
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(com.vergipro.mobile.R.string.home_total_revenue), style = MaterialTheme.typography.labelSmall, color = VPColor.SecondaryText)
                                Icon(Icons.Outlined.TrendingUp, contentDescription = null, tint = VPColor.Success, modifier = Modifier.size(16.dp))
                            }
                            Text(metrics.salesTotal.formattedTRYCompact(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("${taxRadarData?.sales?.count ?: 0} E-Fatura Kesildi", style = MaterialTheme.typography.labelSmall, color = VPColor.Success)
                        }
                    }

                    // Gider
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = VPColor.Surface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(VPColor.CardBorder)),
                        onClick = onNavigateToDocuments
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(com.vergipro.mobile.R.string.home_accepted_expense), style = MaterialTheme.typography.labelSmall, color = VPColor.SecondaryText)
                                Icon(Icons.Outlined.TrendingDown, contentDescription = null, tint = VPColor.Brand, modifier = Modifier.size(16.dp))
                            }
                            Text(metrics.expenseTotal.formattedTRYCompact(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("${taxRadarData?.expenses?.count ?: 0} Fiş/Fatura İşlendi", style = MaterialTheme.typography.labelSmall, color = VPColor.SecondaryText)
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // KDV
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = VPColor.Surface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(VPColor.CardBorder)),
                        onClick = onNavigateToFinance
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (isVatPayable) "Ödenecek KDV" else "Devreden KDV", style = MaterialTheme.typography.labelSmall, color = VPColor.SecondaryText)
                                Icon(Icons.Outlined.Radar, contentDescription = null, tint = if (isVatPayable) VPColor.Danger else VPColor.Success, modifier = Modifier.size(16.dp))
                            }
                            Text(abs(vatBal).formattedTRY(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = if (isVatPayable) VPColor.Danger else VPColor.Success)
                            Text(if (isVatPayable) "KDV 1 Farkı" else "Sonraki Aya Devir", style = MaterialTheme.typography.labelSmall, color = if (isVatPayable) VPColor.Danger else VPColor.Success)
                        }
                    }

                    // Bekleyen Belge
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = VPColor.Surface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(VPColor.CardBorder)),
                        onClick = onNavigateToDocuments
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(com.vergipro.mobile.R.string.home_pending_review), style = MaterialTheme.typography.labelSmall, color = VPColor.SecondaryText)
                                Icon(Icons.Outlined.PendingActions, contentDescription = null, tint = if (metrics.pendingCount > 0) VPColor.Warning else VPColor.Success, modifier = Modifier.size(16.dp))
                            }
                            Text("${metrics.pendingCount} Belge", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (metrics.pendingCount > 0) VPColor.Warning else VPColor.TextPrimary)
                            Text(if (metrics.pendingCount > 0) "Yönetici onayı bekliyor" else "Tamamı onaylandı", style = MaterialTheme.typography.labelSmall, color = if (metrics.pendingCount > 0) VPColor.Warning else VPColor.Success)
                        }
                    }
                }
            }
        }

        // 1. Canlı KDV & Vergi Radarı (Patronların En Çok İstediği Özellik)
        item {
            TaxRadarCard(radarData = taxRadarData)
        }

        // 2. Saha Masraf Toplama & Ay Sonu Otomatik Hatırlatıcılar
        item {
            MonthEndReminderCard(
                reminderStatus = reminderStatus,
                onTriggerReminder = onTriggerReminder,
                onNavigateToCapture = onNavigateToCapture
            )
        }

        // 3. Quick Scan CTA
        item {
            Card(
                onClick = onNavigateToCapture,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF3F3F46),
                    contentColor = Color.White,
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(com.vergipro.mobile.R.string.home_quick_scan), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(stringResource(com.vergipro.mobile.R.string.home_quick_scan_detail), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.78f))
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.78f))
                }
            }
        }

        // 4. Quota & Health Bar Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VPColor.Surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(VPColor.CardBorder)),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(stringResource(com.vergipro.mobile.R.string.home_quota), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = VPColor.TextPrimary)
                            Text("${quota.monthlyProcessed} / ${quota.monthlyLimit} Belge İşlendi", style = MaterialTheme.typography.bodySmall, color = VPColor.SecondaryText)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = VPColor.Brand.copy(alpha = 0.1f),
                        ) {
                            Text(
                                "%${quota.percentage.toInt()}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = VPColor.Brand,
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { (quota.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = VPColor.Brand,
                        trackColor = VPColor.Canvas
                    )
                }
            }
        }

        // 5. Action Items List
        item {
            Text(stringResource(com.vergipro.mobile.R.string.home_tasks), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VPColor.TextPrimary)
        }

        items(actionItems.size, key = { actionItems[it].id }) { index ->
            val action = actionItems[index]
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = VPColor.Surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(VPColor.CardBorder)),
                onClick = {
                    if (action.id == "pending") onNavigateToDocuments()
                    else onNavigateToFinance()
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = action.tint.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(action.icon, contentDescription = null, tint = action.tint, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(action.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = VPColor.TextPrimary)
                        Text(action.subtitle, style = MaterialTheme.typography.bodySmall, color = VPColor.SecondaryText)
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = VPColor.SecondaryText, modifier = Modifier.size(18.dp))
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showNotifications) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showNotifications = false },
            title = { Text(stringResource(com.vergipro.mobile.R.string.home_notifications), fontWeight = FontWeight.Bold) },
            text = {
                if (notifications.isEmpty()) Text(stringResource(com.vergipro.mobile.R.string.home_notifications_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 460.dp)) {
                    items(notifications, key = { it.id }) { notification ->
                        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(12.dp)) {
                            Text(notification.title, fontWeight = FontWeight.Bold)
                            if (notification.message.isNotBlank()) Text(notification.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (notification.createdAt.isNotBlank()) Text(notification.createdAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showNotifications = false }) { Text(stringResource(com.vergipro.mobile.R.string.close)) } },
        )
    }
}
