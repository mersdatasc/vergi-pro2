import Foundation
import SwiftUI

enum DocFilterCategory: String, CaseIterable, Identifiable, Sendable {
    case all = "ALL"
    case pending = "PENDING"
    case expense = "EXPENSE"
    case sales = "SALES"
    case fuel = "FUEL"
    case receipt = "RECEIPT"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .all: return String(localized: "documents.filter.all")
        case .pending: return String(localized: "documents.filter.pending")
        case .expense: return String(localized: "documents.filter.expense")
        case .sales: return String(localized: "documents.filter.sales")
        case .fuel: return String(localized: "documents.filter.fuel")
        case .receipt: return String(localized: "documents.filter.receipt")
        }
    }
}

@MainActor
final class DocumentsViewModel: ObservableObject {
    @Published var documents: [BackendDocument] = []
    @Published var isLoading = false
    @Published var isBulkConfirming = false
    @Published var errorMessage: String?
    @Published var successMessage: String?
    @Published var searchQuery = ""
    @Published var selectedFilter: DocFilterCategory = .all

    private let repository: LiveFeatureRepository?

    init() {
        if let configuration = try? AppConfiguration.load() {
            self.repository = LiveFeatureRepository(client: APIClient(baseURL: configuration.apiBaseURL))
        } else {
            self.repository = nil
        }
    }

    var filteredDocuments: [BackendDocument] {
        documents.filter { doc in
            let q = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            let matchQuery = q.isEmpty ||
                doc.displayTitle.lowercased().contains(q) ||
                (doc.invoiceNo?.lowercased().contains(q) == true) ||
                (doc.category?.lowercased().contains(q) == true) ||
                (doc.plate?.lowercased().contains(q) == true) ||
                (doc.supplierVkn?.contains(q) == true)

            let matchFilter = switch selectedFilter {
            case .all: true
            case .pending: doc.status == "PENDING_REVIEW"
            case .expense: doc.docType == "EXPENSE"
            case .sales: doc.docType == "SALES"
            case .fuel: doc.docType == "FUEL"
            case .receipt: doc.docType == "RECEIPT"
            }

            return matchQuery && matchFilter
        }
    }

    var countTotal: Int { documents.count }
    var countPending: Int { documents.filter { $0.status == "PENDING_REVIEW" }.count }
    var countApproved: Int { documents.filter { $0.status == "APPROVED" || $0.status == "CONFIRMED" }.count }

    func fetchDocuments(organizationId: String) async {
        guard let repository, let orgID = Int(organizationId) else { return }
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            documents = try await repository.listDocuments(
                organization: OrganizationContext(organizationID: orgID, membershipID: 0),
                search: searchQuery.isEmpty ? nil : searchQuery,
                docType: nil,
                status: nil
            )
        } catch {
            errorMessage = String(localized: "documents.error.load")
        }
    }

    @discardableResult
    func updateDocumentStatus(organizationId: String, docId: Int, newStatus: String) async -> Bool {
        guard let repository, let orgID = Int(organizationId) else {
            errorMessage = String(localized: "documents.error.organization")
            return false
        }
        do {
            let updated = try await repository.updateStatus(
                organization: OrganizationContext(organizationID: orgID, membershipID: 0),
                docId: docId,
                status: newStatus
            )
            if let index = documents.firstIndex(where: { $0.id == docId }) {
                documents[index] = updated
            }
            return true
        } catch {
            errorMessage = String(localized: "documents.error.status")
            return false
        }
    }

    func quickUpdateCategory(organizationId: String, docId: Int, category: String, plate: String? = nil, isHarici: Bool = false) async {
        guard let repository, let orgID = Int(organizationId) else { return }
        do {
            let updated = try await repository.quickUpdateCategory(
                organization: OrganizationContext(organizationID: orgID, membershipID: 0),
                docId: docId,
                category: category,
                plate: plate,
                isHarici: isHarici
            )
            if let index = documents.firstIndex(where: { $0.id == docId }) {
                documents[index] = updated
            }
            successMessage = String(format: String(localized: "documents.success.category"), category)
        } catch {
            errorMessage = String(localized: "documents.error.category")
        }
    }

    func bulkConfirmAll(organizationId: String) async {
        guard let repository, let orgID = Int(organizationId) else { return }
        isBulkConfirming = true
        errorMessage = nil
        defer { isBulkConfirming = false }

        do {
            let pendingIds = documents.filter { $0.status == "PENDING_REVIEW" }.map(\.id)
            let confirmedCount = try await repository.bulkConfirm(
                organization: OrganizationContext(organizationID: orgID, membershipID: 0),
                documentIds: pendingIds.isEmpty ? nil : pendingIds
            )
            await fetchDocuments(organizationId: organizationId)
            successMessage = String(format: String(localized: "documents.success.bulk"), confirmedCount)
        } catch {
            errorMessage = String(localized: "documents.error.bulk")
        }
    }

    @discardableResult
    func deleteDocument(organizationId: String, docId: Int) async -> Bool {
        guard let repository, let orgID = Int(organizationId) else {
            errorMessage = String(localized: "documents.error.organization")
            return false
        }
        do {
            let success = try await repository.deleteDocument(
                organization: OrganizationContext(organizationID: orgID, membershipID: 0),
                docId: docId
            )
            if success {
                documents.removeAll { $0.id == docId }
                successMessage = String(localized: "documents.success.delete")
                return true
            }
            errorMessage = String(localized: "documents.error.delete")
            return false
        } catch {
            errorMessage = String(localized: "documents.error.delete")
            return false
        }
    }
}
