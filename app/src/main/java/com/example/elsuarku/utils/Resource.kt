package com.example.elsuarku.utils

/**
 * Wrapper for async operation results — UI observes these states.
 */
sealed class Resource<out T> {
    data object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val exception: Throwable? = null) : Resource<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isLoading: Boolean get() = this is Loading
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun errorOrNull(): String? = when (this) {
        is Error -> message
        else -> null
    }

    companion object {
        fun <T> success(data: T): Resource<T> = Success(data)
        fun error(message: String, exception: Throwable? = null): Resource<Nothing> = Error(message, exception)
        fun loading(): Resource<Nothing> = Loading
    }
}
