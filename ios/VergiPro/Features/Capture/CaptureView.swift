import SwiftUI
import PhotosUI
import VisionKit

struct CaptureView: View {
    @EnvironmentObject private var appState: AppState
    @State private var selectedPhotos: [PhotosPickerItem] = []
    @StateObject private var draft = CaptureDraft()
    @State private var isScannerPresented = false
    @State private var isDetailsPresented = false
    @State private var isUploading = false
    @State private var errorMessage: String?
    @State private var isDraftProtectedAfterFailure = false
    @State private var successUploadedDoc: BackendDocument?

    @State private var documentType: String = "EXPENSE"
    @State private var documentDescription: String = ""
    @State private var plate: String = ""
    @State private var isExternal: Bool = false

    private let repository: LiveFeatureRepository?

    init() {
        if let configuration = try? AppConfiguration.load() {
            repository = LiveFeatureRepository(client: APIClient(baseURL: configuration.apiBaseURL))
        } else {
            repository = nil
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                VPColor.canvas.ignoresSafeArea()

                if draft.pages.isEmpty {
                    emptyCaptureView
                } else {
                    reviewPagesView
                }
            }
            .navigationTitle(String(localized: "capture.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if !draft.pages.isEmpty {
                    ToolbarItem(placement: .topBarTrailing) {
                        PhotosPicker(selection: $selectedPhotos, maxSelectionCount: 10, matching: .images) {
                            Image(systemName: "plus.circle.fill")
                                .font(.title3)
                                .foregroundStyle(VPColor.brand)
                        }
                    }
                }
            }
            .sheet(isPresented: $isScannerPresented) {
                DocumentScannerView(
                    onComplete: { scanned in
                        isScannerPresented = false
                        Task {
                            do { try await draft.append(images: scanned) }
                            catch { errorMessage = String(localized: "capture.draft.scan_save_failed") }
                        }
                    },
                    onCancel: { isScannerPresented = false },
                    onError: { err in
                        isScannerPresented = false
                        errorMessage = err.localizedDescription
                    }
                )
                .ignoresSafeArea()
            }
            .sheet(isPresented: $isDetailsPresented) {
                CaptureUploadSheet(
                    imageCount: draft.pages.count,
                    documentType: $documentType,
                    description: $documentDescription,
                    plate: $plate,
                    isExternal: $isExternal,
                    isUploading: isUploading,
                    onUpload: performUpload
                )
            }
            .onChange(of: selectedPhotos) { _, items in
                Task {
                    for item in items {
                        if let data = try? await item.loadTransferable(type: Data.self),
                           let image = UIImage(data: data) {
                            do { try await draft.append(images: [image]) }
                            catch { errorMessage = String(localized: "capture.draft.library_save_failed") }
                        }
                    }
                    selectedPhotos.removeAll()
                }
            }
            .alert(String(localized: "capture.alert.title"), isPresented: Binding(
                get: { errorMessage != nil },
                set: { if !$0 { errorMessage = nil } }
            )) {
                Button(String(localized: "common.ok"), role: .cancel) {}
            } message: {
                Text(errorMessage ?? "")
            }
            .task(id: appState.currentOrganization?.id) {
                guard let rawID = appState.currentOrganization?.id, let organizationID = Int(rawID) else { return }
                do { try await draft.configure(organizationID: organizationID) }
                catch { errorMessage = String(localized: "capture.draft.restore_failed") }
            }
        }
    }

    private var emptyCaptureView: some View {
        VStack(spacing: VPSpace.xl) {
            Spacer()

            ZStack {
                Circle()
                    .fill(VPColor.surface)
                    .frame(width: 120, height: 120)
                    .overlay(Circle().strokeBorder(VPColor.cardBorder, lineWidth: 1))
                Image(systemName: "doc.viewfinder")
                    .font(.system(size: 48, weight: .light))
                    .foregroundStyle(VPColor.brand)
            }

            VStack(spacing: VPSpace.xs) {
                Text("capture.empty.title")
                    .font(.title2.bold())
                    .foregroundStyle(.primary)
                Text("capture.description")
                    .font(.subheadline)
                    .foregroundStyle(VPColor.secondaryText)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, VPSpace.md)
            }

            Spacer()

            VStack(spacing: VPSpace.sm) {
                if VNDocumentCameraViewController.isSupported {
                    Button {
                        isScannerPresented = true
                    } label: {
                        HStack {
                            Image(systemName: "camera.viewfinder")
                            Text("capture.camera")
                        }
                        .frame(maxWidth: .infinity, minHeight: 52)
                    }
                    .buttonStyle(VPPrimaryButtonStyle())
                }

                PhotosPicker(selection: $selectedPhotos, maxSelectionCount: 10, matching: .images) {
                    HStack {
                        Image(systemName: "photo.on.rectangle.angled")
                        Text("capture.library.multiple")
                    }
                    .font(.headline)
                    .frame(maxWidth: .infinity, minHeight: 52)
                    .foregroundStyle(.primary)
                    .background(VPColor.surface, in: RoundedRectangle(cornerRadius: VPRadius.medium, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: VPRadius.medium, style: .continuous).stroke(VPColor.cardBorder, lineWidth: 1))
                }
            }
        }
        .padding(VPSpace.lg)
    }

    private var reviewPagesView: some View {
        VStack(spacing: 0) {
            ScrollView {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 140), spacing: VPSpace.md)], spacing: VPSpace.md) {
                    ForEach(Array(draft.pages.enumerated()), id: \.element.id) { index, page in
                        ZStack(alignment: .topTrailing) {
                            Image(uiImage: page.image)
                                .resizable()
                                .scaledToFill()
                                .frame(height: 180)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(VPColor.cardBorder, lineWidth: 1))

                            Button {
                                Task { await draft.removePage(id: page.id) }
                            } label: {
                                Image(systemName: "xmark.circle.fill")
                                    .font(.title3)
                                    .foregroundStyle(Color.red, Color.white)
                                    .padding(6)
                            }
                        }
                    }
                }
                .padding(VPSpace.md)
            }

            VStack(spacing: VPSpace.sm) {
                if isDraftProtectedAfterFailure {
                    HStack(alignment: .top, spacing: VPSpace.sm) {
                        Image(systemName: "lock.shield.fill")
                            .foregroundStyle(VPColor.warning)
                        VStack(alignment: .leading, spacing: 3) {
                            Text(String(localized: "capture.retry.protected.title"))
                                .font(.subheadline.weight(.semibold))
                            Text(String(localized: "capture.retry.protected.detail"))
                                .font(.caption)
                                .foregroundStyle(VPColor.secondaryText)
                        }
                        Spacer()
                    }
                    .padding(VPSpace.sm)
                    .background(VPColor.warning.opacity(0.10), in: RoundedRectangle(cornerRadius: VPRadius.small, style: .continuous))
                }

                Button {
                    performUpload()
                } label: {
                    HStack {
                        if isUploading {
                            ProgressView().tint(VPColor.brandInverse).padding(.trailing, 4)
                            Text("capture.submission.uploading")
                        } else {
                            Image(systemName: "doc.text.viewfinder")
                            Text(String(format: String(localized: "capture.upload.count"), draft.pages.count))
                        }
                    }
                    .frame(maxWidth: .infinity, minHeight: 52)
                }
                .buttonStyle(VPPrimaryButtonStyle())
                .disabled(isUploading)

                HStack {
                    Button(String(localized: "capture.details.add")) {
                        isDetailsPresented = true
                    }
                    .font(.subheadline)
                    .foregroundStyle(VPColor.brand)

                    Spacer()

                    Button(String(localized: "capture.clear_all"), role: .destructive) {
                        Task {
                            for page in draft.pages { await draft.removePage(id: page.id) }
                        }
                    }
                    .font(.subheadline)
                    .foregroundStyle(Color.red)
                }
                .padding(.horizontal, VPSpace.xs)
            }
            .padding(VPSpace.md)
            .background(VPColor.surface)
            .overlay(Rectangle().frame(height: 1).foregroundStyle(VPColor.cardBorder), alignment: .top)
        }
    }

    private func performUpload() {
        guard let repository,
              let organizationID = appState.currentOrganization?.id,
              let orgId = Int(organizationID) else {
            errorMessage = String(localized: "capture.organization_missing")
            return
        }

        isUploading = true
        isDraftProtectedAfterFailure = false
        Task {
            defer { isUploading = false }
            do {
                _ = try draft.beginSubmission()
                if draft.pages.count == 1 {
                    let img = draft.pages[0].image
                    guard let data = img.jpegData(compressionQuality: 0.85) else {
                        throw CapturePersistenceError.encodingFailed
                    }
                    let doc = try await repository.uploadDocument(
                        organization: OrganizationContext(organizationID: orgId, membershipID: 0),
                        fileData: data,
                        filename: "scan-\(UUID().uuidString.prefix(8)).jpg",
                        docType: documentType,
                        description: documentDescription.isEmpty ? nil : documentDescription,
                        plate: plate.isEmpty ? nil : plate,
                        isHarici: isExternal
                    )
                    successUploadedDoc = doc
                } else {
                    let files: [(Data, String)] = draft.pages.compactMap { page in
                        guard let d = page.image.jpegData(compressionQuality: 0.85) else { return nil }
                        return (d, "scan-\(UUID().uuidString.prefix(8)).jpg")
                    }
                    let docs = try await repository.uploadBatch(
                        organization: OrganizationContext(organizationID: orgId, membershipID: 0),
                        files: files,
                        docType: documentType,
                        description: documentDescription.isEmpty ? nil : documentDescription,
                        plate: plate.isEmpty ? nil : plate,
                        isHarici: isExternal
                    )
                    guard let firstDocument = docs.first else { throw APIError.decoding }
                    successUploadedDoc = firstDocument
                }

                await draft.completeUpload()
                isDraftProtectedAfterFailure = false
                isDetailsPresented = false
                appState.selectedDestination = .documents
            } catch {
                if let submissionID = draft.submissionID { draft.rollbackSubmission(submissionID) }
                isDraftProtectedAfterFailure = true
                errorMessage = uploadFailureMessage(for: error)
            }
        }
    }

    private func uploadFailureMessage(for error: Error) -> String {
        guard let apiError = error as? APIError else {
            return String(localized: "capture.retry.failed")
        }
        return switch apiError {
        case .networkUnavailable:
            String(localized: "capture.retry.offline")
        case .authenticationRequired:
            String(localized: "capture.retry.authentication")
        case .forbidden:
            String(localized: "capture.retry.forbidden")
        case .rateLimited:
            String(localized: "capture.retry.rate_limited")
        case .maintenance:
            String(localized: "capture.retry.maintenance")
        default:
            String(localized: "capture.retry.failed")
        }
    }
}

// MARK: - Capture Upload Sheet
private struct CaptureUploadSheet: View {
    @Environment(\.dismiss) private var dismiss
    let imageCount: Int
    @Binding var documentType: String
    @Binding var description: String
    @Binding var plate: String
    @Binding var isExternal: Bool
    let isUploading: Bool
    let onUpload: () -> Void

    var body: some View {
        NavigationStack {
            Form {
                Section(String(localized: "capture.details.type")) {
                    Picker(String(localized: "capture.details.type"), selection: $documentType) {
                        Text("capture.type.expense").tag("EXPENSE")
                        Text("capture.type.fuel").tag("FUEL")
                        Text("capture.type.receipt").tag("RECEIPT")
                        Text("capture.type.sales").tag("SALES")
                    }
                    .pickerStyle(.menu)
                }

                Section(String(localized: "capture.details.optional")) {
                    TextField(String(localized: "capture.details.description"), text: $description)
                    if documentType == "FUEL" {
                        TextField(String(localized: "capture.details.plate"), text: $plate)
                            .textInputAutocapitalization(.characters)
                            .autocorrectionDisabled()
                    }
                    Toggle(String(localized: "capture.details.external"), isOn: $isExternal)
                }

                Section {
                    Button(action: onUpload) {
                        HStack {
                            Spacer()
                            if isUploading {
                                ProgressView().tint(.white).padding(.trailing, 6)
                            }
                            Text(isUploading ? String(localized: "capture.submission.uploading") : String(format: String(localized: "capture.upload.count"), imageCount))
                                .fontWeight(.bold)
                            Spacer()
                        }
                    }
                    .disabled(isUploading)
                    .listRowBackground(isUploading ? Color.gray : VPColor.brand)
                    .foregroundStyle(VPColor.brandInverse)
                } footer: {
                    Text("capture.details.review_notice")
                }
            }
            .navigationTitle(String(localized: "capture.details.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "common.cancel")) { dismiss() }
                        .disabled(isUploading)
                }
            }
        }
        .interactiveDismissDisabled(isUploading)
    }
}
