package com.vergipro.mobile.core.data

import com.vergipro.mobile.BuildConfig
import com.vergipro.mobile.core.network.OrganizationContext

enum class RepositoryEnvironment { Production, Preview }

fun RepositoryEnvironment.validated(): RepositoryEnvironment =
    if (BuildConfig.DEBUG) this else RepositoryEnvironment.Production

fun interface TenantScopedRepository<T> {
    suspend fun load(organization: OrganizationContext): T
}

class RepositoryContainer private constructor(
    val environment: RepositoryEnvironment,
) {
    companion object {
        fun create(environment: RepositoryEnvironment) =
            RepositoryContainer(environment.validated())
    }
}
