package com.example.elsuarku.utils

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Circuit breaker for Firestore and external API operations.
 *
 * Prevents cascading failures by short-circuiting requests when a dependency
 * is known to be failing. Uses the standard three-state model:
 *
 * ```
 * CLOSED ──[failures >= threshold]──→ OPEN
 *   ↑                                    │
 *   └────[success]───────────────────────┘
 *                  (after resetTimeout)
 * OPEN ──[timeout elapsed]──→ HALF_OPEN
 *   ↑                            │
 *   └────[failure]───────────────┘
 * HALF_OPEN ──[success]──→ CLOSED
 * ```
 *
 * ## Firestore-Specific Notes
 * - Firestore SDK already has internal retry logic
 * - This prevents app-level retry storms when Firestore is degraded
 * - HALF_OPEN state allows a single probe request to test recovery
 */
class CircuitBreaker(
    private val name: String,
    private val failureThreshold: Int = 5,
    private val resetTimeoutMs: Long = 30_000L,
    private val halfOpenMaxAttempts: Int = 1
) {
    enum class State { CLOSED, OPEN, HALF_OPEN }

    private val mutex = Mutex()
    @Volatile private var state: State = State.CLOSED
    @Volatile private var failureCount: Int = 0
    @Volatile private var lastFailureTime: Long = 0L
    @Volatile private var halfOpenAttempts: Int = 0

    companion object {
        private const val TAG = "CircuitBreaker"
    }

    /**
     * Execute [block] with circuit breaker protection.
     *
     * - If CLOSED: executes normally; records failures and opens circuit if threshold exceeded
     * - If OPEN: fails fast with [CircuitOpenException] without executing the block
     * - If HALF_OPEN: allows a limited number of probe requests to test if service recovered
     *
     * @param block The operation to protect
     * @return Result of the operation
     * @throws CircuitOpenException if the circuit is open
     * @throws T if the block throws any exception (after recording the failure)
     */
    suspend fun <T> execute(block: suspend () -> T): T {
        // Fast path: OPEN state — fail immediately without lock contention
        if (state == State.OPEN) {
            if (shouldAttemptReset()) {
                transitionToHalfOpen()
            } else {
                val remainingMs = resetTimeoutMs - (System.currentTimeMillis() - lastFailureTime)
                throw CircuitOpenException(
                    "Circuit '$name' is OPEN. Reset in ${remainingMs / 1000}s"
                )
            }
        }

        // HALF_OPEN: limit probe attempts
        if (state == State.HALF_OPEN) {
            mutex.withLock {
                if (halfOpenAttempts >= halfOpenMaxAttempts) {
                    throw CircuitOpenException(
                        "Circuit '$name' is HALF_OPEN — probe limit reached"
                    )
                }
                halfOpenAttempts++
            }
        }

        return try {
            val result = block()
            onSuccess()
            result
        } catch (e: Exception) {
            onFailure(e)
            throw e
        }
    }

    /**
     * Check if circuit is currently accepting requests.
     * Use as a pre-check before dispatching operations.
     */
    fun isOpen(): Boolean = state == State.OPEN

    /**
     * Current circuit state (for UI diagnostics).
     */
    fun currentState(): State = state

    /**
     * Force the circuit to CLOSED (e.g., after manual intervention).
     */
    suspend fun reset() {
        mutex.withLock {
            state = State.CLOSED
            failureCount = 0
            halfOpenAttempts = 0
            Log.i(TAG, "Circuit '$name' manually reset to CLOSED")
        }
    }

    // ── Private state transitions ──

    private suspend fun onSuccess() {
        mutex.withLock {
            when (state) {
                State.HALF_OPEN -> {
                    state = State.CLOSED
                    failureCount = 0
                    halfOpenAttempts = 0
                    Log.i(TAG, "Circuit '$name' HALF_OPEN → CLOSED (probe succeeded)")
                }
                State.CLOSED -> {
                    failureCount = 0 // Reset on any success
                }
                State.OPEN -> { /* shouldn't reach here, but handle gracefully */ }
            }
        }
    }

    private suspend fun onFailure(error: Throwable) {
        mutex.withLock {
            failureCount++
            lastFailureTime = System.currentTimeMillis()

            when (state) {
                State.CLOSED -> {
                    if (failureCount >= failureThreshold) {
                        state = State.OPEN
                        Log.w(TAG, "Circuit '$name' CLOSED → OPEN after $failureCount failures (last: ${error.message})")
                    }
                }
                State.HALF_OPEN -> {
                    state = State.OPEN
                    Log.w(TAG, "Circuit '$name' HALF_OPEN → OPEN (probe failed: ${error.message})")
                }
                State.OPEN -> { /* already open */ }
            }
        }
    }

    private suspend fun transitionToHalfOpen() {
        mutex.withLock {
            if (state == State.OPEN && shouldAttemptReset()) {
                state = State.HALF_OPEN
                halfOpenAttempts = 0
                Log.i(TAG, "Circuit '$name' OPEN → HALF_OPEN (timeout elapsed)")
            }
        }
    }

    private fun shouldAttemptReset(): Boolean {
        return System.currentTimeMillis() - lastFailureTime >= resetTimeoutMs
    }
}

/**
 * Thrown when a circuit is OPEN and the operation is rejected.
 */
class CircuitOpenException(message: String) : Exception(message)
