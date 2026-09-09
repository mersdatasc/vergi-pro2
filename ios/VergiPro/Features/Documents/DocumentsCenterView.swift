import SwiftUI
import PDFKit

struct DocumentsCenterView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = DocumentsViewModel()
    @State private var selectedDocument: BackendDocument?

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: VPSpace.sm) {
                    filterScrollView
                    statsCard

                    if viewModel.countPending > 0 {
                        bulkConfirmBanner
                    }

                    if viewModel.isLoading && viewModel.documents.isEmpty {
                        ProgressView(String(localized: "documents.loading"))
                            .padding(.top, VPSpace.xl)
                    } else if viewModel.filteredDocuments.isEmpty {
                        emptyState
                    } else {
                        ForEach(viewModel.filteredDocuments) { document in
                            Button {
                                selectedDocument = document
                            } label: {
                                DocumentRowView(document: document)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .padding(.horizontal, VPSpace.md)
                .padding(.bottom, VPSpace.xxl)
            }
            .background(VPColor.canvas)
            .navigationTitle(String(localized: "documents.title"))
            .searchable(text: $viewModel.searchQuery, prompt: String(localized: "documents.search"))
            .refreshable {
                if let orgId = appState.currentOrganization?.id {
                    await viewModel.fetchDocuments(organizationId: orgId)
                }
            }
            .sheet(item: $selectedDocument) { doc in
                DocumentDetailSheet(
                    document: doc,
                    viewModel: viewModel,
                    organizationId: appState.currentOrganization?.id ?? ""
                )
            }
        }
        .task(id: appState.currentOrganization?.id) {
            if let orgId = appState.currentOrganization?.id {
                await viewModel.fetchDocuments(organizationId: orgId)
            }
        }
    }

    private var filterScrollView: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: VPSpace.xs) {
                ForEach(DocFilterCategory.allCases) { cat in
                    Button {
                        withAnimation(.snappy) { viewModel.selectedFilter = cat }
                    } label: {
                        Text(cat.title)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(viewModel.selectedFilter == cat ? VPColor.brandInverse : Color.primary)
                            .padding(.horizontal, VPSpace.md)
                            .padding(.vertical, 8)
                            .background(viewModel.selectedFilter == cat ? VPColor.brand : VPColor.surface, in: Capsule())
                            .overlay(Capsule().strokeBorder(viewModel.selectedFilter == cat ? VPColor.brand : VPColor.cardBorder, lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(.vertical, VPSpace.xs)
    }

    private var statsCard: some View {
        HStack(spacing: VPSpace.sm) {
            statItem(value: "\(viewModel.countTotal)", label: String(localized: "documents.stats.total"), tint: .primary)
            Divider().frame(height: 32)
            statItem(value: "\(viewModel.countPending)", label: String(localized: "documents.stats.pending"), tint: VPColor.warning)
            Divider().frame(height: 32)
            statItem(value: "\(viewModel.countApproved)", label: String(localized: "documents.stats.approved"), tint: VPColor.success)
        }
        .vpCard()
    }

    private func statItem(value: String, label: String, tint: Color) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value).font(.title3.bold().monospacedDigit()).foregroundStyle(tint)
            Text(label).font(.caption2).foregroundStyle(VPColor.secondaryText)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var bulkConfirmBanner: some View {
        Button {
            if let orgId = appState.currentOrganization?.id {
                Task {
                    await viewModel.bulkConfirmAll(organizationId: orgId)
                }
            }
        } label: {
            HStack(spacing: VPSpace.sm) {
                Image(systemName: "bolt.fill")
                    .font(.headline)
                    .foregroundStyle(Color.yellow)
                VStack(alignment: .leading, spacing: 2) {
                    Text(String(localized: "documents.bulk.title"))
                        .font(.subheadline.bold())
                        .foregroundStyle(VPColor.brandInverse)
                    Text(String(format: String(localized: "documents.bulk.detail"), viewModel.countPending))
                        .font(.caption2)
                        .foregroundStyle(VPColor.brandInverse.opacity(0.85))
                }
                Spacer()
                if viewModel.isBulkConfirming {
                    ProgressView().tint(VPColor.brandInverse)
                } else {
                    Image(systemName: "arrow.right.circle.fill")
                        .font(.title3)
                        .foregroundStyle(VPColor.brandInverse)
                }
            }
            .padding(VPSpace.md)
            .background(VPColor.brand, in: RoundedRectangle(cornerRadius: VPRadius.card, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(viewModel.isBulkConfirming)
    }

    private var emptyState: some View {
        VStack(spacing: VPSpace.md) {
            Image(systemName: "doc.text.magnifyingglass")
                .font(.system(size: 44, weight: .light))
                .foregroundStyle(VPColor.secondaryText)
                .padding(.top, VPSpace.xl)
            Text(String(localized: "documents.empty.title"))
                .font(.headline)
            Text(String(localized: "documents.empty.detail"))
                .font(.subheadline)
                .foregroundStyle(VPColor.secondaryText)
                .multilineTextAlignment(.center)
        }
        .padding(VPSpace.lg)
    }
}

// MARK: - Document Row
private struct DocumentRowView: View {
    let document: BackendDocument

    var body: some View {
        HStack(spacing: VPSpace.sm) {
            Image(systemName: document.typeSymbol)
                .font(.headline)
                .foregroundStyle(iconColor)
                .frame(width: 42, height: 42)
                .background(iconColor.opacity(0.12), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .strokeBorder(iconColor.opacity(0.25), lineWidth: 1)
                )

            VStack(alignment: .leading, spacing: 3) {
                Text(document.displayTitle)
                    .font(.body.weight(.semibold))
                    .foregroundStyle(.primary)
                    .lineLimit(1)

                HStack(spacing: 6) {
                    Text(document.invoiceNumber)
                        .font(.caption2.monospaced())
                        .foregroundStyle(VPColor.secondaryText)

                    if document.isDuplicateFlag {
                        Text(String(localized: "documents.badge.duplicate"))
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(Color.orange)
                            .padding(.horizontal, 4)
                            .padding(.vertical, 1)
                            .background(Color.orange.opacity(0.15), in: RoundedRectangle(cornerRadius: 4))
                    }

                    if document.isOutOfPeriodFlag {
                        Text(String(localized: "documents.badge.period_difference"))
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(Color.blue)
                            .padding(.horizontal, 4)
                            .padding(.vertical, 1)
                            .background(Color.blue.opacity(0.15), in: RoundedRectangle(cornerRadius: 4))
                    }

                    if document.isVehicleExpense {
                        Text(String(localized: "documents.badge.vehicle_kkeg"))
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(VPColor.warning)
                            .padding(.horizontal, 4)
                            .padding(.vertical, 1)
                            .background(VPColor.warning.opacity(0.15), in: RoundedRectangle(cornerRadius: 4))
                    }
                }
            }

            Spacer(minLength: VPSpace.xs)

            VStack(alignment: .trailing, spacing: 3) {
                Text(document.formattedAmount)
                    .font(.subheadline.bold().monospacedDigit())
                    .foregroundStyle(.primary)
                Text(document.issueDate)
                    .font(.caption2)
                    .foregroundStyle(VPColor.secondaryText)
            }
        }
        .vpCard()
    }

    private var iconColor: Color {
        if document.docType == "SALES" { return VPColor.success }
        if document.docType == "FUEL" { return Color.blue }
        return VPColor.brand
    }
}

// MARK: - PDFKit Representable
struct PDFKitView: UIViewRepresentable {
    let data: Data

    func makeUIView(context: Context) -> PDFView {
        let pdfView = PDFView()
        pdfView.autoScales = true
        pdfView.displayMode = .singlePageContinuous
        pdfView.displayDirection = .vertical
        pdfView.document = PDFDocument(data: data)
        return pdfView
    }

    func updateUIView(_ uiView: PDFView, context: Context) {
        if uiView.document?.dataRepresentation() != data {
            uiView.document = PDFDocument(data: data)
        }
    }
}

// MARK: - Document Detail Sheet
struct DocumentDetailSheet: View {
    @Environment(\.dismiss) private var dismiss
    let document: BackendDocument
    @ObservedObject var viewModel: DocumentsViewModel
    let organizationId: String

    @State private var selectedTab: Int = 0 // 0: Muhasebe & Kategori, 1: Belge / PDF Önizleme

    private var liveDoc: BackendDocument {
        viewModel.documents.first(where: { $0.id == document.id }) ?? document
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Segmented Tab Picker
                Picker("", selection: $selectedTab) {
                    Text(String(localized: "documents.detail.tab.accounting")).tag(0)
                    Text(String(localized: "documents.detail.tab.preview")).tag(1)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, VPSpace.md)
                .padding(.vertical, VPSpace.sm)

                ScrollView {
                    if selectedTab == 0 {
                        accountingTabContent
                    } else {
                        previewTabContent
                    }
                }

                bottomActionBar
            }
            .background(VPColor.canvas)
            .navigationTitle(String(format: String(localized: "documents.detail.navigation_title"), liveDoc.id))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "common.close")) { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(role: .destructive) {
                        Task {
                            if await viewModel.deleteDocument(organizationId: organizationId, docId: liveDoc.id) {
                                dismiss()
                            }
                        }
                    } label: {
                        Image(systemName: "trash")
                            .foregroundStyle(Color.red)
                    }
                }
            }
        }
        .alert(String(localized: "common.error"), isPresented: Binding(
            get: { viewModel.errorMessage != nil },
            set: { if !$0 { viewModel.errorMessage = nil } }
        )) {
            Button(String(localized: "common.ok"), role: .cancel) { viewModel.errorMessage = nil }
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
    }

    private var accountingTabContent: some View {
        VStack(spacing: VPSpace.md) {
            // Warning Banners
            if liveDoc.isDuplicateFlag {
                HStack(spacing: VPSpace.sm) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundStyle(Color.orange)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(String(localized: "documents.detail.duplicate.title"))
                            .font(.caption.bold())
                            .foregroundStyle(Color.orange)
                        Text(liveDoc.duplicateReason ?? String(localized: "documents.detail.duplicate.default"))
                            .font(.caption2)
                            .foregroundStyle(Color.orange.opacity(0.9))
                    }
                    Spacer()
                }
                .padding(VPSpace.sm)
                .background(Color.orange.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))
            }

            if liveDoc.isOutOfPeriodFlag {
                HStack(spacing: VPSpace.sm) {
                    Image(systemName: "calendar.badge.exclamationmark")
                        .foregroundStyle(Color.blue)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(String(localized: "documents.detail.period.title"))
                            .font(.caption.bold())
                            .foregroundStyle(Color.blue)
                        Text(liveDoc.fiscalPeriodWarning ?? String(localized: "documents.detail.period.default"))
                            .font(.caption2)
                            .foregroundStyle(Color.blue.opacity(0.9))
                    }
                    Spacer()
                }
                .padding(VPSpace.sm)
                .background(Color.blue.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))
            }

            // Quick Telegram Category Chips
            VStack(alignment: .leading, spacing: VPSpace.xs) {
                Text(String(localized: "documents.detail.quick_category"))
                    .font(.caption2.bold())
                    .foregroundStyle(VPColor.secondaryText)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: VPSpace.xs) {
                        quickCategoryChip(title: String(localized: "documents.category.fuel"), category: "Akaryakıt / Taşıt", isHarici: false)
                        quickCategoryChip(title: String(localized: "documents.category.meal"), category: "Yemek / Temsil Ağırlama", isHarici: false)
                        quickCategoryChip(title: String(localized: "documents.category.cloud"), category: "Sunucu / Bulut Hizmetleri", isHarici: false)
                        quickCategoryChip(title: String(localized: "documents.category.office"), category: "Ofis / Kırtasiye", isHarici: false)
                        quickCategoryChip(title: String(localized: "documents.category.cargo"), category: "Kargo / Lojistik", isHarici: false)
                        quickCategoryChip(title: String(localized: "documents.category.consulting"), category: "Müşavirlik & Danışmanlık", isHarici: false)
                        quickCategoryChip(title: String(localized: "documents.category.external"), category: "Harici Masraflar", isHarici: true)
                    }
                }
            }
            .vpCard()

            // Financial Summary Card
            VStack(alignment: .leading, spacing: VPSpace.sm) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(liveDoc.displayTitle).font(.title3.bold())
                        Text(liveDoc.invoiceNumber).font(.subheadline.monospaced()).foregroundStyle(VPColor.secondaryText)
                    }
                    Spacer()
                    Text(liveDoc.formattedAmount).font(.title2.bold().monospacedDigit())
                }

                Divider()

                ForEach(liveDoc.extractedFields) { field in
                    HStack {
                        Text(field.title).font(.subheadline).foregroundStyle(VPColor.secondaryText)
                        Spacer()
                        Text(field.value).font(.subheadline.bold().monospacedDigit())
                    }
                }
            }
            .vpCard()

            // Timeline
            VStack(alignment: .leading, spacing: VPSpace.xs) {
                Text(String(localized: "documents.detail.timeline"))
                    .font(.caption2.bold())
                    .foregroundStyle(VPColor.secondaryText)

                ForEach(liveDoc.timeline) { event in
                    HStack(spacing: VPSpace.sm) {
                        Image(systemName: event.symbol)
                            .foregroundStyle(VPColor.brand)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(event.title).font(.subheadline.bold())
                            Text(event.detail).font(.caption2).foregroundStyle(VPColor.secondaryText)
                        }
                        Spacer()
                        Text(event.timestamp).font(.caption2.monospacedDigit()).foregroundStyle(VPColor.secondaryText)
                    }
                    .padding(.vertical, 4)
                }
            }
            .vpCard()
        }
        .padding(VPSpace.md)
    }

    private func quickCategoryChip(title: String, category: String, isHarici: Bool) -> some View {
        let isSelected = liveDoc.category == category
        return Button {
            Task {
                await viewModel.quickUpdateCategory(
                    organizationId: organizationId,
                    docId: liveDoc.id,
                    category: category,
                    plate: liveDoc.plate,
                    isHarici: isHarici
                )
            }
        } label: {
            Text(title)
                .font(.caption.weight(isSelected ? .bold : .medium))
                .foregroundStyle(isSelected ? VPColor.brandInverse : Color.primary)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(isSelected ? VPColor.brand : VPColor.surface, in: Capsule())
                .overlay(Capsule().strokeBorder(isSelected ? VPColor.brand : VPColor.cardBorder, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }

    private var previewTabContent: some View {
        DocumentPreviewViewer(documentId: liveDoc.id, organizationId: organizationId)
            .padding(VPSpace.md)
    }

    private var bottomActionBar: some View {
        HStack(spacing: VPSpace.sm) {
            if liveDoc.isPending {
                Button(role: .destructive) {
                    Task {
                        if await viewModel.updateDocumentStatus(organizationId: organizationId, docId: liveDoc.id, newStatus: "REJECTED") {
                            dismiss()
                        }
                    }
                } label: {
                    Text(String(localized: "documents.action.reject"))
                        .font(.headline)
                        .frame(maxWidth: .infinity, minHeight: 46)
                }
                .buttonStyle(VPSecondaryButtonStyle())

                Button {
                    Task {
                        if await viewModel.updateDocumentStatus(organizationId: organizationId, docId: liveDoc.id, newStatus: "APPROVED") {
                            dismiss()
                        }
                    }
                } label: {
                    Text(String(localized: "documents.action.approve"))
                        .font(.headline)
                        .frame(maxWidth: .infinity, minHeight: 46)
                }
                .buttonStyle(VPPrimaryButtonStyle())
            } else {
                Text(String(format: String(localized: "documents.detail.status"), localizedStatus(liveDoc.status)))
                    .font(.subheadline.bold())
                    .foregroundStyle(liveDoc.status == "APPROVED" ? VPColor.success : Color.primary)
                Spacer()
                Button(String(localized: "common.close")) { dismiss() }
                    .buttonStyle(VPSecondaryButtonStyle())
            }
        }
        .padding(VPSpace.md)
        .background(VPColor.surface)
        .overlay(Rectangle().frame(height: 1).foregroundStyle(VPColor.cardBorder), alignment: .top)
    }

    private func localizedStatus(_ status: String) -> String {
        switch status {
        case "APPROVED", "CONFIRMED": return String(localized: "documents.status.approved")
        case "PENDING_REVIEW": return String(localized: "documents.status.pending")
        case "REJECTED": return String(localized: "documents.status.rejected")
        default: return String(localized: "documents.status.unknown")
        }
    }
}

// MARK: - Document File Preview Viewer (PDF & Image)
struct DocumentPreviewViewer: View {
    let documentId: Int
    let organizationId: String
    @State private var fileData: Data?
    @State private var isPDF = false
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var fileUrl: URL?

    var body: some View {
        VStack(spacing: VPSpace.md) {
            if isLoading {
                VStack(spacing: VPSpace.sm) {
                    ProgressView()
                    Text(String(localized: "documents.preview.loading"))
                        .font(.caption)
                        .foregroundStyle(VPColor.secondaryText)
                }
                .frame(maxWidth: .infinity, minHeight: 320)
                .vpCard()
            } else if let data = fileData {
                if isPDF {
                    VStack(alignment: .leading, spacing: VPSpace.xs) {
                        HStack {
                            Label(String(localized: "documents.preview.pdf"), systemImage: "doc.text.fill")
                                .font(.caption.bold())
                                .foregroundStyle(VPColor.brand)
                            Spacer()
                            if let fileUrl {
                                Link(String(localized: "documents.preview.open_fullscreen"), destination: fileUrl)
                                    .font(.caption.bold())
                                    .foregroundStyle(Color.blue)
                            }
                        }
                        PDFKitView(data: data)
                            .frame(minHeight: 460)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                            .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(VPColor.cardBorder, lineWidth: 1))
                    }
                } else if let uiImage = UIImage(data: data) {
                    Image(uiImage: uiImage)
                        .resizable()
                        .scaledToFit()
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                        .shadow(color: Color.black.opacity(0.12), radius: 8, x: 0, y: 4)
                } else {
                    emptyFallback
                }
            } else {
                emptyFallback
            }
        }
        .task {
            await loadFile()
        }
    }

    private var emptyFallback: some View {
        VStack(spacing: VPSpace.sm) {
            Image(systemName: "doc.text.fill")
                .font(.system(size: 48))
                .foregroundStyle(VPColor.secondaryText)
            Text(errorMessage ?? String(localized: "documents.preview.unavailable"))
                .font(.subheadline)
                .foregroundStyle(VPColor.secondaryText)
            if let fileUrl {
                Link(String(localized: "documents.preview.open_browser"), destination: fileUrl)
                    .font(.subheadline.bold())
                    .foregroundStyle(Color.blue)
            }
        }
        .frame(maxWidth: .infinity, minHeight: 240)
        .vpCard()
    }

    private func loadFile() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            let token = await SessionController.shared.currentAccessToken() ?? ""
            let configuration = try AppConfiguration.load()
            let url = configuration.apiBaseURL.appendingPathComponent("app/documents/\(documentId)/file")

            var request = URLRequest(url: url, timeoutInterval: 25)
            if !token.isEmpty {
                request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            }
            request.setValue(organizationId, forHTTPHeaderField: "X-Organization-ID")
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
                errorMessage = String(localized: "documents.preview.access_error")
                return
            }
            self.fileData = data
            let isPdfHeader = data.prefix(4) == Data([0x25, 0x50, 0x44, 0x46]) // %PDF
            self.isPDF = isPdfHeader || (http.mimeType?.contains("pdf") == true)
            let fileExtension = self.isPDF ? "pdf" : (http.mimeType?.contains("png") == true ? "png" : "jpg")
            let localURL = FileManager.default.temporaryDirectory
                .appendingPathComponent("vergipro-document-\(documentId)")
                .appendingPathExtension(fileExtension)
            try data.write(to: localURL, options: .atomic)
            self.fileUrl = localURL
        } catch {
            errorMessage = String(localized: "documents.preview.network_error")
        }
    }
}
