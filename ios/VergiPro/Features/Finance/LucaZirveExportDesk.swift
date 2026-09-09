import SwiftUI
import UniformTypeIdentifiers

struct LucaZirveExportDesk: View {
    let organizationId: String
    var year: Int? = nil
    var month: Int? = nil
    var onFetchJournalPreview: (() async -> JournalPreviewDTO?)? = nil

    @State private var showingJournalPreview = false
    @State private var journalPreview: JournalPreviewDTO? = nil
    @State private var isLoadingPreview = false
    @State private var shareURL: URL? = nil
    @State private var showingShareSheet = false
    @State private var exportError: String?

    var body: some View {
        VStack(alignment: .leading, spacing: VPSpace.md) {
            // Section Header
            HStack {
                HStack(spacing: 8) {
                    Image(systemName: "folder.badge.gearshape")
                        .font(.headline)
                        .foregroundStyle(VPColor.brand)
                    Text(String(localized: "finance.export.desk_title"))
                        .font(.caption.weight(.bold))
                        .foregroundStyle(VPColor.secondaryText)
                        .tracking(0.8)
                }
                Spacer()
                Text(String(localized: "finance.export.formats_badge"))
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(VPColor.accentBlue)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(VPColor.accentBlue.opacity(0.12), in: Capsule())
            }

            Text(String(localized: "finance.export.description"))
                .font(.caption)
                .foregroundStyle(VPColor.secondaryText)

            // Primary 2 Big Actions: Luca & Zirve
            HStack(spacing: VPSpace.sm) {
                // Luca Button
                exportTile(
                    title: String(localized: "finance.export.luca.title"),
                    subtitle: String(localized: "finance.export.luca.subtitle"),
                    icon: "arrow.down.doc.fill",
                    tint: VPColor.brand,
                    path: "app/export/luca",
                    query: []
                )

                // Zirve Button
                exportTile(
                    title: String(localized: "finance.export.zirve.title"),
                    subtitle: String(localized: "finance.export.zirve.subtitle"),
                    icon: "chevron.left.forwardslash.chevron.right",
                    tint: VPColor.success,
                    path: "app/export/zirve",
                    query: [URLQueryItem(name: "format", value: "xml")]
                )
            }

            // Secondary 2 Actions: Master Excel & ZIP Archive
            HStack(spacing: VPSpace.sm) {
                exportTile(
                    title: String(localized: "finance.export.excel.title"),
                    subtitle: String(localized: "finance.export.excel.subtitle"),
                    icon: "tablecells.fill",
                    tint: VPColor.accentBlue,
                    path: "app/export/excel",
                    query: []
                )

                exportTile(
                    title: String(localized: "finance.export.zip.title"),
                    subtitle: String(localized: "finance.export.zip.subtitle"),
                    icon: "doc.zipper",
                    tint: VPColor.secondaryText,
                    path: "app/export/zip",
                    query: []
                )
            }

            // Interactive TDHP Live Preview Trigger
            Button {
                Task {
                    isLoadingPreview = true
                    if let onFetchJournalPreview {
                        journalPreview = await onFetchJournalPreview()
                    }
                    isLoadingPreview = false
                    showingJournalPreview = true
                }
            } label: {
                HStack(spacing: 10) {
                    Image(systemName: "list.bullet.rectangle.portrait")
                        .font(.headline)
                        .foregroundStyle(VPColor.accentBlue)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(String(localized: "finance.export.preview.title"))
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(VPColor.textPrimary)
                        Text(String(localized: "finance.export.preview.subtitle"))
                            .font(.caption2)
                            .foregroundStyle(VPColor.secondaryText)
                    }

                    Spacer()

                    if isLoadingPreview {
                        ProgressView().scaleEffect(0.8)
                    } else {
                        Image(systemName: "chevron.right")
                            .font(.caption.bold())
                            .foregroundStyle(VPColor.secondaryText)
                    }
                }
                .padding(12)
                .background(VPColor.canvas, in: RoundedRectangle(cornerRadius: VPRadius.medium))
            }
            .buttonStyle(.plain)
        }
        .vpCard()
        .sheet(isPresented: $showingJournalPreview) {
            JournalPreviewSheet(preview: journalPreview)
        }
        .sheet(isPresented: $showingShareSheet) {
            if let shareURL {
                ShareSheet(activityItems: [shareURL])
            }
        }
        .alert(String(localized: "finance.export.error.title"), isPresented: Binding(
            get: { exportError != nil },
            set: { if !$0 { exportError = nil } }
        )) {
            Button(String(localized: "common.ok"), role: .cancel) { exportError = nil }
        } message: {
            Text(exportError ?? String(localized: "finance.export.error.download"))
        }
    }

    private func exportTile(title: String, subtitle: String, icon: String, tint: Color, path: String, query: [URLQueryItem]) -> some View {
        Button {
            Task {
                do {
                    shareURL = try await downloadExport(path: path, query: query)
                    showingShareSheet = true
                } catch {
                    exportError = String(localized: "finance.export.error.secure_download")
                }
            }
        } label: {
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Image(systemName: icon)
                        .font(.headline)
                        .foregroundStyle(tint)
                    Spacer()
                    Image(systemName: "square.and.arrow.up")
                        .font(.caption2)
                        .foregroundStyle(VPColor.secondaryText)
                }
                Text(title)
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(VPColor.textPrimary)
                Text(subtitle)
                    .font(.caption2)
                    .foregroundStyle(VPColor.secondaryText)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(VPColor.canvas, in: RoundedRectangle(cornerRadius: VPRadius.medium))
        }
        .buttonStyle(.plain)
    }

    private func downloadExport(path: String, query: [URLQueryItem]) async throws -> URL {
        let configuration = try AppConfiguration.load()
        let endpoint = configuration.apiBaseURL.appendingPathComponent(path)
        guard var components = URLComponents(url: endpoint, resolvingAgainstBaseURL: false) else {
            throw URLError(.badURL)
        }
        var effectiveQuery = query
        if let year { effectiveQuery.append(URLQueryItem(name: "year", value: String(year))) }
        if let month { effectiveQuery.append(URLQueryItem(name: "month", value: String(month))) }
        components.queryItems = effectiveQuery.isEmpty ? nil : effectiveQuery
        guard let url = components.url else { throw URLError(.badURL) }

        var request = URLRequest(url: url, timeoutInterval: 60)
        request.setValue("application/octet-stream", forHTTPHeaderField: "Accept")
        request.setValue(organizationId, forHTTPHeaderField: "X-Organization-ID")
        if let token = await SessionController.shared.currentAccessToken(), !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        let (temporaryURL, response) = try await URLSession.shared.download(for: request)
        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
            throw URLError(.badServerResponse)
        }
        let suggestedName = response.suggestedFilename ?? "vergipro-export"
        let destination = FileManager.default.temporaryDirectory.appendingPathComponent(suggestedName)
        try? FileManager.default.removeItem(at: destination)
        try FileManager.default.moveItem(at: temporaryURL, to: destination)
        return destination
    }
}

// MARK: - Journal Preview Sheet
struct JournalPreviewSheet: View {
    let preview: JournalPreviewDTO?
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: VPSpace.md) {
                    if let preview {
                        // Summary Card
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(preview.period)
                                    .font(.title3.bold())
                                Text(String(format: String(localized: "finance.export.preview.voucher_count"), preview.totalVouchers))
                                    .font(.caption)
                                    .foregroundStyle(VPColor.secondaryText)
                            }
                            Spacer()
                            Label(
                                preview.isBalanced
                                    ? String(localized: "finance.export.preview.balanced")
                                    : String(localized: "finance.export.preview.unbalanced"),
                                systemImage: preview.isBalanced ? "checkmark.seal.fill" : "exclamationmark.triangle.fill"
                            )
                                .font(.caption.bold())
                                .foregroundStyle(preview.isBalanced ? VPColor.success : VPColor.danger)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(preview.isBalanced ? VPColor.success.opacity(0.12) : VPColor.danger.opacity(0.12), in: Capsule())
                        }
                        .vpCard()

                        // Totals Row
                        HStack(spacing: VPSpace.sm) {
                            VStack(alignment: .leading) {
                                Text(String(localized: "finance.export.preview.total_debit"))
                                    .font(.caption2)
                                    .foregroundStyle(VPColor.secondaryText)
                                Text(preview.totalDebit.formattedTRY)
                                    .font(.subheadline.bold().monospacedDigit())
                                    .foregroundStyle(VPColor.textPrimary)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .vpCard()

                            VStack(alignment: .leading) {
                                Text(String(localized: "finance.export.preview.total_credit"))
                                    .font(.caption2)
                                    .foregroundStyle(VPColor.secondaryText)
                                Text(preview.totalCredit.formattedTRY)
                                    .font(.subheadline.bold().monospacedDigit())
                                    .foregroundStyle(VPColor.textPrimary)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .vpCard()
                        }

                        // Vouchers List
                        ForEach(preview.entries) { entry in
                            VStack(alignment: .leading, spacing: VPSpace.xs) {
                                HStack {
                                    Text(entry.voucherNo)
                                        .font(.caption.bold().monospaced())
                                        .foregroundStyle(VPColor.brand)
                                    Spacer()
                                    Text(entry.date)
                                        .font(.caption2)
                                        .foregroundStyle(VPColor.secondaryText)
                                }
                                Text(entry.description)
                                    .font(.caption)
                                    .foregroundStyle(VPColor.secondaryText)
                                    .lineLimit(1)

                                Divider().overlay(VPColor.cardBorder)

                                ForEach(entry.lines) { line in
                                    HStack {
                                        Text(line.accountCode)
                                            .font(.caption2.bold().monospaced())
                                            .foregroundStyle(VPColor.brand)
                                            .frame(width: 44, alignment: .leading)
                                        Text(line.accountName)
                                            .font(.caption2)
                                            .foregroundStyle(VPColor.textPrimary)
                                            .lineLimit(1)
                                        Spacer()
                                        if line.debit > 0 {
                                            Text(line.debit.formattedTRY)
                                                .font(.caption2.monospacedDigit())
                                                .foregroundStyle(VPColor.textPrimary)
                                        } else {
                                            Text(line.credit.formattedTRY)
                                                .font(.caption2.monospacedDigit())
                                                .foregroundStyle(VPColor.secondaryText)
                                        }
                                    }
                                }
                            }
                            .vpCard()
                        }
                    } else {
                        ContentUnavailableView(
                            String(localized: "finance.export.preview.unavailable.title"),
                            systemImage: "doc.text.magnifyingglass",
                            description: Text(String(localized: "finance.export.preview.unavailable.detail"))
                        )
                        .frame(maxWidth: .infinity)
                        .padding(.top, 40)
                    }
                }
                .padding(VPSpace.md)
            }
            .background(VPColor.canvas)
            .navigationTitle(String(localized: "finance.export.preview.navigation_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(String(localized: "common.close")) { dismiss() }
                }
            }
        }
    }
}

// MARK: - UIActivityViewController Wrapper
struct ShareSheet: UIViewControllerRepresentable {
    let activityItems: [Any]
    let applicationActivities: [UIActivity]? = nil

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: applicationActivities)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
