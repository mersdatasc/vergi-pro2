import SwiftUI

struct TaxRadarCard: View {
    let radarData: TaxRadarDTO?
    var isLoading: Bool = false
    var onRefresh: (() -> Void)? = nil

    private var currentVatBalance: Double {
        radarData?.metrics.vat.balance ?? 0
    }

    private var isPayable: Bool {
        currentVatBalance > 0
    }

    var body: some View {
        if let radarData {
            radarContent(radarData)
        } else {
            unavailableContent
        }
    }

    private var unavailableContent: some View {
        HStack(spacing: VPSpace.sm) {
            if isLoading { ProgressView() }
            Image(systemName: "chart.bar.xaxis")
                .foregroundStyle(VPColor.secondaryText)
            VStack(alignment: .leading, spacing: 3) {
                Text("finance.tax_radar.title")
                    .font(.headline.weight(.bold))
                Text(isLoading ? String(localized: "finance.tax_radar.loading") : String(localized: "finance.tax_radar.unavailable"))
                    .font(.caption)
                    .foregroundStyle(VPColor.secondaryText)
            }
            Spacer()
            if !isLoading, let onRefresh {
                Button(action: onRefresh) { Image(systemName: "arrow.clockwise") }
                    .accessibilityLabel(String(localized: "common.refresh"))
            }
        }
        .vpCard()
    }

    private func radarContent(_ radarData: TaxRadarDTO) -> some View {
        VStack(alignment: .leading, spacing: VPSpace.md) {
            // Header: Title & Period & Status Badge
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("finance.tax_radar.title")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(VPColor.textPrimary)
                    Text(radarData.period)
                        .font(.caption)
                        .foregroundStyle(VPColor.secondaryText)
                }

                Spacer()

                Text(isPayable ? String(localized: "home.metrics.vat_payable") : String(localized: "home.metrics.vat_carryover"))
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(isPayable ? VPColor.textPrimary : VPColor.success)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(isPayable ? VPColor.cardBorder : VPColor.success.opacity(0.12), in: RoundedRectangle(cornerRadius: 6))
            }

            // Main VAT Balance Hero
            VStack(alignment: .leading, spacing: 4) {
                Text(abs(currentVatBalance).formattedTRY)
                    .font(.system(size: 30, weight: .bold, design: .default))
                    .monospacedDigit()
                    .foregroundStyle(VPColor.textPrimary)

                Text(isPayable ? String(localized: "finance.tax_radar.payable_detail") : String(localized: "finance.tax_radar.carryover_detail"))
                    .font(.caption)
                    .foregroundStyle(VPColor.secondaryText)
            }

            Divider().overlay(VPColor.cardBorder)

            // Breakdown Grid: Satış vs Gider vs Taşıt
            VStack(spacing: 8) {
                breakdownRow(
                    title: String(localized: "finance.tax_radar.sales_vat"),
                    amount: radarData.metrics.sales.kdvTotal,
                    isPositive: true
                )

                breakdownRow(
                    title: String(localized: "finance.tax_radar.expense_vat"),
                    amount: -radarData.metrics.expenses.kdvTotal,
                    isPositive: false
                )

                breakdownRow(
                    title: String(localized: "finance.tax_radar.vehicle_vat"),
                    amount: -radarData.metrics.fuel.kdv70,
                    isPositive: false
                )

                if radarData.metrics.fuel.totalKkeg > 0 {
                    let kkeg = radarData.metrics.fuel.totalKkeg
                    HStack {
                        Text("finance.tax_radar.vehicle_kkeg")
                            .font(.caption)
                            .foregroundStyle(VPColor.secondaryText)
                        Spacer()
                        Text(kkeg.formattedTRY)
                            .font(.caption.monospacedDigit())
                            .foregroundStyle(VPColor.secondaryText)
                    }
                    .padding(.top, 2)
                }
            }

            // Month-end Projection Card
            let proj = radarData.metrics.projection
            if proj.projectedSales != 0 || proj.projectedVatCalculated != 0 || proj.projectedVatDeductible != 0 || proj.projectedVatBalance != 0 {
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text("finance.tax_radar.projection")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(VPColor.textPrimary)
                        Spacer()
                        if radarData.remainingDays > 0 {
                            Text(String(format: String(localized: "finance.days_remaining"), radarData.remainingDays))
                                .font(.caption2.monospacedDigit())
                                .foregroundStyle(VPColor.secondaryText)
                        }
                    }

                    HStack(alignment: .firstTextBaseline) {
                        Text(proj.projectedVatBalance > 0 ? String(localized: "finance.tax_radar.projected_payable") : String(localized: "finance.tax_radar.projected_carryover"))
                            .font(.subheadline)
                            .foregroundStyle(VPColor.secondaryText)
                        Spacer()
                        Text(abs(proj.projectedVatBalance).formattedTRY)
                            .font(.subheadline.bold().monospacedDigit())
                            .foregroundStyle(VPColor.textPrimary)
                    }

                    if proj.neutralizingExpenseNeeded > 0 {
                        Text(String(format: String(localized: "finance.tax_radar.neutralizing_expense"), proj.neutralizingExpenseNeeded.formattedTRYCompact))
                            .font(.caption2)
                            .foregroundStyle(VPColor.secondaryText)
                    }
                }
                .padding(VPSpace.sm)
                .background(VPColor.canvas, in: RoundedRectangle(cornerRadius: VPRadius.medium))
            }

            Text("finance.tax_radar.scenario_notice")
                .font(.caption2)
                .foregroundStyle(VPColor.secondaryText)
        }
        .vpCard()
    }

    private func breakdownRow(title: String, amount: Double, isPositive: Bool) -> some View {
        HStack {
            Text(title)
                .font(.subheadline)
                .foregroundStyle(VPColor.textPrimary)

            Spacer()

            Text(abs(amount).formattedTRY)
                .font(.subheadline.monospacedDigit())
                .foregroundStyle(VPColor.textPrimary)
        }
    }
}
