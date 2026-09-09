import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = HomeViewModel()
    @State private var showsNotifications = false

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: VPSpace.lg) {
                    topExecutiveHeader

                    if let errorMessage = viewModel.errorMessage {
                        HStack(spacing: VPSpace.sm) {
                            Image(systemName: "exclamationmark.triangle.fill")
                            Text(errorMessage).font(.footnote)
                            Spacer()
                            Button(String(localized: "common.refresh")) {
                                Task { await viewModel.refreshDashboard(organizationID: Int(appState.currentOrganization?.id ?? "")) }
                            }
                            .font(.footnote.weight(.semibold))
                        }
                        .foregroundStyle(VPColor.danger)
                        .padding(VPSpace.sm)
                        .background(VPColor.danger.opacity(0.08), in: RoundedRectangle(cornerRadius: 12))
                    }
                    keyMetricsGrid

                    // 1. Canlı KDV & Vergi Radarı
                    TaxRadarCard(
                        radarData: viewModel.taxRadar,
                        isLoading: viewModel.isLoading,
                        onRefresh: {
                            Task { await viewModel.refreshDashboard(organizationID: Int(appState.currentOrganization?.id ?? "")) }
                        }
                    )

                    // 2. Saha Masraf Toplama Radarı
                    MonthEndReminderCard(
                        reminderStatus: viewModel.reminderStatus,
                        onTriggerReminder: {
                            await viewModel.triggerMonthEndReminder(organizationID: Int(appState.currentOrganization?.id ?? ""))
                        },
                        onNavigateToCapture: {
                            appState.selectedDestination = .capture
                        }
                    )

                    // 3. Quick Scan CTA
                    quickScanCard

                    // 4. Action Items & Quota
                    pendingActions
                }
                .padding(.horizontal, VPSpace.md)
                .padding(.top, VPSpace.xs)
                .padding(.bottom, VPSpace.xxl)
            }
            .refreshable {
                await viewModel.refreshDashboard(organizationID: Int(appState.currentOrganization?.id ?? ""))
            }
            .background(VPColor.canvas)
            .toolbar(.hidden, for: .navigationBar)
            .navigationBarHidden(true)
        }
        .task(id: appState.currentOrganization?.id) {
            await viewModel.refreshDashboard(organizationID: Int(appState.currentOrganization?.id ?? ""))
        }
    }

    private var topExecutiveHeader: some View {
        VStack(alignment: .leading, spacing: VPSpace.md) {
            // Row 1: Brand Mark & Executive Notifications
            HStack(alignment: .center) {
                HStack(spacing: 10) {
                    Image("VPMark")
                        .resizable()
                        .interpolation(.high)
                        .scaledToFit()
                        .frame(width: 30, height: 30)

                    Text("VergiPro")
                        .font(.system(size: 20, weight: .bold, design: .default))
                        .foregroundStyle(VPColor.textPrimary)
                        .tracking(-0.4)
                }

                Spacer()

                Button(action: { showsNotifications = true }) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .fill(VPColor.surface)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .strokeBorder(VPColor.cardBorder, lineWidth: 1)
                            )

                        Image(systemName: "bell")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(VPColor.textPrimary)
                        if viewModel.notifications.contains(where: { !$0.isRead }) {
                            Circle().fill(VPColor.danger).frame(width: 7, height: 7).offset(x: 10, y: -10)
                        }
                    }
                    .frame(width: 38, height: 38)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(String(localized: "home.notifications"))
                .sheet(isPresented: $showsNotifications) {
                    NavigationStack {
                        Group {
                            if viewModel.notifications.isEmpty {
                                ContentUnavailableView(String(localized: "home.notifications.empty"), systemImage: "bell.slash")
                            } else {
                                List(viewModel.notifications) { notification in
                                    VStack(alignment: .leading, spacing: 5) {
                                        Text(notification.title).font(.headline)
                                        if !notification.message.isEmpty { Text(notification.message).font(.subheadline).foregroundStyle(VPColor.secondaryText) }
                                        if !notification.createdAt.isEmpty { Text(notification.createdAt).font(.caption).foregroundStyle(.tertiary) }
                                    }.padding(.vertical, 4)
                                }
                            }
                        }
                        .navigationTitle(String(localized: "home.notifications"))
                        .toolbar { ToolbarItem(placement: .topBarTrailing) { Button(String(localized: "common.close")) { showsNotifications = false } } }
                    }
                    .presentationDetents([.medium, .large])
                }
            }
            .padding(.top, VPSpace.xxs)

            // Row 2: Company Overview & Regime Badge
            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .center) {
                    Text(String(localized: "home.overview"))
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(VPColor.secondaryText)
                        .tracking(1.0)

                    Spacer()

                    if let regime = viewModel.dashboardData?.organization.taxRegime, !regime.isEmpty {
                        Text(regime)
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(VPColor.accentBlue)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(VPColor.accentBlue.opacity(0.12), in: Capsule())
                    }
                }

                Text(appState.currentOrganization?.name ?? String(localized: "home.company_placeholder"))
                    .font(.system(size: 24, weight: .bold, design: .default))
                    .tracking(-0.6)
                    .foregroundStyle(VPColor.textPrimary)
                    .lineLimit(1)

                if let vkn = viewModel.dashboardData?.organization.vkn, !vkn.isEmpty {
                    Text(String(format: String(localized: "home.vkn"), vkn))
                        .font(.caption.monospaced())
                        .foregroundStyle(VPColor.secondaryText)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var keyMetricsGrid: some View {
        Group {
            if viewModel.dashboardData == nil && viewModel.taxRadar == nil {
                HStack(spacing: VPSpace.sm) {
                    if viewModel.isLoading { ProgressView() }
                    Text(viewModel.isLoading ? String(localized: "home.metrics.loading") : String(localized: "home.metrics.unavailable"))
                        .font(.subheadline)
                        .foregroundStyle(VPColor.secondaryText)
                    Spacer()
                }
                .vpCard()
            } else {
                metricsTiles
            }
        }
    }

    private var metricsTiles: some View {
        LazyVGrid(columns: [.init(.flexible()), .init(.flexible())], spacing: VPSpace.sm) {
            let salesTotal = viewModel.taxRadar?.metrics.sales.total ?? (viewModel.dashboardData?.metrics.salesTotal ?? 0)
            statTile(
                title: String(localized: "home.metrics.revenue"),
                value: salesTotal.formattedTRYCompact,
                caption: String(format: String(localized: "home.metrics.sales_count"), viewModel.taxRadar?.metrics.sales.count ?? 0),
                icon: "chart.line.uptrend.xyaxis",
                tint: VPColor.brand,
                action: { appState.selectedDestination = .finance }
            )

            let expTotal = viewModel.taxRadar?.metrics.expenses.total ?? (viewModel.dashboardData?.metrics.expenseTotal ?? 0)
            statTile(
                title: String(localized: "home.metrics.expense"),
                value: expTotal.formattedTRYCompact,
                caption: String(format: String(localized: "home.metrics.expense_count"), viewModel.taxRadar?.metrics.expenses.count ?? 0),
                icon: "creditcard.fill",
                tint: VPColor.brand,
                action: { appState.selectedDestination = .documents }
            )

            let vatBal = viewModel.taxRadar?.metrics.vat.balance ?? (viewModel.dashboardData?.metrics.vatBalance ?? 0)
            statTile(
                title: vatBal > 0 ? String(localized: "home.metrics.vat_payable") : String(localized: "home.metrics.vat_carryover"),
                value: abs(vatBal).formattedTRY,
                caption: vatBal > 0 ? String(localized: "home.metrics.vat_difference") : String(localized: "home.metrics.next_period"),
                icon: "chart.bar.xaxis",
                tint: vatBal > 0 ? VPColor.danger : VPColor.success,
                action: { appState.selectedDestination = .finance }
            )

            let pending = viewModel.pendingTasksCount
            statTile(
                title: String(localized: "home.metrics.pending"),
                value: String(format: String(localized: "home.metrics.document_count"), pending),
                caption: pending > 0 ? String(format: String(localized: "home.metrics.pending_count"), pending) : String(localized: "home.metrics.all_approved"),
                icon: "doc.badge.ellipsis",
                tint: pending > 0 ? VPColor.warning : VPColor.success,
                action: { appState.selectedDestination = .documents }
            )
        }
    }

    private func statTile(title: String, value: String, caption: String, icon: String, tint: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(title)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(VPColor.secondaryText)
                    Spacer()
                    Image(systemName: icon)
                        .font(.caption)
                        .foregroundStyle(tint)
                }

                Text(value)
                    .font(.title3.bold().monospacedDigit())
                    .foregroundStyle(VPColor.textPrimary)
                    .minimumScaleFactor(0.75)
                    .lineLimit(1)

                Text(caption)
                    .font(.caption2)
                    .foregroundStyle(tint)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .vpCard()
        }
        .buttonStyle(.plain)
    }

    private var quickScanCard: some View {
        Button {
            appState.selectedDestination = .capture
        } label: {
            HStack(spacing: VPSpace.md) {
                SurfaceIcon(icon: "viewfinder", color: VPColor.brand, background: VPColor.brand.opacity(0.12))

                VStack(alignment: .leading, spacing: 2) {
                    Text(String(localized: "home.quick_scan.title"))
                        .font(.headline)
                        .foregroundStyle(VPColor.textPrimary)
                    Text(String(localized: "home.quick_scan.subtitle"))
                        .font(.subheadline)
                        .foregroundStyle(VPColor.secondaryText)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.subheadline.bold())
                    .foregroundStyle(VPColor.secondaryText)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .vpCard()
    }

    private var pendingActions: some View {
        VStack(alignment: .leading, spacing: VPSpace.sm) {
            Text(String(localized: "home.pending"))
                .font(.title3.weight(.bold))
                .foregroundStyle(VPColor.textPrimary)
                .padding(.top, VPSpace.xs)

            ForEach(viewModel.actionItems) { item in
                Button {
                    appState.selectedDestination = item.destination
                } label: {
                    HStack(spacing: VPSpace.md) {
                        SurfaceIcon(icon: item.symbol, color: item.tint, background: item.tint.opacity(0.12))

                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.title)
                                .font(.body.weight(.semibold))
                                .foregroundStyle(VPColor.textPrimary)
                            Text(item.subtitle)
                                .font(.subheadline)
                                .foregroundStyle(VPColor.secondaryText)
                                .lineLimit(2)
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(VPColor.secondaryText)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .vpCard()
            }
        }
    }
}

private struct SurfaceIcon: View {
    let icon: String
    let color: Color
    let background: Color

    var body: some View {
        Image(systemName: icon)
            .font(.system(size: 18, weight: .semibold))
            .foregroundStyle(color)
            .frame(width: 44, height: 44)
            .background(background, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(color.opacity(0.20), lineWidth: 1)
            )
    }
}
