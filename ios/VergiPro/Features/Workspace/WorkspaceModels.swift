import Foundation

enum WorkspaceRole: String, CaseIterable, Identifiable {
    case owner
    case accountant
    case employee

    var id: Self { self }
}

struct ClosingTask: Identifiable {
    enum State { case complete, business, accountant, risk }
    let id = UUID()
    let titleKey: String
    let detailKey: String
    let state: State

    static let preview: [ClosingTask] = [
        .init(titleKey: "workspace.closing.task.sales", detailKey: "workspace.closing.task.sales.detail", state: .complete),
        .init(titleKey: "workspace.closing.task.missing", detailKey: "workspace.closing.task.missing.detail", state: .business),
        .init(titleKey: "workspace.closing.task.vat", detailKey: "workspace.closing.task.vat.detail", state: .accountant),
        .init(titleKey: "workspace.closing.task.anomaly", detailKey: "workspace.closing.task.anomaly.detail", state: .risk)
    ]
}

