package com.vergipro.mobile.feature.identity

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vergipro.mobile.core.configuration.AppConfiguration
import com.vergipro.mobile.core.designsystem.VPColor
import com.vergipro.mobile.core.designsystem.VPMark
import com.vergipro.mobile.core.navigation.VergiProApp
import com.vergipro.mobile.core.security.KeystoreCredentialStore
import com.vergipro.mobile.core.session.SessionController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IdentityFlow() {
    var state by remember { mutableStateOf(IdentityUiState(stage = IdentityStage.Welcome)) }
    var isSplashActive by remember { mutableStateOf(true) }
    var isPortalZooming by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val localeTag = context.resources.configuration.locales[0].toLanguageTag()
    val configuration = remember { runCatching { AppConfiguration.load() }.getOrNull() }
    val sessionController = remember(context) {
        SessionController(KeystoreCredentialStore(context.applicationContext))
    }
    val repository = remember(configuration, sessionController) {
        runCatching {
            IdentityRepository(
                requireNotNull(configuration),
                sessionController,
            )
        }.getOrNull()
    }

    // Auto-Restore Persistent Session on App Start with Brand Splash and Portal Zoom Transition
    LaunchedEffect(Unit) {
        scope.launch {
            val startTime = System.currentTimeMillis()
            var nextStage = IdentityStage.Welcome
            var nextOrgs = emptyList<OrganizationSummary>()
            var nextSelectedOrg: OrganizationSummary? = null

            if (repository != null) {
                try {
                    val restoredOrgs = repository.restore()
                    if (!restoredOrgs.isNullOrEmpty()) {
                        nextOrgs = restoredOrgs
                        if (restoredOrgs.size == 1) {
                            nextSelectedOrg = restoredOrgs.first()
                            nextStage = IdentityStage.Authenticated
                        } else {
                            nextStage = IdentityStage.OrganizationSelection
                        }
                    }
                } catch (_: Throwable) {
                    nextStage = IdentityStage.Welcome
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            val remainingDelay = (850L - elapsed).coerceAtLeast(0L)
            if (remainingDelay > 0) {
                delay(remainingDelay)
            }

            // Set destination before starting portal warp
            state = state.copy(
                organizations = nextOrgs,
                selectedOrganization = nextSelectedOrg,
                stage = nextStage
            )

            // Trigger Portal Zoom Transition
            isPortalZooming = true

            // Wait for 650ms zoom animation to finish
            delay(650L)
            isSplashActive = false
        }
    }

    // Cooldown Timer
    LaunchedEffect(state.resendCooldownRemaining) {
        if (state.resendCooldownRemaining > 0) {
            delay(1000)
            state = state.copy(resendCooldownRemaining = state.resendCooldownRemaining - 1)
        }
    }

    val contentScale by animateFloatAsState(
        targetValue = if (isSplashActive && !isPortalZooming) 0.92f else 1.0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "content-scale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (isSplashActive && !isPortalZooming) 0f else 1.0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "content-alpha"
    )

    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main Destination Screen Layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = contentScale
                        scaleY = contentScale
                        alpha = contentAlpha
                    }
            ) {
                AnimatedContent(
                    targetState = state.stage,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "identity-stage",
                ) { stage ->
                    when (stage) {
                        IdentityStage.Splash,
                        IdentityStage.Welcome -> WelcomeScreen(
                            onSignIn = { state = state.copy(stage = IdentityStage.SignIn, errorMessage = null) },
                            onRegister = { state = state.copy(stage = IdentityStage.RegisterEmail, errorMessage = null) },
                        )
                        IdentityStage.SignIn -> SignInScreen(
                            state = state,
                            onBack = { state = state.copy(stage = IdentityStage.Welcome, errorMessage = null) },
                            onRegisterClick = { state = state.copy(stage = IdentityStage.RegisterEmail, errorMessage = null) },
                            onForgotPassword = { state = state.copy(stage = IdentityStage.ForgotPassword, password = "", verificationCode = "", errorMessage = null) },
                            onEmailChange = { state = state.copy(email = it, errorMessage = null) },
                            onPasswordChange = { state = state.copy(password = it, errorMessage = null) },
                            onSubmit = {
                                if (state.canSubmitSignIn && repository != null) {
                                    scope.launch {
                                        state = state.copy(isSubmitting = true, errorMessage = null)
                                        try {
                                            val orgs = repository.signIn(state.email, state.password)
                                            state = if (orgs.size == 1) {
                                                state.copy(isSubmitting = false, organizations = orgs, selectedOrganization = orgs.first(), stage = IdentityStage.Authenticated)
                                            } else {
                                                state.copy(isSubmitting = false, organizations = orgs, stage = IdentityStage.OrganizationSelection)
                                            }
                                        } catch (e: IdentityRequestException) {
                                            state = state.copy(isSubmitting = false, errorMessage = e.identityError.localized())
                                        } catch (_: Throwable) {
                                            state = state.copy(isSubmitting = false, errorMessage = IdentityError.ServiceUnavailable.localized())
                                        }
                                    }
                                }
                            },
                        )
                        IdentityStage.ForgotPassword -> ForgotPasswordScreen(
                            state = state,
                            onBack = { state = state.copy(stage = IdentityStage.SignIn, errorMessage = null) },
                            onEmailChange = { state = state.copy(email = it, errorMessage = null) },
                            onSubmit = {
                                if (state.canRequestCode && repository != null) scope.launch {
                                    state = state.copy(isSubmitting = true, errorMessage = null)
                                    try {
                                        repository.requestPasswordReset(state.email)
                                        state = state.copy(isSubmitting = false, stage = IdentityStage.ResetPassword)
                                    } catch (e: IdentityRequestException) {
                                        state = state.copy(isSubmitting = false, errorMessage = e.identityError.localized())
                                    } catch (_: Throwable) {
                                        state = state.copy(isSubmitting = false, errorMessage = IdentityError.ServiceUnavailable.localized())
                                    }
                                }
                            },
                        )
                        IdentityStage.ResetPassword -> ResetPasswordScreen(
                            state = state,
                            onBack = { state = state.copy(stage = IdentityStage.ForgotPassword, errorMessage = null) },
                            onCodeChange = { state = state.copy(verificationCode = it.filter(Char::isDigit).take(6), errorMessage = null) },
                            onPasswordChange = { state = state.copy(password = it, errorMessage = null) },
                            onSubmit = {
                                if (state.verificationCode.length == 6 && state.password.length >= 6 && repository != null) scope.launch {
                                    state = state.copy(isSubmitting = true, errorMessage = null)
                                    try {
                                        repository.resetPassword(state.email, state.verificationCode, state.password)
                                        state = state.copy(isSubmitting = false, stage = IdentityStage.SignIn, verificationCode = "", password = "", errorMessage = context.getString(com.vergipro.mobile.R.string.identity_reset_success))
                                    } catch (e: IdentityRequestException) {
                                        state = state.copy(isSubmitting = false, errorMessage = e.identityError.localized())
                                    } catch (_: Throwable) {
                                        state = state.copy(isSubmitting = false, errorMessage = IdentityError.ServiceUnavailable.localized())
                                    }
                                }
                            },
                        )
                IdentityStage.RegisterEmail -> RegisterEmailScreen(
                    state = state,
                    onBack = { state = state.copy(stage = IdentityStage.Welcome, errorMessage = null) },
                    onSignInClick = { state = state.copy(stage = IdentityStage.SignIn, errorMessage = null) },
                    onEmailChange = { state = state.copy(email = it, errorMessage = null) },
                    onSubmit = {
                        if (state.canRequestCode && repository != null) {
                            scope.launch {
                                state = state.copy(isSubmitting = true, errorMessage = null)
                                try {
                                    repository.requestRegistrationCode(state.email, localeTag)
                                    state = state.copy(isSubmitting = false, resendCooldownRemaining = 60, stage = IdentityStage.RegisterCode)
                                } catch (e: IdentityRequestException) {
                                    state = state.copy(isSubmitting = false, errorMessage = e.identityError.localized())
                                } catch (_: Throwable) {
                                    state = state.copy(isSubmitting = false, errorMessage = IdentityError.ServiceUnavailable.localized())
                                }
                            }
                        }
                    },
                )
                IdentityStage.RegisterCode -> RegisterCodeScreen(
                    state = state,
                    onBack = { state = state.copy(stage = IdentityStage.RegisterEmail, errorMessage = null) },
                    onCodeChange = { code ->
                        val filtered = code.filter { it.isDigit() }.take(6)
                        state = state.copy(verificationCode = filtered, errorMessage = null)
                    },
                    onResend = {
                        if (state.resendCooldownRemaining == 0 && repository != null) {
                            scope.launch {
                                state = state.copy(isSubmitting = true, errorMessage = null)
                                try {
                                    repository.requestRegistrationCode(state.email, localeTag)
                                    state = state.copy(isSubmitting = false, resendCooldownRemaining = 60)
                                } catch (e: IdentityRequestException) {
                                    state = state.copy(isSubmitting = false, errorMessage = e.identityError.localized())
                                } catch (_: Throwable) {
                                    state = state.copy(isSubmitting = false, errorMessage = IdentityError.ServiceUnavailable.localized())
                                }
                            }
                        }
                    },
                    onContinue = {
                        if (state.canSubmitCode && repository != null) {
                            scope.launch {
                                state = state.copy(isSubmitting = true, errorMessage = null)
                                try {
                                    repository.verifyRegistrationCode(state.email, state.verificationCode)
                                    state = state.copy(isSubmitting = false, stage = IdentityStage.RegisterProfile)
                                } catch (e: IdentityRequestException) {
                                    state = state.copy(isSubmitting = false, errorMessage = e.identityError.localized())
                                } catch (_: Throwable) {
                                    state = state.copy(isSubmitting = false, errorMessage = IdentityError.ServiceUnavailable.localized())
                                }
                            }
                        }
                    },
                )
                IdentityStage.RegisterProfile -> RegisterProfileScreen(
                    state = state,
                    onBack = { state = state.copy(stage = IdentityStage.RegisterCode, errorMessage = null) },
                    onDisplayNameChange = { state = state.copy(displayName = it, errorMessage = null) },
                    onOrgNameChange = { state = state.copy(organizationName = it, errorMessage = null) },
                    onPasswordChange = { state = state.copy(password = it, errorMessage = null) },
                    onTermsToggle = { state = state.copy(acceptedTerms = !state.acceptedTerms, errorMessage = null) },
                    onSubmit = {
                        val validationErr = state.validateProfileInput()
                        if (validationErr != null) {
                            state = state.copy(errorMessage = validationErr)
                        } else if (repository != null) {
                            scope.launch {
                                state = state.copy(isSubmitting = true, errorMessage = null)
                                try {
                                    val orgs = repository.register(
                                        email = state.email,
                                        code = state.verificationCode,
                                        displayName = state.displayName,
                                        password = state.password,
                                        organizationName = state.organizationName,
                                        locale = localeTag,
                                    )
                                    state = if (orgs.size == 1) {
                                        state.copy(isSubmitting = false, organizations = orgs, selectedOrganization = orgs.first(), stage = IdentityStage.Authenticated)
                                    } else {
                                        state.copy(isSubmitting = false, organizations = orgs, stage = IdentityStage.OrganizationSelection)
                                    }
                                } catch (e: IdentityRequestException) {
                                    state = state.copy(isSubmitting = false, errorMessage = e.identityError.localized())
                                } catch (_: Throwable) {
                                    state = state.copy(isSubmitting = false, errorMessage = IdentityError.ServiceUnavailable.localized())
                                }
                            }
                        }
                    },
                )
                IdentityStage.OrganizationSelection -> OrganizationSelectionScreen(
                    organizations = state.organizations,
                    onSelect = { state = state.copy(selectedOrganization = it, stage = IdentityStage.Authenticated) },
                    onSignInDifferentAccount = {
                        scope.launch {
                            repository?.signOut()
                            state = IdentityUiState(stage = IdentityStage.SignIn)
                        }
                    },
                    onRegisterNewAccount = {
                        scope.launch {
                            repository?.signOut()
                            state = IdentityUiState(stage = IdentityStage.RegisterEmail)
                        }
                    },
                    onSignOut = {
                        scope.launch {
                            repository?.signOut()
                            state = IdentityUiState(stage = IdentityStage.Welcome)
                        }
                    },
                )
                IdentityStage.Authenticated -> {
                    val organizationId = state.selectedOrganization?.id?.toIntOrNull()
                    if (organizationId != null && configuration != null) {
                        VergiProApp(
                            companyName = state.selectedOrganization?.name ?: "Şirketiniz",
                            organizationId = organizationId,
                            organizations = state.organizations,
                            configuration = configuration,
                            sessionController = sessionController,
                            onOrganizationSelected = { organization ->
                                state = state.copy(selectedOrganization = organization)
                            },
                            onSignOut = {
                                repository?.signOut()
                                state = IdentityUiState(stage = IdentityStage.Welcome)
                            },
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            state = state.copy(
                                stage = IdentityStage.OrganizationSelection,
                                errorMessage = "Şirket bilgisi geçersiz. Lütfen yeniden seçim yapın.",
                            )
                        }
                    }
                }
            }
        }
    }

    // Cinematic Portal Splash Overlay Layer
    if (isSplashActive) {
        PortalSplashScreenView(isZooming = isPortalZooming)
    }
}
}
}

// MARK: - Screens

@Composable
private fun WelcomeScreen(onSignIn: () -> Unit, onRegister: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height(1.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            VPMark(size = 60.dp)
            Text(
                stringResource(com.vergipro.mobile.R.string.identity_hub),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(com.vergipro.mobile.R.string.identity_landing_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                lineHeight = 40.sp,
            )
            Text(
                stringResource(com.vergipro.mobile.R.string.identity_landing_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(stringResource(com.vergipro.mobile.R.string.identity_login), fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onRegister,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(com.vergipro.mobile.R.string.identity_create_account), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(com.vergipro.mobile.R.string.identity_secure_audit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SignInScreen(
    state: IdentityUiState,
    onBack: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPassword: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Geri")
        }
        VPMark(size = 52.dp)
        Text(stringResource(com.vergipro.mobile.R.string.identity_signin_heading), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(com.vergipro.mobile.R.string.identity_signin_description), color = MaterialTheme.colorScheme.onSurfaceVariant)

        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(com.vergipro.mobile.R.string.identity_email_address)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(16.dp),
        )

        TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(com.vergipro.mobile.R.string.identity_forgot_password), fontWeight = FontWeight.SemiBold)
        }

        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(com.vergipro.mobile.R.string.identity_password)) },
            placeholder = { Text("••••••••••••") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(16.dp),
        )

        state.errorMessage?.let {
            ErrorMessageCard(it)
        }

        Button(
            onClick = onSubmit,
            enabled = state.canSubmitSignIn,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(com.vergipro.mobile.R.string.identity_signin_action), fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(com.vergipro.mobile.R.string.identity_no_account), color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onRegisterClick) {
                Text(stringResource(com.vergipro.mobile.R.string.identity_register), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ForgotPasswordScreen(
    state: IdentityUiState,
    onBack: () -> Unit,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null) }
        VPMark(size = 52.dp)
        Text(stringResource(com.vergipro.mobile.R.string.identity_forgot_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(com.vergipro.mobile.R.string.identity_forgot_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = state.email, onValueChange = onEmailChange, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(com.vergipro.mobile.R.string.identity_email_address)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, shape = RoundedCornerShape(16.dp))
        state.errorMessage?.let { ErrorMessageCard(it) }
        Button(onClick = onSubmit, enabled = state.canRequestCode, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
            if (state.isSubmitting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text(stringResource(com.vergipro.mobile.R.string.identity_send_reset_code), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ResetPasswordScreen(
    state: IdentityUiState,
    onBack: () -> Unit,
    onCodeChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null) }
        VPMark(size = 52.dp)
        Text(stringResource(com.vergipro.mobile.R.string.identity_reset_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(com.vergipro.mobile.R.string.identity_reset_body, state.email), color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = state.verificationCode, onValueChange = onCodeChange, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(com.vergipro.mobile.R.string.identity_verification_code)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(16.dp))
        OutlinedTextField(value = state.password, onValueChange = onPasswordChange, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(com.vergipro.mobile.R.string.identity_new_password)) }, visualTransformation = PasswordVisualTransformation(), singleLine = true, shape = RoundedCornerShape(16.dp))
        state.errorMessage?.let { ErrorMessageCard(it) }
        Button(onClick = onSubmit, enabled = state.verificationCode.length == 6 && state.password.length >= 6 && !state.isSubmitting, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
            if (state.isSubmitting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text(stringResource(com.vergipro.mobile.R.string.identity_reset_action), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RegisterEmailScreen(
    state: IdentityUiState,
    onBack: () -> Unit,
    onSignInClick: () -> Unit,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Geri")
        }
        StepHeader(currentStep = 1, totalSteps = 3, title = stringResource(com.vergipro.mobile.R.string.identity_email_step))
        Text(stringResource(com.vergipro.mobile.R.string.identity_enter_email), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(com.vergipro.mobile.R.string.identity_email_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)

        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(com.vergipro.mobile.R.string.identity_business_email)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(16.dp),
        )

        state.errorMessage?.let { ErrorMessageCard(it) }

        Button(
            onClick = onSubmit,
            enabled = state.canRequestCode,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(com.vergipro.mobile.R.string.identity_send_code), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(com.vergipro.mobile.R.string.identity_have_account), color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onSignInClick) {
                Text(stringResource(com.vergipro.mobile.R.string.identity_signin_heading), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RegisterCodeScreen(
    state: IdentityUiState,
    onBack: () -> Unit,
    onCodeChange: (String) -> Unit,
    onResend: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(com.vergipro.mobile.R.string.identity_change_email))
        }
        StepHeader(currentStep = 2, totalSteps = 3, title = "KOD ONAYI")
        Text(stringResource(com.vergipro.mobile.R.string.identity_enter_code), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(com.vergipro.mobile.R.string.identity_code_description, state.email), color = MaterialTheme.colorScheme.onSurfaceVariant)

        OutlinedTextField(
            value = state.verificationCode,
            onValueChange = onCodeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(com.vergipro.mobile.R.string.identity_six_digit_code)) },
            placeholder = { Text("123456") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(16.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(com.vergipro.mobile.R.string.identity_code_validity), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.resendCooldownRemaining > 0) {
                Text(stringResource(com.vergipro.mobile.R.string.identity_resend_countdown, state.resendCooldownRemaining), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                TextButton(onClick = onResend) {
                    Text(stringResource(com.vergipro.mobile.R.string.identity_resend), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        state.errorMessage?.let { ErrorMessageCard(it) }

        Button(
            onClick = onContinue,
            enabled = state.canSubmitCode,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(com.vergipro.mobile.R.string.identity_continue_profile), fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun RegisterProfileScreen(
    state: IdentityUiState,
    onBack: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onOrgNameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTermsToggle: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Geri")
        }
        StepHeader(currentStep = 3, totalSteps = 3, title = stringResource(com.vergipro.mobile.R.string.identity_account_step))
        Text(stringResource(com.vergipro.mobile.R.string.identity_complete_account), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(com.vergipro.mobile.R.string.identity_complete_description), color = MaterialTheme.colorScheme.onSurfaceVariant)

        OutlinedTextField(
            value = state.displayName,
            onValueChange = onDisplayNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(com.vergipro.mobile.R.string.identity_full_name)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )

        OutlinedTextField(
            value = state.organizationName,
            onValueChange = onOrgNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(com.vergipro.mobile.R.string.identity_company_name)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(com.vergipro.mobile.R.string.identity_secure_password)) },
            placeholder = { Text("••••••••••••") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(16.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onTermsToggle),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = state.acceptedTerms,
                onCheckedChange = { onTermsToggle() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "VergiPro Kullanım Şartları'nı ve Gizlilik/KVKK Politikasını okudum, kabul ediyorum.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        state.errorMessage?.let { ErrorMessageCard(it) }

        Button(
            onClick = onSubmit,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(com.vergipro.mobile.R.string.identity_create_and_start), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OrganizationSelectionScreen(
    organizations: List<OrganizationSummary>,
    onSelect: (OrganizationSummary) -> Unit,
    onSignInDifferentAccount: () -> Unit,
    onRegisterNewAccount: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VPMark(size = 36.dp)
            TextButton(onClick = onSignOut) {
                Text(stringResource(com.vergipro.mobile.R.string.identity_sign_out), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(stringResource(com.vergipro.mobile.R.string.identity_choose_workspace), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(com.vergipro.mobile.R.string.identity_choose_workspace_description), color = MaterialTheme.colorScheme.onSurfaceVariant)

        organizations.forEach { organization ->
            Card(
                onClick = { onSelect(organization) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(46.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                organization.name.take(1),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(organization.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(organization.role, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onSignInDifferentAccount,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(stringResource(com.vergipro.mobile.R.string.identity_different_account), fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick = onRegisterNewAccount,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(stringResource(com.vergipro.mobile.R.string.identity_new_company), fontWeight = FontWeight.SemiBold)
        }
    }
}

// MARK: - Components

@Composable
private fun StepHeader(currentStep: Int, totalSteps: Int, title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(totalSteps) { idx ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .background(
                            if (idx + 1 <= currentStep) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(2.dp),
                        )
                )
            }
        }
        Text(
            "ADIM $currentStep/$totalSteps · $title",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ErrorMessageCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VPColor.Danger.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.Warning, contentDescription = null, tint = VPColor.Danger, modifier = Modifier.size(18.dp))
            Text(message, color = VPColor.Danger, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PortalSplashScreenView(isZooming: Boolean) {
    val haptic = LocalHapticFeedback.current
    var isEntered by remember { mutableStateOf(false) }
    val splashBackground = MaterialTheme.colorScheme.background
    val splashForeground = MaterialTheme.colorScheme.onBackground
    val isDarkSplash = splashBackground.luminance() < 0.5f

    val iconScale by animateFloatAsState(
        targetValue = if (isZooming) 38f else if (isEntered) 1f else 0.85f,
        animationSpec = if (isZooming) {
            tween(durationMillis = 650, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f))
        } else {
            spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "portal-icon-scale"
    )

    val iconAlpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = if (isZooming) {
            tween(durationMillis = 450, delayMillis = 150, easing = LinearEasing)
        } else {
            spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "portal-icon-alpha"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (isZooming) 0f else if (isEntered) 1f else 0f,
        animationSpec = if (isZooming) {
            tween(durationMillis = 160, easing = LinearEasing)
        } else {
            spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "portal-text-alpha"
    )

    val bgAlpha by animateFloatAsState(
        targetValue = if (isZooming) 0f else 1f,
        animationSpec = if (isZooming) {
            tween(durationMillis = 480, delayMillis = 120, easing = LinearEasing)
        } else {
            tween(durationMillis = 0)
        },
        label = "portal-bg-alpha"
    )

    val textOffsetY by animateDpAsState(
        targetValue = if (isEntered) 0.dp else 8.dp,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "portal-text-offset"
    )

    LaunchedEffect(Unit) {
        isEntered = true
    }

    LaunchedEffect(isZooming) {
        if (isZooming) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(splashBackground.copy(alpha = bgAlpha)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        alpha = iconAlpha
                        transformOrigin = if (isZooming) {
                            TransformOrigin(pivotFractionX = 0.6591f, pivotFractionY = 0.3846f)
                        } else {
                            TransformOrigin.Center
                        }
                    }
            ) {
                VPMark(size = 84.dp, contained = false, darkTheme = isDarkSplash)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .offset(y = textOffsetY)
                    .graphicsLayer { alpha = textAlpha }
            ) {
                Text(
                    text = "VergiPro",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = splashForeground,
                    letterSpacing = 0.6.sp
                )
                Text(
                    text = "AKILLI FİNANS & VERGİ RADARI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = splashForeground.copy(alpha = 0.48f),
                    letterSpacing = 2.0.sp
                )
            }
        }
    }
}
