package com.vergipro.mobile.feature.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vergipro.mobile.core.data.TaxRadarData
import com.vergipro.mobile.core.designsystem.VPColor
import com.vergipro.mobile.core.designsystem.formattedTRY
import com.vergipro.mobile.core.designsystem.formattedTRYCompact
import kotlin.math.abs

@Composable
fun TaxRadarCard(
    radarData: TaxRadarData?,
    modifier: Modifier = Modifier,
) {
    if (radarData == null) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(com.vergipro.mobile.R.string.tax_radar_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(com.vergipro.mobile.R.string.tax_radar_unavailable), style = MaterialTheme.typography.bodySmall, color = VPColor.TextSecondary)
            }
        }
        return
    }
    val data = radarData
    val balance = data.vat.balance
    val isPayable = balance > 0


    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Title & Period & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "KDV Dengesi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = VPColor.TextPrimary
                    )
                    Text(
                        data.period.ifBlank { "Güncel dönem" },
                        style = MaterialTheme.typography.bodySmall,
                        color = VPColor.TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isPayable) VPColor.CardBorder else VPColor.Success.copy(alpha = 0.12f)
                ) {
                    Text(
                        if (isPayable) "Ödenecek KDV" else "Devreden KDV",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPayable) VPColor.TextPrimary else VPColor.Success
                    )
                }
            }

            // Hero VAT Amount
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    abs(balance).formattedTRY(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (isPayable) "Dönem için hesaplanan net ödenecek vergi" else "Sonraki döneme devreden KDV bakiyesi",
                    style = MaterialTheme.typography.bodySmall,
                    color = VPColor.TextSecondary,
                    fontSize = 12.sp
                )
            }

            HorizontalDivider(color = VPColor.CardBorder)

            // Breakdown Rows: Satış vs Gider vs Taşıt
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                breakdownRow(
                    title = "Satış Faturaları KDV",
                    amount = data.sales.kdvTotal
                )
                breakdownRow(
                    title = "Gider Fişleri KDV",
                    amount = -data.expenses.kdvTotal
                )
                breakdownRow(
                    title = "Taşıt Yakıtı (%70 Gider KDV)",
                    amount = -data.fuel.kdv70
                )

                if (data.fuel.totalKkeg > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Taşıt KKEG (%30 Kısıtlama)",
                            style = MaterialTheme.typography.bodySmall,
                            color = VPColor.TextSecondary
                        )
                        Text(
                            data.fuel.totalKkeg.formattedTRY(),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = VPColor.TextSecondary
                        )
                    }
                }
            }

            // Month-end Projection Card
            Surface(
                color = VPColor.Canvas,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Ay Sonu Tahmini",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = VPColor.TextPrimary
                        )
                        if (data.remainingDays > 0) {
                            Text(
                                "${data.remainingDays} gün kaldı",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = VPColor.TextSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (data.projection.projectedVatBalance > 0) "Tahmini Ödenecek:" else "Tahmini Devreden:",
                            style = MaterialTheme.typography.bodySmall,
                            color = VPColor.TextSecondary
                        )
                        Text(
                            abs(data.projection.projectedVatBalance).formattedTRY(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (data.projection.neutralizingExpenseNeeded > 0) {
                        Text(
                            "KDV'yi dengelemek için önerilen ek gider: ${data.projection.neutralizingExpenseNeeded.formattedTRYCompact()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VPColor.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Tax scenarios must be calculated by the versioned backend tax engine.
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(com.vergipro.mobile.R.string.tax_radar_scenario_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = VPColor.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun breakdownRow(
    title: String,
    amount: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            abs(amount).formattedTRY(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
