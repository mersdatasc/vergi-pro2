import Foundation
import SwiftUI

struct MobileBootstrapResponse: Decodable, Sendable {
    let apiVersion: String
    let serviceStatus: String
    let minimumAppVersion: String
    let latestAppVersion: String
    let enabledCapabilities: [String]
    let serverTime: String

    enum CodingKeys: String, CodingKey {
        case apiVersion = "api_version"
        case serviceStatus = "service_status"
        case minimumAppVersion = "minimum_app_version"
        case latestAppVersion = "latest_app_version"
        case enabledCapabilities = "enabled_capabilities"
        case serverTime = "server_time"
    }
}

struct DashboardActionItem: Identifiable, Sendable {
    let id: String
    let symbol: String
    let title: String
    let subtitle: String
    let tint: Color
    let destination: AppDestination
}

@MainActor
final class HomeViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var dashboardData: DashboardData?
    @Published var taxRadar: TaxRadarDTO?
    @Published var reminderStatus: MonthEndReminderStatusDTO?
    @Published var periodHealthPercent: Int = 0
    @Published var pendingTasksCount: Int = 0
    @Published var actionItems: [DashboardActionItem] = []
    @Published var lastSyncTime: Date?
    @Published var errorMessage: String?
    @Published var notifications: [AppNotificationDTO] = []

    private let repository: LiveFeatureRepository?
    private var activeOrganizationID: Int?

    init() {
        if let configuration = try? AppConfiguration.load() {
            repository = LiveFeatureRepository(client: APIClient(baseURL: configuration.apiBaseURL))
        } else {
            repository = nil
        }
    }

    func refreshDashboard(organizationID: Int?) async {
        guard !isLoading else { return }
        if activeOrganizationID != organizationID {
            activeOrganizationID = organizationID
            dashboardData = nil
            taxRadar = nil
            reminderStatus = nil
            actionItems = []
            pendingTasksCount = 0
            periodHealthPercent = 0
        }
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        guard let repository, let organizationID else { return }
        let context = OrganizationContext(organizationID: organizationID, membershipID: 0)

        async let dashTask = repository.loadDashboard(organization: context)
        async let radarTask = repository.fetchTaxRadar(organization: context, year: nil, month: nil)
        async let reminderTask = repository.fetchMonthEndReminderStatus(organization: context)
        async let notificationsTask = repository.fetchNotifications(organization: context)

        do {
            let dashboard = try await dashTask
            dashboardData = dashboard
            pendingTasksCount = dashboard.metrics.pendingCount ?? 0
            let total = max(dashboard.metrics.totalCount ?? 0, 1)
            periodHealthPercent = Int((Double(dashboard.metrics.approvedCount ?? 0) / Double(total)) * 100)
            actionItems = [
                DashboardActionItem(id: "pending", symbol: "doc.badge.ellipsis", title: String(localized: "home.action.pending.title"), subtitle: String(format: String(localized: "home.action.pending.subtitle"), pendingTasksCount), tint: VPColor.warning, destination: .documents),
                DashboardActionItem(id: "vat", symbol: "chart.line.uptrend.xyaxis", title: String(localized: "home.action.vat.title"), subtitle: String(localized: "home.action.vat.subtitle"), tint: VPColor.accentBlue, destination: .finance),
                DashboardActionItem(id: "quota", symbol: "chart.bar.fill", title: String(localized: "home.action.quota.title"), subtitle: String(format: String(localized: "home.action.quota.subtitle"), Int(dashboard.quota.percentage ?? 0)), tint: VPColor.accentBlue, destination: .workspace),
            ]
            lastSyncTime = Date()
        } catch {
            errorMessage = String(localized: "home.error.dashboard")
        }

        do {
            taxRadar = try await radarTask
        } catch {
            errorMessage = errorMessage ?? String(localized: "home.error.tax_radar")
        }

        do {
            reminderStatus = try await reminderTask
        } catch {
            errorMessage = errorMessage ?? String(localized: "home.error.reminder")
        }
        notifications = (try? await notificationsTask) ?? []
    }

    func triggerMonthEndReminder(organizationID: Int?) async -> TriggerReminderResponse? {
        guard let repository, let organizationID else { return nil }
        do {
            return try await repository.triggerMonthEndReminder(
                organization: OrganizationContext(organizationID: organizationID, membershipID: 0)
            )
        } catch {
            return nil
        }
    }
}
