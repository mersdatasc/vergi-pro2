import SwiftUI

struct RootView: View {
    @EnvironmentObject private var appState: AppState
    let onSignOut: () -> Void
    @State private var loadedDestinations: Set<AppDestination> = [.today]

    var body: some View {
        TabView(selection: $appState.selectedDestination) {
            lazyTab(.today) { HomeView() }
                .tag(AppDestination.today)
                .tabItem { Label(AppDestination.today.title, systemImage: AppDestination.today.symbol) }

            lazyTab(.documents) { DocumentsCenterView() }
                .tag(AppDestination.documents)
                .tabItem { Label(AppDestination.documents.title, systemImage: AppDestination.documents.symbol) }

            lazyTab(.capture) { CaptureView() }
                .tag(AppDestination.capture)
                .tabItem { Label(AppDestination.capture.title, systemImage: AppDestination.capture.symbol) }

            lazyTab(.finance) { FinanceCenterView() }
                .tag(AppDestination.finance)
                .tabItem { Label(AppDestination.finance.title, systemImage: AppDestination.finance.symbol) }

            lazyTab(.workspace) { WorkspaceView(onSignOut: onSignOut) }
                .tag(AppDestination.workspace)
                .tabItem { Label(AppDestination.workspace.title, systemImage: AppDestination.workspace.symbol) }
        }
        .tint(VPColor.brand)
        .onChange(of: appState.selectedDestination) { _, destination in
            loadedDestinations.insert(destination)
        }
    }

    @ViewBuilder
    private func lazyTab<Content: View>(
        _ destination: AppDestination,
        @ViewBuilder content: () -> Content
    ) -> some View {
        if loadedDestinations.contains(destination) {
            content()
        } else {
            Color.clear
        }
    }
}

private struct PlaceholderFeatureView: View {
    let destination: AppDestination

    var body: some View {
        NavigationStack {
            ContentUnavailableView(destination.title, systemImage: destination.symbol)
                .navigationTitle(destination.title)
        }
    }
}
