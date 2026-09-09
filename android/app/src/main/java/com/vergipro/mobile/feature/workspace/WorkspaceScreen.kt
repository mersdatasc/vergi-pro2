package com.vergipro.mobile.feature.workspace

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vergipro.mobile.core.data.AccountantPortfolioData
import com.vergipro.mobile.core.data.AffiliateDashboardData
import com.vergipro.mobile.core.data.BillingInfo
import com.vergipro.mobile.core.data.TeamMember
import com.vergipro.mobile.core.data.TenantOrg
import com.vergipro.mobile.core.data.VehicleItem
import com.vergipro.mobile.core.designsystem.VPColor
import com.vergipro.mobile.core.designsystem.AppThemeManager
import com.vergipro.mobile.core.designsystem.AppThemePreference
import com.vergipro.mobile.core.designsystem.formattedTRY
import com.vergipro.mobile.core.designsystem.formattedTRYCompact
import com.vergipro.mobile.core.localization.AppLanguage
import com.vergipro.mobile.core.localization.AppLanguageManager
import kotlinx.coroutines.launch

private enum class WorkspaceSection { PORTFOLIO, AFFILIATE, TELEGRAM, ACCOUNTANT, FLEET, TEAM }

@Composable
private fun WorkspaceMenuRow(title: String, value: String = "", icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, VPColor.CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(color = tint.copy(alpha = 0.10f), shape = RoundedCornerShape(13.dp), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp)) }
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (value.isNotBlank()) Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = tint)
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun WorkspaceScreen(
    currentOrg: TenantOrg,
    organizations: List<TenantOrg> = emptyList(),
    vehicles: List<VehicleItem> = emptyList(),
    teamMembers: List<TeamMember> = emptyList(),
    billing: BillingInfo?,
    portfolio: AccountantPortfolioData? = null,
    affiliate: AffiliateDashboardData? = null,
    onSwitchOrg: (orgId: Int) -> Unit = {},
    onAddVehicle: (plate: String, model: String?) -> Unit = { _, _ -> },
    onInviteMember: (fullName: String, email: String, role: String) -> Unit = { _, _, _ -> },
    onRegenerateTelegramCode: suspend () -> String = { "" },
    onInviteAccountant: suspend (String, String?, String?, String?) -> Unit = { _, _, _, _ -> },
    onUpdateOrganization: suspend (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onLinkOrganization: suspend (String) -> Unit = {},
    onCreateOrganization: suspend (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onUpdatePayout: suspend (String, String?, String?) -> Unit = { _, _, _ -> },
    onUpdateReferralCode: suspend (String) -> Unit = {},
    onNavigateToDocuments: () -> Unit = {},
    onNavigateToCapture: () -> Unit = {},
    onNavigateToFinance: () -> Unit = {},
    onNavigateToClosing: () -> Unit = onNavigateToFinance,
    onNavigateToExports: () -> Unit = onNavigateToFinance,
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showOrgSwitchDialog by remember { mutableStateOf(false) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showInviteMemberDialog by remember { mutableStateOf(false) }
    var showBillingScreen by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var expandedSection by remember { mutableStateOf<WorkspaceSection?>(null) }
    var showRegenerateTelegramDialog by remember { mutableStateOf(false) }
    var showOrganizationDialog by remember { mutableStateOf(false) }
    var showLinkOrganizationDialog by remember { mutableStateOf(false) }
    var showCreateOrganizationDialog by remember { mutableStateOf(false) }
    var showPayoutDialog by remember { mutableStateOf(false) }; var showReferralDialog by remember { mutableStateOf(false) }

    val normalizedRole = currentOrg.role.uppercase()
    val isOwner = normalizedRole == "OWNER" || normalizedRole == "ADMIN"
    val isAccountant = normalizedRole == "SMMM" || normalizedRole == "ACCOUNTANT"
    val roleLabel = when {
        isOwner -> stringResource(com.vergipro.mobile.R.string.workspace_role_owner)
        isAccountant -> stringResource(com.vergipro.mobile.R.string.workspace_role_accountant)
        else -> stringResource(com.vergipro.mobile.R.string.workspace_role_employee)
    }
    val planLabel = currentOrg.planTier.takeIf { it.isNotBlank() }
    val organizationCaption = if (planLabel == null) roleLabel else "$roleLabel · $planLabel plan"
    val accountantConnected = teamMembers.any { it.role.uppercase() in setOf("SMMM", "ACCOUNTANT") }

    var newPlate by remember { mutableStateOf("") }
    var newVehicleModel by remember { mutableStateOf("") }

    var newMemberName by remember { mutableStateOf("") }
    var newMemberEmail by remember { mutableStateOf("") }
    var newMemberRole by remember { mutableStateOf("EMPLOYEE") }
    var accountantEmail by remember { mutableStateOf("") }
    var accountantName by remember { mutableStateOf("") }
    var accountantLicense by remember { mutableStateOf("") }
    var organizationName by remember(currentOrg.id) { mutableStateOf(currentOrg.name) }
    var organizationVkn by remember(currentOrg.id) { mutableStateOf(currentOrg.vkn) }
    var organizationTaxOffice by remember(currentOrg.id) { mutableStateOf(currentOrg.taxOffice) }
    var organizationTaxRegime by remember(currentOrg.id) { mutableStateOf(currentOrg.taxRegime.ifBlank { "STANDART" }) }
    var organizationCode by remember { mutableStateOf("") }
    var newOrgName by remember { mutableStateOf("") }; var newOrgVkn by remember { mutableStateOf("") }; var newOrgTaxOffice by remember { mutableStateOf("") }; var newOrgTaxRegime by remember { mutableStateOf("STANDART") }
    var payoutIban by remember { mutableStateOf("") }; var payoutBank by remember { mutableStateOf("") }; var payoutHolder by remember { mutableStateOf("") }; var referralCode by remember { mutableStateOf("") }

    if (showLanguageDialog) {
        val selectedLanguage = AppLanguageManager.selected(context)
        val selectedTheme = AppThemeManager.selected(context)
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(com.vergipro.mobile.R.string.workspace_language), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(com.vergipro.mobile.R.string.workspace_theme),
                        style = MaterialTheme.typography.labelLarge,
                        color = VPColor.SecondaryText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                    listOf(
                        AppThemePreference.SYSTEM to stringResource(com.vergipro.mobile.R.string.workspace_theme_system),
                        AppThemePreference.LIGHT to stringResource(com.vergipro.mobile.R.string.workspace_theme_light),
                        AppThemePreference.DARK to stringResource(com.vergipro.mobile.R.string.workspace_theme_dark),
                    ).forEach { (theme, label) ->
                        TextButton(
                            onClick = {
                                AppThemeManager.select(context, theme)
                                showLanguageDialog = false
                                (context as? Activity)?.recreate()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (theme == selectedTheme) "✓  $label" else label,
                                modifier = Modifier.fillMaxWidth(),
                                color = if (theme == selectedTheme) VPColor.Brand else VPColor.TextPrimary,
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Text(
                        stringResource(com.vergipro.mobile.R.string.workspace_language),
                        style = MaterialTheme.typography.labelLarge,
                        color = VPColor.SecondaryText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                    listOf(
                        AppLanguage.SYSTEM to stringResource(com.vergipro.mobile.R.string.workspace_system_language),
                        AppLanguage.TURKISH to "Türkçe",
                        AppLanguage.ENGLISH to "English",
                    ).forEach { (language, label) ->
                        TextButton(
                            onClick = {
                                AppLanguageManager.select(context, language)
                                showLanguageDialog = false
                                (context as? Activity)?.recreate()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (language == selectedLanguage) "✓  $label" else label,
                                modifier = Modifier.fillMaxWidth(),
                                color = if (language == selectedLanguage) VPColor.Brand else VPColor.TextPrimary,
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(com.vergipro.mobile.R.string.close)) }
            },
        )
    }

    if (showBillingScreen) {
        SubscriptionBillingScreen(
            currentOrg = currentOrg,
            billing = billing,
            onBack = { showBillingScreen = false },
        )
        return
    }

    // Organization Switch Dialog
    if (showOrgSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showOrgSwitchDialog = false },
            title = { Text(stringResource(com.vergipro.mobile.R.string.workspace_choose_company), fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val allOrgs = if (organizations.isNotEmpty()) organizations else listOf(currentOrg)
                    items(allOrgs) { org ->
                        val isSelected = org.id == currentOrg.id
                        Card(
                            onClick = {
                                onSwitchOrg(org.id)
                                showOrgSwitchDialog = false
                                Toast.makeText(context, context.getString(com.vergipro.mobile.R.string.workspace_switch_requested), Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) VPColor.AccentBlue.copy(alpha = 0.08f) else VPColor.Surface
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(if (isSelected) VPColor.AccentBlue else VPColor.CardBorder)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(org.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = VPColor.TextPrimary)
                                    Text("VKN: ${org.vkn.ifBlank { "-" }} · ${org.taxRegime}", fontSize = 11.sp, color = VPColor.SecondaryText)
                                }
                                if (isSelected) {
                                    Surface(color = VPColor.Brand, shape = RoundedCornerShape(4.dp)) {
                                        Text(stringResource(com.vergipro.mobile.R.string.workspace_active), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showOrgSwitchDialog = false }) { Text(stringResource(com.vergipro.mobile.R.string.close)) }
            }
        )
    }

    // Add Vehicle Dialog
    if (showAddVehicleDialog) {
        AlertDialog(
            onDismissRequest = { showAddVehicleDialog = false },
            title = { Text(stringResource(com.vergipro.mobile.R.string.workspace_add_vehicle), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(com.vergipro.mobile.R.string.workspace_add_vehicle_detail), style = MaterialTheme.typography.bodySmall, color = VPColor.SecondaryText)
                    OutlinedTextField(
                        value = newPlate,
                        onValueChange = { newPlate = it.uppercase() },
                        label = { Text(stringResource(com.vergipro.mobile.R.string.workspace_plate)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newVehicleModel,
                        onValueChange = { newVehicleModel = it },
                        label = { Text(stringResource(com.vergipro.mobile.R.string.workspace_vehicle_model)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlate.isNotBlank()) {
                            onAddVehicle(newPlate, newVehicleModel.ifBlank { null })
                            newPlate = ""
                            newVehicleModel = ""
                            showAddVehicleDialog = false
                            Toast.makeText(context, context.getString(com.vergipro.mobile.R.string.workspace_request_submitted), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VPColor.Brand)
                ) {
                    Text(stringResource(com.vergipro.mobile.R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVehicleDialog = false }) { Text(stringResource(com.vergipro.mobile.R.string.cancel)) }
            }
        )
    }

    // Invite Member Dialog
    if (showInviteMemberDialog) {
        AlertDialog(
            onDismissRequest = { showInviteMemberDialog = false },
            title = { Text(stringResource(com.vergipro.mobile.R.string.workspace_invite_member), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(com.vergipro.mobile.R.string.workspace_invite_detail), style = MaterialTheme.typography.bodySmall, color = VPColor.SecondaryText)
                    OutlinedTextField(
                        value = newMemberName,
                        onValueChange = { newMemberName = it },
                        label = { Text(stringResource(com.vergipro.mobile.R.string.workspace_full_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newMemberEmail,
                        onValueChange = { newMemberEmail = it },
                        label = { Text(stringResource(com.vergipro.mobile.R.string.identity_email_address)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMemberEmail.isNotBlank() && newMemberName.isNotBlank()) {
                            onInviteMember(newMemberName, newMemberEmail, newMemberRole)
                            newMemberName = ""
                            newMemberEmail = ""
                            showInviteMemberDialog = false
                            Toast.makeText(context, context.getString(com.vergipro.mobile.R.string.workspace_request_submitted), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VPColor.Brand)
                ) {
                    Text(stringResource(com.vergipro.mobile.R.string.workspace_send_invite))
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteMemberDialog = false }) { Text(stringResource(com.vergipro.mobile.R.string.cancel)) }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(com.vergipro.mobile.R.string.workspace_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = VPColor.TextPrimary)
        }

        // 1. Current Organization Card
        item {
            Card(
                onClick = { if (organizations.size > 1) showOrgSwitchDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VPColor.Surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(VPColor.CardBorder)),
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(
                            color = VPColor.Brand,
                            shape = RoundedCornerShape(17.dp),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    currentOrg.name.take(1).uppercase(),
                                    color = VPColor.BrandInverse,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(Modifier.weight(1f)) {
                            Text(currentOrg.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = VPColor.TextPrimary)
                            Text(
                                organizationCaption,
                                style = MaterialTheme.typography.bodyMedium,
                                color = VPColor.SecondaryText,
                            )
                        }

                        if (organizations.size > 1) {
                            Icon(Icons.Outlined.SyncAlt, contentDescription = stringResource(com.vergipro.mobile.R.string.workspace_switch_company), tint = VPColor.Brand)
                        }
                    }
            }
        }

        item {
            Text(stringResource(com.vergipro.mobile.R.string.workspace_primary), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        if (isOwner || isAccountant) item { WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_create_company), icon = Icons.Outlined.Add, tint = VPColor.Success) { showCreateOrganizationDialog = true } }
        if (isAccountant) {
            item { WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_payout_settings), icon = Icons.Outlined.AccountBalance, tint = VPColor.Brand) { showPayoutDialog = true } }
            item { WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_referral_code), icon = Icons.Outlined.Share, tint = VPColor.Success) { showReferralDialog = true } }
        }
        if (isOwner) {
            item { WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_organization), icon = Icons.Outlined.Business, tint = VPColor.Brand) { showOrganizationDialog = true } }
            item { WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_closing), icon = Icons.Outlined.CalendarMonth, tint = VPColor.Brand, onClick = onNavigateToClosing) }
            item { WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_team), teamMembers.size.toString(), Icons.Outlined.Groups, VPColor.Success) { expandedSection = if (expandedSection == WorkspaceSection.TEAM) null else WorkspaceSection.TEAM } }
            item { WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_telegram_bot), icon = Icons.Outlined.Send, tint = VPColor.AccentBlue) { expandedSection = if (expandedSection == WorkspaceSection.TELEGRAM) null else WorkspaceSection.TELEGRAM } }
            item { WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_vehicles), vehicles.size.toString(), Icons.Outlined.DirectionsCar, VPColor.Warning) { expandedSection = if (expandedSection == WorkspaceSection.FLEET) null else WorkspaceSection.FLEET } }
            item { WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_exports), icon = Icons.Outlined.Share, tint = VPColor.TextSecondary, onClick = onNavigateToExports) }
            item { WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_accountant_portfolio), (portfolio?.summary?.totalClients ?: 0).toString(), Icons.Outlined.Business, VPColor.Brand) { expandedSection = if (expandedSection == WorkspaceSection.PORTFOLIO) null else WorkspaceSection.PORTFOLIO } }
            item { WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_commission_desk), icon = Icons.Outlined.PieChart, tint = VPColor.Success) { expandedSection = if (expandedSection == WorkspaceSection.AFFILIATE) null else WorkspaceSection.AFFILIATE } }
        } else if (isAccountant) {
            item { WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_accountant_portfolio), (portfolio?.summary?.totalClients ?: 0).toString(), Icons.Outlined.Business, VPColor.Brand) { expandedSection = if (expandedSection == WorkspaceSection.PORTFOLIO) null else WorkspaceSection.PORTFOLIO } }
            item { WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_commission_desk), icon = Icons.Outlined.PieChart, tint = VPColor.Success) { expandedSection = if (expandedSection == WorkspaceSection.AFFILIATE) null else WorkspaceSection.AFFILIATE } }
            item { WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_closing), icon = Icons.Outlined.CalendarMonth, tint = VPColor.Warning, onClick = onNavigateToClosing) }
            item { WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_exports), icon = Icons.Outlined.Share, tint = VPColor.TextSecondary, onClick = onNavigateToExports) }
        } else {
            item { WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_my_documents), icon = Icons.Outlined.Description, tint = VPColor.Success, onClick = onNavigateToDocuments) }
            item { WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_scan), icon = Icons.Outlined.CameraAlt, tint = VPColor.Brand, onClick = onNavigateToCapture) }
        }

        item { WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_link_company), icon = Icons.Outlined.SyncAlt, tint = VPColor.AccentBlue) { showLinkOrganizationDialog = true } }

        if (isOwner) {
            item {
                WorkspaceMenuRow(
                    stringResource(com.vergipro.mobile.R.string.workspace_accountant),
                    stringResource(if (accountantConnected) com.vergipro.mobile.R.string.workspace_connected else com.vergipro.mobile.R.string.workspace_not_connected),
                    Icons.Outlined.Person,
                    VPColor.Brand,
                ) { expandedSection = if (expandedSection == WorkspaceSection.ACCOUNTANT) null else WorkspaceSection.ACCOUNTANT }
            }
        }

        if (expandedSection == WorkspaceSection.ACCOUNTANT) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = VPColor.Surface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(com.vergipro.mobile.R.string.workspace_accountant_invite), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(accountantEmail, { accountantEmail = it }, label = { Text(stringResource(com.vergipro.mobile.R.string.workspace_accountant_email)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(accountantName, { accountantName = it }, label = { Text(stringResource(com.vergipro.mobile.R.string.workspace_accountant_name)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(accountantLicense, { accountantLicense = it }, label = { Text(stringResource(com.vergipro.mobile.R.string.workspace_accountant_license)) }, modifier = Modifier.fillMaxWidth())
                        Button(enabled = accountantEmail.contains("@"), onClick = {
                            scope.launch {
                                runCatching { onInviteAccountant(accountantEmail.trim(), accountantName, accountantLicense, null) }
                                    .onSuccess { Toast.makeText(context, context.getString(com.vergipro.mobile.R.string.workspace_accountant_invited), Toast.LENGTH_SHORT).show(); accountantEmail = ""; accountantName = ""; accountantLicense = "" }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(com.vergipro.mobile.R.string.workspace_invite)) }
                    }
                }
            }
        }

        // Detailed tools stay out of the main hierarchy until explicitly opened.
        // 2. SMMM Mükellef Portföyü (Mali Müşavir Portföy Masası)
        if (expandedSection == WorkspaceSection.PORTFOLIO) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VPColor.Surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(VPColor.CardBorder)),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = VPColor.Brand.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Business, contentDescription = null, tint = VPColor.Brand, modifier = Modifier.size(18.dp))
                                }
                            }
                            Column {
                                Text(stringResource(com.vergipro.mobile.R.string.workspace_accountant_portfolio), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = VPColor.TextPrimary)
                                Text(stringResource(com.vergipro.mobile.R.string.workspace_accountant_portfolio_detail), style = MaterialTheme.typography.bodySmall, color = VPColor.SecondaryText)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = VPColor.AccentBlue.copy(alpha = 0.12f)
                        ) {
                            Text(
                                stringResource(com.vergipro.mobile.R.string.workspace_client_count, portfolio?.summary?.totalClients ?: 0),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = VPColor.AccentBlue
                            )
                        }
                    }

                    HorizontalDivider(color = VPColor.CardBorder)

                    // Summary Stats Row
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(com.vergipro.mobile.R.string.workspace_total_documents), style = MaterialTheme.typography.labelSmall, color = VPColor.SecondaryText, fontSize = 10.sp)
                            Text("${portfolio?.summary?.totalDocuments ?: 0}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(com.vergipro.mobile.R.string.workspace_pending_review), style = MaterialTheme.typography.labelSmall, color = VPColor.SecondaryText, fontSize = 10.sp)
                            val pending = portfolio?.summary?.totalPendingReviews ?: 0
                            Text("$pending", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = if (pending > 0) VPColor.Warning else VPColor.Success)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(com.vergipro.mobile.R.string.workspace_net_portfolio_vat), style = MaterialTheme.typography.labelSmall, color = VPColor.SecondaryText, fontSize = 10.sp)
                            Text((portfolio?.summary?.totalNetKdvBalance ?: 0.0).formattedTRYCompact(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = VPColor.Danger)
                        }
                    }

                    // Client Company Cards
                    val clients = portfolio?.clients.orEmpty()

                    clients.forEach { client ->
                        val isCurrent = client.id == currentOrg.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) VPColor.Brand.copy(alpha = 0.04f) else VPColor.Canvas,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(client.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = VPColor.TextPrimary)
                                    if (isCurrent) {
                                        Surface(color = VPColor.Brand, shape = RoundedCornerShape(4.dp)) {
                                            Text(stringResource(com.vergipro.mobile.R.string.workspace_current_company), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("VKN: ${client.vkn.ifBlank { "-" }} · ${client.taxRegime}", style = MaterialTheme.typography.bodySmall, color = VPColor.SecondaryText, fontSize = 11.sp)
                                    Text(stringResource(com.vergipro.mobile.R.string.workspace_net_vat, client.netKdvBalance.formattedTRY()), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = if (client.netKdvBalance > 0) VPColor.Danger else VPColor.Success)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(com.vergipro.mobile.R.string.workspace_document_pending_count, client.documentCount, client.pendingCount), style = MaterialTheme.typography.labelSmall, color = if (client.pendingCount > 0) VPColor.Warning else VPColor.SecondaryText)

                                    if (!isCurrent) {
                                        TextButton(
                                            onClick = {
                                                onSwitchOrg(client.id)
                                                Toast.makeText(context, context.getString(com.vergipro.mobile.R.string.workspace_switch_requested), Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Text(stringResource(com.vergipro.mobile.R.string.workspace_switch_to_company), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VPColor.AccentBlue)
                                            Spacer(Modifier.width(2.dp))
                                            Icon(Icons.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp), tint = VPColor.AccentBlue)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        }

        // 3. SMMM Komisyon ve Gelir Masası (%10-%20 Kademeli Model)
        if (expandedSection == WorkspaceSection.AFFILIATE) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VPColor.Surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(VPColor.CardBorder)),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = VPColor.Success.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Paid, contentDescription = null, tint = VPColor.Success, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column {
                                Text(stringResource(com.vergipro.mobile.R.string.workspace_commission_desk), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = VPColor.TextPrimary)
                                Text(stringResource(com.vergipro.mobile.R.string.workspace_commission_detail), style = MaterialTheme.typography.bodySmall, color = VPColor.SecondaryText)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = VPColor.Brand
                        ) {
                            Text(
                                affiliate?.tierName.orEmpty(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = VPColor.BrandInverse
                            )
                        }
                    }

                    HorizontalDivider(color = VPColor.CardBorder)

                    // Current Commission & Earnings
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = VPColor.Canvas)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(com.vergipro.mobile.R.string.workspace_current_rate), style = MaterialTheme.typography.labelSmall, color = VPColor.SecondaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(affiliate?.commissionPercent?.takeIf { it > 0 }?.let { "%$it" } ?: "—", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = VPColor.Success)
                                Text(stringResource(com.vergipro.mobile.R.string.workspace_recurring_income_share), style = MaterialTheme.typography.labelSmall, color = VPColor.SecondaryText)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = VPColor.Canvas)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(com.vergipro.mobile.R.string.workspace_estimated_monthly), style = MaterialTheme.typography.labelSmall, color = VPColor.SecondaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(affiliate?.estimatedMonthlyCommissionTRY?.formattedTRYCompact() ?: stringResource(com.vergipro.mobile.R.string.workspace_not_available), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = VPColor.TextPrimary)
                                Text(affiliate?.estimatedAnnualCommissionTRY?.let { stringResource(com.vergipro.mobile.R.string.workspace_annual, it.formattedTRYCompact()) } ?: stringResource(com.vergipro.mobile.R.string.workspace_not_available), style = MaterialTheme.typography.labelSmall, color = VPColor.Success)
                            }
                        }
                    }

                    // Tiered Table (%10 - %20)
                    Text(stringResource(com.vergipro.mobile.R.string.workspace_tiered_model), style = MaterialTheme.typography.labelSmall, color = VPColor.SecondaryText, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)

                    val tiers = affiliate?.allTiers.orEmpty()
                    if (tiers.isEmpty()) Text(stringResource(com.vergipro.mobile.R.string.workspace_tiers_unavailable), style = MaterialTheme.typography.bodySmall, color = VPColor.SecondaryText)
                    tiers.forEach { tier ->
                        val isCurrentTier = tier.isCurrent
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isCurrentTier) VPColor.Brand.copy(alpha = 0.06f) else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tier.range.ifBlank { tier.name }, style = MaterialTheme.typography.bodySmall, fontWeight = if (isCurrentTier) FontWeight.Bold else FontWeight.Normal, color = VPColor.TextPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(stringResource(com.vergipro.mobile.R.string.workspace_commission_percent, tier.percent), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (isCurrentTier) VPColor.Success else VPColor.SecondaryText)
                                if (isCurrentTier) {
                                    Surface(color = VPColor.Success, shape = CircleShape, modifier = Modifier.size(6.dp)) {}
                                }
                            }
                        }
                    }

                    // Referral URL Card
                    val refUrl = affiliate?.referralUrl.orEmpty()
                    Surface(
                        color = VPColor.Canvas,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(com.vergipro.mobile.R.string.workspace_referral_link), style = MaterialTheme.typography.labelSmall, color = VPColor.SecondaryText, fontSize = 10.sp)
                                Text(refUrl.ifBlank { stringResource(com.vergipro.mobile.R.string.workspace_not_available) }, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = VPColor.Brand, maxLines = 1)
                            }
                            IconButton(enabled = refUrl.isNotBlank(), onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Referral URL", refUrl)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, context.getString(com.vergipro.mobile.R.string.workspace_referral_copied), Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(com.vergipro.mobile.R.string.copy), tint = VPColor.Brand)
                            }
                        }
                    }

                    // Payout info note
                    affiliate?.nextPayoutDate?.takeIf { it.isNotBlank() }?.let { nextPayoutDate -> Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = VPColor.Success, modifier = Modifier.size(14.dp))
                        Text(stringResource(com.vergipro.mobile.R.string.workspace_next_payout, nextPayoutDate), style = MaterialTheme.typography.bodySmall, color = VPColor.SecondaryText, fontSize = 11.sp)
                    } }
                }
            }
        }

        }

        // 4. Telegram Bot Connect Code Card
        if (expandedSection == WorkspaceSection.TELEGRAM) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VPColor.Brand),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Send, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(20.dp))
                        Text(stringResource(com.vergipro.mobile.R.string.workspace_telegram_bot), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VPColor.BrandInverse)
                    }
                    Text(stringResource(com.vergipro.mobile.R.string.workspace_telegram_detail), style = MaterialTheme.typography.bodySmall, color = VPColor.BrandInverse.copy(alpha = 0.68f))

                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(stringResource(com.vergipro.mobile.R.string.workspace_company_join_code), style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 10.sp)
                                Text(
                                    currentOrg.telegramCode.ifBlank { stringResource(com.vergipro.mobile.R.string.workspace_not_created) },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                            IconButton(enabled = currentOrg.telegramCode.isNotBlank(), onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Telegram Code", currentOrg.telegramCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, context.getString(com.vergipro.mobile.R.string.workspace_telegram_copied), Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(com.vergipro.mobile.R.string.copy), tint = Color.White)
                            }
                        }
                    }
                    TextButton(onClick = { showRegenerateTelegramDialog = true }) {
                        Text(stringResource(com.vergipro.mobile.R.string.workspace_telegram_regenerate), color = Color.White)
                    }
                }
            }
        }

        }

        // 5. Fleet Vehicles Management (GVK 40/1)
        if (expandedSection == WorkspaceSection.FLEET) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = VPColor.Brand, modifier = Modifier.size(20.dp))
                    Text(stringResource(com.vergipro.mobile.R.string.workspace_fleet_count, vehicles.size), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VPColor.TextPrimary)
                }
                TextButton(onClick = { showAddVehicleDialog = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = VPColor.Brand)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(com.vergipro.mobile.R.string.workspace_add_vehicle_short), color = VPColor.Brand, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(vehicles, key = { it.id }) { veh ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = VPColor.Surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(VPColor.CardBorder)),
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = VPColor.Brand.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = VPColor.Brand, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(veh.plate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = VPColor.TextPrimary)
                        Text(veh.brandModel, style = MaterialTheme.typography.bodySmall, color = VPColor.SecondaryText)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(veh.monthlyFuelTotal.formattedTRYCompact(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = VPColor.TextPrimary)
                        Text(stringResource(com.vergipro.mobile.R.string.workspace_deductible_expense, veh.gvk40_1_deductible.formattedTRYCompact()), style = MaterialTheme.typography.labelSmall, color = VPColor.Success, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        }

        // 6. Team Members
        if (expandedSection == WorkspaceSection.TEAM) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Groups, contentDescription = null, tint = VPColor.Brand, modifier = Modifier.size(20.dp))
                    Text(stringResource(com.vergipro.mobile.R.string.workspace_team_count, teamMembers.size), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VPColor.TextPrimary)
                }
                TextButton(onClick = { showInviteMemberDialog = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = VPColor.Brand)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(com.vergipro.mobile.R.string.workspace_invite_short), color = VPColor.Brand, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(teamMembers, key = { it.id }) { member ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = VPColor.Surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(VPColor.CardBorder)),
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = VPColor.Brand,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(member.fullName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(member.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = VPColor.TextPrimary)
                        Text(member.email, style = MaterialTheme.typography.bodySmall, color = VPColor.SecondaryText)
                    }
                    Surface(
                        color = when (member.role) {
                            "OWNER" -> VPColor.Warning.copy(alpha = 0.12f)
                            "SMMM" -> VPColor.Success.copy(alpha = 0.12f)
                            else -> VPColor.Canvas
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            member.role,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (member.role) {
                                "OWNER" -> VPColor.Warning
                                "SMMM" -> VPColor.Success
                                else -> VPColor.SecondaryText
                            },
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        }

        // 7. Preferences and sign out
        item {
            Text(stringResource(com.vergipro.mobile.R.string.workspace_controls), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_billing), icon = Icons.Outlined.CreditCard, tint = VPColor.Brand) { showBillingScreen = true }
        }
        item {
            WorkspaceMenuRow(stringResource(com.vergipro.mobile.R.string.workspace_preferences), icon = Icons.Outlined.Language, tint = VPColor.AccentBlue) { showLanguageDialog = true }
        }
        item {
            Card(
                onClick = onSignOut,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, VPColor.CardBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Logout, contentDescription = null, tint = VPColor.Danger)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(com.vergipro.mobile.R.string.workspace_sign_out), color = VPColor.Danger, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showRegenerateTelegramDialog) {
        AlertDialog(
            onDismissRequest = { showRegenerateTelegramDialog = false },
            title = { Text(stringResource(com.vergipro.mobile.R.string.workspace_telegram_regenerate)) },
            text = { Text(stringResource(com.vergipro.mobile.R.string.workspace_telegram_regenerate_warning)) },
            confirmButton = {
                Button(onClick = {
                    showRegenerateTelegramDialog = false
                    scope.launch {
                        runCatching { onRegenerateTelegramCode() }
                            .onSuccess { Toast.makeText(context, context.getString(com.vergipro.mobile.R.string.workspace_telegram_regenerated), Toast.LENGTH_SHORT).show() }
                    }
                }) { Text(stringResource(com.vergipro.mobile.R.string.workspace_telegram_regenerate)) }
            },
            dismissButton = { TextButton(onClick = { showRegenerateTelegramDialog = false }) { Text(stringResource(com.vergipro.mobile.R.string.cancel)) } }
        )
    }

    if (showOrganizationDialog) {
        AlertDialog(
            onDismissRequest = { showOrganizationDialog = false },
            title = { Text(stringResource(com.vergipro.mobile.R.string.workspace_organization)) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(organizationName, { organizationName = it }, label = { Text(stringResource(com.vergipro.mobile.R.string.workspace_company_name)) })
                OutlinedTextField(organizationVkn, { organizationVkn = it.filter(Char::isDigit).take(11) }, label = { Text("VKN / TCKN") })
                OutlinedTextField(organizationTaxOffice, { organizationTaxOffice = it }, label = { Text(stringResource(com.vergipro.mobile.R.string.workspace_tax_office)) })
                OutlinedTextField(organizationTaxRegime, { organizationTaxRegime = it.uppercase() }, label = { Text(stringResource(com.vergipro.mobile.R.string.workspace_tax_regime)) })
            } },
            confirmButton = { Button(enabled = organizationName.isNotBlank() && organizationVkn.length in 10..11, onClick = { scope.launch { runCatching { onUpdateOrganization(organizationName.trim(), organizationVkn, organizationTaxOffice.trim(), organizationTaxRegime) }.onSuccess { showOrganizationDialog = false } } }) { Text(stringResource(com.vergipro.mobile.R.string.save)) } },
            dismissButton = { TextButton(onClick = { showOrganizationDialog = false }) { Text(stringResource(com.vergipro.mobile.R.string.cancel)) } }
        )
    }
    if (showLinkOrganizationDialog) {
        AlertDialog(
            onDismissRequest = { showLinkOrganizationDialog = false },
            title = { Text(stringResource(com.vergipro.mobile.R.string.workspace_link_company)) },
            text = { OutlinedTextField(organizationCode, { organizationCode = it }, label = { Text(stringResource(com.vergipro.mobile.R.string.workspace_company_code)) }) },
            confirmButton = { Button(enabled = organizationCode.trim().length >= 6, onClick = { scope.launch { runCatching { onLinkOrganization(organizationCode) }.onSuccess { showLinkOrganizationDialog = false; organizationCode = ""; Toast.makeText(context, context.getString(com.vergipro.mobile.R.string.workspace_company_linked), Toast.LENGTH_SHORT).show() } } }) { Text(stringResource(com.vergipro.mobile.R.string.workspace_connect)) } },
            dismissButton = { TextButton(onClick = { showLinkOrganizationDialog = false }) { Text(stringResource(com.vergipro.mobile.R.string.cancel)) } }
        )
    }
    if (showCreateOrganizationDialog) {
        AlertDialog(onDismissRequest = { showCreateOrganizationDialog = false }, title = { Text(stringResource(com.vergipro.mobile.R.string.workspace_create_company)) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(newOrgName, { newOrgName = it }, label = { Text(stringResource(com.vergipro.mobile.R.string.workspace_company_name)) })
                OutlinedTextField(newOrgVkn, { newOrgVkn = it.filter(Char::isDigit).take(11) }, label = { Text("VKN / TCKN") })
                OutlinedTextField(newOrgTaxOffice, { newOrgTaxOffice = it }, label = { Text(stringResource(com.vergipro.mobile.R.string.workspace_tax_office)) })
                OutlinedTextField(newOrgTaxRegime, { newOrgTaxRegime = it.uppercase() }, label = { Text(stringResource(com.vergipro.mobile.R.string.workspace_tax_regime)) })
            } },
            confirmButton = { Button(enabled = newOrgName.isNotBlank() && newOrgVkn.length in 10..11 && newOrgTaxOffice.isNotBlank(), onClick = { scope.launch { runCatching { onCreateOrganization(newOrgName.trim(), newOrgVkn, newOrgTaxOffice.trim(), newOrgTaxRegime) }.onSuccess { showCreateOrganizationDialog = false; newOrgName = ""; newOrgVkn = ""; newOrgTaxOffice = ""; Toast.makeText(context, context.getString(com.vergipro.mobile.R.string.workspace_company_created), Toast.LENGTH_LONG).show() } } }) { Text(stringResource(com.vergipro.mobile.R.string.workspace_create)) } },
            dismissButton = { TextButton(onClick = { showCreateOrganizationDialog = false }) { Text(stringResource(com.vergipro.mobile.R.string.cancel)) } })
    }
    if (showPayoutDialog) AlertDialog(onDismissRequest = { showPayoutDialog = false }, title = { Text(stringResource(com.vergipro.mobile.R.string.workspace_payout_settings)) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(payoutIban, { payoutIban = it.uppercase().replace(" ", "") }, label = { Text("IBAN") }); OutlinedTextField(payoutBank, { payoutBank = it }, label = { Text(stringResource(com.vergipro.mobile.R.string.workspace_bank_name)) }); OutlinedTextField(payoutHolder, { payoutHolder = it }, label = { Text(stringResource(com.vergipro.mobile.R.string.workspace_account_holder)) }) } }, confirmButton = { Button(enabled = payoutIban.startsWith("TR") && payoutIban.length == 26, onClick = { scope.launch { runCatching { onUpdatePayout(payoutIban, payoutBank, payoutHolder) }.onSuccess { showPayoutDialog = false } } }) { Text(stringResource(com.vergipro.mobile.R.string.save)) } }, dismissButton = { TextButton(onClick = { showPayoutDialog = false }) { Text(stringResource(com.vergipro.mobile.R.string.cancel)) } })
    if (showReferralDialog) AlertDialog(onDismissRequest = { showReferralDialog = false }, title = { Text(stringResource(com.vergipro.mobile.R.string.workspace_referral_code)) }, text = { OutlinedTextField(referralCode, { referralCode = it.uppercase().filter { ch -> ch.isLetterOrDigit() || ch == '-' } }, label = { Text(stringResource(com.vergipro.mobile.R.string.workspace_referral_code)) }) }, confirmButton = { Button(enabled = referralCode.length >= 4, onClick = { scope.launch { runCatching { onUpdateReferralCode(referralCode) }.onSuccess { showReferralDialog = false } } }) { Text(stringResource(com.vergipro.mobile.R.string.save)) } }, dismissButton = { TextButton(onClick = { showReferralDialog = false }) { Text(stringResource(com.vergipro.mobile.R.string.cancel)) } })
}
