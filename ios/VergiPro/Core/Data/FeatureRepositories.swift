import Foundation

// ==========================================
// REAL DATA MODELS (iOS SwiftUI)
// ==========================================

struct BackendDocument: Identifiable, Decodable, Hashable, Sendable {
    let id: Int
    let tenantId: Int?
    let userId: Int?
    let userName: String?
    let docType: String
    let invoiceNo: String?
    let date: String
    let supplierName: String?
    let supplierVkn: String?
    let customerName: String?
    let customerVkn: String?
    let matrah20: Double?
    let kdv20: Double?
    let matrah10: Double?
    let kdv10: Double?
    let matrah1: Double?
    let kdv1: Double?
    let matrah0: Double?
    let kdvIstisnaKodu: String?
    let tevkifatKdv: Double?
    let oivTutari: Double?
    let totalKdv: Double
    let totalAmount: Double
    let currency: String?
    let category: String?
    let plate: String?
    let isHarici: Bool?
    let isTeknoparkIstisna: Bool?
    let description: String?
    let status: String
    let ocrConfidence: Double?
    let needsStaffReview: Bool?
    let isDuplicate: Bool?
    let duplicateReason: String?
    let duplicateOfId: Int?
    let isOutOfPeriod: Bool?
    let fiscalPeriodWarning: String?
    let originalFilename: String?
    let fileUrl: String?
    let createdAt: String?

    var displayTitle: String {
        if docType == "SALES" {
            return customerName?.isEmpty == false ? customerName! : "Müşteri Satış Faturası"
        }
        return supplierName?.isEmpty == false ? supplierName! : (category ?? "Gider Belgesi")
    }

    var supplier: String {
        displayTitle
    }

    var invoiceNumber: String {
        invoiceNo?.isEmpty == false ? invoiceNo! : "Belge #\(id)"
    }

    var issueDate: String {
        date.isEmpty ? (createdAt ?? "Tarih belirtilmemiş") : date
    }

    var formattedAmount: String {
        String(format: "₺%.2f", totalAmount)
    }

    var formattedVat: String {
        String(format: "₺%.2f", totalKdv)
    }

    var isVehicleExpense: Bool {
        docType == "FUEL" || plate?.isEmpty == false
    }

    var isDuplicateFlag: Bool {
        isDuplicate == true
    }

    var isOutOfPeriodFlag: Bool {
        isOutOfPeriod == true
    }

    var typeSymbol: String {
        switch docType {
        case "SALES": return "arrow.up.right.circle.fill"
        case "FUEL": return "fuelpump.fill"
        case "RECEIPT": return "receipt.fill"
        default: return "doc.text.fill"
        }
    }

    var attentionReason: String? {
        if isDuplicateFlag {
            return duplicateReason ?? "Mükerrer Kayıt"
        }
        if isOutOfPeriodFlag {
            return fiscalPeriodWarning ?? "Dönem Dışı Belge"
        }
        if status == "PENDING_REVIEW" {
            return "İnceleme bekliyor"
        }
        return nil
    }

    var isPending: Bool {
        status == "PENDING_REVIEW"
    }

    var isApproved: Bool {
        status == "APPROVED" || status == "CONFIRMED"
    }

    var isRejected: Bool {
        status == "REJECTED"
    }

    var extractedFields: [ExtractedFieldModel] {
        var fields: [ExtractedFieldModel] = [
            ExtractedFieldModel(id: "1", title: "Fatura No", value: invoiceNumber, requiresAttention: invoiceNo?.isEmpty != false),
            ExtractedFieldModel(id: "2", title: "Toplam KDV", value: formattedVat, requiresAttention: false),
            ExtractedFieldModel(id: "3", title: "Genel Toplam", value: formattedAmount, requiresAttention: false)
        ]
        if let m20 = matrah20, m20 > 0 {
            fields.append(ExtractedFieldModel(id: "m20", title: "Matrah (%20)", value: String(format: "₺%.2f", m20), requiresAttention: false))
        }
        if let m10 = matrah10, m10 > 0 {
            fields.append(ExtractedFieldModel(id: "m10", title: "Matrah (%10)", value: String(format: "₺%.2f", m10), requiresAttention: false))
        }
        if let m1 = matrah1, m1 > 0 {
            fields.append(ExtractedFieldModel(id: "m1", title: "Matrah (%1)", value: String(format: "₺%.2f", m1), requiresAttention: false))
        }
        if let p = plate, !p.isEmpty {
            fields.append(ExtractedFieldModel(id: "plate", title: "Araç Plakası", value: p, requiresAttention: false))
        }
        if let cat = category, !cat.isEmpty {
            fields.append(ExtractedFieldModel(id: "cat", title: "Gider Kategorisi", value: cat, requiresAttention: false))
        }
        return fields
    }

    var timeline: [DocumentTimelineEvent] {
        var events = [
            DocumentTimelineEvent(id: "1", title: "Belge Yüklendi", detail: "\(userName ?? "Personel") tarafından yüklendi.", timestamp: createdAt ?? issueDate, symbol: "arrow.up.doc.fill"),
            DocumentTimelineEvent(id: "2", title: "Otomatik Ayrıştırma", detail: "KDV ve matrahlar %\(Int((ocrConfidence ?? 0.98) * 100)) doğrulukla ayrıştırıldı.", timestamp: issueDate, symbol: "doc.text.magnifyingglass")
        ]
        if isApproved {
            events.append(DocumentTimelineEvent(id: "3", title: "Onaylandı & Muhasebeleştirildi", detail: "Vergi matrah havuzuna dahil edildi.", timestamp: issueDate, symbol: "checkmark.seal.fill"))
        }
        return events
    }
}

struct ExtractedFieldModel: Identifiable, Hashable, Sendable {
    let id: String
    let title: String
    let value: String
    let requiresAttention: Bool
}

struct DocumentTimelineEvent: Identifiable, Hashable, Sendable {
    let id: String
    let title: String
    let detail: String
    let timestamp: String
    let symbol: String
}

struct DocumentCommentModel: Identifiable, Hashable, Sendable {
    let id: String
    let authorName: String
    let authorRole: String
    let text: String
}

struct VehicleModel: Identifiable, Decodable, Hashable, Sendable {
    let id: Int
    let tenantId: Int?
    let plate: String
    let brandModel: String?
    let assignedDriver: String?
    let monthlyFuelTotal: Double?
    let fuelInvoiceCount: Int?
    let gvk40_1_deductible: Double?
    let gvk40_1_kkeg: Double?
    let isGvkApplicable: Bool?
}

struct FinanceSummary: Decodable, Sendable {
    let tenantName: String?
    let tenantVkn: String?
    let taxRegime: String?
    let period: String?
    let monthName: String?
    let year: Int?
    let salesTotal: Double?
    let salesMatrah20: Double?
    let salesKdvTotal: Double?
    let salesCount: Int?
    let purchasesTotal: Double?
    let purchasesMatrah: Double?
    let purchasesKdvTotal: Double?
    let purchasesCount: Int?
    let vehicleTotal: Double?
    let vehicleGider70: Double?
    let vehicleKkeg30: Double?
    let vehicleKdv70: Double?
    let vehicleCount: Int?
    let staffTotal: Double?
    let staffCount: Int?
    let vatCalculated: Double?
    let vatDeductible: Double?
    let vatBalance: Double?
    let totalIncomeMatrah: Double?
    let totalDeductibleExpenses: Double?
    let grossProfit: Double?
    let corporateTaxEstimated: Double?
    let teknoparkAdvantage: Double?
}

struct DashboardMetrics: Decodable, Sendable {
    let salesTotal: Double?
    let expenseTotal: Double?
    let vatCalculated: Double?
    let vatDeductible: Double?
    let vatBalance: Double?
    let pendingCount: Int?
    let approvedCount: Int?
    let totalCount: Int?
}

struct QuotaData: Decodable, Sendable {
    let monthlyLimit: Int?
    let monthlyProcessed: Int?
    let percentage: Double?
    let planTier: String?
}

struct TenantOrg: Identifiable, Decodable, Hashable, Sendable {
    let id: Int
    let name: String
    let vkn: String?
    let taxOffice: String?
    let taxRegime: String?
    let planTier: String?
    let subscriptionStatus: String?
    let maxUsers: Int?
    let maxMonthlyInvoices: Int?
    let monthlyProcessedCount: Int?
    let telegramCode: String?
    let isActive: Bool?
    let role: String?
}

struct DashboardData: Decodable, Sendable {
    let organization: TenantOrg
    let metrics: DashboardMetrics
    let quota: QuotaData
    let recentDocuments: [BackendDocument]?
}

struct TeamMember: Identifiable, Decodable, Hashable, Sendable {
    let id: Int
    let membershipId: Int?
    let fullName: String
    let email: String
    let role: String
    let telegramUsername: String?
    let isActive: Bool?
    let lastActive: String?
}

struct UploadDocumentResponse: Decodable {
    let document: BackendDocument
    let message: String?
}

struct UploadBatchResponse: Decodable {
    let success: Bool
    let count: Int
    let documents: [BackendDocument]
    let message: String?
}

struct BulkConfirmResponse: Decodable {
    let success: Bool
    let confirmedCount: Int
    let message: String?
}

struct QuickCategoryResponse: Decodable {
    let success: Bool
    let document: BackendDocument
    let message: String?
}

// MARK: - Tax Radar DTOs
struct TaxRadarDTO: Decodable, Sendable {
    let tenantName: String
    let tenantVkn: String?
    let period: String
    let monthName: String
    let year: Int
    let month: Int
    let daysInMonth: Int
    let currentDay: Int
    let remainingDays: Int
    let velocity: Double
    let isCurrentMonth: Bool
    let metrics: TaxRadarMetricsDTO
    let recommendations: [String]
}

struct TaxRadarMetricsDTO: Decodable, Sendable {
    let sales: TaxRadarSalesDTO
    let expenses: TaxRadarExpensesDTO
    let fuel: TaxRadarFuelDTO
    let vat: TaxRadarVatDTO
    let projection: TaxRadarProjectionDTO
    let radar: TaxRadarScoreDTO
}

struct TaxRadarSalesDTO: Decodable, Sendable {
    let total: Double
    let kdvTotal: Double
    let count: Int
    let m20: Double?
    let k20: Double?
    let m10: Double?
    let k10: Double?
    let m1: Double?
    let k1: Double?
    let m0: Double?
}

struct TaxRadarExpensesDTO: Decodable, Sendable {
    let total: Double
    let kdvTotal: Double
    let count: Int
    let m20: Double?
    let k20: Double?
    let m10: Double?
    let k10: Double?
    let m1: Double?
    let k1: Double?
}

struct TaxRadarFuelDTO: Decodable, Sendable {
    let total: Double
    let kdvRaw: Double
    let matrah70: Double
    let kdv70: Double
    let matrah30Kkeg: Double
    let kdv30Kkeg: Double
    let totalKkeg: Double
    let count: Int
}

struct TaxRadarVatDTO: Decodable, Sendable {
    let calculated: Double
    let deductible: Double
    let balance: Double
    let status: String
}

struct TaxRadarProjectionDTO: Decodable, Sendable {
    let projectedSales: Double
    let projectedVatCalculated: Double
    let projectedVatDeductible: Double
    let projectedVatBalance: Double
    let projectedVatStatus: String
    let neutralizingExpenseNeeded: Double
}

struct TaxRadarScoreDTO: Decodable, Sendable {
    let riskScore: Int
    let riskLevel: String
    let riskBadge: String
}

// MARK: - End of Month Reminder DTO
struct MonthEndReminderStatusDTO: Decodable, Sendable {
    let targetDay: Int
    let currentDay: Int
    let daysRemaining: Int
    let isUrgent: Bool
    let totalTeamCount: Int
    let telegramConnectedCount: Int
    let botUsername: String
    let botInviteCode: String
    let nextScheduledDate: String
}

struct TriggerReminderResponse: Decodable, Sendable {
    let success: Bool
    let telegramSent: Int
    let emailSent: Int
    let totalRecipients: Int
    let message: String?
}

// MARK: - Journal Voucher Preview DTO
struct JournalPreviewDTO: Decodable, Sendable {
    let tenantId: Int
    let companyName: String
    let companyVkn: String?
    let period: String
    let totalVouchers: Int
    let totalDebit: Double
    let totalCredit: Double
    let isBalanced: Bool
    let entries: [JournalEntryDTO]
}

struct JournalEntryDTO: Identifiable, Decodable, Sendable {
    var id: String { voucherNo }
    let voucherNo: String
    let date: String
    let docType: String
    let description: String
    let lines: [JournalLineDTO]
}

struct JournalLineDTO: Identifiable, Decodable, Sendable {
    var id: String { "\(accountCode)-\(lineNo)" }
    let lineNo: Int
    let accountCode: String
    let accountName: String
    let debit: Double
    let credit: Double
    let explanation: String
}

// MARK: - Accountant Portfolio & Affiliate DTO
struct AccountantPortfolioDTO: Decodable, Sendable {
    let summary: AccountantPortfolioSummaryDTO
    let clients: [AccountantClientDTO]
}

struct AccountantPortfolioSummaryDTO: Decodable, Sendable {
    let totalClients: Int
    let totalDocuments: Int
    let totalPendingReviews: Int
    let totalNetKdvBalance: Double
}

struct AccountantClientDTO: Identifiable, Decodable, Sendable {
    let id: Int
    let name: String
    let vkn: String?
    let taxOffice: String?
    let taxRegime: String?
    let planTier: String?
    let role: String?
    let telegramCode: String?
    let documentCount: Int
    let pendingCount: Int
    let hesaplananKdv: Double
    let indirilecekKdv: Double
    let netKdvBalance: Double
}

struct AffiliateDashboardDTO: Decodable, Sendable {
    let affiliate: AffiliateProfileDTO
    let tierInfo: AffiliateTierInfoDTO
    let metrics: AffiliateMetricsDTO
}

struct AffiliateProfileDTO: Decodable, Sendable {
    let id: Int
    let referralCode: String
    let referralUrl: String
    let commissionRate: Double
    let commissionPercent: Int
    let tier: String
    let tierName: String
    let badgeColor: String?
}

struct AffiliateTierInfoDTO: Decodable, Sendable {
    let currentTier: String
    let tierName: String
    let rate: Double
    let percent: Int
    let activeCount: Int
    let nextTierName: String?
    let nextPercent: Int?
    let clientsToNext: Int?
    let progressPercent: Double?
    let allTiers: [AffiliateTierItemDTO]
}

struct AffiliateTierItemDTO: Identifiable, Decodable, Sendable {
    let id: String
    let name: String
    let range: String
    let percent: Int
    let rate: Double
    let isCurrent: Bool
}

struct AffiliateMetricsDTO: Decodable, Sendable {
    let totalReferredClients: Int
    let activePaidClients: Int
    let estimatedMonthlyCommissionTRY: Double
    let estimatedAnnualCommissionTRY: Double
    let pendingPayoutTRY: Double
    let totalEarnedTRY: Double
    let nextPayoutDate: String?
}

struct AppNotificationDTO: Identifiable, Decodable, Sendable {
    let id: String
    let title: String
    let message: String
    let type: String
    let isRead: Bool
    let createdAt: String
    enum CodingKeys: String, CodingKey { case id, title, subject, message, body, type, isRead, is_read, createdAt, created_at }
    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        if let stringID = try? values.decode(String.self, forKey: .id) { id = stringID }
        else if let intID = try? values.decode(Int.self, forKey: .id) { id = String(intID) }
        else { id = UUID().uuidString }
        title = (try? values.decode(String.self, forKey: .title)) ?? (try? values.decode(String.self, forKey: .subject)) ?? "Bildirim"
        message = (try? values.decode(String.self, forKey: .message)) ?? (try? values.decode(String.self, forKey: .body)) ?? ""
        type = (try? values.decode(String.self, forKey: .type)) ?? "INFO"
        isRead = (try? values.decode(Bool.self, forKey: .isRead)) ?? (try? values.decode(Bool.self, forKey: .is_read)) ?? false
        createdAt = (try? values.decode(String.self, forKey: .createdAt)) ?? (try? values.decode(String.self, forKey: .created_at)) ?? ""
    }
}

private struct NotificationsEnvelope: Decodable {
    let values: [AppNotificationDTO]
    enum CodingKeys: String, CodingKey { case notifications, items }
    init(from decoder: Decoder) throws {
        if let array = try? decoder.singleValueContainer().decode([AppNotificationDTO].self) { values = array; return }
        let container = try decoder.container(keyedBy: CodingKeys.self)
        values = (try? container.decode([AppNotificationDTO].self, forKey: .notifications)) ?? (try? container.decode([AppNotificationDTO].self, forKey: .items)) ?? []
    }
}

struct FiscalPeriodDTO: Identifiable, Decodable, Sendable, Hashable {
    let year: Int
    let month: Int
    let label: String

    var id: String { String(format: "%04d-%02d", year, month) }

    private enum CodingKeys: String, CodingKey { case year, month, label, name, period }

    init(from decoder: Decoder) throws {
        if let raw = try? decoder.singleValueContainer().decode(String.self),
           let parsed = Self.parse(raw) {
            year = parsed.year
            month = parsed.month
            label = raw
            return
        }

        let values = try decoder.container(keyedBy: CodingKeys.self)
        if let decodedYear = try? values.decode(Int.self, forKey: .year),
           let decodedMonth = try? values.decode(Int.self, forKey: .month) {
            year = decodedYear
            month = decodedMonth
            label = (try? values.decode(String.self, forKey: .label))
                ?? (try? values.decode(String.self, forKey: .name))
                ?? String(format: "%04d-%02d", decodedYear, decodedMonth)
            return
        }

        if let raw = (try? values.decode(String.self, forKey: .period)),
           let parsed = Self.parse(raw) {
            year = parsed.year
            month = parsed.month
            label = (try? values.decode(String.self, forKey: .label)) ?? raw
            return
        }
        throw DecodingError.dataCorrupted(.init(codingPath: decoder.codingPath, debugDescription: "Invalid fiscal period"))
    }

    private static func parse(_ raw: String) -> (year: Int, month: Int)? {
        let numbers = raw.split(whereSeparator: { !$0.isNumber }).compactMap { Int($0) }
        guard numbers.count >= 2 else { return nil }
        if numbers[0] > 1900, (1...12).contains(numbers[1]) { return (numbers[0], numbers[1]) }
        if numbers[1] > 1900, (1...12).contains(numbers[0]) { return (numbers[1], numbers[0]) }
        return nil
    }
}

private struct FiscalPeriodsEnvelope: Decodable {
    let values: [FiscalPeriodDTO]
    private enum CodingKeys: String, CodingKey { case periods, items }
    init(from decoder: Decoder) throws {
        if let array = try? decoder.singleValueContainer().decode([FiscalPeriodDTO].self) {
            values = array
            return
        }
        let container = try decoder.container(keyedBy: CodingKeys.self)
        values = (try? container.decode([FiscalPeriodDTO].self, forKey: .periods))
            ?? (try? container.decode([FiscalPeriodDTO].self, forKey: .items))
            ?? []
    }
}

// ==========================================
// REPOSITORY PROTOCOLS & IMPLEMENTATION
// ==========================================

protocol DocumentsRepository: Sendable {
    func listDocuments(organization: OrganizationContext, search: String?, docType: String?, status: String?) async throws -> [BackendDocument]
    func uploadDocument(organization: OrganizationContext, fileData: Data, filename: String, docType: String?, description: String?, plate: String?, isHarici: Bool) async throws -> BackendDocument
    func uploadBatch(organization: OrganizationContext, files: [(Data, String)], docType: String?, description: String?, plate: String?, isHarici: Bool) async throws -> [BackendDocument]
    func updateStatus(organization: OrganizationContext, docId: Int, status: String) async throws -> BackendDocument
    func quickUpdateCategory(organization: OrganizationContext, docId: Int, category: String, plate: String?, isHarici: Bool) async throws -> BackendDocument
    func bulkConfirm(organization: OrganizationContext, documentIds: [Int]?) async throws -> Int
    func deleteDocument(organization: OrganizationContext, docId: Int) async throws -> Bool
}

protocol FinanceRepository: Sendable {
    func loadFinanceSummary(organization: OrganizationContext, year: Int, month: Int) async throws -> FinanceSummary
    func fetchTaxRadar(organization: OrganizationContext, year: Int?, month: Int?) async throws -> TaxRadarDTO
    func fetchMonthEndReminderStatus(organization: OrganizationContext) async throws -> MonthEndReminderStatusDTO
    func triggerMonthEndReminder(organization: OrganizationContext) async throws -> TriggerReminderResponse
    func fetchJournalPreview(organization: OrganizationContext, year: Int?, month: Int?) async throws -> JournalPreviewDTO
}

protocol DashboardRepository: Sendable {
    func loadDashboard(organization: OrganizationContext) async throws -> DashboardData
}

protocol FleetRepository: Sendable {
    func loadVehicles(organization: OrganizationContext) async throws -> [VehicleModel]
    func addVehicle(organization: OrganizationContext, plate: String, model: String?) async throws -> VehicleModel
}

protocol WorkspaceRepository: Sendable {
    func loadTeam(organization: OrganizationContext) async throws -> [TeamMember]
    func inviteMember(organization: OrganizationContext, fullName: String, email: String, role: String) async throws -> TeamMember
    func fetchPortfolio(organization: OrganizationContext) async throws -> AccountantPortfolioDTO
    func fetchAffiliateDashboard(organization: OrganizationContext) async throws -> AffiliateDashboardDTO
}

actor LiveFeatureRepository: DocumentsRepository, FinanceRepository, DashboardRepository, FleetRepository, WorkspaceRepository {
    private let client: APIClient

    init(client: APIClient) {
        self.client = client
    }

    func fetchNotifications(organization: OrganizationContext) async throws -> [AppNotificationDTO] {
        let envelope: NotificationsEnvelope = try await client.send(
            APIEndpoint<NotificationsEnvelope>(path: "app/notifications", method: .get),
            organization: organization
        )
        return envelope.values
    }

    func fetchPeriods(organization: OrganizationContext) async throws -> [FiscalPeriodDTO] {
        let envelope: FiscalPeriodsEnvelope = try await client.send(
            APIEndpoint<FiscalPeriodsEnvelope>(path: "app/periods", method: .get),
            organization: organization
        )
        return envelope.values.sorted { ($0.year, $0.month) > ($1.year, $1.month) }
    }

    func regenerateTelegramCode(organization: OrganizationContext) async throws -> String {
        struct Response: Decodable {
            let telegramCode: String?
            let organization: TenantOrg?
            enum CodingKeys: String, CodingKey { case telegramCode, telegram_code, organization }
            init(from decoder: Decoder) throws {
                let values = try decoder.container(keyedBy: CodingKeys.self)
                telegramCode = (try? values.decode(String.self, forKey: .telegramCode))
                    ?? (try? values.decode(String.self, forKey: .telegram_code))
                organization = try? values.decode(TenantOrg.self, forKey: .organization)
            }
        }
        let response: Response = try await client.send(
            APIEndpoint<Response>(path: "app/organizations/regenerate-telegram-code", method: .post),
            organization: organization
        )
        return response.telegramCode ?? response.organization?.telegramCode ?? ""
    }

    func inviteAccountant(organization: OrganizationContext, email: String, name: String?, licenseNo: String?, message: String?) async throws {
        struct Response: Decodable {}
        var payload: [String: Any] = ["accountant_email": email]
        if let name, !name.isEmpty { payload["accountant_name"] = name }
        if let licenseNo, !licenseNo.isEmpty { payload["license_no"] = licenseNo }
        if let message, !message.isEmpty { payload["message"] = message }
        let body = try JSONSerialization.data(withJSONObject: payload)
        let _: Response = try await client.send(APIEndpoint<Response>(path: "app/settings/invite-accountant", method: .post, body: body), organization: organization)
    }

    func updateOrganization(organization: OrganizationContext, name: String, vkn: String, taxOffice: String, taxRegime: String) async throws {
        struct Response: Decodable {}
        let body = try JSONSerialization.data(withJSONObject: ["name": name, "vkn": vkn, "taxOffice": taxOffice, "taxRegime": taxRegime])
        let _: Response = try await client.send(APIEndpoint<Response>(path: "app/organizations/\(organization.organizationID)", method: .patch, body: body), organization: organization)
    }

    func linkOrganizationByCode(_ code: String) async throws {
        struct Response: Decodable {}
        let body = try JSONSerialization.data(withJSONObject: ["code": code])
        let _: Response = try await client.send(APIEndpoint<Response>(path: "app/organizations/link-by-code", method: .post, body: body))
    }
    func createOrganization(name: String, vkn: String, taxOffice: String, taxRegime: String) async throws {
        struct Response: Decodable {}
        let body = try JSONSerialization.data(withJSONObject: ["name": name, "vkn": vkn, "tax_office": taxOffice, "tax_regime": taxRegime])
        let _: Response = try await client.send(APIEndpoint<Response>(path: "app/organizations", method: .post, body: body))
    }
    func updatePayoutSettings(iban: String, bankName: String?, accountHolder: String?) async throws {
        struct Response: Decodable {}; var payload: [String: Any] = ["iban": iban]
        if let bankName, !bankName.isEmpty { payload["bank_name"] = bankName }; if let accountHolder, !accountHolder.isEmpty { payload["account_holder"] = accountHolder }
        let body = try JSONSerialization.data(withJSONObject: payload); let _: Response = try await client.send(APIEndpoint<Response>(path: "portal/musavir/payout-settings", method: .post, body: body))
    }
    func updateReferralCode(_ code: String) async throws {
        struct Response: Decodable {}; let body = try JSONSerialization.data(withJSONObject: ["referral_code": code]); let _: Response = try await client.send(APIEndpoint<Response>(path: "portal/musavir/custom-referral-code", method: .post, body: body))
    }

    func listDocuments(organization: OrganizationContext, search: String? = nil, docType: String? = nil, status: String? = nil) async throws -> [BackendDocument] {
        var queryItems: [URLQueryItem] = []
        if let search, !search.isEmpty { queryItems.append(URLQueryItem(name: "search", value: search)) }
        if let docType, !docType.isEmpty { queryItems.append(URLQueryItem(name: "doc_type", value: docType)) }
        if let status, !status.isEmpty { queryItems.append(URLQueryItem(name: "status_filter", value: status)) }

        return try await client.send(
            APIEndpoint<[BackendDocument]>(path: "app/documents", method: .get, query: queryItems),
            organization: organization
        )
    }

    func uploadDocument(
        organization: OrganizationContext,
        fileData: Data,
        filename: String,
        docType: String? = nil,
        description: String? = nil,
        plate: String? = nil,
        isHarici: Bool = false
    ) async throws -> BackendDocument {
        var formFields: [String: String] = [:]
        if let docType { formFields["doc_type_hint"] = docType }
        if let description { formFields["description"] = description }
        if let plate { formFields["plate"] = plate }
        formFields["is_harici"] = isHarici ? "true" : "false"

        let resp: UploadDocumentResponse = try await client.uploadMultipart(
            path: "app/documents",
            fileData: fileData,
            filename: filename,
            formFields: formFields,
            organization: organization
        )
        return resp.document
    }

    func uploadBatch(
        organization: OrganizationContext,
        files: [(Data, String)],
        docType: String? = nil,
        description: String? = nil,
        plate: String? = nil,
        isHarici: Bool = false
    ) async throws -> [BackendDocument] {
        var formFields: [String: String] = [:]
        formFields["document_type"] = docType ?? "EXPENSE"
        if let description { formFields["description"] = description }
        if let plate { formFields["plate"] = plate }
        formFields["is_harici"] = isHarici ? "true" : "false"

        let filePayloads = files.map { (data: $0.0, filename: $0.1, formKey: "files") }
        let resp: UploadBatchResponse = try await client.uploadMultipartBatch(
            path: "app/documents/batch",
            files: filePayloads,
            formFields: formFields,
            organization: organization
        )
        return resp.documents
    }

    func updateStatus(organization: OrganizationContext, docId: Int, status: String) async throws -> BackendDocument {
        let bodyData = try JSONSerialization.data(withJSONObject: ["status": status])
        return try await client.send(
            APIEndpoint<BackendDocument>(path: "app/documents/\(docId)/status", method: .patch, body: bodyData),
            organization: organization
        )
    }

    func quickUpdateCategory(organization: OrganizationContext, docId: Int, category: String, plate: String? = nil, isHarici: Bool = false) async throws -> BackendDocument {
        var payload: [String: Any] = [
            "category": category,
            "is_harici": isHarici
        ]
        if let plate { payload["plate"] = plate }
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        let resp: QuickCategoryResponse = try await client.send(
            APIEndpoint<QuickCategoryResponse>(path: "app/documents/\(docId)/quick-category", method: .patch, body: bodyData),
            organization: organization
        )
        return resp.document
    }

    func bulkConfirm(organization: OrganizationContext, documentIds: [Int]? = nil) async throws -> Int {
        var payload: [String: Any] = [:]
        if let documentIds { payload["document_ids"] = documentIds }
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        let resp: BulkConfirmResponse = try await client.send(
            APIEndpoint<BulkConfirmResponse>(path: "app/documents/bulk-confirm", method: .post, body: bodyData),
            organization: organization
        )
        return resp.confirmedCount
    }

    func deleteDocument(organization: OrganizationContext, docId: Int) async throws -> Bool {
        struct DeleteResponse: Decodable { let success: Bool }
        let res: DeleteResponse = try await client.send(
            APIEndpoint<DeleteResponse>(path: "app/documents/\(docId)", method: .delete),
            organization: organization
        )
        return res.success
    }

    func loadFinanceSummary(organization: OrganizationContext, year: Int, month: Int) async throws -> FinanceSummary {
        let query = [URLQueryItem(name: "year", value: String(year)), URLQueryItem(name: "month", value: String(month))]
        return try await client.send(
            APIEndpoint<FinanceSummary>(path: "app/finance/summary", method: .get, query: query),
            organization: organization
        )
    }

    func loadDashboard(organization: OrganizationContext) async throws -> DashboardData {
        return try await client.send(
            APIEndpoint<DashboardData>(path: "app/dashboard", method: .get),
            organization: organization
        )
    }

    func loadVehicles(organization: OrganizationContext) async throws -> [VehicleModel] {
        return try await client.send(
            APIEndpoint<[VehicleModel]>(path: "app/fleet", method: .get),
            organization: organization
        )
    }

    func addVehicle(organization: OrganizationContext, plate: String, model: String?) async throws -> VehicleModel {
        let payload: [String: Any] = ["plate": plate.uppercased(), "model": model ?? "Şirket Binek Aracı"]
        let body = try JSONSerialization.data(withJSONObject: payload)
        return try await client.send(
            APIEndpoint<VehicleModel>(path: "app/fleet", method: .post, body: body),
            organization: organization
        )
    }

    func loadTeam(organization: OrganizationContext) async throws -> [TeamMember] {
        return try await client.send(
            APIEndpoint<[TeamMember]>(path: "app/team", method: .get),
            organization: organization
        )
    }

    func inviteMember(organization: OrganizationContext, fullName: String, email: String, role: String) async throws -> TeamMember {
        let payload: [String: Any] = ["full_name": fullName, "email": email, "role": role]
        let body = try JSONSerialization.data(withJSONObject: payload)
        return try await client.send(
            APIEndpoint<TeamMember>(path: "app/team/invite", method: .post, body: body),
            organization: organization
        )
    }

    func fetchTaxRadar(organization: OrganizationContext, year: Int? = nil, month: Int? = nil) async throws -> TaxRadarDTO {
        var query: [URLQueryItem] = []
        if let year { query.append(URLQueryItem(name: "year", value: String(year))) }
        if let month { query.append(URLQueryItem(name: "month", value: String(month))) }
        return try await client.send(
            APIEndpoint<TaxRadarDTO>(path: "app/finance/tax-radar", method: .get, query: query),
            organization: organization
        )
    }

    func fetchMonthEndReminderStatus(organization: OrganizationContext) async throws -> MonthEndReminderStatusDTO {
        return try await client.send(
            APIEndpoint<MonthEndReminderStatusDTO>(path: "app/reminders/month-end-status", method: .get),
            organization: organization
        )
    }

    func triggerMonthEndReminder(organization: OrganizationContext) async throws -> TriggerReminderResponse {
        return try await client.send(
            APIEndpoint<TriggerReminderResponse>(path: "app/reminders/trigger-month-end", method: .post),
            organization: organization
        )
    }

    func fetchJournalPreview(organization: OrganizationContext, year: Int? = nil, month: Int? = nil) async throws -> JournalPreviewDTO {
        var query: [URLQueryItem] = []
        if let year { query.append(URLQueryItem(name: "year", value: String(year))) }
        if let month { query.append(URLQueryItem(name: "month", value: String(month))) }
        return try await client.send(
            APIEndpoint<JournalPreviewDTO>(path: "app/export/journal-preview", method: .get, query: query),
            organization: organization
        )
    }

    func fetchPortfolio(organization: OrganizationContext) async throws -> AccountantPortfolioDTO {
        return try await client.send(
            APIEndpoint<AccountantPortfolioDTO>(path: "app/accountant/portfolio-summary", method: .get),
            organization: organization
        )
    }

    func fetchAffiliateDashboard(organization: OrganizationContext) async throws -> AffiliateDashboardDTO {
        return try await client.send(
            APIEndpoint<AffiliateDashboardDTO>(path: "app/accountant/affiliate", method: .get),
            organization: organization
        )
    }
}
