package com.vergipro.mobile.core.network

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class HttpMethod { GET, POST, PATCH, DELETE }
data class OrganizationContext(val organizationId: Int, val membershipId: Int = 0)
data class ApiEndpoint(val path: String, val method: HttpMethod, val body: ByteArray? = null, val requiresTenant: Boolean = true, val isMutation: Boolean = false)
data class TransportResponse(val status: Int, val body: ByteArray, val headers: Map<String, List<String>>) {
    val text: String get() = body.decodeToString()
}

sealed class ApiFailure(message: String? = null) : Exception(message) {
    data object InvalidRequest : ApiFailure("Geçersiz istek.")
    data object NetworkUnavailable : ApiFailure("İnternet bağlantısı kurulamadı.")
    data object AuthenticationRequired : ApiFailure("Oturum süreniz doldu, lütfen tekrar giriş yapın.")
    data object Forbidden : ApiFailure("Bu işlem için yetkiniz bulunmuyor.")
    data class Validation(val requestId: String?, val detail: String? = null) : ApiFailure(detail ?: "Doğrulama hatası.")
    data object Conflict : ApiFailure("Kayıt çakışması.")
    data class RateLimited(val retryAfterSeconds: Long?) : ApiFailure("Çok fazla istek yapıldı. Lütfen bekleyin.")
    data class Server(val requestId: String?, val detail: String? = null) : ApiFailure(detail ?: "Sunucu hatası.")
    data object Maintenance : ApiFailure("Sistem bakımda.")
}

fun interface AccessTokenProvider { suspend fun token(): String? }

interface HttpTransport {
    suspend fun execute(uri: URI, endpoint: ApiEndpoint, headers: Map<String, String>): TransportResponse
    suspend fun executeMultipart(
        uri: URI,
        fileBytes: ByteArray,
        filename: String,
        formFields: Map<String, String>,
        headers: Map<String, String>
    ): TransportResponse
    suspend fun executeMultipartBatch(
        uri: URI,
        files: List<Triple<ByteArray, String, String>>, // bytes, filename, formKey
        formFields: Map<String, String>,
        headers: Map<String, String>
    ): TransportResponse
}

class PlatformHttpTransport : HttpTransport {
    override suspend fun execute(uri: URI, endpoint: ApiEndpoint, headers: Map<String, String>): TransportResponse = withContext(Dispatchers.IO) {
        val connection = uri.toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = endpoint.method.name
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000
            headers.forEach(connection::setRequestProperty)
            endpoint.body?.let { body ->
                connection.doOutput = true
                connection.outputStream.use { it.write(body) }
            }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseHeaders = connection.headerFields.entries
                .filter { it.key != null }
                .associate { it.key!! to it.value.orEmpty() }
            TransportResponse(connection.responseCode, stream?.use { it.readBytes() } ?: byteArrayOf(), responseHeaders)
        } catch (_: java.io.IOException) {
            throw ApiFailure.NetworkUnavailable
        } finally {
            connection.disconnect()
        }
    }

    override suspend fun executeMultipart(
        uri: URI,
        fileBytes: ByteArray,
        filename: String,
        formFields: Map<String, String>,
        headers: Map<String, String>
    ): TransportResponse = withContext(Dispatchers.IO) {
        executeMultipartBatch(
            uri = uri,
            files = listOf(Triple(fileBytes, filename, "file")),
            formFields = formFields,
            headers = headers
        )
    }

    override suspend fun executeMultipartBatch(
        uri: URI,
        files: List<Triple<ByteArray, String, String>>,
        formFields: Map<String, String>,
        headers: Map<String, String>
    ): TransportResponse = withContext(Dispatchers.IO) {
        val boundary = "===VergiProBoundary" + UUID.randomUUID().toString().replace("-", "") + "==="
        val connection = uri.toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 60_000
            connection.readTimeout = 60_000
            connection.doOutput = true
            headers.forEach(connection::setRequestProperty)
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            val output = connection.outputStream
            val crlf = "\r\n"

            // 1. Write text fields
            formFields.forEach { (key, value) ->
                output.write(("--$boundary$crlf").toByteArray())
                output.write(("Content-Disposition: form-data; name=\"$key\"$crlf$crlf").toByteArray())
                output.write(value.toByteArray(Charsets.UTF_8))
                output.write(crlf.toByteArray())
            }

            // 2. Write files
            files.forEach { (fileBytes, filename, formKey) ->
                output.write(("--$boundary$crlf").toByteArray())
                output.write(("Content-Disposition: form-data; name=\"$formKey\"; filename=\"$filename\"$crlf").toByteArray())
                val contentType = if (filename.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/jpeg"
                output.write(("Content-Type: $contentType$crlf$crlf").toByteArray())
                output.write(fileBytes)
                output.write(crlf.toByteArray())
            }

            // 3. End of multipart
            output.write(("--$boundary--$crlf").toByteArray())
            output.flush()
            output.close()

            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseHeaders = connection.headerFields.entries
                .filter { it.key != null }
                .associate { it.key!! to it.value.orEmpty() }
            TransportResponse(connection.responseCode, stream?.use { it.readBytes() } ?: byteArrayOf(), responseHeaders)
        } catch (_: java.io.IOException) {
            throw ApiFailure.NetworkUnavailable
        } finally {
            connection.disconnect()
        }
    }
}

class ApiClient(
    val baseUri: URI,
    private val transport: HttpTransport = PlatformHttpTransport(),
    private val accessTokenProvider: AccessTokenProvider,
) {
    suspend fun execute(endpoint: ApiEndpoint, organization: OrganizationContext? = null, idempotencyKey: UUID? = null): TransportResponse {
        val targetPath = if (endpoint.path.startsWith("/")) endpoint.path.substring(1) else endpoint.path
        val fullUri = baseUri.resolve(targetPath)

        val headers = buildMap {
            put("Accept", "application/json")
            put("X-Request-ID", UUID.randomUUID().toString())
            endpoint.body?.let { put("Content-Type", "application/json") }
            organization?.let { put("X-Organization-ID", it.organizationId.toString()) }
            idempotencyKey?.let { put("Idempotency-Key", it.toString()) }
            accessTokenProvider.token()?.let { put("Authorization", "Bearer $it") }
        }
        val response = transport.execute(fullUri, endpoint, headers)
        if (response.status !in 200..299) throw response.toFailure()
        return response
    }

    suspend fun uploadFile(
        path: String,
        fileBytes: ByteArray,
        filename: String,
        formFields: Map<String, String> = emptyMap(),
        organization: OrganizationContext? = null
    ): TransportResponse {
        val targetPath = if (path.startsWith("/")) path.substring(1) else path
        val fullUri = baseUri.resolve(targetPath)

        val headers = buildMap {
            put("Accept", "application/json")
            put("X-Request-ID", UUID.randomUUID().toString())
            organization?.let { put("X-Organization-ID", it.organizationId.toString()) }
            accessTokenProvider.token()?.let { put("Authorization", "Bearer $it") }
        }
        val response = transport.executeMultipart(fullUri, fileBytes, filename, formFields, headers)
        if (response.status !in 200..299) throw response.toFailure()
        return response
    }

    suspend fun uploadBatch(
        path: String,
        files: List<Pair<ByteArray, String>>, // bytes to filename
        formFields: Map<String, String> = emptyMap(),
        organization: OrganizationContext? = null
    ): TransportResponse {
        val targetPath = if (path.startsWith("/")) path.substring(1) else path
        val fullUri = baseUri.resolve(targetPath)

        val headers = buildMap {
            put("Accept", "application/json")
            put("X-Request-ID", UUID.randomUUID().toString())
            organization?.let { put("X-Organization-ID", it.organizationId.toString()) }
            accessTokenProvider.token()?.let { put("Authorization", "Bearer $it") }
        }
        val fileTriples = files.map { Triple(it.first, it.second, "files") }
        val response = transport.executeMultipartBatch(fullUri, fileTriples, formFields, headers)
        if (response.status !in 200..299) throw response.toFailure()
        return response
    }
}

private fun TransportResponse.toFailure(): ApiFailure {
    val requestId = headers["X-Request-ID"]?.firstOrNull()
    return when (status) {
        401 -> ApiFailure.AuthenticationRequired
        403 -> ApiFailure.Forbidden
        409 -> ApiFailure.Conflict
        422 -> ApiFailure.Validation(requestId)
        429 -> ApiFailure.RateLimited(headers["Retry-After"]?.firstOrNull()?.toLongOrNull())
        503 -> ApiFailure.Maintenance
        else -> ApiFailure.Server(requestId)
    }
}
