import SwiftUI

struct IdentityFlowView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var model = IdentityViewModel()

    var body: some View {
        ZStack {
            VPColor.canvas.ignoresSafeArea()

            // Destination Content Layer (Revealed smoothly from inside the portal)
            Group {
                switch model.stage {
                case .welcome:
                    WelcomeView(model: model)
                        .transition(.opacity.combined(with: .scale(scale: 0.98)))
                case .signIn:
                    SignInView(model: model)
                        .transition(.asymmetric(
                            insertion: .move(edge: .trailing).combined(with: .opacity),
                            removal: .move(edge: .leading).combined(with: .opacity)
                        ))
                case .forgotPassword:
                    ForgotPasswordView(model: model)
                        .transition(.move(edge: .trailing).combined(with: .opacity))
                case .resetPassword:
                    ResetPasswordView(model: model)
                        .transition(.move(edge: .trailing).combined(with: .opacity))
                case .registerEmail:
                    RegisterEmailView(model: model)
                        .transition(.asymmetric(
                            insertion: .move(edge: .trailing).combined(with: .opacity),
                            removal: .move(edge: .leading).combined(with: .opacity)
                        ))
                case .registerCode:
                    RegisterCodeView(model: model)
                        .transition(.asymmetric(
                            insertion: .move(edge: .trailing).combined(with: .opacity),
                            removal: .move(edge: .leading).combined(with: .opacity)
                        ))
                case .registerProfile:
                    RegisterProfileView(model: model)
                        .transition(.asymmetric(
                            insertion: .move(edge: .trailing).combined(with: .opacity),
                            removal: .move(edge: .leading).combined(with: .opacity)
                        ))
                case .organizationSelection:
                    OrganizationSelectionView(model: model)
                        .transition(.move(edge: .trailing).combined(with: .opacity))
                case .authenticated:
                    RootView(onSignOut: { Task { await model.signOut() } })
                }
            }
            .scaleEffect(model.isRestoringSession ? (model.isPortalZooming ? 1.0 : 0.92) : 1.0)
            .opacity(model.isRestoringSession ? (model.isPortalZooming ? 1.0 : 0.0) : 1.0)
            .animation(.easeInOut(duration: 0.65), value: model.isPortalZooming)
            .animation(model.stage == .authenticated ? nil : .smooth(duration: 0.28), value: model.stage)

            // Cinematic Portal Splash Overlay
            if model.isRestoringSession {
                PortalSplashView(isZooming: model.isPortalZooming)
                    .transition(.opacity)
                    .zIndex(999)
            }
        }
        .task {
            model.applyLanguage(appState.language)
            await model.restoreSessionIfAvailable()
        }
        .onChange(of: appState.language) { _, language in
            model.applyLanguage(language)
        }
        .onChange(of: model.selectedOrganization) { _, newOrg in
            appState.currentOrganization = newOrg
            appState.availableOrganizations = newOrg == nil ? [] : model.organizations
        }
    }
}

private enum SplashPhase: Equatable {
    case initial
    case resting
    case zooming
}

private struct PortalSplashView: View {
    let isZooming: Bool
    @State private var phase: SplashPhase = .initial
    
    // Mathematical centroid of the P aperture inside the 1024x1024 glyph: (674.91, 393.80) -> (0.6591, 0.3846)
    private let portalAnchor = UnitPoint(x: 0.6591, y: 0.3846)

    var body: some View {
        ZStack {
            // The selected appearance is visible from the first branded frame.
            VPColor.canvas
                .ignoresSafeArea()
                .opacity(phase == .zooming ? 0.0 : 1.0)

            VStack(spacing: 22) {
                // VPMark Icon that zooms 38x directly into the 'P' hole
                Image("VPMark")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 84, height: 84)
                    .scaleEffect(
                        phase == .zooming ? 38.0 : (phase == .resting ? 1.0 : 0.85),
                        anchor: phase == .zooming ? portalAnchor : .center
                    )
                    // Keep the vector-sharp mark visible during the zoom so the app
                    // is revealed through the P aperture instead of a plain crossfade.
                    .opacity(phase == .initial ? 0.0 : 1.0)

                // Subtitle and branding text that dissolve instantly when portal engages
                VStack(spacing: 6) {
                    Text("VergiPro")
                        .font(.system(size: 26, weight: .bold, design: .default))
                        .foregroundStyle(Color.primary)
                        .tracking(0.6)

                    Text("AKILLI FİNANS & VERGİ RADARI")
                        .font(.system(size: 11, weight: .semibold, design: .default))
                        .foregroundStyle(Color.primary.opacity(0.48))
                        .tracking(2.0)
                }
                .opacity(phase == .resting ? 1.0 : 0.0)
                .offset(y: phase == .initial ? 8 : 0)
            }
        }
        .onAppear {
            withAnimation(.spring(response: 0.55, dampingFraction: 0.78)) {
                phase = .resting
            }
        }
        .onChange(of: isZooming) { _, zooming in
            if zooming && phase != .zooming {
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                withAnimation(.easeInOut(duration: 0.65)) {
                    phase = .zooming
                }
            }
        }
        .allowsHitTesting(false)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("VergiPro Akıllı Finans ve Vergi Radarı")
    }
}

// MARK: - Welcome View

private struct WelcomeView: View {
    @ObservedObject var model: IdentityViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: VPSpace.xl) {
            Spacer()
            VPBrandMark(size: 60, cornerRadius: 16)

            VStack(alignment: .leading, spacing: VPSpace.md) {
                Text(String(localized: "identity.welcome.eyebrow"))
                    .font(.caption.weight(.bold))
                    .foregroundStyle(VPColor.secondaryText)
                    .tracking(2.0)

                Text(String(localized: "identity.welcome.title"))
                    .font(.system(size: 34, weight: .bold, design: .default))
                    .lineSpacing(2)
                    .tracking(-1.0)
                    .foregroundStyle(.primary)

                Text(String(localized: "identity.welcome.body"))
                    .font(.body)
                    .foregroundStyle(VPColor.secondaryText)
                    .lineSpacing(4)
            }

            Spacer()

            VStack(spacing: VPSpace.sm) {
                Button(action: model.showSignIn) {
                    Text(String(localized: "identity.signin.action"))
                }
                .buttonStyle(VPPrimaryButtonStyle())

                Button(action: model.showRegister) {
                    Text(String(localized: "identity.register.action"))
                }
                .buttonStyle(VPSecondaryButtonStyle())
            }

            HStack(spacing: 6) {
                Image(systemName: "lock.shield.fill")
                    .font(.footnote)
                Text(String(localized: "identity.welcome.security"))
                    .font(.footnote)
            }
            .foregroundStyle(VPColor.secondaryText)
            .frame(maxWidth: .infinity, alignment: .center)
            .padding(.top, VPSpace.xs)
        }
        .padding(VPSpace.lg)
    }
}

// MARK: - Sign In View

private struct SignInView: View {
    @ObservedObject var model: IdentityViewModel
    @FocusState private var focusedField: Field?

    private enum Field { case email, password }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VPSpace.lg) {
                Button(action: model.showWelcome) {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                        Text(String(localized: "common.back"))
                    }
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                }
                .padding(.top, VPSpace.xs)

                VPBrandMark(size: 52, cornerRadius: 14)

                VStack(alignment: .leading, spacing: VPSpace.xs) {
                    Text(String(localized: "identity.signin.title"))
                        .font(.system(size: 28, weight: .bold))
                        .tracking(-0.6)
                    Text(String(localized: "identity.signin.body"))
                        .font(.subheadline)
                        .foregroundStyle(VPColor.secondaryText)
                }

                VStack(spacing: VPSpace.md) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(String(localized: "identity.email"))
                            .font(.caption2.weight(.bold))
                            .foregroundStyle(VPColor.secondaryText)
                            .tracking(1.0)
                        TextField("", text: $model.email)
                            .textContentType(.username)
                            .keyboardType(.emailAddress)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .focused($focusedField, equals: .email)
                            .submitLabel(.next)
                            .onSubmit { focusedField = .password }
                            .vpInput()
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text(String(localized: "identity.password"))
                            .font(.caption2.weight(.bold))
                            .foregroundStyle(VPColor.secondaryText)
                            .tracking(1.0)
                        SecureField("••••••••••••", text: $model.password)
                            .textContentType(.password)
                            .focused($focusedField, equals: .password)
                            .submitLabel(.go)
                            .onSubmit { submit() }
                            .vpInput()
                    }
                }

                if let err = model.errorMessage {
                    ErrorMessageBadge(message: err)
                }

                Button(action: submit) {
                    if model.isSubmitting {
                        ProgressView().tint(.white)
                    } else {
                        Text(String(localized: "identity.signin.submit"))
                    }
                }
                .buttonStyle(VPPrimaryButtonStyle())
                .disabled(!model.canSubmitSignIn)

                Button(String(localized: "identity.forgot.action"), action: model.showForgotPassword)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                    .frame(maxWidth: .infinity, alignment: .trailing)

                HStack {
                    Text(String(localized: "identity.signin.no_account"))
                        .font(.subheadline)
                        .foregroundStyle(VPColor.secondaryText)
                    Button(String(localized: "identity.register.short_action"), action: model.showRegister)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.primary)
                }
                .frame(maxWidth: .infinity, alignment: .center)
                .padding(.top, VPSpace.xs)
            }
            .padding(VPSpace.lg)
        }
        .scrollDismissesKeyboard(.interactively)
    }

    private func submit() {
        guard model.canSubmitSignIn else { return }
        Task { await model.signIn() }
    }
}

private struct ForgotPasswordView: View {
    @ObservedObject var model: IdentityViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VPSpace.lg) {
                Button(action: model.showSignIn) { Label(String(localized: "common.back"), systemImage: "chevron.left") }
                    .font(.subheadline.weight(.semibold)).foregroundStyle(.primary)
                VPBrandMark(size: 52, cornerRadius: 14)
                Text(String(localized: "identity.forgot.title")).font(.system(size: 28, weight: .bold))
                Text(String(localized: "identity.forgot.body")).font(.subheadline).foregroundStyle(VPColor.secondaryText)
                TextField("", text: $model.email).textContentType(.emailAddress).keyboardType(.emailAddress).textInputAutocapitalization(.never).autocorrectionDisabled().vpInput()
                if let error = model.errorMessage { ErrorMessageBadge(message: error) }
                Button {
                    Task { await model.requestPasswordReset() }
                } label: {
                    if model.isSubmitting { ProgressView().tint(.white) } else { Text(String(localized: "identity.forgot.submit")) }
                }
                .buttonStyle(VPPrimaryButtonStyle()).disabled(!model.canRequestCode)
            }.padding(VPSpace.lg)
        }
    }
}

private struct ResetPasswordView: View {
    @ObservedObject var model: IdentityViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VPSpace.lg) {
                Button { model.stage = .forgotPassword } label: { Label(String(localized: "common.back"), systemImage: "chevron.left") }
                    .font(.subheadline.weight(.semibold)).foregroundStyle(.primary)
                VPBrandMark(size: 52, cornerRadius: 14)
                Text(String(localized: "identity.reset.title")).font(.system(size: 28, weight: .bold))
                Text(String(format: String(localized: "identity.reset.body"), model.email)).font(.subheadline).foregroundStyle(VPColor.secondaryText)
                TextField(String(localized: "identity.reset.code"), text: $model.verificationCode).keyboardType(.numberPad).vpInput()
                SecureField(String(localized: "identity.reset.password"), text: $model.password).textContentType(.newPassword).vpInput()
                if let error = model.errorMessage { ErrorMessageBadge(message: error) }
                Button {
                    Task { await model.completePasswordReset() }
                } label: {
                    if model.isSubmitting { ProgressView().tint(.white) } else { Text(String(localized: "identity.reset.submit")) }
                }
                .buttonStyle(VPPrimaryButtonStyle())
                .disabled(model.verificationCode.count != 6 || model.password.count < 6 || model.isSubmitting)
            }.padding(VPSpace.lg)
        }
    }
}

// MARK: - Register Step 1: Email View

private struct RegisterEmailView: View {
    @ObservedObject var model: IdentityViewModel
    @FocusState private var isEmailFocused: Bool

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VPSpace.lg) {
                Button(action: model.showWelcome) {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                        Text(String(localized: "common.back"))
                    }
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                }
                .padding(.top, VPSpace.xs)

                StepIndicator(currentStep: 1, totalSteps: 3, title: String(localized: "identity.register.step.email"))

                VStack(alignment: .leading, spacing: VPSpace.xs) {
                    Text(String(localized: "identity.register.email.title"))
                        .font(.system(size: 28, weight: .bold))
                        .tracking(-0.6)
                    Text(String(localized: "identity.register.email.body"))
                        .font(.subheadline)
                        .foregroundStyle(VPColor.secondaryText)
                        .lineSpacing(3)
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text(String(localized: "identity.register.email.label"))
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(VPColor.secondaryText)
                        .tracking(1.0)
                    TextField(String(localized: "identity.register.email.placeholder"), text: $model.email)
                        .textContentType(.emailAddress)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .focused($isEmailFocused)
                        .submitLabel(.continue)
                        .onSubmit { submit() }
                        .vpInput()
                }

                if let err = model.errorMessage {
                    ErrorMessageBadge(message: err)
                }

                Button(action: submit) {
                    if model.isSubmitting {
                        ProgressView().tint(.white)
                    } else {
                        HStack(spacing: 6) {
                            Text(String(localized: "identity.register.email.submit"))
                            Image(systemName: "arrow.right")
                        }
                    }
                }
                .buttonStyle(VPPrimaryButtonStyle())
                .disabled(!model.canRequestCode)

                HStack {
                    Text(String(localized: "identity.register.has_account"))
                        .font(.subheadline)
                        .foregroundStyle(VPColor.secondaryText)
                    Button(String(localized: "identity.signin.title"), action: model.showSignIn)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.primary)
                }
                .frame(maxWidth: .infinity, alignment: .center)
            }
            .padding(VPSpace.lg)
        }
        .scrollDismissesKeyboard(.interactively)
        .onAppear { isEmailFocused = true }
    }

    private func submit() {
        guard model.canRequestCode else { return }
        Task { await model.requestCode() }
    }
}

// MARK: - Register Step 2: Code (OTP) View

private struct RegisterCodeView: View {
    @ObservedObject var model: IdentityViewModel
    @FocusState private var isCodeFocused: Bool

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VPSpace.lg) {
                Button(action: model.showRegister) {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                        Text(String(localized: "identity.register.code.change_email"))
                    }
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                }
                .padding(.top, VPSpace.xs)

                StepIndicator(currentStep: 2, totalSteps: 3, title: String(localized: "identity.register.step.code"))

                VStack(alignment: .leading, spacing: VPSpace.xs) {
                    Text(String(localized: "identity.register.code.title"))
                        .font(.system(size: 28, weight: .bold))
                        .tracking(-0.6)
                    Text(String(format: String(localized: "identity.register.code.body"), model.email))
                        .font(.subheadline)
                        .foregroundStyle(VPColor.secondaryText)
                }

                // OTP Display Box
                ZStack {
                    TextField("", text: $model.verificationCode)
                        .keyboardType(.numberPad)
                        .textContentType(.oneTimeCode)
                        .focused($isCodeFocused)
                        .opacity(0.01)
                        .onChange(of: model.verificationCode) { _, newValue in
                            let filtered = String(newValue.filter { $0.isNumber }.prefix(6))
                            if filtered != model.verificationCode {
                                model.verificationCode = filtered
                            }
                        }

                    HStack(spacing: 10) {
                        ForEach(0..<6, id: \.self) { index in
                            let char = characterAt(index)
                            ZStack {
                                RoundedRectangle(cornerRadius: VPRadius.medium, style: .continuous)
                                    .fill(VPColor.surface)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: VPRadius.medium, style: .continuous)
                                            .stroke(
                                                index == model.verificationCode.count ? Color.primary : Color(uiColor: .separator),
                                                lineWidth: index == model.verificationCode.count ? 1.5 : 1
                                            )
                                    )
                                Text(char)
                                    .font(.system(size: 24, weight: .bold, design: .monospaced))
                                    .foregroundStyle(.primary)
                            }
                            .frame(height: 58)
                        }
                    }
                }
                .padding(.vertical, VPSpace.xs)

                HStack {
                    Image(systemName: "clock")
                        .font(.caption)
                    Text(String(localized: "identity.register.code.validity"))
                        .font(.caption)
                    Spacer()
                    if model.resendCooldownRemaining > 0 {
                        Text(String(format: String(localized: "identity.register.code.resend_countdown"), model.resendCooldownRemaining))
                            .font(.caption.weight(.medium))
                            .foregroundStyle(VPColor.secondaryText)
                    } else {
                        Button(String(localized: "identity.register.code.resend")) {
                            Task { await model.resendCode() }
                        }
                        .font(.caption.weight(.bold))
                        .foregroundStyle(.primary)
                    }
                }
                .foregroundStyle(VPColor.secondaryText)

                if let err = model.errorMessage {
                    ErrorMessageBadge(message: err)
                }

                Button {
                    Task { await model.verifyCodeAndProceed() }
                } label: {
                    HStack(spacing: 6) {
                        Text(String(localized: "identity.register.code.submit"))
                        Image(systemName: "arrow.right")
                    }
                }
                .buttonStyle(VPPrimaryButtonStyle())
                .disabled(!model.canSubmitCode)
            }
            .padding(VPSpace.lg)
        }
        .scrollDismissesKeyboard(.interactively)
        .onAppear { isCodeFocused = true }
    }

    private func characterAt(_ index: Int) -> String {
        guard index < model.verificationCode.count else { return "" }
        let strIndex = model.verificationCode.index(model.verificationCode.startIndex, offsetBy: index)
        return String(model.verificationCode[strIndex])
    }
}

// MARK: - Register Step 3: Profile & Company View

private struct RegisterProfileView: View {
    @ObservedObject var model: IdentityViewModel
    @FocusState private var focusedField: Field?

    private enum Field { case displayName, organizationName, password }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VPSpace.lg) {
                Button(action: { model.stage = .registerCode }) {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                        Text(String(localized: "common.back"))
                    }
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                }
                .padding(.top, VPSpace.xs)

                StepIndicator(currentStep: 3, totalSteps: 3, title: String(localized: "identity.register.step.profile"))

                VStack(alignment: .leading, spacing: VPSpace.xs) {
                    Text(String(localized: "identity.register.profile.title"))
                        .font(.system(size: 28, weight: .bold))
                        .tracking(-0.6)
                    Text(String(localized: "identity.register.profile.body"))
                        .font(.subheadline)
                        .foregroundStyle(VPColor.secondaryText)
                }

                VStack(spacing: VPSpace.md) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(String(localized: "identity.register.profile.name"))
                            .font(.caption2.weight(.bold))
                            .foregroundStyle(VPColor.secondaryText)
                            .tracking(1.0)
                        TextField(String(localized: "identity.register.profile.name_placeholder"), text: $model.displayName)
                            .textContentType(.name)
                            .textInputAutocapitalization(.words)
                            .focused($focusedField, equals: .displayName)
                            .submitLabel(.next)
                            .onSubmit { focusedField = .organizationName }
                            .vpInput()
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text(String(localized: "identity.register.profile.organization"))
                            .font(.caption2.weight(.bold))
                            .foregroundStyle(VPColor.secondaryText)
                            .tracking(1.0)
                        TextField(String(localized: "identity.register.profile.organization_placeholder"), text: $model.organizationName)
                            .textContentType(.organizationName)
                            .textInputAutocapitalization(.words)
                            .focused($focusedField, equals: .organizationName)
                            .submitLabel(.next)
                            .onSubmit { focusedField = .password }
                            .vpInput()
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text(String(localized: "identity.register.profile.password"))
                                .font(.caption2.weight(.bold))
                                .foregroundStyle(VPColor.secondaryText)
                                .tracking(1.0)
                            Spacer()
                            Text(String(localized: "identity.register.profile.password_hint"))
                                .font(.caption2)
                                .foregroundStyle(model.password.count >= 12 ? VPColor.success : VPColor.secondaryText)
                        }
                        SecureField(String(localized: "identity.register.profile.password_placeholder"), text: $model.password)
                            .textContentType(.newPassword)
                            .focused($focusedField, equals: .password)
                            .submitLabel(.done)
                            .onSubmit { submit() }
                            .vpInput()
                    }
                }

                // Terms Acceptance Checkbox
                Button {
                    model.acceptedTerms.toggle()
                } label: {
                    HStack(alignment: .top, spacing: 12) {
                        Image(systemName: model.acceptedTerms ? "checkmark.square.fill" : "square")
                            .font(.title3)
                            .foregroundStyle(model.acceptedTerms ? Color.primary : VPColor.secondaryText)

                        Text(String(localized: "identity.register.profile.terms"))
                            .font(.footnote)
                            .foregroundStyle(VPColor.secondaryText)
                            .multilineTextAlignment(.leading)
                    }
                }
                .buttonStyle(.plain)
                .padding(.top, VPSpace.xs)

                // Password length live hint
                if !model.password.isEmpty && model.password.count < 12 {
                    HStack(spacing: 4) {
                        Image(systemName: "info.circle")
                        Text(String(format: String(localized: "identity.register.profile.password_length"), model.password.count))
                    }
                    .font(.caption)
                    .foregroundStyle(VPColor.warning)
                }

                if let err = model.errorMessage {
                    ErrorMessageBadge(message: err)
                }

                Button(action: submit) {
                    if model.isSubmitting {
                        ProgressView().tint(.white)
                    } else {
                        Text(String(localized: "identity.register.profile.submit"))
                    }
                }
                .buttonStyle(VPPrimaryButtonStyle())
                .disabled(model.isSubmitting)
            }
            .padding(VPSpace.lg)
        }
        .scrollDismissesKeyboard(.interactively)
        .onAppear { focusedField = .displayName }
    }

    private func submit() {
        Task { await model.completeRegistration() }
    }
}

// MARK: - Organization Selection View

private struct OrganizationSelectionView: View {
    @ObservedObject var model: IdentityViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VPSpace.lg) {
                HStack {
                    VPBrandMark(size: 36, cornerRadius: 10)
                    Spacer()
                    Button(String(localized: "workspace.sign_out")) {
                        Task { await model.signOut() }
                    }
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(VPColor.secondaryText)
                }
                .padding(.top, VPSpace.xs)

                VStack(alignment: .leading, spacing: VPSpace.xs) {
                    Text(String(localized: "identity.organization.title"))
                        .font(.system(size: 28, weight: .bold))
                        .tracking(-0.6)
                    Text(String(localized: "identity.organization.body"))
                        .font(.subheadline)
                        .foregroundStyle(VPColor.secondaryText)
                }

                VStack(spacing: VPSpace.sm) {
                    ForEach(model.organizations) { organization in
                        Button { model.select(organization) } label: {
                            HStack(spacing: VPSpace.md) {
                                Text(String(organization.name.prefix(1)))
                                    .font(.headline.weight(.bold))
                                    .foregroundStyle(VPColor.brand)
                                    .frame(width: 44, height: 44)
                                    .background(VPColor.brand.opacity(0.12), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                                            .stroke(VPColor.cardBorder, lineWidth: 1)
                                    )
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(organization.name).font(.headline).foregroundStyle(.primary)
                                    Text(organization.role)
                                        .font(.subheadline)
                                        .foregroundStyle(VPColor.secondaryText)
                                }
                                Spacer()
                                Image(systemName: "chevron.right").foregroundStyle(.tertiary)
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .vpCard()
                    }
                }

                VStack(spacing: VPSpace.sm) {
                    Button {
                        Task {
                            await model.signOut()
                            model.showSignIn()
                        }
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "arrow.left.square")
                            Text(String(localized: "identity.organization.different_account"))
                        }
                    }
                    .buttonStyle(VPSecondaryButtonStyle())

                    Button {
                        Task {
                            await model.signOut()
                            model.showRegister()
                        }
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "plus.circle")
                            Text(String(localized: "identity.organization.new_account"))
                        }
                    }
                    .buttonStyle(VPSecondaryButtonStyle())
                }
                .padding(.top, VPSpace.md)
            }
            .padding(VPSpace.lg)
        }
    }
}

// MARK: - Shared UI Components

private struct StepIndicator: View {
    let currentStep: Int
    let totalSteps: Int
    let title: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 4) {
                ForEach(1...totalSteps, id: \.self) { step in
                    Capsule()
                        .fill(step <= currentStep ? Color.primary : Color(uiColor: .separator))
                        .frame(height: 3)
                }
            }
            Text(String(format: String(localized: "identity.register.step_format"), currentStep, totalSteps, title))
                .font(.caption2.weight(.bold))
                .foregroundStyle(VPColor.secondaryText)
                .tracking(1.5)
        }
    }
}

private struct ErrorMessageBadge: View {
    let message: String

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(VPColor.danger)
                .font(.footnote)
                .padding(.top, 1)
            Text(message)
                .font(.footnote)
                .foregroundStyle(VPColor.danger)
        }
        .padding(VPSpace.sm)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(VPColor.danger.opacity(0.08), in: RoundedRectangle(cornerRadius: VPRadius.small, style: .continuous))
    }
}
