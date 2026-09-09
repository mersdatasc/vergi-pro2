package com.vergipro.mobile.feature.identity

import com.vergipro.mobile.core.configuration.AppConfiguration
import com.vergipro.mobile.core.session.SessionController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.Locale
import java.util.UUID

enum class IdentityStage {
    Splash,
    Welcome,
    SignIn,
    ForgotPassword,
    ResetPassword,
    RegisterEmail,
    RegisterCode,
    RegisterProfile,
    OrganizationSelection,
    Authenticated,
}

data class OrganizationSummary(
    val id: String,
    val name: String,
    val role: String,
)

data class IdentityUiState(
    val stage: IdentityStage = IdentityStage.Splash,
    val email: String = "",
    val password: String = "",
    val verificationCode: String = "",
    val displayName: String = "",
    val organizationName: String = "",
    val acceptedTerms: Boolean = false,
    val resendCooldownRemaining: Int = 0,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val selectedOrganization: OrganizationSummary? = null,
    val organizations: List<OrganizationSummary> = emptyList(),
) {
    val isEmailValid: Boolean get() = email.contains("@") && email.contains(".") && email.length >= 5
    val canSubmitSignIn: Boolean get() = isEmailValid && password.length >= 6 && !isSubmitting
    val canRequestCode: Boolean get() = isEmailValid && !isSubmitting
    val canSubmitCode: Boolean get() = verificationCode.trim().length == 6 && !isSubmitting

    fun validateProfileInput(): String? {
        val english = Locale.getDefault().language == "en"
        if (displayName.trim().length < 2) return if (english) "Enter your full name (at least 2 characters)." else "Lütfen adınızı ve soyadınızı girin (en az 2 karakter)."
        if (organizationName.trim().length < 2) return if (english) "Enter your company or business name." else "Lütfen şirket veya işletme adınızı girin."
        if (password.length < 6) return if (english) "Your password must contain at least 6 characters." else "Güvenliğiniz için parola en az 6 karakter olmalıdır."
        if (!acceptedTerms) return if (english) "Accept the Terms of Use and Privacy Policy to continue." else "Devam etmek için Kullanım Şartları ve Gizlilik Politikasını onaylamalısınız."
        return null
    }
}

enum class IdentityError(val messageTr: String, val messageEn: String) {
    InvalidCredentials("E-posta veya parola hatalı.", "Invalid email or password."),
    CodeInvalid("Doğrulama kodu geçersiz veya süresi dolmuş.", "Verification code is invalid or expired."),
    CodeCooldown("Yeni kod istemeden önce lütfen biraz bekleyin.", "Please wait before requesting a new code."),
    EmailAlreadyRegistered("Bu e-posta adresiyle zaten bir hesap mevcut.", "An account with this email already exists."),
    RegistrationDisabled("Kayıt sistemi şu anda bakımdadır.", "Registration is currently disabled."),
    WeakPassword("Parola en az 6 karakter olmalıdır.", "Password must be at least 6 characters."),
    ValidationFailed("Lütfen tüm alanları kurallara uygun doldurun.", "Please fill in all fields correctly."),
    NetworkUnavailable("İnternet bağlantısı kurulamadı.", "Internet connection unavailable."),
    AccountLocked("Bu hesap geçici olarak kilitlenmiştir.", "This account is temporarily locked."),
    ServiceUnavailable("Sunucuya bağlanılamadı. Lütfen tekrar deneyin.", "Service unavailable. Please try again.");

    fun localized(): String = if (Locale.getDefault().language == "en") messageEn else messageTr
}

class IdentityRepository(
    private val configuration: AppConfiguration,
    private val sessionController: SessionController,
) {
    suspend fun requestPasswordReset(email: String): Unit = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("email", email.trim().lowercase()).toString()
        request("auth/forgot-password", "POST", payload)
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String): Unit = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("email", email.trim().lowercase())
            put("code", code.trim())
            put("new_password", newPassword)
        }.toString()
        request("auth/reset-password", "POST", payload)
    }

    suspend fun requestRegistrationCode(email: String, locale: String): Unit = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("email", email.trim().lowercase())
            put("locale", locale)
        }.toString()
        request("auth/send-verification-code", "POST", payload)
    }

    suspend fun verifyRegistrationCode(email: String, code: String): Unit = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("email", email.trim().lowercase())
            put("code", code.trim())
        }.toString()
        request("auth/verify-code", "POST", payload)
    }

    fun signOut() = sessionController.signOut()

    suspend fun register(
        email: String,
        code: String,
        displayName: String,
        password: String,
        organizationName: String,
        locale: String,
    ): List<OrganizationSummary> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("email", email.trim().lowercase())
            put("full_name", displayName.trim())
            put("company_name", organizationName.trim())
            put("password", password)
        }.toString()
        val registered = request("auth/register", "POST", payload)
        val accessToken = registered.optString("accessToken").ifBlank { registered.optString("access_token") }
        val refreshToken = registered.optString("refreshToken").ifBlank { registered.optString("refresh_token") }
        if (accessToken.isBlank() || refreshToken.isBlank()) {
            throw IdentityRequestException(IdentityError.ServiceUnavailable)
        }
        sessionController.establish(accessToken, refreshToken)
        val directOrgs = parseOrganizations(registered)
        if (directOrgs.isNotEmpty()) return@withContext directOrgs
        memberships(accessToken)
    }

    suspend fun signIn(email: String, password: String): List<OrganizationSummary> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("email", email.trim().lowercase())
            put("password", password)
        }.toString()
        val login = request("auth/login", "POST", payload)
        val accessToken = login.optString("accessToken").ifBlank { login.optString("access_token") }
        val refreshToken = login.optString("refreshToken").ifBlank { login.optString("refresh_token") }
        if (accessToken.isBlank() || refreshToken.isBlank()) {
            throw IdentityRequestException(IdentityError.ServiceUnavailable)
        }
        sessionController.establish(accessToken, refreshToken)
        val directOrgs = parseOrganizations(login)
        if (directOrgs.isNotEmpty()) return@withContext directOrgs
        memberships(accessToken)
    }

    suspend fun restore(): List<OrganizationSummary>? = withContext(Dispatchers.IO) {
        val stored = sessionController.beginRestore() ?: return@withContext null
        val refreshToken = stored.decodeToString()
        try {
            val payload = JSONObject().put("refresh_token", refreshToken).toString()
            val refreshed = request("auth/refresh", "POST", payload)
            val accessToken = refreshed.optString("accessToken").ifBlank { refreshed.optString("access_token") }
            val newRefresh = refreshed.optString("refreshToken").ifBlank { refreshed.optString("refresh_token", refreshToken) }
            if (accessToken.isBlank() || newRefresh.isBlank()) {
                throw IdentityRequestException(IdentityError.ServiceUnavailable)
            }
            sessionController.completeRestore(accessToken, newRefresh)
            val directOrgs = parseOrganizations(refreshed)
            if (directOrgs.isNotEmpty()) return@withContext directOrgs
            memberships(accessToken)
        } catch (error: Throwable) {
            sessionController.signOut()
            throw error
        }
    }

    private fun parseOrganizations(json: JSONObject): List<OrganizationSummary> {
        val items = json.optJSONArray("organizations") ?: json.optJSONArray("memberships") ?: return emptyList()
        return buildList {
            repeat(items.length()) { index ->
                val item = items.getJSONObject(index)
                val orgId = item.optString("id").ifBlank { item.optString("organizationId") }
                val orgName = item.optString("name").ifBlank { item.optString("organizationName") }
                val role = item.optString("role", "OWNER")
                if (orgId.toIntOrNull() != null && orgName.isNotBlank()) {
                    add(OrganizationSummary(orgId, orgName, role))
                }
            }
        }
    }

    private fun memberships(accessToken: String): List<OrganizationSummary> {
        val json = request("auth/me", "GET", accessToken = accessToken)
        val orgs = parseOrganizations(json)
        if (orgs.isEmpty()) throw IdentityRequestException(IdentityError.ServiceUnavailable)
        return orgs
    }

    private fun request(path: String, method: String, body: String? = null, accessToken: String? = null): JSONObject {
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        val fullUrl = if (configuration.apiBaseUri.toString().endsWith("/")) {
            configuration.apiBaseUri.resolve(cleanPath)
        } else {
            URL("${configuration.apiBaseUri}/$cleanPath").toURI()
        }

        val connection = (fullUrl.toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 25_000
            readTimeout = 25_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-Request-ID", UUID.randomUUID().toString())
            accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                outputStream.use { it.write(body.encodeToByteArray()) }
            }
        }
        return try {
            val status = connection.responseCode
            val payload = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (status in 200..299) {
                if (payload.isBlank()) JSONObject() else JSONObject(payload)
            } else {
                when (status) {
                    401 -> throw IdentityRequestException(IdentityError.InvalidCredentials)
                    403 -> throw IdentityRequestException(IdentityError.RegistrationDisabled)
                    409 -> throw IdentityRequestException(IdentityError.EmailAlreadyRegistered)
                    429 -> throw IdentityRequestException(IdentityError.CodeCooldown)
                    400 -> {
                        val parsed = runCatching { JSONObject(payload) }.getOrNull()
                        val message = parsed?.optJSONObject("error")?.optString("message") ?: parsed?.optString("detail")
                        if (message == "REGISTRATION_CODE_INVALID") {
                            throw IdentityRequestException(IdentityError.CodeInvalid)
                        }
                        throw IdentityRequestException(IdentityError.ValidationFailed)
                    }
                    else -> throw IdentityRequestException(IdentityError.ServiceUnavailable)
                }
            }
        } catch (_: SocketTimeoutException) {
            throw IdentityRequestException(IdentityError.NetworkUnavailable)
        } catch (error: IOException) {
            throw IdentityRequestException(IdentityError.NetworkUnavailable, error)
        } finally {
            connection.disconnect()
        }
    }
}

class IdentityRequestException(val identityError: IdentityError, cause: Throwable? = null) : Exception(cause)
