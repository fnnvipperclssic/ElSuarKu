package com.example.elsuarku.domain.error

/**
 * Sealed hierarchy for typed errors — enables specific error handling
 * instead of string matching. Each error carries structured metadata
 * for diagnostics and user-facing messages.
 *
 * ## Severity levels:
 * - FATAL: App cannot continue operating (e.g., Firebase not initialized)
 * - ERROR: Operation failed but app can continue (e.g., network timeout)
 * - WARNING: Non-critical issue (e.g., audit log write failed)
 * - INFO: Informational (e.g., data served from cache)
 */
enum class ErrorSeverity {
    FATAL,
    ERROR,
    WARNING,
    INFO
}

sealed class AppError(
    val severity: ErrorSeverity,
    val technical: String,
    val userMessage: String,
    open val cause: Throwable? = null
) {
    // ── Network ──
    data class NetworkError(
        val reason: String = "Koneksi gagal",
        override val cause: Throwable? = null
    ) : AppError(ErrorSeverity.ERROR, technical = reason, userMessage = "Gagal terhubung ke server. Periksa koneksi internet Anda.", cause = cause)

    data class TimeoutError(
        val operation: String = ""
    ) : AppError(ErrorSeverity.ERROR, technical = "Timeout: $operation", userMessage = "Koneksi lambat atau timeout. Coba lagi.")

    data class ServerError(
        val code: String = "",
        override val cause: Throwable? = null
    ) : AppError(ErrorSeverity.ERROR, technical = "Server error: $code", userMessage = "Server sedang sibuk. Coba beberapa saat lagi.", cause = cause)

    // ── Authentication ──
    sealed class Auth(technical: String, userMessage: String, cause: Throwable? = null) :
        AppError(ErrorSeverity.ERROR, technical, userMessage, cause) {

        data class InvalidCredentials(
            override val cause: Throwable? = null
        ) : Auth("Invalid email/password", "Email atau password salah.", cause)

        data class UserNotFound(
            val email: String = "",
            override val cause: Throwable? = null
        ) : Auth("User not found: $email", "Akun tidak ditemukan. Periksa email Anda.", cause)

        data class EmailAlreadyInUse(
            val email: String = "",
            override val cause: Throwable? = null
        ) : Auth("Email already in use: $email", "Email sudah terdaftar. Gunakan email lain atau login.", cause)

        data class WeakPassword(
            override val cause: Throwable? = null
        ) : Auth("Weak password", "Password terlalu lemah. Gunakan minimal 8 karakter dengan huruf besar, kecil, dan angka.", cause)

        data class SessionExpired(
            override val cause: Throwable? = null
        ) : Auth("Session expired", "Sesi Anda telah berakhir. Silakan login ulang.", cause)

        data class TokenRefreshFailed(
            override val cause: Throwable? = null
        ) : Auth("Token refresh failed", "Gagal memperbarui sesi. Silakan login ulang.", cause)
    }

    // ── Voting ──
    sealed class Vote(technical: String, userMessage: String, cause: Throwable? = null) :
        AppError(ErrorSeverity.ERROR, technical, userMessage, cause) {

        data class AlreadyVoted(
            val electionId: String = "",
            override val cause: Throwable? = null
        ) : Vote("Already voted in election $electionId", "Anda sudah memberikan suara dalam pemilihan ini.", cause)

        data class ElectionNotActive(
            val status: String = "",
            override val cause: Throwable? = null
        ) : Vote("Election not active: $status", "Pemilihan sudah tidak aktif.", cause)

        data class ElectionExpired(
            override val cause: Throwable? = null
        ) : Vote("Election expired", "Pemilihan sudah berakhir.", cause)

        data class ElectionNotStarted(
            override val cause: Throwable? = null
        ) : Vote("Election not yet started", "Pemilihan belum dimulai.", cause)

        data class EncryptionFailed(
            override val cause: Throwable? = null
        ) : Vote("Encryption failed", "Gagal mengamankan suara. Coba lagi.", cause)

        data class CounterIncrementFailed(
            override val cause: Throwable? = null
        ) : Vote("Counter increment failed — needs reconciliation", "Suara tercatat tetapi penghitungan tertunda.", cause)
    }

    // ── Permissions / Access ──
    data class PermissionDenied(
        val resource: String = "",
        override val cause: Throwable? = null
    ) : AppError(ErrorSeverity.ERROR, technical = "Permission denied: $resource", userMessage = "Anda tidak memiliki izin untuk mengakses data ini.", cause = cause)

    // ── Validation ──
    data class ValidationError(
        val field: String = "",
        val reason: String = ""
    ) : AppError(ErrorSeverity.ERROR, technical = "Validation: $field — $reason", userMessage = reason.ifBlank { "Data tidak valid." })

    // ── Security ──
    sealed class Security(technical: String, userMessage: String, cause: Throwable? = null) :
        AppError(ErrorSeverity.FATAL, technical, userMessage, cause) {

        data class TamperDetected(
            val detail: String = ""
        ) : Security("Tamper detected: $detail", "Aplikasi mendeteksi modifikasi tidak sah. Demi keamanan, aplikasi tidak dapat dilanjutkan.")

        data class RootDetected(
            val detail: String = ""
        ) : Security(
            "Root/jailbreak detected: $detail",
            "Device terdeteksi telah di-root. Demi keamanan, voting tidak diizinkan di device ini."
        )

        data class EmulatorDetected(
            val detail: String = ""
        ) : Security(
            "Emulator detected: $detail",
            "Emulator terdeteksi. Voting hanya diizinkan di device fisik."
        )

        data class KeystoreError(override val cause: Throwable? = null) :
            Security("Keystore error", "Terjadi kesalahan keamanan perangkat. Restart device Anda dan coba lagi.", cause)

        data class AppCheckFailed(
            override val cause: Throwable? = null
        ) : Security("App Check failed", "Verifikasi integritas aplikasi gagal. Pastikan Anda menggunakan versi resmi.", cause)
    }

    // ── Data ──
    data class NotFound(
        val entity: String = "",
        val id: String = ""
    ) : AppError(ErrorSeverity.ERROR, technical = "$entity not found: $id", userMessage = "Data tidak ditemukan.")

    data class DataConflict(
        val entity: String = "",
        val detail: String = ""
    ) : AppError(ErrorSeverity.ERROR, technical = "Conflict: $entity — $detail", userMessage = "Data telah diubah oleh pengguna lain. Silakan refresh.")

    // ── Unknown ──
    data class UnknownError(
        override val cause: Throwable? = null
    ) : AppError(ErrorSeverity.ERROR, technical = cause?.localizedMessage ?: "Unknown error", userMessage = "Terjadi kesalahan yang tidak diketahui.", cause = cause)
}
