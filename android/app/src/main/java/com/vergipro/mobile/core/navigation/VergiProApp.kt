package com.vergipro.mobile.core.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vergipro.mobile.R
import com.vergipro.mobile.core.configuration.AppConfiguration
import com.vergipro.mobile.core.data.AccountantPortfolioData
import com.vergipro.mobile.core.data.AffiliateDashboardData
import com.vergipro.mobile.core.data.BillingInfo
import com.vergipro.mobile.core.data.DashboardData
import com.vergipro.mobile.core.data.DocumentItem
import com.vergipro.mobile.core.data.FinancialSummary
import com.vergipro.mobile.core.data.LiveFeatureRepository
import com.vergipro.mobile.core.data.MonthEndReminderStatus
import com.vergipro.mobile.core.data.TaxRadarData
import com.vergipro.mobile.core.data.TeamMember
import com.vergipro.mobile.core.data.TenantOrg
import com.vergipro.mobile.core.data.VehicleItem
import com.vergipro.mobile.core.network.ApiClient
import com.vergipro.mobile.core.network.OrganizationContext
import com.vergipro.mobile.core.session.SessionController
import com.vergipro.mobile.feature.capture.CaptureScreen
import com.vergipro.mobile.feature.documents.DocumentsCenterScreen
import com.vergipro.mobile.feature.finance.FinanceCenterScreen
import com.vergipro.mobile.feature.finance.FinanceSection
import com.vergipro.mobile.feature.home.HomeScreen
import com.vergipro.mobile.feature.identity.OrganizationSummary
import com.vergipro.mobile.feature.workspace.WorkspaceScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Destination(@StringRes val label: Int, val icon: ImageVector) {
    Today(R.string.nav_today, Icons.Outlined.AutoAwesome),
    Documents(R.string.nav_documents, Icons.Outlined.Description),
    Capture(R.string.nav_capture, Icons.Outlined.DocumentScanner),
    Finance(R.string.nav_finance, Icons.Outlined.AccountBalanceWallet),
    Workspace(R.string.nav_workspace, Icons.Outlined.Dashboard),
}

@Composable
fun VergiProApp(
    companyName: String = "Şirketiniz",
    organizationId: Int,
    organizations: List<OrganizationSummary>,
    configuration: AppConfiguration,
    sessionController: SessionController,
    onOrganizationSelected: (OrganizationSummary) -> Unit,
    onSignOut: () -> Unit = {},
) {
    var destination by remember { mutableStateOf(Destination.Today) }
    var financeSection by remember { mutableStateOf(FinanceSection.OVERVIEW) }
    val scope = rememberCoroutineScope()

    val client = remember(configuration, sessionController) {
        ApiClient(
            baseUri = configuration.apiBaseUri,
            accessTokenProvider = sessionController::currentAccessToken,
        )
    }
    val repository = remember(client) { LiveFeatureRepository(client) }
    val orgContext = remember(organizationId) { OrganizationContext(organizationId) }
    val snackbarHostState = remember { SnackbarHostState() }

    var dashboardData by remember(organizationId) { mutableStateOf<DashboardData?>(null) }
    var documentsList by remember(organizationId) { mutableStateOf<List<DocumentItem>>(emptyList()) }
    var financeSummary by remember(organizationId) { mutableStateOf<FinancialSummary?>(null) }
    var vehiclesList by remember(organizationId) { mutableStateOf<List<VehicleItem>>(emptyList()) }
    var teamList by remember(organizationId) { mutableStateOf<List<TeamMember>>(emptyList()) }
    var billingInfo by remember(organizationId) { mutableStateOf<BillingInfo?>(null) }
    var taxRadarData by remember(organizationId) { mutableStateOf<TaxRadarData?>(null) }
    var reminderStatus by remember(organizationId) { mutableStateOf<MonthEndReminderStatus?>(null) }
    var portfolioData by remember(organizationId) { mutableStateOf<AccountantPortfolioData?>(null) }
    var affiliateData by remember(organizationId) { mutableStateOf<AffiliateDashboardData?>(null) }
    var notifications by remember(organizationId) { mutableStateOf<List<com.vergipro.mobile.core.data.AppNotification>>(emptyList()) }
    var periods by remember(organizationId) { mutableStateOf<List<com.vergipro.mobile.core.data.FiscalPeriod>>(emptyList()) }
    var selectedPeriod by remember(organizationId) { mutableStateOf<com.vergipro.mobile.core.data.FiscalPeriod?>(null) }
    var loadedDestinations by remember(organizationId) { mutableStateOf(emptySet<Destination>()) }
    var isImmersiveCapture by remember { mutableStateOf(false) }

    fun refresh(destinationToRefresh: Destination, force: Boolean = false) {
        if (!force && destinationToRefresh in loadedDestinations) return
        loadedDestinations = loadedDestinations + destinationToRefresh
        scope.launch {
            val failures = coroutineScope {
                when (destinationToRefresh) {
                    Destination.Today -> listOf(
                        async { loadOffMain({ repository.getDashboard(orgContext) }) { dashboardData = it } },
                        async { loadOffMain({ repository.getTaxRadar(orgContext) }) { taxRadarData = it } },
                        async { loadOffMain({ repository.getMonthEndReminderStatus(orgContext) }) { reminderStatus = it } },
                        async { loadOffMain({ repository.getNotifications(orgContext) }) { notifications = it } },
                    )
                    Destination.Documents -> listOf(
                        async { loadOffMain({ repository.listDocuments(orgContext) }) { documentsList = it } },
                    )
                    Destination.Capture -> emptyList()
                    Destination.Finance -> listOf(
                        async { loadOffMain({ repository.getSummary(orgContext, selectedPeriod?.year ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR), selectedPeriod?.month ?: (java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1)) }) { financeSummary = it } },
                        async { loadOffMain({ repository.getTaxRadar(orgContext, selectedPeriod?.year, selectedPeriod?.month) }) { taxRadarData = it } },
                        async { loadOffMain({ repository.getMonthEndReminderStatus(orgContext) }) { reminderStatus = it } },
                        async { loadOffMain({ repository.getPeriods(orgContext) }) { loaded -> periods = loaded; if (selectedPeriod == null) selectedPeriod = loaded.firstOrNull() } },
                    )
                    Destination.Workspace -> listOf(
                        async { loadOffMain({ repository.getDashboard(orgContext) }) { dashboardData = it } },
                        async { loadOffMain({ repository.listVehicles(orgContext) }) { vehiclesList = it } },
                        async { loadOffMain({ repository.getTeam(orgContext) }) { teamList = it } },
                        async { loadOffMain({ repository.getBilling(orgContext) }) { billingInfo = it } },
                        async { loadOffMain({ repository.getPortfolio(orgContext) }) { portfolioData = it } },
                        async { loadOffMain({ repository.getAffiliate(orgContext) }) { affiliateData = it } },
                    )
                }.awaitAll().filterNotNull()
            }
            if (failures.isNotEmpty()) {
                loadedDestinations = loadedDestinations - destinationToRefresh
                snackbarHostState.showSnackbar(
                    "Bazı bilgiler güncellenemedi. Bağlantınızı kontrol edip yeniden deneyin."
                )
            }
        }
    }

    LaunchedEffect(organizationId, destination) {
        refresh(destination)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isImmersiveCapture) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(18.dp, RoundedCornerShape(32.dp), clip = false),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 2.dp,
                    shadowElevation = 0.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.58f)),
                ) {
                    NavigationBar(
                        modifier = Modifier.height(72.dp),
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0),
                    ) {
                        Destination.entries.forEach { item ->
                            val selected = destination == item
                            val scale by animateFloatAsState(
                                targetValue = if (selected) 1.08f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                                label = "glass-tab-scale",
                            )
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (item == Destination.Finance) financeSection = FinanceSection.OVERVIEW
                                    destination = item
                                },
                                icon = {
                                    Icon(
                                        item.icon,
                                        contentDescription = null,
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        },
                                    )
                                },
                                label = { Text(stringResource(item.label), fontSize = 10.sp, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
            }
        },
    ) { padding ->
        when (destination) {
            Destination.Today -> HomeScreen(
                companyName = dashboardData?.organization?.name ?: companyName,
                dashboardData = dashboardData,
                taxRadarData = taxRadarData,
                reminderStatus = reminderStatus,
                notifications = notifications,
                onTriggerReminder = {
                    repository.triggerMonthEndReminder(orgContext)
                },
                onNavigateToCapture = { destination = Destination.Capture },
                onNavigateToDocuments = { destination = Destination.Documents },
                onNavigateToFinance = { financeSection = FinanceSection.OVERVIEW; destination = Destination.Finance },
                onNavigateToTasks = { destination = Destination.Workspace },
                modifier = Modifier.padding(padding),
            )
            Destination.Documents -> DocumentsCenterScreen(
                documents = documentsList,
                accessToken = sessionController.currentAccessToken(),
                apiBaseUri = configuration.apiBaseUri,
                onStatusUpdate = { docId, newStatus, onResult ->
                    scope.launch {
                        val success = runCatching { repository.updateStatus(orgContext, docId, newStatus) }.isSuccess
                        if (success) refresh(Destination.Documents, force = true)
                        onResult(success)
                    }
                },
                onQuickCategory = { docId, category, plate, isHarici, onResult ->
                    scope.launch {
                        val success = runCatching { repository.quickUpdateCategory(orgContext, docId, category, plate, isHarici) }.isSuccess
                        if (success) refresh(Destination.Documents, force = true)
                        onResult(success)
                    }
                },
                onBulkConfirm = { onResult ->
                    scope.launch {
                        val success = runCatching { repository.bulkConfirm(orgContext) }.isSuccess
                        if (success) refresh(Destination.Documents, force = true)
                        onResult(success)
                    }
                },
                onDeleteDocument = { docId, onResult ->
                    scope.launch {
                        val success = runCatching { repository.deleteDocument(orgContext, docId) }.isSuccess
                        if (success) refresh(Destination.Documents, force = true)
                        onResult(success)
                    }
                },
                modifier = Modifier.padding(padding)
            )
            Destination.Capture -> CaptureScreen(
                organizationId = orgContext.organizationId,
                onUploadDocument = { fileBytes, filename, docType, description, plate, isHarici, onResult ->
                    scope.launch {
                        val result = runCatching {
                            repository.uploadDocument(orgContext, fileBytes, filename, docType, description, plate, isHarici)
                        }
                        onResult(result)
                        if (result.isSuccess) {
                            loadedDestinations = loadedDestinations - setOf(
                                Destination.Today,
                                Destination.Documents,
                                Destination.Finance,
                                Destination.Workspace,
                            )
                        }
                    }
                },
                onUploadBatch = { files, docType, description, plate, isHarici, onResult ->
                    scope.launch {
                        val result = runCatching {
                            repository.uploadBatch(orgContext, files, docType, description, plate, isHarici)
                        }
                        onResult(result)
                        if (result.isSuccess) {
                            loadedDestinations = loadedDestinations - setOf(
                                Destination.Today,
                                Destination.Documents,
                                Destination.Finance,
                                Destination.Workspace,
                            )
                        }
                    }
                },
                onImmersiveChanged = { isImmersiveCapture = it },
                modifier = Modifier.padding(padding)
            )
            Destination.Finance -> FinanceCenterScreen(
                summary = financeSummary,
                organizationId = orgContext.organizationId,
                periods = periods,
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { period ->
                    selectedPeriod = period
                    scope.launch {
                        coroutineScope {
                            listOf(
                                async { loadOffMain({ repository.getSummary(orgContext, period.year, period.month) }) { financeSummary = it } },
                                async { loadOffMain({ repository.getTaxRadar(orgContext, period.year, period.month) }) { taxRadarData = it } },
                            ).awaitAll()
                        }
                    }
                },
                taxRadarData = taxRadarData,
                reminderStatus = reminderStatus,
                onTriggerReminder = { repository.triggerMonthEndReminder(orgContext) },
                onFetchJournalPreview = { repository.getJournalPreview(orgContext, selectedPeriod?.year, selectedPeriod?.month) },
                onDownloadExport = { type, format -> repository.downloadExport(orgContext, type, format, selectedPeriod?.year, selectedPeriod?.month) },
                onNavigateToCapture = { destination = Destination.Capture },
                initialSection = financeSection,
                modifier = Modifier.padding(padding)
            )
            Destination.Workspace -> WorkspaceScreen(
                currentOrg = dashboardData?.organization ?: TenantOrg(organizationId, companyName),
                organizations = organizations.map { organization ->
                    val portfolioOrganization = portfolioData?.clients?.firstOrNull {
                        it.id.toString() == organization.id
                    }
                    TenantOrg(
                        id = organization.id.toInt(),
                        name = organization.name,
                        vkn = portfolioOrganization?.vkn.orEmpty(),
                        taxOffice = portfolioOrganization?.taxOffice.orEmpty(),
                        taxRegime = portfolioOrganization?.taxRegime.orEmpty(),
                        planTier = portfolioOrganization?.planTier ?: "",
                        monthlyProcessedCount = portfolioOrganization?.documentCount ?: 0,
                        telegramCode = portfolioOrganization?.telegramCode.orEmpty(),
                        role = organization.role,
                    )
                },
                vehicles = vehiclesList,
                teamMembers = teamList,
                billing = billingInfo,
                portfolio = portfolioData,
                affiliate = affiliateData,
                onSwitchOrg = { targetOrgId ->
                    organizations.firstOrNull { it.id.toIntOrNull() == targetOrgId }?.let {
                        onOrganizationSelected(it)
                    }
                },
                onAddVehicle = { plate, model ->
                    scope.launch {
                        runCatching { repository.addVehicle(orgContext, plate, model) }
                        refresh(Destination.Workspace, force = true)
                    }
                },
                onInviteMember = { fullName, email, role ->
                    scope.launch {
                        runCatching { repository.inviteMember(orgContext, fullName, email, role) }
                        refresh(Destination.Workspace, force = true)
                    }
                },
                onRegenerateTelegramCode = {
                    val code = repository.regenerateTelegramCode(orgContext)
                    refresh(Destination.Workspace, force = true)
                    code
                },
                onInviteAccountant = { email, name, license, message ->
                    repository.inviteAccountant(orgContext, email, name, license, message)
                    refresh(Destination.Workspace, force = true)
                },
                onUpdateOrganization = { name, vkn, taxOffice, taxRegime ->
                    repository.updateOrganization(orgContext, name, vkn, taxOffice, taxRegime)
                    refresh(Destination.Workspace, force = true)
                },
                onLinkOrganization = { code -> repository.linkOrganizationByCode(code) },
                onCreateOrganization = { name, vkn, taxOffice, taxRegime -> repository.createOrganization(name, vkn, taxOffice, taxRegime); refresh(Destination.Workspace, force = true) },
                onUpdatePayout = { iban, bank, holder -> repository.updatePayoutSettings(iban, bank, holder) },
                onUpdateReferralCode = { code -> repository.updateReferralCode(code); refresh(Destination.Workspace, force = true) },
                onNavigateToDocuments = { destination = Destination.Documents },
                onNavigateToCapture = { destination = Destination.Capture },
                onNavigateToFinance = { financeSection = FinanceSection.OVERVIEW; destination = Destination.Finance },
                onNavigateToClosing = { financeSection = FinanceSection.CLOSING; destination = Destination.Finance },
                onNavigateToExports = { financeSection = FinanceSection.EXPORTS; destination = Destination.Finance },
                onSignOut = onSignOut,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

private suspend fun <T> loadOffMain(
    block: suspend () -> T,
    apply: (T) -> Unit,
): Throwable? {
    val result = withContext(Dispatchers.IO) { runCatching { block() } }
    result.onSuccess(apply)
    return result.exceptionOrNull()
}
