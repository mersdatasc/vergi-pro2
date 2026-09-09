package com.vergipro.mobile.core.data

import com.vergipro.mobile.core.network.ApiClient
import com.vergipro.mobile.core.network.ApiEndpoint
import com.vergipro.mobile.core.network.HttpMethod
import com.vergipro.mobile.core.network.OrganizationContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Calendar

// ==========================================
// DATA MODELS (Directly matched with Backend & Frontend)
// ==========================================

data class DocumentItem(
    val id: Int,
    val tenantId: Int = 0,
    val userId: Int? = null,
    val userName: String = "Personel",
    val docType: String = "EXPENSE",       // SALES, EXPENSE, FUEL, RECEIPT
    val invoiceNo: String = "",
    val date: String = "",
    val supplierName: String = "",
    val supplierVkn: String = "",
    val customerName: String = "",
    val customerVkn: String = "",
    val matrah20: Double = 0.0,
    val kdv20: Double = 0.0,
    val matrah10: Double = 0.0,
    val kdv10: Double = 0.0,
    val matrah1: Double = 0.0,
    val kdv1: Double = 0.0,
    val matrah0: Double = 0.0,
    val kdvIstisnaKodu: String = "",
    val tevkifatKdv: Double = 0.0,
    val oivTutari: Double = 0.0,
    val totalKdv: Double = 0.0,
    val totalAmount: Double = 0.0,
    val currency: String = "TRY",
    val category: String = "Diğer Giderler",
    val plate: String? = null,
    val isHarici: Boolean = false,
    val isTeknoparkIstisna: Boolean = false,
    val description: String = "",
    val status: String = "PENDING_REVIEW", // PENDING_REVIEW, APPROVED, REJECTED
    val ocrConfidence: Double = 0.98,
    val needsStaffReview: Boolean = false,
    val isDuplicate: Boolean = false,
    val duplicateReason: String = "",
    val duplicateOfId: Int? = null,
    val isOutOfPeriod: Boolean = false,
    val fiscalPeriodWarning: String = "",
    val originalFilename: String = "belge.pdf",
    val fileUrl: String = "",
    val createdAt: String = "",
) {
    val formattedTotalAmount: String
        get() = "₺" + java.text.DecimalFormat(
            "#,##0.00",
            java.text.DecimalFormatSymbols(java.util.Locale("tr", "TR")).apply {
                groupingSeparator = '.'
                decimalSeparator = ','
            }
        ).format(totalAmount)
}

data class VehicleItem(
    val id: Int,
    val tenantId: Int = 0,
    val plate: String,
    val brandModel: String = "Şirket Aracı",
    val assignedDriver: String = "Personel",
    val monthlyFuelTotal: Double = 0.0,
    val fuelInvoiceCount: Int = 0,
    val gvk40_1_deductible: Double = 0.0,
    val gvk40_1_kkeg: Double = 0.0,
    val isGvkApplicable: Boolean = true,
)

data class FinancialSummary(
    val tenantName: String = "Şirket",
    val tenantVkn: String = "",
    val taxRegime: String = "",
    val period: String = "",
    val monthName: String = "",
    val year: Int = 0,
    val salesTotal: Double = 0.0,
    val salesMatrah20: Double = 0.0,
    val salesKdvTotal: Double = 0.0,
    val salesCount: Int = 0,
    val purchasesTotal: Double = 0.0,
    val purchasesMatrah: Double = 0.0,
    val purchasesKdvTotal: Double = 0.0,
    val purchasesCount: Int = 0,
    val vehicleTotal: Double = 0.0,
    val vehicleGider70: Double = 0.0,
    val vehicleKkeg30: Double = 0.0,
    val vehicleKdv70: Double = 0.0,
    val vehicleCount: Int = 0,
    val staffTotal: Double = 0.0,
    val staffCount: Int = 0,
    val vatCalculated: Double = 0.0,
    val vatDeductible: Double = 0.0,
    val vatBalance: Double = 0.0,
    val totalIncomeMatrah: Double = 0.0,
    val totalDeductibleExpenses: Double = 0.0,
    val grossProfit: Double = 0.0,
    val corporateTaxEstimated: Double = 0.0,
    val teknoparkAdvantage: Double = 0.0,
)

data class DashboardMetrics(
    val salesTotal: Double = 0.0,
    val expenseTotal: Double = 0.0,
    val vatCalculated: Double = 0.0,
    val vatDeductible: Double = 0.0,
    val vatBalance: Double = 0.0,
    val pendingCount: Int = 0,
    val approvedCount: Int = 0,
    val totalCount: Int = 0,
)

data class QuotaData(
    val monthlyLimit: Int = 0,
    val monthlyProcessed: Int = 0,
    val percentage: Double = 0.0,
    val planTier: String = "",
)

data class TenantOrg(
    val id: Int,
    val name: String,
    val vkn: String = "",
    val taxOffice: String = "",
    val taxRegime: String = "",
    val planTier: String = "",
    val subscriptionStatus: String = "",
    val maxUsers: Int = 0,
    val maxMonthlyInvoices: Int = 0,
    val monthlyProcessedCount: Int = 0,
    val telegramCode: String = "",
    val isActive: Boolean = true,
    val role: String = "",
)

data class DashboardData(
    val organization: TenantOrg,
    val metrics: DashboardMetrics,
    val quota: QuotaData,
    val recentDocuments: List<DocumentItem>,
)

data class TeamMember(
    val id: Int,
    val membershipId: Int,
    val fullName: String,
    val email: String,
    val role: String,
    val telegramUsername: String = "",
    val isActive: Boolean = true,
    val lastActive: String = "Aktif",
)

data class BillingInfo(
    val organizationId: Int,
    val organizationName: String,
    val planTier: String,
    val subscriptionStatus: String,
    val usageMonthlyCount: Int?,
    val usageMaxMonthly: Int?,
    val usagePercentage: Double?,
    val maxUsers: Int?,
)

// ==========================================
// TAX RADAR & EXTENDED FEATURES
// ==========================================

data class TaxRadarSales(
    val total: Double = 0.0,
    val kdvTotal: Double = 0.0,
    val count: Int = 0,
    val m20: Double = 0.0,
    val k20: Double = 0.0,
    val m10: Double = 0.0,
    val k10: Double = 0.0,
    val m1: Double = 0.0,
    val k1: Double = 0.0,
    val m0: Double = 0.0,
)

data class TaxRadarExpenses(
    val total: Double = 0.0,
    val kdvTotal: Double = 0.0,
    val count: Int = 0,
    val m20: Double = 0.0,
    val k20: Double = 0.0,
    val m10: Double = 0.0,
    val k10: Double = 0.0,
    val m1: Double = 0.0,
    val k1: Double = 0.0,
)

data class TaxRadarFuel(
    val total: Double = 0.0,
    val kdvRaw: Double = 0.0,
    val matrah70: Double = 0.0,
    val kdv70: Double = 0.0,
    val matrah30Kkeg: Double = 0.0,
    val kdv30Kkeg: Double = 0.0,
    val totalKkeg: Double = 0.0,
    val count: Int = 0,
)

data class TaxRadarVat(
    val calculated: Double = 0.0,
    val deductible: Double = 0.0,
    val balance: Double = 0.0,
    val status: String = "",
)

data class TaxRadarProjection(
    val projectedSales: Double = 0.0,
    val projectedVatCalculated: Double = 0.0,
    val projectedVatDeductible: Double = 0.0,
    val projectedVatBalance: Double = 0.0,
    val projectedVatStatus: String = "",
    val neutralizingExpenseNeeded: Double = 0.0,
)

data class TaxRadarScore(
    val riskScore: Int = 0,
    val riskLevel: String = "",
    val riskBadge: String = "",
)

data class TaxRadarData(
    val tenantName: String = "",
    val tenantVkn: String = "",
    val period: String = "",
    val monthName: String = "",
    val year: Int = 0,
    val month: Int = 0,
    val daysInMonth: Int = 0,
    val currentDay: Int = 0,
    val remainingDays: Int = 0,
    val velocity: Double = 0.0,
    val isCurrentMonth: Boolean = false,
    val sales: TaxRadarSales = TaxRadarSales(),
    val expenses: TaxRadarExpenses = TaxRadarExpenses(),
    val fuel: TaxRadarFuel = TaxRadarFuel(),
    val vat: TaxRadarVat = TaxRadarVat(),
    val projection: TaxRadarProjection = TaxRadarProjection(),
    val radar: TaxRadarScore = TaxRadarScore(),
    val recommendations: List<String> = emptyList(),
)

data class MonthEndReminderStatus(
    val targetDay: Int = 0,
    val currentDay: Int = 0,
    val daysRemaining: Int = 0,
    val isUrgent: Boolean = false,
    val totalTeamCount: Int = 0,
    val telegramConnectedCount: Int = 0,
    val botUsername: String = "",
    val botInviteCode: String = "",
    val nextScheduledDate: String = "",
)

data class TriggerReminderResult(
    val success: Boolean = false,
    val telegramSent: Int = 0,
    val emailSent: Int = 0,
    val totalRecipients: Int = 0,
    val message: String = "",
)

data class JournalLine(
    val lineNo: Int,
    val accountCode: String,
    val accountName: String,
    val debit: Double,
    val credit: Double,
    val explanation: String,
)

data class JournalEntry(
    val voucherNo: String,
    val date: String,
    val docType: String,
    val description: String,
    val lines: List<JournalLine>,
)

data class JournalPreviewData(
    val tenantId: Int = 0,
    val companyName: String = "",
    val companyVkn: String = "",
    val period: String = "",
    val totalVouchers: Int = 0,
    val totalDebit: Double = 0.0,
    val totalCredit: Double = 0.0,
    val isBalanced: Boolean = true,
    val entries: List<JournalEntry> = emptyList(),
)

data class AccountantClientItem(
    val id: Int,
    val name: String,
    val vkn: String = "",
    val taxOffice: String = "",
    val taxRegime: String = "",
    val planTier: String = "",
    val role: String = "OWNER",
    val telegramCode: String = "",
    val documentCount: Int = 0,
    val pendingCount: Int = 0,
    val hesaplananKdv: Double = 0.0,
    val indirilecekKdv: Double = 0.0,
    val netKdvBalance: Double = 0.0,
)

data class AccountantPortfolioSummary(
    val totalClients: Int = 0,
    val totalDocuments: Int = 0,
    val totalPendingReviews: Int = 0,
    val totalNetKdvBalance: Double = 0.0,
)

data class AccountantPortfolioData(
    val summary: AccountantPortfolioSummary = AccountantPortfolioSummary(),
    val clients: List<AccountantClientItem> = emptyList(),
)

data class AffiliateTierItem(
    val id: String,
    val name: String,
    val range: String,
    val percent: Int,
    val rate: Double,
    val isCurrent: Boolean,
)

data class AffiliateDashboardData(
    val referralCode: String = "",
    val referralUrl: String = "",
    val commissionPercent: Int = 0,
    val tierName: String = "",
    val estimatedMonthlyCommissionTRY: Double? = null,
    val estimatedAnnualCommissionTRY: Double? = null,
    val pendingPayoutTRY: Double = 0.0,
    val totalEarnedTRY: Double = 0.0,
    val nextPayoutDate: String = "",
    val allTiers: List<AffiliateTierItem> = emptyList(),
)

data class ExportFile(
    val bytes: ByteArray,
    val filename: String,
    val contentType: String,
)

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val type: String = "INFO",
    val isRead: Boolean = false,
    val createdAt: String = "",
)

data class FiscalPeriod(val year: Int, val month: Int, val label: String) {
    val key: String get() = "%04d-%02d".format(year, month)
}

// ==========================================
// REPOSITORIES
// ==========================================

interface DocumentsRepository {
    suspend fun listDocuments(org: OrganizationContext, search: String? = null, docType: String? = null, status: String? = null): List<DocumentItem>
    suspend fun uploadDocument(org: OrganizationContext, fileBytes: ByteArray, filename: String, docType: String? = null, description: String? = null, plate: String? = null, isHarici: Boolean = false): DocumentItem
    suspend fun uploadBatch(org: OrganizationContext, files: List<Pair<ByteArray, String>>, docType: String? = null, description: String? = null, plate: String? = null, isHarici: Boolean = false): List<DocumentItem>
    suspend fun updateStatus(org: OrganizationContext, docId: Int, status: String): DocumentItem
    suspend fun quickUpdateCategory(org: OrganizationContext, docId: Int, category: String, plate: String? = null, isHarici: Boolean = false): DocumentItem
    suspend fun bulkConfirm(org: OrganizationContext, documentIds: List<Int>? = null): Int
    suspend fun deleteDocument(org: OrganizationContext, docId: Int): Boolean
}

interface FinanceRepository {
    suspend fun getSummary(
        org: OrganizationContext,
        year: Int = Calendar.getInstance().get(Calendar.YEAR),
        month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    ): FinancialSummary
    suspend fun getTaxRadar(org: OrganizationContext, year: Int? = null, month: Int? = null): TaxRadarData
    suspend fun getMonthEndReminderStatus(org: OrganizationContext): MonthEndReminderStatus
    suspend fun triggerMonthEndReminder(org: OrganizationContext): TriggerReminderResult
    suspend fun getJournalPreview(org: OrganizationContext, year: Int? = null, month: Int? = null): JournalPreviewData
}

interface DashboardRepository {
    suspend fun getDashboard(org: OrganizationContext): DashboardData
}

interface FleetRepository {
    suspend fun listVehicles(org: OrganizationContext): List<VehicleItem>
    suspend fun addVehicle(org: OrganizationContext, plate: String, model: String? = null): VehicleItem
}

interface WorkspaceRepository {
    suspend fun getTeam(org: OrganizationContext): List<TeamMember>
    suspend fun inviteMember(org: OrganizationContext, fullName: String, email: String, role: String): TeamMember
    suspend fun getBilling(org: OrganizationContext): BillingInfo
    suspend fun getPortfolio(org: OrganizationContext): AccountantPortfolioData
    suspend fun getAffiliate(org: OrganizationContext): AffiliateDashboardData
}

// ==========================================
// LIVE REPOSITORY IMPLEMENTATION
// ==========================================

class LiveFeatureRepository(
    private val client: ApiClient
) : DocumentsRepository, FinanceRepository, DashboardRepository, FleetRepository, WorkspaceRepository {

    suspend fun getNotifications(org: OrganizationContext): List<AppNotification> {
        val response = client.execute(ApiEndpoint("app/notifications", HttpMethod.GET), org)
        val root = runCatching { JSONObject(response.text) }.getOrNull()
        val array = root?.optJSONArray("notifications") ?: root?.optJSONArray("items")
            ?: runCatching { JSONArray(response.text) }.getOrElse { JSONArray() }
        return buildList {
            repeat(array.length()) { index ->
                val item = array.optJSONObject(index) ?: return@repeat
                add(
                    AppNotification(
                        id = item.optString("id", index.toString()),
                        title = item.optString("title").ifBlank { item.optString("subject", "Bildirim") },
                        message = item.optString("message").ifBlank { item.optString("body") },
                        type = item.optString("type", "INFO"),
                        isRead = item.optBoolean("isRead", item.optBoolean("is_read", false)),
                        createdAt = item.optString("createdAt").ifBlank { item.optString("created_at") },
                    )
                )
            }
        }
    }

    suspend fun getPeriods(org: OrganizationContext): List<FiscalPeriod> {
        val response = client.execute(ApiEndpoint("app/periods", HttpMethod.GET), org)
        val root = runCatching { JSONObject(response.text) }.getOrNull()
        val array = root?.optJSONArray("periods") ?: root?.optJSONArray("items")
            ?: runCatching { JSONArray(response.text) }.getOrElse { JSONArray() }
        return buildList {
            repeat(array.length()) { index ->
                when (val value = array.opt(index)) {
                    is JSONObject -> {
                        val year = value.optInt("year")
                        val month = value.optInt("month")
                        if (year > 0 && month in 1..12) add(FiscalPeriod(year, month, value.optString("label", "%04d-%02d".format(year, month))))
                    }
                    is String -> Regex("(\\d{4})[-/.](\\d{1,2})").find(value)?.let { match ->
                        add(FiscalPeriod(match.groupValues[1].toInt(), match.groupValues[2].toInt(), value))
                    }
                }
            }
        }
    }

    suspend fun regenerateTelegramCode(org: OrganizationContext): String {
        val response = client.execute(ApiEndpoint("app/organizations/regenerate-telegram-code", HttpMethod.POST), org)
        val json = JSONObject(response.text)
        return json.optString("telegramCode").ifBlank { json.optString("telegram_code") }
            .ifBlank { json.optJSONObject("organization")?.optString("telegramCode").orEmpty() }
    }

    suspend fun inviteAccountant(org: OrganizationContext, email: String, name: String?, licenseNo: String?, message: String?) {
        val payload = JSONObject().put("accountant_email", email)
        name?.takeIf { it.isNotBlank() }?.let { payload.put("accountant_name", it) }
        licenseNo?.takeIf { it.isNotBlank() }?.let { payload.put("license_no", it) }
        message?.takeIf { it.isNotBlank() }?.let { payload.put("message", it) }
        client.execute(ApiEndpoint("app/settings/invite-accountant", HttpMethod.POST, payload.toString().toByteArray()), org)
    }

    suspend fun updateOrganization(org: OrganizationContext, name: String, vkn: String, taxOffice: String, taxRegime: String) {
        val payload = JSONObject()
            .put("name", name)
            .put("vkn", vkn)
            .put("taxOffice", taxOffice)
            .put("taxRegime", taxRegime)
        client.execute(ApiEndpoint("app/organizations/${org.organizationId}", HttpMethod.PATCH, payload.toString().toByteArray()), org)
    }

    suspend fun linkOrganizationByCode(code: String) {
        val payload = JSONObject().put("code", code.trim()).toString().toByteArray()
        client.execute(ApiEndpoint("app/organizations/link-by-code", HttpMethod.POST, payload))
    }
    suspend fun createOrganization(name: String, vkn: String, taxOffice: String, taxRegime: String) {
        val payload = JSONObject().put("name", name).put("vkn", vkn).put("tax_office", taxOffice).put("tax_regime", taxRegime)
        client.execute(ApiEndpoint("app/organizations", HttpMethod.POST, payload.toString().toByteArray()))
    }
    suspend fun updatePayoutSettings(iban: String, bankName: String?, accountHolder: String?) {
        val payload = JSONObject().put("iban", iban)
        bankName?.takeIf(String::isNotBlank)?.let { payload.put("bank_name", it) }
        accountHolder?.takeIf(String::isNotBlank)?.let { payload.put("account_holder", it) }
        client.execute(ApiEndpoint("portal/musavir/payout-settings", HttpMethod.POST, payload.toString().toByteArray()))
    }
    suspend fun updateReferralCode(code: String) {
        val payload = JSONObject().put("referral_code", code).toString().toByteArray()
        client.execute(ApiEndpoint("portal/musavir/custom-referral-code", HttpMethod.POST, payload))
    }

    suspend fun downloadExport(
        org: OrganizationContext,
        exportType: String,
        format: String? = null,
        year: Int? = null,
        month: Int? = null,
    ): ExportFile {
        require(exportType in setOf("luca", "zirve", "excel", "zip"))
        val query = buildList {
            format?.takeIf(String::isNotBlank)?.let { add("format=${it.encoded()}") }
            year?.let { add("year=$it") }
            month?.let { add("month=$it") }
        }.joinToString("&").let { if (it.isEmpty()) "" else "?$it" }
        val response = client.execute(ApiEndpoint("app/export/$exportType$query", HttpMethod.GET), org)
        val contentDisposition = response.headers.entries
            .firstOrNull { it.key.equals("Content-Disposition", ignoreCase = true) }
            ?.value?.firstOrNull().orEmpty()
        val serverFilename = Regex("filename\\*?=(?:UTF-8''|\\\")?([^\\\";]+)", RegexOption.IGNORE_CASE)
            .find(contentDisposition)?.groupValues?.getOrNull(1)
            ?.let { java.net.URLDecoder.decode(it, StandardCharsets.UTF_8) }
        val defaultExtension = when (exportType) {
            "zirve" -> "xml"
            "zip" -> "zip"
            else -> "xlsx"
        }
        val contentType = response.headers.entries
            .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }
            ?.value?.firstOrNull()?.substringBefore(';') ?: when (defaultExtension) {
                "xml" -> "application/xml"
                "zip" -> "application/zip"
                else -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            }
        return ExportFile(response.body, serverFilename ?: "vergipro-$exportType.$defaultExtension", contentType)
    }

    override suspend fun listDocuments(org: OrganizationContext, search: String?, docType: String?, status: String?): List<DocumentItem> {
        val queryParams = buildList {
            search?.takeIf(String::isNotBlank)?.let { add("search=${it.encoded()}") }
            docType?.takeIf(String::isNotBlank)?.let { add("doc_type=${it.encoded()}") }
            status?.takeIf(String::isNotBlank)?.let { add("status_filter=${it.encoded()}") }
        }.joinToString("&")

        val path = if (queryParams.isNotEmpty()) "app/documents?$queryParams" else "app/documents"
        val resp = client.execute(ApiEndpoint(path, HttpMethod.GET), org)
        val array = JSONArray(resp.text)
        return buildList {
            for (i in 0 until array.length()) {
                add(parseDocument(array.getJSONObject(i)))
            }
        }
    }

    override suspend fun uploadDocument(
        org: OrganizationContext,
        fileBytes: ByteArray,
        filename: String,
        docType: String?,
        description: String?,
        plate: String?,
        isHarici: Boolean
    ): DocumentItem {
        val formFields = mutableMapOf<String, String>()
        if (!docType.isNullOrBlank()) formFields["doc_type_hint"] = docType
        if (!description.isNullOrBlank()) formFields["description"] = description
        if (!plate.isNullOrBlank()) formFields["plate"] = plate
        formFields["is_harici"] = isHarici.toString()

        val resp = client.uploadFile("app/documents", fileBytes, filename, formFields, org)
        val json = JSONObject(resp.text)
        return parseDocument(json.getJSONObject("document"))
    }

    override suspend fun uploadBatch(
        org: OrganizationContext,
        files: List<Pair<ByteArray, String>>,
        docType: String?,
        description: String?,
        plate: String?,
        isHarici: Boolean
    ): List<DocumentItem> {
        val formFields = mutableMapOf<String, String>()
        formFields["document_type"] = docType ?: "EXPENSE"
        if (!description.isNullOrBlank()) formFields["description"] = description
        if (!plate.isNullOrBlank()) formFields["plate"] = plate
        formFields["is_harici"] = isHarici.toString()

        val resp = client.uploadBatch("app/documents/batch", files, formFields, org)
        val json = JSONObject(resp.text)
        val array = json.optJSONArray("documents") ?: JSONArray()
        return buildList {
            for (i in 0 until array.length()) {
                add(parseDocument(array.getJSONObject(i)))
            }
        }
    }

    override suspend fun updateStatus(org: OrganizationContext, docId: Int, status: String): DocumentItem {
        val payload = JSONObject().put("status", status).toString()
        val resp = client.execute(
            ApiEndpoint("app/documents/$docId/status", HttpMethod.PATCH, payload.toByteArray(Charsets.UTF_8)),
            org
        )
        return parseDocument(JSONObject(resp.text))
    }

    override suspend fun quickUpdateCategory(
        org: OrganizationContext,
        docId: Int,
        category: String,
        plate: String?,
        isHarici: Boolean
    ): DocumentItem {
        val payload = JSONObject().apply {
            put("category", category)
            if (!plate.isNullOrBlank()) put("plate", plate)
            put("is_harici", isHarici)
        }.toString()
        val resp = client.execute(
            ApiEndpoint("app/documents/$docId/quick-category", HttpMethod.PATCH, payload.toByteArray(Charsets.UTF_8)),
            org
        )
        val json = JSONObject(resp.text)
        return parseDocument(json.getJSONObject("document"))
    }

    override suspend fun bulkConfirm(org: OrganizationContext, documentIds: List<Int>?): Int {
        val payload = JSONObject().apply {
            if (documentIds != null) {
                put("document_ids", JSONArray(documentIds))
            }
        }.toString()
        val resp = client.execute(
            ApiEndpoint("app/documents/bulk-confirm", HttpMethod.POST, payload.toByteArray(Charsets.UTF_8)),
            org
        )
        val json = JSONObject(resp.text)
        return json.optInt("confirmedCount", 0)
    }

    override suspend fun deleteDocument(org: OrganizationContext, docId: Int): Boolean {
        client.execute(ApiEndpoint("app/documents/$docId", HttpMethod.DELETE), org)
        return true
    }

    override suspend fun getSummary(org: OrganizationContext, year: Int, month: Int): FinancialSummary {
        val resp = client.execute(ApiEndpoint("app/finance/summary?year=$year&month=$month", HttpMethod.GET), org)
        val j = JSONObject(resp.text)
        return FinancialSummary(
            tenantName = j.optString("tenantName", ""),
            tenantVkn = j.optString("tenantVkn", ""),
            taxRegime = j.optString("taxRegime", ""),
            period = j.optString("period", ""),
            monthName = j.optString("monthName", ""),
            year = j.optInt("year", year),
            salesTotal = j.optDouble("salesTotal", 0.0),
            salesMatrah20 = j.optDouble("salesMatrah20", 0.0),
            salesKdvTotal = j.optDouble("salesKdvTotal", 0.0),
            salesCount = j.optInt("salesCount", 0),
            purchasesTotal = j.optDouble("purchasesTotal", 0.0),
            purchasesMatrah = j.optDouble("purchasesMatrah", 0.0),
            purchasesKdvTotal = j.optDouble("purchasesKdvTotal", 0.0),
            purchasesCount = j.optInt("purchasesCount", 0),
            vehicleTotal = j.optDouble("vehicleTotal", 0.0),
            vehicleGider70 = j.optDouble("vehicleGider70", 0.0),
            vehicleKkeg30 = j.optDouble("vehicleKkeg30", 0.0),
            vehicleKdv70 = j.optDouble("vehicleKkeg30", 0.0),
            vehicleCount = j.optInt("vehicleCount", 0),
            staffTotal = j.optDouble("staffTotal", 0.0),
            staffCount = j.optInt("staffCount", 0),
            vatCalculated = j.optDouble("vatCalculated", 0.0),
            vatDeductible = j.optDouble("vatDeductible", 0.0),
            vatBalance = j.optDouble("vatBalance", 0.0),
            totalIncomeMatrah = j.optDouble("totalIncomeMatrah", 0.0),
            totalDeductibleExpenses = j.optDouble("totalDeductibleExpenses", 0.0),
            grossProfit = j.optDouble("grossProfit", 0.0),
            corporateTaxEstimated = j.optDouble("corporateTaxEstimated", 0.0),
            teknoparkAdvantage = j.optDouble("teknoparkAdvantage", 0.0),
        )
    }

    override suspend fun getDashboard(org: OrganizationContext): DashboardData {
        val resp = client.execute(ApiEndpoint("app/dashboard", HttpMethod.GET), org)
        val j = JSONObject(resp.text)

        val orgJson = j.getJSONObject("organization")
        val tenantOrg = parseTenantOrg(orgJson)

        val mJson = j.getJSONObject("metrics")
        val metrics = DashboardMetrics(
            salesTotal = mJson.optDouble("salesTotal", 0.0),
            expenseTotal = mJson.optDouble("expenseTotal", 0.0),
            vatCalculated = mJson.optDouble("vatCalculated", 0.0),
            vatDeductible = mJson.optDouble("vatDeductible", 0.0),
            vatBalance = mJson.optDouble("vatBalance", 0.0),
            pendingCount = mJson.optInt("pendingCount", 0),
            approvedCount = mJson.optInt("approvedCount", 0),
            totalCount = mJson.optInt("totalCount", 0),
        )

        val qJson = j.getJSONObject("quota")
        val quota = QuotaData(
            monthlyLimit = qJson.optInt("monthlyLimit", 0),
            monthlyProcessed = qJson.optInt("monthlyProcessed", 0),
            percentage = qJson.optDouble("percentage", 0.0),
            planTier = qJson.optString("planTier", ""),
        )

        val docsArray = j.optJSONArray("recentDocuments") ?: JSONArray()
        val recentDocs = buildList {
            for (i in 0 until docsArray.length()) {
                add(parseDocument(docsArray.getJSONObject(i)))
            }
        }

        return DashboardData(tenantOrg, metrics, quota, recentDocs)
    }

    override suspend fun listVehicles(org: OrganizationContext): List<VehicleItem> {
        val resp = client.execute(ApiEndpoint("app/fleet", HttpMethod.GET), org)
        val array = JSONArray(resp.text)
        return buildList {
            for (i in 0 until array.length()) {
                val v = array.getJSONObject(i)
                add(
                    VehicleItem(
                        id = v.optInt("id", 0),
                        tenantId = v.optInt("tenantId", org.organizationId),
                        plate = v.optString("plate", ""),
                        brandModel = v.optString("brandModel", "Şirket Aracı"),
                        assignedDriver = v.optString("assignedDriver", "Personel"),
                        monthlyFuelTotal = v.optDouble("monthlyFuelTotal", 0.0),
                        fuelInvoiceCount = v.optInt("fuelInvoiceCount", 0),
                        gvk40_1_deductible = v.optDouble("gvk40_1_deductible", 0.0),
                        gvk40_1_kkeg = v.optDouble("gvk40_1_kkeg", 0.0),
                        isGvkApplicable = v.optBoolean("isGvkApplicable", true),
                    )
                )
            }
        }
    }

    override suspend fun addVehicle(org: OrganizationContext, plate: String, model: String?): VehicleItem {
        val payload = JSONObject().apply {
            put("plate", plate.uppercase().trim())
            put("model", model ?: "Şirket Binek Aracı")
        }.toString()
        val resp = client.execute(
            ApiEndpoint("app/fleet", HttpMethod.POST, payload.toByteArray(Charsets.UTF_8)),
            org
        )
        val v = JSONObject(resp.text)
        return VehicleItem(
            id = v.optInt("id", 0),
            tenantId = v.optInt("tenantId", org.organizationId),
            plate = v.optString("plate", plate),
            brandModel = v.optString("brandModel", model ?: "Şirket Aracı"),
            assignedDriver = v.optString("assignedDriver", "Personel"),
            monthlyFuelTotal = 0.0,
            fuelInvoiceCount = 0,
            gvk40_1_deductible = 0.0,
            gvk40_1_kkeg = 0.0,
            isGvkApplicable = true,
        )
    }

    override suspend fun getTeam(org: OrganizationContext): List<TeamMember> {
        val resp = client.execute(ApiEndpoint("app/team", HttpMethod.GET), org)
        val array = JSONArray(resp.text)
        return buildList {
            for (i in 0 until array.length()) {
                val t = array.getJSONObject(i)
                add(
                    TeamMember(
                        id = t.optInt("id", 0),
                        membershipId = t.optInt("membershipId", 0),
                        fullName = t.optString("fullName", ""),
                        email = t.optString("email", ""),
                        role = t.optString("role", "EMPLOYEE"),
                        telegramUsername = t.optString("telegramUsername", "Bağlı Değil"),
                        isActive = t.optBoolean("isActive", true),
                        lastActive = t.optString("lastActive", "Aktif"),
                    )
                )
            }
        }
    }

    override suspend fun inviteMember(org: OrganizationContext, fullName: String, email: String, role: String): TeamMember {
        val payload = JSONObject().apply {
            put("full_name", fullName.trim())
            put("email", email.trim().lowercase())
            put("role", role)
        }.toString()
        val resp = client.execute(
            ApiEndpoint("app/team/invite", HttpMethod.POST, payload.toByteArray(Charsets.UTF_8)),
            org
        )
        val t = JSONObject(resp.text)
        return TeamMember(
            id = t.optInt("id", 0),
            membershipId = t.optInt("membershipId", 0),
            fullName = t.optString("fullName", fullName),
            email = t.optString("email", email),
            role = t.optString("role", role),
            telegramUsername = "Davet Gönderildi",
            isActive = true,
            lastActive = "Az önce davet edildi",
        )
    }

    override suspend fun getBilling(org: OrganizationContext): BillingInfo {
        val resp = client.execute(ApiEndpoint("app/settings/billing", HttpMethod.GET), org)
        val j = JSONObject(resp.text)
        val usage = j.optJSONObject("usage") ?: JSONObject()
        return BillingInfo(
            organizationId = j.optInt("organizationId", org.organizationId),
            organizationName = j.optString("organizationName", ""),
            planTier = j.optString("planTier", ""),
            subscriptionStatus = j.optString("subscriptionStatus", ""),
            usageMonthlyCount = if (usage.has("monthlyProcessedCount") && !usage.isNull("monthlyProcessedCount")) usage.getInt("monthlyProcessedCount") else null,
            usageMaxMonthly = if (usage.has("maxMonthlyInvoices") && !usage.isNull("maxMonthlyInvoices")) usage.getInt("maxMonthlyInvoices") else null,
            usagePercentage = if (usage.has("invoiceUsagePct") && !usage.isNull("invoiceUsagePct")) usage.getDouble("invoiceUsagePct") else null,
            maxUsers = if (usage.has("maxUsers") && !usage.isNull("maxUsers")) usage.getInt("maxUsers") else null,
        )
    }

    override suspend fun getTaxRadar(org: OrganizationContext, year: Int?, month: Int?): TaxRadarData {
        val query = buildList {
            if (year != null) add("year" to year.toString())
            if (month != null) add("month" to month.toString())
        }
        val queryString = query.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, StandardCharsets.UTF_8)}=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
        }
        val path = "app/finance/tax-radar" + queryString.takeIf { it.isNotEmpty() }?.let { "?$it" }.orEmpty()
        val resp = client.execute(ApiEndpoint(path, HttpMethod.GET), org)
        val j = JSONObject(resp.text)
        val m = j.optJSONObject("metrics") ?: JSONObject()
        val s = m.optJSONObject("sales") ?: JSONObject()
        val e = m.optJSONObject("expenses") ?: JSONObject()
        val f = m.optJSONObject("fuel") ?: JSONObject()
        val v = m.optJSONObject("vat") ?: JSONObject()
        val p = m.optJSONObject("projection") ?: JSONObject()
        val r = m.optJSONObject("radar") ?: JSONObject()

        val recsArray = j.optJSONArray("recommendations") ?: JSONArray()
        val recs = (0 until recsArray.length()).map { recsArray.getString(it) }

        return TaxRadarData(
            tenantName = j.optString("tenantName", ""),
            tenantVkn = j.optString("tenantVkn", ""),
            period = j.optString("period", ""),
            monthName = j.optString("monthName", ""),
            year = j.optInt("year", 0),
            month = j.optInt("month", 0),
            daysInMonth = j.optInt("daysInMonth", 0),
            currentDay = j.optInt("currentDay", 0),
            remainingDays = j.optInt("remainingDays", 0),
            velocity = j.optDouble("velocity", 0.0),
            isCurrentMonth = j.optBoolean("isCurrentMonth", false),
            sales = TaxRadarSales(
                total = s.optDouble("total", 0.0),
                kdvTotal = s.optDouble("kdvTotal", 0.0),
                count = s.optInt("count", 0),
                m20 = s.optDouble("m20", 0.0),
                k20 = s.optDouble("k20", 0.0),
                m10 = s.optDouble("m10", 0.0),
                k10 = s.optDouble("k10", 0.0),
                m1 = s.optDouble("m1", 0.0),
                k1 = s.optDouble("k1", 0.0),
                m0 = s.optDouble("m0", 0.0),
            ),
            expenses = TaxRadarExpenses(
                total = e.optDouble("total", 0.0),
                kdvTotal = e.optDouble("kdvTotal", 0.0),
                count = e.optInt("count", 0),
                m20 = e.optDouble("m20", 0.0),
                k20 = e.optDouble("k20", 0.0),
                m10 = e.optDouble("m10", 0.0),
                k10 = e.optDouble("k10", 0.0),
                m1 = e.optDouble("m1", 0.0),
                k1 = e.optDouble("k1", 0.0),
            ),
            fuel = TaxRadarFuel(
                total = f.optDouble("total", 0.0),
                kdvRaw = f.optDouble("kdvRaw", 0.0),
                matrah70 = f.optDouble("matrah70", 0.0),
                kdv70 = f.optDouble("kdv70", 0.0),
                matrah30Kkeg = f.optDouble("matrah30Kkeg", 0.0),
                kdv30Kkeg = f.optDouble("kdv30Kkeg", 0.0),
                totalKkeg = f.optDouble("totalKkeg", 0.0),
                count = f.optInt("count", 0),
            ),
            vat = TaxRadarVat(
                calculated = v.optDouble("calculated", 0.0),
                deductible = v.optDouble("deductible", 0.0),
                balance = v.optDouble("balance", 0.0),
                status = v.optString("status", ""),
            ),
            projection = TaxRadarProjection(
                projectedSales = p.optDouble("projectedSales", 0.0),
                projectedVatCalculated = p.optDouble("projectedVatCalculated", 0.0),
                projectedVatDeductible = p.optDouble("projectedVatDeductible", 0.0),
                projectedVatBalance = p.optDouble("projectedVatBalance", 0.0),
                projectedVatStatus = p.optString("projectedVatStatus", ""),
                neutralizingExpenseNeeded = p.optDouble("neutralizingExpenseNeeded", 0.0),
            ),
            radar = TaxRadarScore(
                riskScore = r.optInt("riskScore", 0),
                riskLevel = r.optString("riskLevel", ""),
                riskBadge = r.optString("riskBadge", ""),
            ),
            recommendations = recs,
        )
    }

    override suspend fun getMonthEndReminderStatus(org: OrganizationContext): MonthEndReminderStatus {
        val resp = client.execute(ApiEndpoint("app/reminders/month-end-status", HttpMethod.GET), org)
        val j = JSONObject(resp.text)
        return MonthEndReminderStatus(
            targetDay = j.optInt("targetDay", 0),
            currentDay = j.optInt("currentDay", 0),
            daysRemaining = j.optInt("daysRemaining", 0),
            isUrgent = j.optBoolean("isUrgent", false),
            totalTeamCount = j.optInt("totalTeamCount", 0),
            telegramConnectedCount = j.optInt("telegramConnectedCount", 0),
            botUsername = j.optString("botUsername", ""),
            botInviteCode = j.optString("botInviteCode", ""),
            nextScheduledDate = j.optString("nextScheduledDate", ""),
        )
    }

    override suspend fun triggerMonthEndReminder(org: OrganizationContext): TriggerReminderResult {
        val resp = client.execute(ApiEndpoint("app/reminders/trigger-month-end", HttpMethod.POST), org)
        val j = JSONObject(resp.text)
        return TriggerReminderResult(
            success = j.optBoolean("success", false),
            telegramSent = j.optInt("telegramSent", 0),
            emailSent = j.optInt("emailSent", 0),
            totalRecipients = j.optInt("totalRecipients", 0),
            message = j.optString("message", ""),
        )
    }

    override suspend fun getJournalPreview(org: OrganizationContext, year: Int?, month: Int?): JournalPreviewData {
        val query = buildList {
            if (year != null) add("year" to year.toString())
            if (month != null) add("month" to month.toString())
        }
        val queryString = query.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, StandardCharsets.UTF_8)}=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
        }
        val path = "app/export/journal-preview" + queryString.takeIf { it.isNotEmpty() }?.let { "?$it" }.orEmpty()
        val resp = client.execute(ApiEndpoint(path, HttpMethod.GET), org)
        val j = JSONObject(resp.text)
        val entriesArr = j.optJSONArray("entries") ?: JSONArray()
        val entries = buildList {
            for (i in 0 until entriesArr.length()) {
                val ej = entriesArr.getJSONObject(i)
                val linesArr = ej.optJSONArray("lines") ?: JSONArray()
                val lines = buildList {
                    for (k in 0 until linesArr.length()) {
                        val lj = linesArr.getJSONObject(k)
                        add(
                            JournalLine(
                                lineNo = lj.optInt("lineNo", k + 1),
                                accountCode = lj.optString("accountCode", ""),
                                accountName = lj.optString("accountName", ""),
                                debit = lj.optDouble("debit", 0.0),
                                credit = lj.optDouble("credit", 0.0),
                                explanation = lj.optString("explanation", ""),
                            )
                        )
                    }
                }
                add(
                    JournalEntry(
                        voucherNo = ej.optString("voucherNo", ""),
                        date = ej.optString("date", ""),
                        docType = ej.optString("docType", ""),
                        description = ej.optString("description", ""),
                        lines = lines,
                    )
                )
            }
        }
        return JournalPreviewData(
            tenantId = j.optInt("tenantId", org.organizationId),
            companyName = j.optString("companyName", "Şirket"),
            companyVkn = j.optString("companyVkn", ""),
            period = j.optString("period", ""),
            totalVouchers = j.optInt("totalVouchers", entries.size),
            totalDebit = j.optDouble("totalDebit", 0.0),
            totalCredit = j.optDouble("totalCredit", 0.0),
            isBalanced = j.optBoolean("isBalanced", true),
            entries = entries,
        )
    }

    override suspend fun getPortfolio(org: OrganizationContext): AccountantPortfolioData {
        val resp = client.execute(ApiEndpoint("app/accountant/portfolio-summary", HttpMethod.GET), org)
        val j = JSONObject(resp.text)
        val sumJson = j.optJSONObject("summary") ?: JSONObject()
        val clientsArr = j.optJSONArray("clients") ?: JSONArray()
        val clients = buildList {
            for (i in 0 until clientsArr.length()) {
                val c = clientsArr.getJSONObject(i)
                add(
                    AccountantClientItem(
                        id = c.optInt("id", 0),
                        name = c.optString("name", ""),
                        vkn = c.optString("vkn", ""),
                        taxOffice = c.optString("taxOffice", ""),
                        taxRegime = c.optString("taxRegime", ""),
                        planTier = c.optString("planTier", ""),
                        role = c.optString("role", ""),
                        telegramCode = c.optString("telegramCode", ""),
                        documentCount = c.optInt("documentCount", 0),
                        pendingCount = c.optInt("pendingCount", 0),
                        hesaplananKdv = c.optDouble("hesaplananKdv", 0.0),
                        indirilecekKdv = c.optDouble("indirilecekKdv", 0.0),
                        netKdvBalance = c.optDouble("netKdvBalance", 0.0),
                    )
                )
            }
        }
        return AccountantPortfolioData(
            summary = AccountantPortfolioSummary(
                totalClients = sumJson.optInt("totalClients", clients.size),
                totalDocuments = sumJson.optInt("totalDocuments", 0),
                totalPendingReviews = sumJson.optInt("totalPendingReviews", 0),
                totalNetKdvBalance = sumJson.optDouble("totalNetKdvBalance", 0.0),
            ),
            clients = clients,
        )
    }

    override suspend fun getAffiliate(org: OrganizationContext): AffiliateDashboardData {
        val resp = client.execute(ApiEndpoint("app/accountant/affiliate", HttpMethod.GET), org)
        val j = JSONObject(resp.text)
        val aff = j.optJSONObject("affiliate") ?: JSONObject()
        val metrics = j.optJSONObject("metrics") ?: JSONObject()
        val tierInfo = j.optJSONObject("tierInfo") ?: JSONObject()
        val allTiersArr = tierInfo.optJSONArray("allTiers") ?: JSONArray()
        val tiers = buildList {
            for (i in 0 until allTiersArr.length()) {
                val t = allTiersArr.getJSONObject(i)
                add(
                    AffiliateTierItem(
                        id = t.optString("id", ""),
                        name = t.optString("name", ""),
                        range = t.optString("range", ""),
                        percent = t.optInt("percent", 0),
                        rate = t.optDouble("rate", 0.0),
                        isCurrent = t.optBoolean("isCurrent", false),
                    )
                )
            }
        }
        return AffiliateDashboardData(
            referralCode = aff.optString("referralCode", ""),
            referralUrl = aff.optString("referralUrl", ""),
            commissionPercent = aff.optInt("commissionPercent", 0),
            tierName = aff.optString("tierName", ""),
            estimatedMonthlyCommissionTRY = metrics.optionalDouble("estimatedMonthlyCommissionTRY"),
            estimatedAnnualCommissionTRY = metrics.optionalDouble("estimatedAnnualCommissionTRY"),
            pendingPayoutTRY = metrics.optDouble("pendingPayoutTRY", 0.0),
            totalEarnedTRY = metrics.optDouble("totalEarnedTRY", 0.0),
            nextPayoutDate = metrics.optString("nextPayoutDate", ""),
            allTiers = tiers,
        )
    }

    private fun parseDocument(d: JSONObject): DocumentItem = DocumentItem(
        id = d.optInt("id", 0),
        tenantId = d.optInt("tenantId", 1),
        userId = if (d.has("userId") && !d.isNull("userId")) d.getInt("userId") else null,
        userName = d.optString("userName", "Personel"),
        docType = d.optString("docType", "EXPENSE"),
        invoiceNo = d.optString("invoiceNo", ""),
        date = d.optString("date", ""),
        supplierName = d.optString("supplierName", ""),
        supplierVkn = d.optString("supplierVkn", ""),
        customerName = d.optString("customerName", ""),
        customerVkn = d.optString("customerVkn", ""),
        matrah20 = d.optDouble("matrah20", 0.0),
        kdv20 = d.optDouble("kdv20", 0.0),
        matrah10 = d.optDouble("matrah10", 0.0),
        kdv10 = d.optDouble("kdv10", 0.0),
        matrah1 = d.optDouble("matrah1", 0.0),
        kdv1 = d.optDouble("kdv1", 0.0),
        matrah0 = d.optDouble("matrah0", 0.0),
        kdvIstisnaKodu = d.optString("kdvIstisnaKodu", ""),
        tevkifatKdv = d.optDouble("tevkifatKdv", 0.0),
        oivTutari = d.optDouble("oivTutari", 0.0),
        totalKdv = d.optDouble("totalKdv", 0.0),
        totalAmount = d.optDouble("totalAmount", 0.0),
        currency = d.optString("currency", "TRY"),
        category = d.optString("category", "Diğer Giderler"),
        plate = if (d.has("plate") && !d.isNull("plate")) d.getString("plate") else null,
        isHarici = d.optBoolean("isHarici", false),
        isTeknoparkIstisna = d.optBoolean("isTeknoparkIstisna", false),
        description = d.optString("description", ""),
        status = d.optString("status", "PENDING_REVIEW"),
        ocrConfidence = d.optDouble("ocrConfidence", 0.98),
        needsStaffReview = d.optBoolean("needsStaffReview", false),
        isDuplicate = d.optBoolean("isDuplicate", false),
        duplicateReason = d.optString("duplicateReason", ""),
        duplicateOfId = if (d.has("duplicateOfId") && !d.isNull("duplicateOfId")) d.getInt("duplicateOfId") else null,
        isOutOfPeriod = d.optBoolean("isOutOfPeriod", false),
        fiscalPeriodWarning = d.optString("fiscalPeriodWarning", ""),
        originalFilename = d.optString("originalFilename", "belge.pdf"),
        fileUrl = d.optString("fileUrl", ""),
        createdAt = d.optString("createdAt", ""),
    )

    private fun parseTenantOrg(o: JSONObject): TenantOrg = TenantOrg(
        id = o.optInt("id", 0),
        name = o.optString("name", ""),
        vkn = o.optString("vkn", ""),
        taxOffice = o.optString("taxOffice", ""),
        taxRegime = o.optString("taxRegime", ""),
        planTier = o.optString("planTier", ""),
        subscriptionStatus = o.optString("subscriptionStatus", ""),
        maxUsers = o.optInt("maxUsers", 0),
        maxMonthlyInvoices = o.optInt("maxMonthlyInvoices", 0),
        monthlyProcessedCount = o.optInt("monthlyProcessedCount", 0),
        telegramCode = o.optString("telegramCode", ""),
        isActive = o.optBoolean("isActive", true),
        role = o.optString("role", ""),
    )
}

private fun String.encoded(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())

private fun JSONObject.optionalDouble(key: String): Double? =
    if (has(key) && !isNull(key)) getDouble(key) else null
