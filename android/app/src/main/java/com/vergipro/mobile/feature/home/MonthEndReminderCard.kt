package com.vergipro.mobile.feature.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.vergipro.mobile.core.data.MonthEndReminderStatus
import com.vergipro.mobile.core.data.TriggerReminderResult
import com.vergipro.mobile.core.designsystem.VPColor
import kotlinx.coroutines.launch

@Composable
fun MonthEndReminderCard(
    reminderStatus: MonthEndReminderStatus?,
    onTriggerReminder: (suspend () -> TriggerReminderResult?)? = null,
    onNavigateToCapture: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val data = reminderStatus ?: MonthEndReminderStatus()

    var isSending by remember { mutableStateOf(false) }
    var sentMessage by remember { mutableStateOf<String?>(null) }

    val daysLeft = data.daysRemaining
    val isUrgent = data.isUrgent || daysLeft <= 5

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = if (isUrgent) VPColor.Danger else VPColor.Warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        stringResource(com.vergipro.mobile.R.string.reminder_radar_title),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = VPColor.TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = VPColor.Canvas
                ) {
                    Text(
                        if (data.targetDay > 0) stringResource(com.vergipro.mobile.R.string.reminder_target_day, data.targetDay) else stringResource(com.vergipro.mobile.R.string.reminder_schedule_unavailable),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = VPColor.TextSecondary
                    )
                }
            }

            // Countdown & Scan CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (data.targetDay > 0) stringResource(com.vergipro.mobile.R.string.reminder_days_left, daysLeft) else "—",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isUrgent) VPColor.Danger else VPColor.TextPrimary
                    )
                    Text(
                        stringResource(com.vergipro.mobile.R.string.reminder_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = VPColor.TextSecondary,
                        fontSize = 12.sp
                    )
                }

                if (onNavigateToCapture != null) {
                    Button(
                        onClick = onNavigateToCapture,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3F3F46),
                            contentColor = Color.White,
                        )
                    ) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(com.vergipro.mobile.R.string.reminder_scan_receipt), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Telegram Bot Connect Code Bar
            Surface(
                color = VPColor.Canvas,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Send,
                            contentDescription = null,
                            tint = VPColor.AccentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            data.botUsername.takeIf { it.isNotBlank() }?.let { "@$it" } ?: stringResource(com.vergipro.mobile.R.string.reminder_bot_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = VPColor.TextPrimary
                        )
                    }

                    Surface(
                        modifier = Modifier.clickable(enabled = data.botInviteCode.isNotBlank()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Telegram Code", data.botInviteCode))
                            Toast.makeText(context, context.getString(com.vergipro.mobile.R.string.reminder_code_copied), Toast.LENGTH_SHORT).show()
                        },
                        color = VPColor.AccentBlue.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                tint = VPColor.AccentBlue,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                if (data.botInviteCode.isNotBlank()) stringResource(com.vergipro.mobile.R.string.reminder_code, data.botInviteCode) else stringResource(com.vergipro.mobile.R.string.reminder_code_unavailable),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = VPColor.AccentBlue,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Trigger Button
            if (onTriggerReminder != null) {
                Button(
                    onClick = {
                        scope.launch {
                            isSending = true
                            val result = onTriggerReminder()
                            isSending = false
                            if (result?.success == true) {
                                sentMessage = context.getString(com.vergipro.mobile.R.string.reminder_sent_count, result.totalRecipients)
                                Toast.makeText(context, context.getString(com.vergipro.mobile.R.string.reminder_sent), Toast.LENGTH_SHORT).show()
                            } else Toast.makeText(context, context.getString(com.vergipro.mobile.R.string.reminder_failed), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3F3F46),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF3F3F46).copy(alpha = 0.45f),
                        disabledContentColor = Color.White.copy(alpha = 0.75f),
                    ),
                    enabled = !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(com.vergipro.mobile.R.string.reminder_send_team), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                if (sentMessage != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = VPColor.Success, modifier = Modifier.size(16.dp))
                        Text(sentMessage!!, style = MaterialTheme.typography.bodySmall, color = VPColor.Success, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
