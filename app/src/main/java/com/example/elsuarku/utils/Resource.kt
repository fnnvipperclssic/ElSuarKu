package com.example.elsuarku.utils

import com.example.elsuarku.domain.error.AppError

/**
 * Wrapper for async operation results — UI observes these states.
 *
 * Extended with:
 * - Typed errors via [AppError] for granular error handling
 * - [Empty] state for empty data sets (distinct from loading/error)
 * - [Cached] state for offline-first patterns
 */
sealed class Resource<out T> {
    data object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val exception: Throwable? = null, val typedError: AppError? = null) : Resource<Nothing>()
    data class Empty(val reason: String = "") : Resource<Nothing>()
    data class Cached<T>(val data: T, val isFresh: Boolean = false) : Resource<T>()

    val isSuccess: Boolean get() = this is Success
    val isLoading: Boolean get() = this is Loading
    val isError: Boolean get() = this is Error
    val isEmpty: Boolean get() = this is Empty
    val isCached: Boolean get() = this is Cached

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Cached -> data
        else -> null
    }

    fun errorOrNull(): String? = when (this) {
        is Error -> message
        else -> null
    }

    fun typedErrorOrNull(): AppError? = when (this) {
        is Error -> typedError
        else -> null
    }

    companion object {
        fun <T> success(data: T): Resource<T> = Success(data)
        fun error(message: String, exception: Throwable? = null, typedError: AppError? = null): Resource<Nothing> =
            Error(message, exception, typedError)

        fun loading(): Resource<Nothing> = Loading
        fun empty(reason: String = ""): Resource<Nothing> = Empty(reason)
        fun <T> cached(data: T, isFresh: Boolean = false): Resource<T> = Cached(data, isFresh)
    }
}
