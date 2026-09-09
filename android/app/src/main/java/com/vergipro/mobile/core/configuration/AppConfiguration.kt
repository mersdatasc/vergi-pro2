package com.vergipro.mobile.core.configuration

import com.vergipro.mobile.BuildConfig
import java.net.URI

enum class AppEnvironment { Development, Staging, Production }

sealed class ConfigurationFailure : IllegalStateException() {
    data object MissingBaseUrl : ConfigurationFailure()
    data object InsecureBaseUrl : ConfigurationFailure()
    data object InvalidBaseUrl : ConfigurationFailure()
}

data class AppConfiguration(val environment: AppEnvironment, val apiBaseUri: URI) {
    companion object {
        fun load(rawUrl: String = BuildConfig.API_BASE_URL, rawEnvironment: String = BuildConfig.APP_ENVIRONMENT): AppConfiguration {
            if (rawUrl.isBlank()) throw ConfigurationFailure.MissingBaseUrl
            val uri = runCatching { URI(rawUrl) }.getOrElse { throw ConfigurationFailure.InvalidBaseUrl }
            if (!uri.scheme.equals("https", ignoreCase = true)) throw ConfigurationFailure.InsecureBaseUrl
            if (uri.host.isNullOrBlank() || uri.userInfo != null || uri.query != null || uri.fragment != null || (uri.path.isNotEmpty() && uri.path != "/")) throw ConfigurationFailure.InvalidBaseUrl
            val environment = when (rawEnvironment.lowercase()) {
                "production" -> AppEnvironment.Production
                "staging" -> AppEnvironment.Staging
                else -> AppEnvironment.Development
            }
            val base = URI("${rawUrl.trimEnd('/')}/api/")
            return AppConfiguration(environment, base)
        }
    }
}
