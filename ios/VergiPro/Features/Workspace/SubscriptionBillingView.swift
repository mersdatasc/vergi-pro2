import SwiftUI

struct SubscriptionBillingView: View {
    @EnvironmentObject private var appState: AppState
    @State private var billingData: BillingInfo?
    @State private var isLoading = true
    @State private var errorMessage: String?

    private var currentTier: String? {
        billingData?.planTier ?? appState.currentOrganization?.planTier
    }

    var body: some View {
        ScrollView {
            VStack(spacing: VPSpace.lg) {
                if isLoading && billingData == nil {
                    ProgressView(String(localized: "billing.loading"))
                        .padding(.top, VPSpace.xl)
                } else {
                    currentPlanCard
                    usageCard
                    revenueCatNotice
                }
            }
            .padding(VPSpace.md)
            .padding(.bottom, VPSpace.xxl)
        }
        .background(VPColor.canvas)
        .navigationTitle(String(localized: "billing.title"))
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await loadBillingInfo()
        }
        .refreshable {
            await loadBillingInfo()
        }
        .alert(String(localized: "common.error"), isPresented: Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )) {
            Button(String(localized: "common.ok"), role: .cancel) { errorMessage = nil }
        } message: {
            Text(errorMessage ?? "")
        }
    }

    private var currentPlanCard: some View {
        VStack(alignment: .leading, spacing: VPSpace.sm) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(String(localized: "billing.current"))
                        .font(.caption2.bold())
                        .foregroundStyle(VPColor.secondaryText)
                    Text(tierDisplayName(currentTier))
                        .font(.title2.bold())
                        .foregroundStyle(.primary)
                }
                Spacer()
                statusBadge
            }

            Divider()

            HStack(spacing: VPSpace.md) {
                Label(String(localized: "billing.period.backend_managed"), systemImage: "calendar")
                    .font(.caption)
                    .foregroundStyle(VPColor.secondaryText)
                Spacer()
                Label(String(localized: "billing.source.backend"), systemImage: "checkmark.seal.fill")
                    .font(.caption)
                    .foregroundStyle(VPColor.success)
            }
        }
        .vpCard()
    }

    private var statusBadge: some View {
        let status = billingData?.subscriptionStatus ?? appState.currentOrganization?.subscriptionStatus
        let isTrial = status == "TRIALING"
        let isActive = status == "ACTIVE"
        return Text(isTrial ? String(localized: "billing.status.trial") : (isActive ? String(localized: "billing.status.active") : String(localized: "billing.status.unknown")))
            .font(.caption.bold())
            .foregroundStyle(isTrial ? Color.orange : VPColor.success)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background((isTrial ? Color.orange : VPColor.success).opacity(0.15), in: Capsule())
    }

    private var usageCard: some View {
        VStack(alignment: .leading, spacing: VPSpace.sm) {
            Text(String(localized: "billing.usage.title"))
                .font(.caption2.bold())
                .foregroundStyle(VPColor.secondaryText)

            let usage = billingData?.usage
            let processed = usage?.monthlyProcessedCount ?? appState.currentOrganization?.monthlyProcessedCount
            let maxInvoices = usage?.maxMonthlyInvoices ?? appState.currentOrganization?.maxMonthlyInvoices
            let progress: Double? = if let processed, let maxInvoices {
                min(1.0, Double(processed) / Double(max(1, maxInvoices)))
            } else {
                nil
            }

            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(String(localized: "billing.usage.documents"))
                        .font(.subheadline.bold())
                    Spacer()
                    Text(usageCount(processed: processed, limit: maxInvoices))
                        .font(.subheadline.monospacedDigit())
                        .foregroundStyle(VPColor.secondaryText)
                }

                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule().fill(VPColor.cardBorder)
                        if let progress {
                            Capsule()
                                .fill(progress > 0.8 ? VPColor.danger : VPColor.brand)
                                .frame(width: max(8, geo.size.width * CGFloat(progress)))
                        }
                    }
                }
                .frame(height: 8)

                Text(usageDetail(processed: processed, limit: maxInvoices, progress: progress))
                    .font(.caption2)
                    .foregroundStyle(progress == 1.0 ? VPColor.danger : VPColor.secondaryText)
            }

            Divider()

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(String(localized: "billing.usage.users"))
                        .font(.caption)
                        .foregroundStyle(VPColor.secondaryText)
                    Text(usage?.maxUsers.map { String(format: String(localized: "billing.usage.user_count"), $0) } ?? "—")
                        .font(.headline)
                }
                Spacer()
                VStack(alignment: .leading, spacing: 2) {
                    Text(String(localized: "billing.usage.telegram"))
                        .font(.caption)
                        .foregroundStyle(VPColor.secondaryText)
                    Text(String(localized: "billing.usage.backend_scope"))
                        .font(.headline)
                        .foregroundStyle(VPColor.secondaryText)
                }
            }
        }
        .vpCard()
    }

    private var revenueCatNotice: some View {
        Label(String(localized: "billing.revenuecat.notice"), systemImage: "lock.shield")
            .font(.subheadline)
            .foregroundStyle(VPColor.secondaryText)
            .vpCard()
    }

    private func tierDisplayName(_ tier: String?) -> String {
        switch tier?.uppercased() {
        case "STARTER": return String(localized: "billing.plan.starter")
        case "PRO": return String(localized: "billing.plan.pro")
        case "OFFICE": return String(localized: "billing.plan.office")
        case "ENTERPRISE": return String(localized: "billing.plan.enterprise")
        case .some(let tier): return tier
        case .none: return String(localized: "billing.plan.unknown")
        }
    }

    private func usageCount(processed: Int?, limit: Int?) -> String {
        guard let processed else { return String(localized: "billing.usage.unavailable") }
        guard let limit else {
            return String(format: String(localized: "billing.usage.count_unknown"), processed)
        }
        return String(format: String(localized: "billing.usage.count"), processed, limit)
    }

    private func usageDetail(processed: Int?, limit: Int?, progress: Double?) -> String {
        guard let processed, let limit, let progress else {
            return String(localized: "billing.usage.unavailable")
        }
        return progress >= 1.0
            ? String(localized: "billing.usage.limit_reached")
            : String(format: String(localized: "billing.usage.remaining"), max(0, limit - processed))
    }

    private func loadBillingInfo() async {
        isLoading = true
        defer { isLoading = false }
        do {
            let token = await SessionController.shared.currentAccessToken() ?? ""
            let configuration = try AppConfiguration.load()
            let url = configuration.apiBaseURL.appendingPathComponent("app/settings/billing")
            var request = URLRequest(url: url, timeoutInterval: 20)
            if !token.isEmpty {
                request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            }
            if let orgId = appState.currentOrganization?.id {
                request.setValue(orgId, forHTTPHeaderField: "X-Organization-ID")
            }
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
                errorMessage = String(localized: "billing.error.load")
                return
            }
            self.billingData = try JSONDecoder().decode(BillingInfo.self, from: data)
        } catch {
            errorMessage = String(localized: "billing.error.load")
        }
    }
}

// MARK: - Models
struct BillingInfo: Decodable {
    let organizationId: Int?
    let organizationName: String?
    let planTier: String?
    let subscriptionStatus: String?
    let usage: BillingUsage?

    struct BillingUsage: Decodable {
        let monthlyProcessedCount: Int?
        let maxMonthlyInvoices: Int?
        let invoiceUsagePct: Double?
        let maxUsers: Int?
    }
}
