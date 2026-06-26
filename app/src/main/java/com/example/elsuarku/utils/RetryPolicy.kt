package com.example.elsuarku.utils

import android.util.Log
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Exponential backoff retry policy with jitter and max attempts.
 *
 * Prevents thundering herd and reduces Firestore quota pressure during
 * transient failures (network blips, Firestore 503, quota exceeded).
 *
 * ## Default configuration
 * | Parameter      | Value   | Rationale                                       |
 * |----------------|---------|--------------------------------------------------|
 * | initialDelayMs | 200ms   | Fast first retry for transient blips             |
 * | maxDelayMs     | 15s     | Cap to prevent excessive user wait               |
 * | maxAttempts    | 3       | Total 4 attempts (1 initial + 3 retries)         |
 * | jitterFactor   | 0.3     | ±30% random jitter to desynchronize retry waves  |
 * | backoffMultiplier | 2.0  | Standard exponential: 200 → 400 → 800 → 1600 ... |
 *
 * ## Usage
 * ```kotlin
 * val result = RetryPolicy.withRetry {
 *     voteRepository.submitVote(vote)
 * }
 * ```
 *
 * ## Firestore-Specific Notes
 * - Firestore SDK already retries some errors internally (UNAVAILABLE, ABORTED)
 * - This adds application-level retry for: RESOURCE_EXHAUSTED, DEADLINE_EXCEEDED
 * - Do NOT retry on: PERMISSION_DENIED, NOT_FOUND, INVALID_ARGUMENT, ALREADY_EXISTS
 */
object RetryPolicy {

    private const val TAG = "RetryPolicy"

    data class Config(
        val initialDelayMs: Long = 200L,
        val maxDelayMs: Long = 15_000L,
        val maxAttempts: Int = 3,
        val jitterFactor: Double = 0.3,
        val backoffMultiplier: Double = 2.0
    )

    /**
     * Result of a retry attempt, tracking all failures.
     */
    data class RetryResult<T>(
        val value: T?,
        val attempts: Int,
        val totalDelayMs: Long,
        val errors: List<Throwable>
    ) {
        val succeeded: Boolean get() = value != null
        val lastError: Throwable? get() = errors.lastOrNull()
    }

    /**
     * Execute [block] with exponential backoff retry.
     *
     * @param config Override default retry configuration
     * @param isRetryable Predicate: should this exception trigger a retry?
     *                    Default: retries on network errors and Firestore transient errors
     * @param block The operation to retry
     * @return The operation result (never throws — errors are captured)
     */
    suspend fun <T> withRetry(
        config: Config = Config(),
        isRetryable: (Throwable) -> Boolean = { isRetryableDefault(it) },
        block: suspend () -> T
    ): RetryResult<T> {
        val errors = mutableListOf<Throwable>()
        var delayMs = config.initialDelayMs
        var totalDelayMs = 0L

        for (attempt in 0..config.maxAttempts) {
            try {
                val result = block()
                if (attempt > 0) {
                    Log.i(TAG, "Operation succeeded after $attempt retry(s), total delay=${totalDelayMs}ms")
                }
                return RetryResult(
                    value = result,
                    attempts = attempt + 1,
                    totalDelayMs = totalDelayMs,
                    errors = errors
                )
            } catch (e: Exception) {
                errors.add(e)

                if (attempt >= config.maxAttempts || !isRetryable(e)) {
                    Log.w(TAG, "Retry exhausted (attempts=${attempt + 1}, not retryable=${!isRetryable(e)}): ${e.message}")
                    return RetryResult(
                        value = null,
                        attempts = attempt + 1,
                        totalDelayMs = totalDelayMs,
                        errors = errors
                    )
                }

                val jitter = (delayMs * config.jitterFactor * (Random.nextDouble() * 2 - 1)).toLong()
                val sleepMs = min(delayMs + jitter, config.maxDelayMs)

                Log.d(TAG, "Retry ${attempt + 1}/${config.maxAttempts}: sleeping ${sleepMs}ms after '${e.message}'")
                delay(sleepMs)
                totalDelayMs += sleepMs

                delayMs = min(
                    (delayMs.toDouble() * config.backoffMultiplier).toLong(),
                    config.maxDelayMs
                )
            }
        }

        // Unreachable (loop always returns), but Kotlin compiler needs it
        return RetryResult(null, config.maxAttempts + 1, totalDelayMs, errors)
    }

    /**
     * Default retry-eligibility check for Firestore / network operations.
     *
     * Retries on:
     *  - Network errors (UnknownHostException, SocketTimeoutException, etc.)
     *  - Firestore transient errors (UNAVAILABLE, RESOURCE_EXHAUSTED, DEADLINE_EXCEEDED)
     *  - Firebase "too many requests" (quota)
     *
     * Does NOT retry on:
     *  - Authentication errors (PERMISSION_DENIED, UNAUTHENTICATED)
     *  - Data errors (NOT_FOUND, ALREADY_EXISTS, INVALID_ARGUMENT)
     *  - Vote-specific errors (double vote)
     */
    fun isRetryableDefault(e: Throwable): Boolean {
        val message = e.message?.lowercase() ?: ""
        val className = e.javaClass.simpleName.lowercase()

        // Network errors
        if (className.contains("unknownhost") || className.contains("sockettimeout") ||
            className.contains("connectexception") || className.contains("ioexception") ||
            message.contains("network") || message.contains("timeout") || message.contains("connection")
        ) return true

        // Firestore transient error codes
        val firestoreTransientCodes = setOf(
            "unavailable", "resource_exhausted", "deadline_exceeded",
            "aborted", "internal", "cancelled"
        )
        for (code in firestoreTransientCodes) {
            if (message.contains(code)) return true
        }

        // Firebase quota
        if (message.contains("too many requests") || message.contains("quota") ||
            message.contains("rate limit")
        ) return true

        // Don't retry on auth, permission, data errors
        val nonRetryableCodes = setOf(
            "permission_denied", "unauthenticated", "not_found",
            "already_exists", "invalid_argument", "failed_precondition",
            "out_of_range", "data_loss"
        )
        for (code in nonRetryableCodes) {
            if (message.contains(code)) return false
        }

        // Default: don't retry unknown errors (fail fast)
        return false
    }
}
