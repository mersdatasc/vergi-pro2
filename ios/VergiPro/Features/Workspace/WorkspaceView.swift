import SwiftUI

struct WorkspaceView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var model = WorkspaceLiveViewModel()
    @State private var isOrgSwitcherPresented = false
    let onSignOut: () -> Void

    private var role: WorkspaceRole {
        switch appState.currentOrganization?.role.uppercased() {
        case "OWNER", "ADMIN": return .owner
        case "SMMM", "ACCOUNTANT": return .accountant
        default: return .employee
        }
    }

    private var organizationCaption: String {
        let roleLabel: String = switch role {
        case .owner: String(localized: "workspace.role.owner")
        case .accountant: String(localized: "workspace.role.accountant")
        case .employee: String(localized: "workspace.role.employee")
        }
        guard let tier = appState.currentOrganization?.planTier, !tier.isEmpty else {
            return String(format: String(localized: "workspace.caption.plan_unavailable"), roleLabel)
        }
        let planLabel: String = switch tier.uppercased() {
        case "STARTER": String(localized: "billing.plan.starter")
        case "PRO": String(localized: "billing.plan.pro")
        case "OFFICE": String(localized: "billing.plan.office")
        case "ENTERPRISE": String(localized: "billing.plan.enterprise")
        default: tier
        }
        let status = appState.currentOrganization?.subscriptionStatus?.uppercased()
        return status == "TRIALING"
            ? String(format: String(localized: "workspace.caption.trial"), roleLabel)
            : String(format: String(localized: "workspace.caption.plan"), roleLabel, planLabel)
    }

    private var accountantConnectionLabel: String {
        model.team.contains { ["SMMM", "ACCOUNTANT"].contains($0.role.uppercased()) }
            ? "Bağlı"
            : "Bağlı Değil"
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: VPSpace.lg) {
                    organizationHeader
                    primaryActions
                    controls
                }
                .padding(VPSpace.md)
                .padding(.bottom, VPSpace.xxl)
            }
            .background(VPColor.canvas)
            .navigationTitle(String(localized: "workspace.title"))
            .task(id: appState.currentOrganization?.id) {
                guard let id = Int(appState.currentOrganization?.id ?? "") else { return }
                await model.load(organizationID: id)
            }
            .refreshable {
                guard let id = Int(appState.currentOrganization?.id ?? "") else { return }
                await model.load(organizationID: id)
            }
        }
    }

    private var organizationHeader: some View {
        let orgName = appState.currentOrganization?.name ?? String(localized: "workspace.organization.placeholder")
        let orgInitial = String(orgName.prefix(1)).uppercased()

        return Button {
            if appState.availableOrganizations.count > 1 {
                isOrgSwitcherPresented = true
            }
        } label: {
            HStack(spacing: VPSpace.md) {
                Text(orgInitial)
                    .font(.title2.bold())
                    .foregroundStyle(VPColor.brandInverse)
                    .frame(width: 54, height: 54)
                    .background(VPColor.brand, in: RoundedRectangle(cornerRadius: 17, style: .continuous))
                VStack(alignment: .leading, spacing: 3) {
                    Text(orgName).font(.title2.bold()).foregroundStyle(.primary)
                    Text(organizationCaption).font(.subheadline).foregroundStyle(VPColor.secondaryText)
                }
                Spacer()
                if appState.availableOrganizations.count > 1 {
                    Image(systemName: "chevron.up.chevron.down").foregroundStyle(VPColor.brand)
                }
            }
            .frame(maxWidth: .infinity, minHeight: 58)
            .contentShape(Rectangle())
            .vpCard()
        }
        .buttonStyle(.plain)
        .sheet(isPresented: $isOrgSwitcherPresented) {
            NavigationStack {
                List(appState.availableOrganizations) { org in
                    Button {
                        appState.currentOrganization = org
                        isOrgSwitcherPresented = false
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(org.name).font(.headline)
                                Text(org.role).font(.caption).foregroundStyle(VPColor.secondaryText)
                            }
                            Spacer()
                            if org.id == appState.currentOrganization?.id {
                                Image(systemName: "checkmark").foregroundStyle(VPColor.brand).bold()
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }
                .navigationTitle(String(localized: "workspace.organization.select"))
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button(String(localized: "common.close")) { isOrgSwitcherPresented = false }
                    }
                }
            }
            .presentationDetents([.medium, .large])
        }
    }

    @ViewBuilder
    private var primaryActions: some View {
        Text(String(localized: "workspace.primary")).font(.title2.bold())
        switch role {
        case .owner:
            workspaceLink("Müşteri şirketi oluştur", "", "plus.square.on.square", VPColor.success) { CreateOrganizationView(model: model) }
            workspaceLink("workspace.organization", "", "building.2", VPColor.brand) { OrganizationProfileView(model: model) }
            workspaceLink("workspace.action.closing", "", "checklist", VPColor.brand) { ClosingRoomView() }
            workspaceLink("workspace.action.team", LocalizedStringKey(String(model.team.count)), "person.2", VPColor.success) { TeamManagementView(model: model) }
            workspaceLink("workspace.accountant", LocalizedStringKey(accountantConnectionLabel), "person.crop.circle.badge.checkmark", VPColor.brand) { AccountantInviteView(model: model) }
            workspaceLink("workspace.action.fleet", LocalizedStringKey(String(model.vehicles.count)), "car", VPColor.warning) { FleetManagementView(model: model) }
            workspaceLink("workspace.telegram.title", "", "paperplane.fill", VPColor.accentBlue) { TelegramConnectionView(model: model) }
            workspaceLink("workspace.action.export", "", "square.and.arrow.up", VPColor.accentBlue) { LucaZirveExportDesk(organizationId: appState.currentOrganization?.id ?? "") }
            workspaceLink("workspace.action.accountant_portfolio", LocalizedStringKey(model.portfolio.map { String($0.clients.count) } ?? ""), "building.2.crop.circle", VPColor.brand) { AccountantPortfolioView(model: model) }
            workspaceLink("workspace.action.affiliate", "", "chart.pie.fill", VPColor.success) { AffiliateCommissionsView(model: model) }
        case .accountant:
            workspaceLink("Müşteri şirketi oluştur", "", "plus.square.on.square", VPColor.success) { CreateOrganizationView(model: model) }
            workspaceLink("workspace.action.client_portfolio", LocalizedStringKey(model.portfolio.map { String($0.clients.count) } ?? ""), "building.2.crop.circle", VPColor.brand) { AccountantPortfolioView(model: model) }
            workspaceLink("workspace.action.affiliate", "", "chart.pie.fill", VPColor.success) { AffiliateCommissionsView(model: model) }
            workspaceLink("workspace.action.closing", "", "checklist", VPColor.warning) { ClosingRoomView() }
            workspaceLink("workspace.action.export_luca_zirve", "", "square.and.arrow.up", VPColor.accentBlue) { LucaZirveExportDesk(organizationId: appState.currentOrganization?.id ?? "") }
        case .employee:
            workspaceLink("workspace.action.documents", "", "doc.text", VPColor.success) { DocumentsCenterView() }
            workspaceLink("workspace.action.scan", "", "camera", VPColor.brand) { CaptureView() }
        }
    }

    private var controls: some View {
        VStack(alignment: .leading, spacing: VPSpace.sm) {
            Text(String(localized: "workspace.controls")).font(.title2.bold())
            NavigationLink { SubscriptionBillingView() } label: { settingsRow("workspace.action.billing", "creditcard.fill") }.buttonStyle(.plain)
            NavigationLink { PreferencesView() } label: { settingsRow("workspace.action.preferences", "paintbrush") }.buttonStyle(.plain)
            NavigationLink { LinkOrganizationView(model: model) } label: { settingsRow("workspace.organization.link", "link") }.buttonStyle(.plain)
            Button(action: onSignOut) {
                HStack { Label(String(localized: "workspace.sign_out"), systemImage: "rectangle.portrait.and.arrow.right").foregroundStyle(VPColor.danger); Spacer() }
                    .vpCard()
            }
            .buttonStyle(.plain)
        }
    }

    private func workspaceLink<Destination: View>(
        _ title: LocalizedStringKey, _ value: LocalizedStringKey, _ symbol: String, _ tint: Color,
        @ViewBuilder destination: () -> Destination
    ) -> some View {
        NavigationLink(destination: destination) {
            HStack(spacing: VPSpace.sm) {
                Image(systemName: symbol).font(.headline).foregroundStyle(tint).frame(width: 42, height: 42).background(tint.opacity(0.10), in: RoundedRectangle(cornerRadius: 13))
                Text(title).font(.headline).foregroundStyle(.primary)
                Spacer()
                Text(value).font(.subheadline.bold().monospacedDigit()).foregroundStyle(tint)
                Image(systemName: "chevron.right").font(.caption.bold()).foregroundStyle(.tertiary)
            }
            .frame(maxWidth: .infinity, minHeight: 56)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .vpCard()
    }

    private func settingsRow(_ title: LocalizedStringKey, _ symbol: String) -> some View {
        HStack { Label(title, systemImage: symbol).foregroundStyle(.primary); Spacer(); Image(systemName: "chevron.right").foregroundStyle(.tertiary) }
            .frame(maxWidth: .infinity, minHeight: 56)
            .contentShape(Rectangle())
            .vpCard()
    }
}

@MainActor
final class WorkspaceLiveViewModel: ObservableObject {
    @Published private(set) var team: [TeamMember] = []
    @Published private(set) var vehicles: [VehicleModel] = []
    @Published private(set) var portfolio: AccountantPortfolioDTO?
    @Published private(set) var affiliate: AffiliateDashboardDTO?
    @Published private(set) var organization: TenantOrg?
    @Published private(set) var isWorking = false
    @Published var errorMessage: String?
    private let repository: LiveFeatureRepository?

    init() {
        if let configuration = try? AppConfiguration.load() {
            repository = LiveFeatureRepository(client: APIClient(baseURL: configuration.apiBaseURL))
        } else {
            repository = nil
        }
    }

    func load(organizationID: Int) async {
        guard let repository else { return }
        isWorking = true
        errorMessage = nil
        defer { isWorking = false }
        let context = OrganizationContext(organizationID: organizationID, membershipID: 0)
        async let loadedTeam = repository.loadTeam(organization: context)
        async let loadedVehicles = repository.loadVehicles(organization: context)
        async let loadedPortfolio = repository.fetchPortfolio(organization: context)
        async let loadedAffiliate = repository.fetchAffiliateDashboard(organization: context)
        async let loadedDashboard = repository.loadDashboard(organization: context)
        do {
            team = try await loadedTeam
            vehicles = try await loadedVehicles
            portfolio = try? await loadedPortfolio
            affiliate = try? await loadedAffiliate
            organization = try? await loadedDashboard.organization
        } catch {
            errorMessage = String(localized: "workspace.error.load")
        }
    }

    func addVehicle(organizationID: Int, plate: String, model: String?) async -> Bool {
        guard let repository else { return false }
        isWorking = true
        defer { isWorking = false }
        do {
            let vehicle = try await repository.addVehicle(
                organization: OrganizationContext(organizationID: organizationID, membershipID: 0),
                plate: plate,
                model: model
            )
            vehicles.append(vehicle)
            return true
        } catch {
            errorMessage = String(localized: "workspace.error.vehicle")
            return false
        }
    }

    func inviteMember(organizationID: Int, fullName: String, email: String, role: String) async -> Bool {
        guard let repository else { return false }
        isWorking = true
        defer { isWorking = false }
        do {
            let member = try await repository.inviteMember(
                organization: OrganizationContext(organizationID: organizationID, membershipID: 0),
                fullName: fullName,
                email: email,
                role: role
            )
            team.append(member)
            return true
        } catch {
            errorMessage = String(localized: "workspace.error.invite")
            return false
        }
    }

    func regenerateTelegramCode(organizationID: Int) async -> Bool {
        guard let repository else { return false }
        isWorking = true
        defer { isWorking = false }
        do {
            let code = try await repository.regenerateTelegramCode(organization: OrganizationContext(organizationID: organizationID, membershipID: 0))
            await load(organizationID: organizationID)
            return !code.isEmpty
        } catch {
            errorMessage = String(localized: "workspace.telegram.error")
            return false
        }
    }

    func inviteAccountant(organizationID: Int, email: String, name: String?, licenseNo: String?) async -> Bool {
        guard let repository else { return false }
        isWorking = true
        defer { isWorking = false }
        do {
            try await repository.inviteAccountant(organization: OrganizationContext(organizationID: organizationID, membershipID: 0), email: email, name: name, licenseNo: licenseNo, message: nil)
            await load(organizationID: organizationID)
            return true
        } catch { errorMessage = "Davet gönderilemedi."; return false }
    }

    func updateOrganization(organizationID: Int, name: String, vkn: String, taxOffice: String, taxRegime: String) async -> Bool {
        guard let repository else { return false }
        isWorking = true; defer { isWorking = false }
        do { try await repository.updateOrganization(organization: OrganizationContext(organizationID: organizationID, membershipID: 0), name: name, vkn: vkn, taxOffice: taxOffice, taxRegime: taxRegime); await load(organizationID: organizationID); return true }
        catch { errorMessage = "Şirket profili güncellenemedi."; return false }
    }

    func linkOrganization(code: String) async -> Bool {
        guard let repository else { return false }
        isWorking = true; defer { isWorking = false }
        do { try await repository.linkOrganizationByCode(code); return true }
        catch { errorMessage = "Şirket bağlantısı kurulamadı."; return false }
    }
    func createOrganization(name: String, vkn: String, taxOffice: String, taxRegime: String) async -> Bool {
        guard let repository else { return false }; isWorking = true; defer { isWorking = false }
        do { try await repository.createOrganization(name: name, vkn: vkn, taxOffice: taxOffice, taxRegime: taxRegime); return true }
        catch { errorMessage = "Şirket oluşturulamadı."; return false }
    }
    func updatePayout(iban: String, bank: String?, holder: String?) async -> Bool { guard let repository else { return false }; isWorking = true; defer { isWorking = false }; do { try await repository.updatePayoutSettings(iban: iban, bankName: bank, accountHolder: holder); return true } catch { return false } }
    func updateReferralCode(_ code: String, organizationID: Int) async -> Bool { guard let repository else { return false }; isWorking = true; defer { isWorking = false }; do { try await repository.updateReferralCode(code); affiliate = try? await repository.fetchAffiliateDashboard(organization: OrganizationContext(organizationID: organizationID, membershipID: 0)); return true } catch { return false } }
}

private struct CreateOrganizationView: View {
    @ObservedObject var model: WorkspaceLiveViewModel
    @State private var name = ""; @State private var vkn = ""; @State private var taxOffice = ""; @State private var taxRegime = "STANDART"; @State private var created = false
    var body: some View {
        Form {
            Section("Şirket bilgileri") { TextField("Şirket adı", text: $name); TextField("VKN / TCKN", text: $vkn).keyboardType(.numberPad); TextField("Vergi dairesi", text: $taxOffice); TextField("Vergi rejimi", text: $taxRegime).textInputAutocapitalization(.characters) }
            Button("Oluştur") { Task { created = await model.createOrganization(name: name.trimmingCharacters(in: .whitespaces), vkn: vkn, taxOffice: taxOffice.trimmingCharacters(in: .whitespaces), taxRegime: taxRegime) } }.disabled(name.isEmpty || !(10...11).contains(vkn.count) || taxOffice.isEmpty || model.isWorking)
            if created { Label("Şirket oluşturuldu. Listede görünmezse yeniden giriş yapın.", systemImage: "checkmark.circle.fill").foregroundStyle(VPColor.success) }
        }.navigationTitle("Müşteri şirketi oluştur").overlay { if model.isWorking { ProgressView() } }
    }
}

private struct OrganizationProfileView: View {
    @EnvironmentObject private var appState: AppState
    @ObservedObject var model: WorkspaceLiveViewModel
    @State private var name = ""; @State private var vkn = ""; @State private var taxOffice = ""; @State private var taxRegime = "STANDART"
    var body: some View {
        Form {
            TextField("Şirket adı", text: $name)
            TextField("VKN / TCKN", text: $vkn).keyboardType(.numberPad)
            TextField("Vergi dairesi", text: $taxOffice)
            TextField("Vergi rejimi", text: $taxRegime).textInputAutocapitalization(.characters)
            Button("Kaydet") { guard let id = Int(appState.currentOrganization?.id ?? "") else { return }; Task { _ = await model.updateOrganization(organizationID: id, name: name, vkn: vkn, taxOffice: taxOffice, taxRegime: taxRegime) } }.disabled(name.isEmpty || !(10...11).contains(vkn.count) || model.isWorking)
        }.navigationTitle("Organizasyon profili").onAppear { name = model.organization?.name ?? appState.currentOrganization?.name ?? ""; vkn = model.organization?.vkn ?? ""; taxOffice = model.organization?.taxOffice ?? ""; taxRegime = model.organization?.taxRegime ?? "STANDART" }.overlay { if model.isWorking { ProgressView() } }
    }
}

private struct LinkOrganizationView: View {
    @ObservedObject var model: WorkspaceLiveViewModel
    @State private var code = ""; @State private var linked = false
    var body: some View {
        Form {
            Section("Şirket katılım kodu") { TextField("VERGI-PRO-…", text: $code).textInputAutocapitalization(.characters).autocorrectionDisabled() }
            Button("Bağlan") { Task { linked = await model.linkOrganization(code: code.trimmingCharacters(in: .whitespacesAndNewlines)) } }.disabled(code.count < 6 || model.isWorking)
            if linked { Label("Şirket bağlandı. Listede görünmezse yeniden giriş yapın.", systemImage: "checkmark.circle.fill").foregroundStyle(VPColor.success) }
        }.navigationTitle("Mevcut şirkete bağlan").overlay { if model.isWorking { ProgressView() } }
    }
}

private struct AccountantInviteView: View {
    @EnvironmentObject private var appState: AppState
    @ObservedObject var model: WorkspaceLiveViewModel
    @State private var email = ""
    @State private var name = ""
    @State private var licenseNo = ""
    @State private var sent = false
    var body: some View {
        Form {
            Section("Mali müşavirinizi davet edin") {
                TextField("Mali müşavir e-postası", text: $email).keyboardType(.emailAddress).textInputAutocapitalization(.never)
                TextField("Ad soyad (isteğe bağlı)", text: $name)
                TextField("Ruhsat numarası (isteğe bağlı)", text: $licenseNo)
            }
            Button("Daveti gönder") {
                guard let id = Int(appState.currentOrganization?.id ?? "") else { return }
                Task { sent = await model.inviteAccountant(organizationID: id, email: email.trimmingCharacters(in: .whitespaces), name: name.isEmpty ? nil : name, licenseNo: licenseNo.isEmpty ? nil : licenseNo) }
            }.disabled(!email.contains("@") || model.isWorking)
            if sent { Label("Davet gönderildi", systemImage: "checkmark.circle.fill").foregroundStyle(VPColor.success) }
        }.navigationTitle("SMMM bağlantısı").overlay { if model.isWorking { ProgressView() } }
    }
}

private struct TelegramConnectionView: View {
    @EnvironmentObject private var appState: AppState
    @ObservedObject var model: WorkspaceLiveViewModel
    @State private var confirmRegeneration = false

    var body: some View {
        VStack(alignment: .leading, spacing: VPSpace.lg) {
            Label(String(localized: "workspace.telegram.title"), systemImage: "paperplane.fill").font(.title2.bold())
            Text(String(localized: "workspace.telegram.detail")).foregroundStyle(VPColor.secondaryText)
            VStack(alignment: .leading, spacing: VPSpace.xs) {
                Text(String(localized: "workspace.telegram.code")).font(.caption).foregroundStyle(VPColor.secondaryText)
                Text(model.organization?.telegramCode ?? String(localized: "workspace.telegram.empty"))
                    .font(.title3.bold().monospaced()).textSelection(.enabled)
            }.vpCard()
            Button(String(localized: "workspace.telegram.regenerate"), systemImage: "arrow.clockwise") { confirmRegeneration = true }
                .buttonStyle(.borderedProminent).tint(VPColor.brand)
            Spacer()
        }
        .padding(VPSpace.md).background(VPColor.canvas).navigationTitle(String(localized: "workspace.telegram.title"))
        .confirmationDialog(String(localized: "workspace.telegram.warning"), isPresented: $confirmRegeneration, titleVisibility: .visible) {
            Button(String(localized: "workspace.telegram.regenerate"), role: .destructive) {
                guard let id = Int(appState.currentOrganization?.id ?? "") else { return }
                Task { _ = await model.regenerateTelegramCode(organizationID: id) }
            }
            Button(String(localized: "common.cancel"), role: .cancel) {}
        }
        .overlay { if model.isWorking { ProgressView() } }
    }
}

private struct TeamManagementView: View {
    @EnvironmentObject private var appState: AppState
    @ObservedObject var model: WorkspaceLiveViewModel
    @State private var showingInvite = false
    @State private var fullName = ""
    @State private var email = ""
    @State private var memberRole = "EMPLOYEE"

    var body: some View {
        List {
            if let error = model.errorMessage { Text(error).foregroundStyle(VPColor.danger) }
            if model.team.isEmpty && !model.isWorking {
                ContentUnavailableView(String(localized: "workspace.team.empty.title"), systemImage: "person.2", description: Text(String(localized: "workspace.team.empty.detail")))
            }
            ForEach(model.team) { member in
                HStack(spacing: VPSpace.sm) {
                    Text(String(member.fullName.prefix(1)).uppercased()).font(.headline).frame(width: 38, height: 38).background(VPColor.brand.opacity(0.10), in: RoundedRectangle(cornerRadius: 11))
                    VStack(alignment: .leading) { Text(member.fullName).font(.headline); Text(member.email).font(.caption).foregroundStyle(VPColor.secondaryText) }
                    Spacer()
                    Text(localizedMemberRole(member.role)).font(.caption.bold()).foregroundStyle(VPColor.brand)
                }
            }
        }
        .overlay { if model.isWorking { ProgressView() } }
        .navigationTitle(String(localized: "workspace.team.title"))
        .toolbar { Button(String(localized: "workspace.team.invite"), systemImage: "person.badge.plus") { showingInvite = true } }
        .sheet(isPresented: $showingInvite) {
            NavigationStack {
                Form {
                    TextField(String(localized: "workspace.team.full_name"), text: $fullName)
                    TextField(String(localized: "workspace.team.email"), text: $email).keyboardType(.emailAddress).textInputAutocapitalization(.never)
                    Picker(String(localized: "workspace.team.role"), selection: $memberRole) {
                        Text(String(localized: "workspace.role.employee")).tag("EMPLOYEE")
                        Text(String(localized: "workspace.role.accountant")).tag("SMMM")
                        Text(String(localized: "workspace.role.admin")).tag("ADMIN")
                    }
                }
                .navigationTitle(String(localized: "workspace.team.invite_title"))
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) { Button(String(localized: "common.cancel")) { showingInvite = false } }
                    ToolbarItem(placement: .confirmationAction) {
                        Button(String(localized: "workspace.team.send")) {
                            guard let id = Int(appState.currentOrganization?.id ?? "") else { return }
                            Task { if await model.inviteMember(organizationID: id, fullName: fullName, email: email, role: memberRole) { showingInvite = false; fullName = ""; email = "" } }
                        }.disabled(fullName.trimmingCharacters(in: .whitespaces).count < 2 || !email.contains("@") || model.isWorking)
                    }
                }
            }
        }
    }

    private func localizedMemberRole(_ role: String) -> String {
        switch role.uppercased() {
        case "OWNER": return String(localized: "workspace.role.owner")
        case "ADMIN": return String(localized: "workspace.role.admin")
        case "SMMM", "ACCOUNTANT": return String(localized: "workspace.role.accountant")
        case "EMPLOYEE": return String(localized: "workspace.role.employee")
        default: return String(localized: "workspace.role.unknown")
        }
    }
}

private struct FleetManagementView: View {
    @EnvironmentObject private var appState: AppState
    @ObservedObject var model: WorkspaceLiveViewModel
    @State private var showingAdd = false
    @State private var plate = ""
    @State private var vehicleModel = ""

    var body: some View {
        List {
            if let error = model.errorMessage { Text(error).foregroundStyle(VPColor.danger) }
            if model.vehicles.isEmpty && !model.isWorking {
                ContentUnavailableView(String(localized: "workspace.fleet.empty.title"), systemImage: "car", description: Text(String(localized: "workspace.fleet.empty.detail")))
            }
            ForEach(model.vehicles) { vehicle in
                HStack { Image(systemName: "car.fill").foregroundStyle(VPColor.warning); VStack(alignment: .leading) { Text(vehicle.plate).font(.headline.monospaced()); Text(vehicle.brandModel ?? String(localized: "workspace.fleet.company_vehicle")).font(.caption).foregroundStyle(VPColor.secondaryText) }; Spacer() }
            }
        }
        .overlay { if model.isWorking { ProgressView() } }
        .navigationTitle(String(localized: "workspace.fleet.title"))
        .toolbar { Button(String(localized: "workspace.fleet.add"), systemImage: "plus") { showingAdd = true } }
        .sheet(isPresented: $showingAdd) {
            NavigationStack {
                Form { TextField(String(localized: "workspace.fleet.plate"), text: $plate).textInputAutocapitalization(.characters); TextField(String(localized: "workspace.fleet.model"), text: $vehicleModel) }
                    .navigationTitle(String(localized: "workspace.fleet.new"))
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) { Button(String(localized: "common.cancel")) { showingAdd = false } }
                        ToolbarItem(placement: .confirmationAction) {
                            Button(String(localized: "common.save")) {
                                guard let id = Int(appState.currentOrganization?.id ?? "") else { return }
                                Task { if await model.addVehicle(organizationID: id, plate: plate, model: vehicleModel.isEmpty ? nil : vehicleModel) { showingAdd = false; plate = ""; vehicleModel = "" } }
                            }.disabled(plate.trimmingCharacters(in: .whitespaces).count < 5 || model.isWorking)
                        }
                    }
            }
        }
    }
}

struct ClosingRoomView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = FinanceViewModel()

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: VPSpace.lg) {
                VStack(alignment: .leading, spacing: VPSpace.sm) {
                    Label(String(localized: "workspace.closing.unavailable.title"), systemImage: "checklist.unchecked")
                        .font(.headline)
                        .foregroundStyle(.primary)
                    Text("workspace.closing.unavailable.detail")
                        .font(.subheadline)
                        .foregroundStyle(VPColor.secondaryText)
                }
                .vpCard()

                if viewModel.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                }
            }
            .padding(VPSpace.md)
        }
        .background(VPColor.canvas)
        .navigationTitle(String(localized: "workspace.closing.title"))
        .refreshable {
            if let orgId = appState.currentOrganization?.id {
                await viewModel.fetchFinanceData(organizationId: String(orgId))
            }
        }
        .task {
            if let orgId = appState.currentOrganization?.id {
                await viewModel.fetchFinanceData(organizationId: String(orgId))
            }
        }
    }
}

private struct PreferencesView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        Form {
            Section(String(localized: "workspace.preferences.appearance")) {
                Picker(String(localized: "workspace.preferences.theme"), selection: $appState.theme) {
                    Text(String(localized: "workspace.preferences.theme.system")).tag(AppTheme.system)
                    Text(String(localized: "workspace.preferences.theme.light")).tag(AppTheme.porcelain)
                    Text(String(localized: "workspace.preferences.theme.dark")).tag(AppTheme.obsidian)
                }

                Picker(String(localized: "workspace.preferences.language"), selection: $appState.language) {
                    Text(String(localized: "workspace.preferences.language.system")).tag(AppLanguage.system)
                    Text(String(localized: "workspace.preferences.language.turkish")).tag(AppLanguage.turkish)
                    Text(String(localized: "workspace.preferences.language.english")).tag(AppLanguage.english)
                }
            }

            Section(String(localized: "workspace.preferences.about")) {
                HStack {
                    Text(String(localized: "workspace.preferences.version"))
                    Spacer()
                    Text(Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "—")
                        .foregroundStyle(VPColor.secondaryText)
                }

                HStack {
                    Text(String(localized: "workspace.preferences.infrastructure"))
                    Spacer()
                    Text(String(localized: "workspace.preferences.infrastructure.value"))
                        .foregroundStyle(VPColor.secondaryText)
                }
            }
        }
        .navigationTitle(String(localized: "workspace.preferences.title"))
    }
}

private struct WorkspacePlaceholder: View {
    let title: LocalizedStringKey
    var body: some View { ContentUnavailableView(title, systemImage: "building.2").navigationTitle(title) }
}

// MARK: - SMMM Mükellef Portföyü
struct AccountantPortfolioView: View {
    @EnvironmentObject private var appState: AppState
    @ObservedObject var model: WorkspaceLiveViewModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: VPSpace.md) {
                // Header Summary Card
                if let summary = model.portfolio?.summary {
                    VStack(alignment: .leading, spacing: VPSpace.sm) {
                        HStack {
                            Text(String(localized: "workspace.portfolio.summary"))
                                .font(.headline)
                            Spacer()
                            Text(String(format: String(localized: "workspace.portfolio.client_count"), summary.totalClients))
                                .font(.caption.bold())
                                .foregroundStyle(VPColor.accentBlue)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 3)
                                .background(VPColor.accentBlue.opacity(0.12), in: Capsule())
                        }

                        Divider().overlay(VPColor.cardBorder)

                        HStack(spacing: VPSpace.sm) {
                            VStack(alignment: .leading) {
                                Text(String(localized: "workspace.portfolio.total_documents"))
                                    .font(.caption2)
                                    .foregroundStyle(VPColor.secondaryText)
                                Text("\(summary.totalDocuments)")
                                    .font(.title3.bold().monospacedDigit())
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)

                            VStack(alignment: .leading) {
                                Text(String(localized: "workspace.portfolio.pending"))
                                    .font(.caption2)
                                    .foregroundStyle(VPColor.secondaryText)
                                Text("\(summary.totalPendingReviews)")
                                    .font(.title3.bold().monospacedDigit())
                                    .foregroundStyle(summary.totalPendingReviews > 0 ? VPColor.warning : VPColor.success)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)

                            VStack(alignment: .leading) {
                                Text(String(localized: "workspace.portfolio.net_vat"))
                                    .font(.caption2)
                                    .foregroundStyle(VPColor.secondaryText)
                                Text(summary.totalNetKdvBalance.formattedTRYCompact)
                                    .font(.title3.bold().monospacedDigit())
                                    .foregroundStyle(summary.totalNetKdvBalance > 0 ? VPColor.danger : VPColor.success)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }
                    .vpCard()
                }

                Text(String(localized: "workspace.portfolio.clients"))
                    .font(.title3.bold())
                    .foregroundStyle(VPColor.textPrimary)

                if let clients = model.portfolio?.clients, !clients.isEmpty {
                    ForEach(clients) { client in
                        let isCurrent = String(client.id) == appState.currentOrganization?.id
                        let verifiedOrganization = appState.availableOrganizations.first { $0.id == String(client.id) }
                        VStack(alignment: .leading, spacing: VPSpace.xs) {
                            HStack {
                                Text(client.name)
                                    .font(.headline)
                                    .foregroundStyle(VPColor.textPrimary)
                                    .lineLimit(1)
                                Spacer()
                                if isCurrent {
                                    Text(String(localized: "workspace.portfolio.active"))
                                        .font(.caption2.bold())
                                        .foregroundStyle(Color.white)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 2)
                                        .background(VPColor.accentBlue, in: Capsule())
                                }
                            }

                            HStack(spacing: 8) {
                                if let vkn = client.vkn, !vkn.isEmpty {
                                    Text(String(format: String(localized: "workspace.portfolio.vkn"), vkn))
                                        .font(.caption2.monospaced())
                                        .foregroundStyle(VPColor.secondaryText)
                                }
                                if let regime = client.taxRegime {
                                    Text(regime)
                                        .font(.caption2.bold())
                                        .foregroundStyle(VPColor.brand)
                                }
                            }

                            Divider().overlay(VPColor.cardBorder)

                            HStack {
                                HStack(spacing: 4) {
                                    Image(systemName: "doc.text")
                                        .font(.caption2)
                                    Text(String(format: String(localized: "workspace.portfolio.document_count"), client.documentCount, client.pendingCount))
                                        .font(.caption2)
                                }
                                .foregroundStyle(client.pendingCount > 0 ? VPColor.warning : VPColor.secondaryText)

                                Spacer()

                                Text(String(format: String(localized: "workspace.portfolio.client_net_vat"), client.netKdvBalance.formattedTRY))
                                    .font(.caption.bold().monospacedDigit())
                                    .foregroundStyle(client.netKdvBalance > 0 ? VPColor.danger : VPColor.success)
                            }

                            if !isCurrent, let verifiedOrganization {
                                Button {
                                    appState.currentOrganization = verifiedOrganization
                                    dismiss()
                                } label: {
                                    HStack {
                                        Image(systemName: "arrow.right.circle.fill")
                                        Text(String(localized: "workspace.portfolio.switch"))
                                    }
                                    .font(.caption.bold())
                                    .frame(maxWidth: .infinity, minHeight: 34)
                                    .background(VPColor.canvas, in: RoundedRectangle(cornerRadius: 8))
                                    .foregroundStyle(VPColor.brand)
                                }
                                .buttonStyle(.plain)
                                .padding(.top, 4)
                            }
                        }
                        .vpCard()
                    }
                } else {
                    VStack(spacing: VPSpace.md) {
                        Image(systemName: "building.2")
                            .font(.system(size: 40))
                            .foregroundStyle(VPColor.secondaryText)
                        Text(String(localized: "workspace.portfolio.empty.title"))
                            .font(.headline)
                        Text(String(localized: "workspace.portfolio.empty.detail"))
                            .font(.subheadline)
                            .foregroundStyle(VPColor.secondaryText)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.top, 30)
                }
            }
            .padding(VPSpace.md)
        }
        .background(VPColor.canvas)
        .navigationTitle(String(localized: "workspace.portfolio.title"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - SMMM Komisyon ve Gelir Masası
struct AffiliateCommissionsView: View {
    @EnvironmentObject private var appState: AppState
    @ObservedObject var model: WorkspaceLiveViewModel
    @State private var isLinkCopied = false
    @State private var showPayout = false; @State private var showReferral = false
    @State private var iban = ""; @State private var bank = ""; @State private var holder = ""; @State private var referralCode = ""

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: VPSpace.md) {
                // Tier & Earnings Header
                VStack(alignment: .leading, spacing: VPSpace.sm) {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(String(localized: "workspace.affiliate.current_rate"))
                                .font(.caption2.bold())
                                .foregroundStyle(VPColor.secondaryText)
                                .tracking(0.8)
                            Text(model.affiliate.map { String(format: String(localized: "workspace.affiliate.share"), $0.affiliate.commissionPercent) } ?? String(localized: "workspace.affiliate.unavailable"))
                                .font(.title2.bold())
                                .foregroundStyle(VPColor.success)
                        }
                        Spacer()
                        Text(model.affiliate?.affiliate.tierName ?? String(localized: "workspace.affiliate.tier_unavailable"))
                            .font(.caption.bold())
                            .foregroundStyle(VPColor.brandInverse)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(VPColor.brand, in: Capsule())
                    }

                    Divider().overlay(VPColor.cardBorder)

                    HStack(spacing: VPSpace.sm) {
                        VStack(alignment: .leading) {
                            Text(String(localized: "workspace.affiliate.monthly"))
                                .font(.caption2)
                                .foregroundStyle(VPColor.secondaryText)
                            Text(model.affiliate.map { $0.metrics.estimatedMonthlyCommissionTRY.formattedTRY } ?? String(localized: "workspace.not_available"))
                                .font(.title3.bold().monospacedDigit())
                                .foregroundStyle(VPColor.textPrimary)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)

                        VStack(alignment: .leading) {
                            Text(String(localized: "workspace.affiliate.annual"))
                                .font(.caption2)
                                .foregroundStyle(VPColor.secondaryText)
                            Text(model.affiliate.map { $0.metrics.estimatedAnnualCommissionTRY.formattedTRY } ?? String(localized: "workspace.not_available"))
                                .font(.title3.bold().monospacedDigit())
                                .foregroundStyle(VPColor.success)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                .vpCard()

                HStack {
                    Button("Ödeme hesabı", systemImage: "building.columns") { showPayout = true }.buttonStyle(.bordered)
                    Button("Referans kodu", systemImage: "link") { showReferral = true }.buttonStyle(.bordered)
                }

                // Commission Tier Table (%10 - %20)
                VStack(alignment: .leading, spacing: VPSpace.xs) {
                    Text(String(localized: "workspace.affiliate.tiers"))
                        .font(.caption2.bold())
                        .foregroundStyle(VPColor.secondaryText)
                        .tracking(0.8)

                    if let tiers = model.affiliate?.tierInfo.allTiers, !tiers.isEmpty {
                        ForEach(tiers) { tier in
                            tierRow(
                                range: tier.range,
                                percent: String(format: String(localized: "workspace.affiliate.commission_percent"), tier.percent),
                                isCurrent: tier.isCurrent
                            )
                        }
                    } else {
                        Text(String(localized: "workspace.affiliate.tiers_unavailable"))
                            .font(.caption)
                            .foregroundStyle(VPColor.secondaryText)
                    }
                }
                .vpCard()

                // Referral Link Card
                VStack(alignment: .leading, spacing: VPSpace.sm) {
                    Text(String(localized: "workspace.affiliate.referral_title"))
                        .font(.headline)
                    Text(String(localized: "workspace.affiliate.referral_detail"))
                        .font(.caption)
                        .foregroundStyle(VPColor.secondaryText)

                    HStack {
                        Text(model.affiliate?.affiliate.referralUrl ?? String(localized: "workspace.not_available"))
                            .font(.caption.monospaced())
                            .foregroundStyle(VPColor.brand)
                            .lineLimit(1)
                        Spacer()
                        Button {
                            guard let url = model.affiliate?.affiliate.referralUrl, !url.isEmpty else { return }
                            UIPasteboard.general.string = url
                            withAnimation { isLinkCopied = true }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                                withAnimation { isLinkCopied = false }
                            }
                        } label: {
                            Text(isLinkCopied ? String(localized: "workspace.affiliate.copied") : String(localized: "workspace.affiliate.copy"))
                                .font(.caption.bold())
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(VPColor.brand, in: RoundedRectangle(cornerRadius: 8))
                                .foregroundStyle(VPColor.brandInverse)
                        }
                        .buttonStyle(.plain)
                        .disabled(model.affiliate?.affiliate.referralUrl.isEmpty != false)
                    }
                    .padding(10)
                    .background(VPColor.canvas, in: RoundedRectangle(cornerRadius: 10))
                }
                .vpCard()

                if let payoutDate = model.affiliate?.metrics.nextPayoutDate, !payoutDate.isEmpty {
                    HStack(spacing: 8) {
                    Image(systemName: "banknote.fill")
                        .font(.subheadline)
                        .foregroundStyle(VPColor.success)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(String(localized: "workspace.affiliate.payout_title"))
                            .font(.caption.bold())
                        Text(String(format: String(localized: "workspace.affiliate.payout_date"), payoutDate))
                            .font(.caption2)
                            .foregroundStyle(VPColor.secondaryText)
                    }
                }
                    .padding(VPSpace.sm)
                    .background(VPColor.canvas, in: RoundedRectangle(cornerRadius: 12))
                }
            }
            .padding(VPSpace.md)
        }
        .background(VPColor.canvas)
        .navigationTitle(String(localized: "workspace.affiliate.title"))
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showPayout) { NavigationStack { Form { TextField("IBAN", text: $iban).textInputAutocapitalization(.characters); TextField("Banka adı", text: $bank); TextField("Hesap sahibi", text: $holder) }.navigationTitle("Ödeme hesabı").toolbar { ToolbarItem(placement: .confirmationAction) { Button("Kaydet") { Task { if await model.updatePayout(iban: iban.replacingOccurrences(of: " ", with: "").uppercased(), bank: bank, holder: holder) { showPayout = false } } }.disabled(!iban.replacingOccurrences(of: " ", with: "").uppercased().hasPrefix("TR")) } } } }
        .sheet(isPresented: $showReferral) { NavigationStack { Form { TextField("Referans kodu", text: $referralCode).textInputAutocapitalization(.characters) }.navigationTitle("Özel referans kodu").toolbar { ToolbarItem(placement: .confirmationAction) { Button("Kaydet") { guard let id = Int(appState.currentOrganization?.id ?? "") else { return }; Task { if await model.updateReferralCode(referralCode.uppercased(), organizationID: id) { showReferral = false } } }.disabled(referralCode.count < 4) } } } }
    }

    private func tierRow(range: String, percent: String, isCurrent: Bool) -> some View {
        HStack {
            Text(range)
                .font(.subheadline.weight(isCurrent ? .bold : .regular))
                .foregroundStyle(isCurrent ? VPColor.brand : VPColor.textPrimary)
            Spacer()
            Text(percent)
                .font(.subheadline.bold().monospacedDigit())
                .foregroundStyle(isCurrent ? VPColor.success : VPColor.secondaryText)
            if isCurrent {
                Image(systemName: "checkmark.circle.fill")
                    .font(.caption)
                    .foregroundStyle(VPColor.success)
            }
        }
        .padding(.vertical, 4)
    }
}

private extension ClosingTask.State {
    var symbol: String { switch self { case .complete: "checkmark.circle.fill"; case .business: "person.crop.circle.badge.clock"; case .accountant: "person.badge.clock"; case .risk: "exclamationmark.triangle.fill" } }
    var tint: Color { switch self { case .complete: VPColor.success; case .business: VPColor.brand; case .accountant: VPColor.warning; case .risk: VPColor.danger } }
}
