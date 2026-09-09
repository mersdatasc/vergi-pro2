import Foundation
import Combine

struct FinanceSummaryDTO: Codable, Sendable {
    let period: String?
    let currency: String
    let completenessPercent: Int?
    let completenessStatus: String?
    let salesTotalMinor: Int64
    let expenseTotalMinor: Int64
    let vatCalculatedMinor: Int64
    let vatDeductibleMinor: Int64
    let vatBalanceMinor: Int64
    let vatStatus: String
    let vehicleExpenseTotalMinor: Int64
    let vehicleDeductibleMinor: Int64
    let vehicleKkegMinor: Int64
    let estimatedTaxMinor: Int64
    let missingDocumentsCount: Int?
    let taxRuleVersion: String?
    let monthlyFlow: [MonthlyFlowItem]

    struct MonthlyFlowItem: Codable, Sendable, Identifiable {
        var id: String { monthKey }
        let monthKey: String
        let monthName: String
        let incomeMinor: Int64
        let expenseMinor: Int64
    }
}

@MainActor
final class FinanceViewModel: ObservableObject {
    @Published var summary: FinanceSummaryDTO?
    @Published var taxRadar: TaxRadarDTO?
    @Published var reminderStatus: MonthEndReminderStatusDTO?
    @Published var periods: [FiscalPeriodDTO] = []
    @Published var selectedPeriod: FiscalPeriodDTO?
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let repository: LiveFeatureRepository?

    init() {
        if let configuration = try? AppConfiguration.load() {
            repository = LiveFeatureRepository(client: APIClient(baseURL: configuration.apiBaseURL))
        } else {
            repository = nil
        }
    }

    func formatCurrency(_ minorUnits: Int64) -> String {
        let amount = Decimal(minorUnits) / 100
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = summary?.currency ?? "TRY"
        formatter.locale = .current
        return formatter.string(from: NSNumber(value: NSDecimalNumber(decimal: amount).doubleValue)) ?? "₺\(amount)"
    }

    func fetchFinanceData(organizationId: String, year: Int? = nil, month: Int? = nil) async {
        guard let repository, let orgID = Int(organizationId) else { return }
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        let context = OrganizationContext(organizationID: orgID, membershipID: 0)
        let targetYear = year ?? selectedPeriod?.year ?? Calendar.current.component(.year, from: Date())
        let targetMonth = month ?? selectedPeriod?.month ?? Calendar.current.component(.month, from: Date())

        if periods.isEmpty {
            if let loadedPeriods = try? await repository.fetchPeriods(organization: context) {
                periods = loadedPeriods
                if selectedPeriod == nil { selectedPeriod = loadedPeriods.first }
            }
        }

        let effectiveYear = year ?? selectedPeriod?.year ?? targetYear
        let effectiveMonth = month ?? selectedPeriod?.month ?? targetMonth

        async let backendSummary = repository.loadFinanceSummary(organization: context, year: effectiveYear, month: effectiveMonth)
        async let loadedRadar = repository.fetchTaxRadar(organization: context, year: effectiveYear, month: effectiveMonth)
        async let loadedReminders = repository.fetchMonthEndReminderStatus(organization: context)

        do {
            let backend = try await backendSummary
            summary = FinanceSummaryDTO(backend: backend)
        } catch {
            self.errorMessage = String(localized: "finance.error.load")
        }

        do {
            taxRadar = try await loadedRadar
        } catch {
            print("Tax radar fetch error: \(error)")
        }

        do {
            reminderStatus = try await loadedReminders
        } catch {
            print("Reminder status fetch error: \(error)")
        }
    }

    func selectPeriod(_ period: FiscalPeriodDTO, organizationId: String) async {
        selectedPeriod = period
        await fetchFinanceData(organizationId: organizationId, year: period.year, month: period.month)
    }

    func triggerMonthEndReminder(organizationId: String) async -> TriggerReminderResponse? {
        guard let repository, let orgID = Int(organizationId) else { return nil }
        do {
            return try await repository.triggerMonthEndReminder(
                organization: OrganizationContext(organizationID: orgID, membershipID: 0)
            )
        } catch {
            print("Trigger reminder error: \(error)")
            return nil
        }
    }

    func fetchJournalPreview(organizationId: String) async -> JournalPreviewDTO? {
        guard let repository, let orgID = Int(organizationId) else { return nil }
        do {
            return try await repository.fetchJournalPreview(
                organization: OrganizationContext(organizationID: orgID, membershipID: 0),
                year: selectedPeriod?.year,
                month: selectedPeriod?.month
            )
        } catch {
            print("Journal preview error: \(error)")
            return nil
        }
    }

}

private extension FinanceSummaryDTO {
    init(backend: FinanceSummary) {
        self.init(
            period: backend.period,
            currency: "TRY",
            completenessPercent: nil,
            completenessStatus: nil,
            salesTotalMinor: Int64(((backend.salesTotal ?? 0) * 100).rounded()),
            expenseTotalMinor: Int64(((backend.totalDeductibleExpenses ?? 0) * 100).rounded()),
            vatCalculatedMinor: Int64(((backend.vatCalculated ?? 0) * 100).rounded()),
            vatDeductibleMinor: Int64(((backend.vatDeductible ?? 0) * 100).rounded()),
            vatBalanceMinor: Int64(((backend.vatBalance ?? 0) * 100).rounded()),
            vatStatus: (backend.vatBalance ?? 0) >= 0 ? "Ödenecek" : "Devreden",
            vehicleExpenseTotalMinor: Int64(((backend.vehicleTotal ?? 0) * 100).rounded()),
            vehicleDeductibleMinor: Int64(((backend.vehicleGider70 ?? 0) * 100).rounded()),
            vehicleKkegMinor: Int64(((backend.vehicleKkeg30 ?? 0) * 100).rounded()),
            estimatedTaxMinor: Int64(((backend.corporateTaxEstimated ?? 0) * 100).rounded()),
            missingDocumentsCount: nil,
            taxRuleVersion: nil,
            monthlyFlow: []
        )
    }
}
