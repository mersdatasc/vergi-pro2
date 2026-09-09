package com.vergipro.mobile.feature.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vergipro.mobile.R
import com.vergipro.mobile.core.data.BillingInfo
import com.vergipro.mobile.core.data.TenantOrg
import com.vergipro.mobile.core.designsystem.VPColor

/** Read-only backend summary. Purchasing will be implemented with RevenueCat. */
@Composable
fun SubscriptionBillingScreen(
    currentOrg: TenantOrg,
    billing: BillingInfo?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tier = billing?.planTier?.takeIf(String::isNotBlank) ?: currentOrg.planTier.takeIf(String::isNotBlank)
    val status = billing?.subscriptionStatus?.takeIf(String::isNotBlank) ?: currentOrg.subscriptionStatus.takeIf(String::isNotBlank)
    val processed = billing?.usageMonthlyCount
    val limit = billing?.usageMaxMonthly
    val maxUsers = billing?.maxUsers
    val progress = if (processed != null && limit != null && limit > 0) (processed.toFloat() / limit).coerceIn(0f, 1f) else null

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, stringResource(R.string.back)) }
            Text(stringResource(R.string.billing_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                BillingCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.billing_current), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Text(tierDisplayName(tier), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        StatusBadge(status)
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BillingLabel(Icons.Outlined.CalendarToday, stringResource(R.string.billing_period_backend))
                        BillingLabel(Icons.Outlined.Verified, stringResource(R.string.billing_source_backend), VPColor.Success)
                    }
                }
            }
            item {
                BillingCard {
                    Text(stringResource(R.string.billing_usage_title), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.billing_usage_documents), fontWeight = FontWeight.SemiBold)
                        Text(
                            when {
                                processed == null -> stringResource(R.string.billing_unavailable)
                                limit == null -> stringResource(R.string.billing_count_unknown, processed)
                                else -> stringResource(R.string.billing_count, processed, limit)
                            },
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress ?: 0f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = if (progress != null && progress > .8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        when {
                            processed == null || limit == null -> stringResource(R.string.billing_unavailable)
                            processed >= limit -> stringResource(R.string.billing_limit_reached)
                            else -> stringResource(R.string.billing_remaining, (limit - processed).coerceAtLeast(0))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (progress == 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.billing_user_limit), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(maxUsers?.let { stringResource(R.string.billing_user_count, it) } ?: "—", fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.billing_telegram), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.billing_backend_scope), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                BillingCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, null)
                        Text(stringResource(R.string.billing_revenuecat_notice), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable private fun BillingCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

@Composable private fun BillingLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp)); Text(text, style = MaterialTheme.typography.bodySmall, color = tint)
    }
}

@Composable private fun StatusBadge(status: String?) {
    val trial = status.equals("TRIALING", true); val active = status.equals("ACTIVE", true)
    val foreground = when { trial -> VPColor.Warning; active -> VPColor.Success; else -> MaterialTheme.colorScheme.onSurfaceVariant }
    val background = foreground.copy(alpha = 0.14f)
    Surface(shape = CircleShape, color = background) {
        Text(when { trial -> stringResource(R.string.billing_status_trial); active -> stringResource(R.string.billing_status_active); else -> stringResource(R.string.billing_status_unknown) }, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = foreground, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun tierDisplayName(tier: String?): String = when (tier?.uppercase()) {
    "STARTER" -> stringResource(R.string.billing_plan_starter)
    "PRO" -> stringResource(R.string.billing_plan_pro)
    "OFFICE" -> stringResource(R.string.billing_plan_office)
    "ENTERPRISE" -> stringResource(R.string.billing_plan_enterprise)
    null -> stringResource(R.string.billing_plan_unknown)
    else -> tier
}
