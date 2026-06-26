package com.example.elsuarku.domain.error

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthEmailException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Maps platform exceptions (Firebase, network, etc.) to typed [AppError] instances.
 *
 * Usage:
 * ```
 * val error = ErrorMapper.mapToAppError(exception)
 * when (error) {
 *     is AppError.Auth.InvalidCredentials -> showLoginError(error.userMessage)
 *     is AppError.NetworkError -> showRetryButton(error.userMessage)
 *     else -> showGenericError(error.userMessage)
 * }
 * ```
 */
object ErrorMapper {

    fun mapToAppError(e: Throwable): AppError {
        return when (e) {
            // ── Firestore ──
            is FirebaseFirestoreException -> mapFirestoreError(e)

            // ── Network ──
            is UnknownHostException ->
                AppError.NetworkError(reason = "DNS/Network unreachable", cause = e)
            is SocketTimeoutException ->
                AppError.TimeoutError(operation = e.message ?: "Unknown")
            is IOException ->
                AppError.NetworkError(reason = "IO error: ${e.localizedMessage}", cause = e)
            is FirebaseNetworkException ->
                AppError.NetworkError(reason = "Firebase network error", cause = e)
            is FirebaseTooManyRequestsException ->
                AppError.ServerError(code = "TOO_MANY_REQUESTS", cause = e)

            // ── Auth ──
            is FirebaseAuthInvalidCredentialsException ->
                AppError.Auth.InvalidCredentials(cause = e)
            is FirebaseAuthInvalidUserException ->
                AppError.Auth.UserNotFound(cause = e)
            is FirebaseAuthUserCollisionException ->
                AppError.Auth.EmailAlreadyInUse(cause = e)
            is FirebaseAuthWeakPasswordException ->
                AppError.Auth.WeakPassword(cause = e)
            is FirebaseAuthEmailException ->
                AppError.ValidationError(field = "email", reason = "Format email tidak valid.")

            // ── Security ──
            is java.security.InvalidKeyException,
            is java.security.KeyStoreException,
            is java.security.NoSuchAlgorithmException ->
                AppError.Security.KeystoreError(cause = e)

            // ── Fallback ──
            else -> AppError.UnknownError(cause = e)
        }
    }

    private fun mapFirestoreError(e: FirebaseFirestoreException): AppError {
        return when (e.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                AppError.PermissionDenied(
                    resource = "Firestore",
                    cause = e
                ).also {
                    // Hint: Check Firestore rules, App Check, or custom claims
                }

            FirebaseFirestoreException.Code.UNAVAILABLE ->
                AppError.ServerError(
                    code = "UNAVAILABLE",
                    cause = e
                )

            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                AppError.ServerError(
                    code = "RESOURCE_EXHAUSTED",
                    cause = e
                )

            FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                AppError.ServerError(
                    code = "FAILED_PRECONDITION_INDEX",
                    cause = e
                )

            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                AppError.TimeoutError(
                    operation = "Firestore query"
                )

            FirebaseFirestoreException.Code.ABORTED ->
                AppError.Vote.AlreadyVoted(cause = e)

            FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                AppError.Auth.SessionExpired(cause = e)

            FirebaseFirestoreException.Code.NOT_FOUND ->
                AppError.NotFound(entity = "Document")

            else ->
                AppError.ServerError(
                    code = e.code.name,
                    cause = e
                )
        }
    }

    /**
     * Map AppError subclasses back to Firebase error codes for logging.
     */
    fun toFirestoreErrorCode(error: AppError): String? {
        return when (error) {
            is AppError.PermissionDenied -> "PERMISSION_DENIED"
            is AppError.Vote.AlreadyVoted -> "ABORTED"
            is AppError.Auth.SessionExpired -> "UNAUTHENTICATED"
            is AppError.TimeoutError -> "DEADLINE_EXCEEDED"
            is AppError.ServerError -> error.code
            else -> null
        }
    }
}
