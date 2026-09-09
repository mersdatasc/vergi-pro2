import SwiftUI

enum AppDestination: String, CaseIterable, Hashable, Identifiable {
    case today
    case documents
    case capture
    case finance
    case workspace

    var id: Self { self }

    var title: LocalizedStringKey {
        switch self {
        case .today: "nav.today"
        case .documents: "nav.documents"
        case .capture: "nav.capture"
        case .finance: "nav.finance"
        case .workspace: "nav.workspace"
        }
    }

    var symbol: String {
        switch self {
        case .today: "sparkles"
        case .documents: "doc.text"
        case .capture: "viewfinder"
        case .finance: "chart.line.uptrend.xyaxis"
        case .workspace: "square.grid.2x2"
        }
    }
}

