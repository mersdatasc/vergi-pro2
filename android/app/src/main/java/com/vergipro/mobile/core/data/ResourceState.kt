package com.vergipro.mobile.core.data

import com.vergipro.mobile.core.network.ApiFailure

sealed interface ResourceState<out T> {
    data object Idle : ResourceState<Nothing>
    data class Loading<T>(val previous: T? = null) : ResourceState<T>
    data class Content<T>(val value: T, val isStale: Boolean = false) : ResourceState<T>
    data object Empty : ResourceState<Nothing>
    data class Failure<T>(val error: ApiFailure, val previous: T? = null) : ResourceState<T>
}

data class Page<T>(val items: List<T>, val nextCursor: String?)
