import SwiftUI

struct FinanceCenterView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = FinanceViewModel()

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: VPSpace.lg) {
                    periodHeader

                    if let errorMessage = viewModel.errorMessage {
                        Label(errorMessage, systemImage: "exclamationmark.triangle.fill")
                            .font(.footnote)
                            .foregroundStyle(VPColor.danger)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(VPSpace.sm)
                            .background(VPColor.danger.opacity(0.08), in: RoundedRectangle(cornerRadius: VPRadius.small))
                    }

                    // 1. Core Feature: Canlı KDV & Vergi Radarı
                    TaxRadarCard(
                        radarData: viewModel.taxRadar,
                        isLoading: viewModel.isLoading,
                        onRefresh: {
                            if let orgId = appState.currentOrganization?.id {
                                Task { await viewModel.fetchFinanceData(organizationId: String(orgId)) }
                            }
                        }
                    )

                    // 2. Core Feature: Saha Masraf Toplama Radarı
                    MonthEndReminderCard(
                        reminderStatus: viewModel.reminderStatus,
                        onTriggerReminder: {
                            if let orgId = appState.currentOrganization?.id {
                                return await viewModel.triggerMonthEndReminder(organizationId: String(orgId))
                            }
                            return nil
                        },
                        onNavigateToCapture: {
                            appState.selectedDestination = .capture
                        }
                    )

                    // 3. Core Feature: Mali Müşavir İhracat Masası (Luca & Zirve)
                    LucaZirveExportDesk(
                        organizationId: appState.currentOrganization?.id ?? "",
                        year: viewModel.selectedPeriod?.year,
                        month: viewModel.selectedPeriod?.month,
                        onFetchJournalPreview: {
                            if let orgId = appState.currentOrganization?.id {
                                return await viewModel.fetchJournalPreview(organizationId: String(orgId))
                            }
                            return nil
                        }
                    )

                    // 4. Financial Health & Metrics
                    keyMetricsGrid
                    completenessCard
                    if let monthlyFlow = viewModel.summary?.monthlyFlow, !monthlyFlow.isEmpty {
                        trendCard(monthlyFlow)
                    }
                    if let fuel = viewModel.taxRadar?.metrics.fuel, fuel.count > 0 || fuel.total != 0 {
                        vehicleRestrictionCard(fuel)
                    }
                }
                .padding(VPSpace.md)
                .padding(.bottom, VPSpace.xxl)
            }
            .background(VPColor.canvas)
            .navigationTitle(String(localized: "finance.title"))
            .refreshable {
                if let orgId = appState.currentOrganization?.id {
                    await viewModel.fetchFinanceData(organizationId: String(orgId))
                }
            }
        }
        .task {
            if let orgId = appState.currentOrganization?.id {
                await viewModel.fetchFinanceData(organizationId: String(orgId))
            }
        }
    }

    private var periodHeader: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: VPSpace.xs) {
                Text("finance.overview")
                    .font(.title2.bold())
                    .foregroundStyle(VPColor.textPrimary)
                Text(viewModel.selectedPeriod?.label ?? viewModel.taxRadar?.period ?? viewModel.summary?.period ?? String(localized: "finance.period.unavailable"))
                    .font(.subheadline)
                    .foregroundStyle(VPColor.secondaryText)
            }
            Spacer()
            if !viewModel.periods.isEmpty {
                Menu {
                    ForEach(viewModel.periods) { period in
                        Button {
                            guard let orgId = appState.currentOrganization?.id else { return }
                            Task { await viewModel.selectPeriod(period, organizationId: String(orgId)) }
                        } label: {
                            if period == viewModel.selectedPeriod {
                                Label(period.label, systemImage: "checkmark")
                            } else {
                                Text(period.label)
                            }
                        }
                    }
                } label: {
                    Image(systemName: "calendar")
                        .font(.headline)
                        .foregroundStyle(VPColor.textPrimary)
                        .frame(width: 42, height: 42)
                        .background(VPColor.surface, in: RoundedRectangle(cornerRadius: VPRadius.small))
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var completenessCard: some View {
        VStack(alignment: .leading, spacing: VPSpace.sm) {
            if let completeness = viewModel.summary?.completenessPercent {
                HStack {
                    Label(String(localized: "finance.completeness.title"), systemImage: "checklist")
                        .font(.headline)
                        .foregroundStyle(VPColor.textPrimary)
                    Spacer()
                    Text("%\(completeness)")
                        .font(.headline.bold().monospacedDigit())
                        .foregroundStyle(VPColor.brand)
                }

                ProgressView(value: Double(completeness) / 100.0)
                    .tint(VPColor.brand)

                Text("finance.completeness.detail")
                    .font(.footnote)
                    .foregroundStyle(VPColor.secondaryText)
            } else {
                Label(String(localized: "finance.completeness.unavailable"), systemImage: "info.circle")
                    .font(.headline)
                    .foregroundStyle(VPColor.secondaryText)
            }
        }
        .vpCard()
    }

    private var keyMetricsGrid: some View {
        Group {
            if let metrics = viewModel.taxRadar?.metrics {
                LazyVGrid(columns: [.init(.flexible()), .init(.flexible())], spacing: VPSpace.sm) {
            metricTile(
                title: String(localized: "finance.metrics.sales"),
                value: metrics.sales.total.formattedTRYCompact,
                caption: String(format: String(localized: "finance.metrics.sales_count"), metrics.sales.count),
                tint: VPColor.brand
            )
            metricTile(
                title: String(localized: "finance.metrics.expenses"),
                value: metrics.expenses.total.formattedTRYCompact,
                caption: String(format: String(localized: "finance.metrics.expense_count"), metrics.expenses.count),
                tint: VPColor.brand
            )
            metricTile(
                title: metrics.vat.balance > 0 ? String(localized: "home.metrics.vat_payable") : String(localized: "home.metrics.vat_carryover"),
                value: abs(metrics.vat.balance).formattedTRY,
                caption: String(localized: "finance.metrics.vat_caption"),
                tint: metrics.vat.balance > 0 ? VPColor.danger : VPColor.success
            )
            metricTile(
                title: String(localized: "finance.metrics.vehicle_kkeg"),
                value: metrics.fuel.totalKkeg.formattedTRY,
                caption: String(localized: "finance.metrics.backend_rule"),
                tint: VPColor.warning
            )
                }
            } else {
                Label(viewModel.isLoading ? String(localized: "home.metrics.loading") : String(localized: "finance.metrics.unavailable"), systemImage: "chart.bar.xaxis")
                    .font(.subheadline)
                    .foregroundStyle(VPColor.secondaryText)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .vpCard()
            }
        }
    }

    private func metricTile(title: String, value: String, caption: String, tint: Color) -> some View {
        VStack(alignment: .leading, spacing: VPSpace.xs) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundStyle(VPColor.secondaryText)
            Text(value)
                .font(.title3.bold().monospacedDigit())
                .foregroundStyle(VPColor.textPrimary)
                .minimumScaleFactor(0.75)
                .lineLimit(1)
            Text(caption)
                .font(.caption2)
                .foregroundStyle(tint)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .vpCard()
    }

    private func trendCard(_ monthlyFlow: [FinanceSummaryDTO.MonthlyFlowItem]) -> some View {
        VStack(alignment: .leading, spacing: VPSpace.md) {
            HStack {
                Text("finance.trend.title")
                    .font(.title3.bold())
                    .foregroundStyle(VPColor.textPrimary)
                Spacer()
                HStack(spacing: 8) {
                    Circle().fill(VPColor.brand).frame(width: 8, height: 8)
                    Text("finance.trend.sales").font(.caption2).foregroundStyle(VPColor.secondaryText)
                    Circle().fill(VPColor.warning).frame(width: 8, height: 8)
                    Text("finance.trend.expense").font(.caption2).foregroundStyle(VPColor.secondaryText)
                }
            }

            HStack(alignment: .bottom, spacing: VPSpace.sm) {
                ForEach(monthlyFlow) { item in
                    VStack(spacing: VPSpace.xs) {
                        HStack(alignment: .bottom, spacing: 3) {
                            RoundedRectangle(cornerRadius: 3)
                                .fill(VPColor.brand)
                                .frame(height: CGFloat(item.incomeMinor) / 300_000)
                            RoundedRectangle(cornerRadius: 3)
                                .fill(VPColor.warning)
                                .frame(height: CGFloat(item.expenseMinor) / 300_000)
                        }
                        Text(item.monthName)
                            .font(.caption2)
                            .foregroundStyle(VPColor.secondaryText)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .frame(height: 110, alignment: .bottom)

        }
        .vpCard()
    }

    private func vehicleRestrictionCard(_ fuel: TaxRadarFuelDTO) -> some View {
        VStack(alignment: .leading, spacing: VPSpace.sm) {
            HStack {
                Label(String(localized: "finance.vehicle.title"), systemImage: "car.fill")
                    .font(.headline)
                    .foregroundStyle(VPColor.textPrimary)
                Spacer()
                Text(fuel.total.formattedTRY)
                    .font(.headline.monospacedDigit())
                    .foregroundStyle(VPColor.textPrimary)
            }

            Divider().overlay(VPColor.cardBorder)

            HStack {
                Text("finance.vehicle.deductible")
                    .font(.subheadline)
                Spacer()
                Text(fuel.matrah70.formattedTRY)
                    .font(.subheadline.bold().monospacedDigit())
                    .foregroundStyle(VPColor.success)
            }

            HStack {
                Text("finance.vehicle.kkeg")
                    .font(.subheadline)
                Spacer()
                Text(fuel.totalKkeg.formattedTRY)
                    .font(.subheadline.bold().monospacedDigit())
                    .foregroundStyle(VPColor.danger)
            }

            Text("finance.vehicle.backend_notice")
                .font(.caption)
                .foregroundStyle(VPColor.secondaryText)
        }
        .vpCard()
    }
}
