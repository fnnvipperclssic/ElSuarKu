package com.example.elsuarku.utils

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility extension functions for ElSuarKu.
 */

/**
 * Unwrap a [Context] to find the hosting [FragmentActivity].
 *
 * In Compose, [androidx.compose.ui.platform.LocalContext] may return a
 * [ContextThemeWrapper] or similar wrapper around the real Activity.
 * This function walks up the [ContextWrapper] chain to find the
 * [FragmentActivity] required for [androidx.biometric.BiometricPrompt].
 *
 * @return The [FragmentActivity] this context belongs to, or null if not found.
 */
fun Context.unwrapFragmentActivity(): FragmentActivity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return this as? FragmentActivity
}

// ---- Date Formatting ----

fun Long.toFormattedDate(pattern: String = "dd MMM yyyy"): String {
    val fmt = SimpleDateFormat(pattern, Locale.forLanguageTag("id"))
    return fmt.format(Date(this))
}

fun Long.toFormattedDateTime(): String {
    val fmt = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.forLanguageTag("id"))
    return fmt.format(Date(this))
}

fun Long.toFormattedDateTimeFull(): String {
    val fmt = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.forLanguageTag("id"))
    return fmt.format(Date(this))
}

// ---- Election Status Helpers ----

fun Long.isElectionOngoing(endDate: Long): Boolean {
    val now = System.currentTimeMillis()
    return this <= now && now <= endDate
}

fun Long.isElectionEnded(endDate: Long): Boolean {
    return System.currentTimeMillis() > endDate
}

fun Long.timeRemainingText(endDate: Long): String {
    val remaining = endDate - System.currentTimeMillis()
    if (remaining <= 0) return "Berakhir"

    val days = remaining / (24 * 60 * 60 * 1000)
    val hours = (remaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)

    return when {
        days > 0 -> "$days hari ${hours}jam lagi"
        hours > 0 -> "$hours jam lagi"
        else -> "Kurang dari 1 jam"
    }
}

// ---- Toast Helpers ----

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

// ---- String Validation ----

fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.isValidPassword(): Boolean {
    return this.length >= 6
}

// ---- Firestore Error Diagnostics ----

/**
 * Produces a user-friendly Indonesian message for Firestore permission errors.
 * Detects common root causes: App Check, security rules, or missing indexes.
 */
fun Exception.toFirestoreErrorMessage(): String {
    val msg = localizedMessage ?: toString()
    return when {
        msg.contains("PERMISSION_DENIED") ->
            "Akses ditolak Firestore. Kemungkinan penyebab:\n" +
            "1. App Check belum dikonfigurasi\n" +
            "2. Aturan keamanan Firestore perlu diperbarui\n" +
            "3. Token autentikasi kadaluarsa\n" +
            "Coba login ulang atau hubungi administrator."
        msg.contains("UNAVAILABLE") || msg.contains("UNAUTHENTICATED") ->
            "Layanan cloud sedang tidak tersedia. Periksa koneksi internet Anda."
        msg.contains("RESOURCE_EXHAUSTED") ->
            "Terlalu banyak permintaan. Silakan tunggu beberapa saat."
        msg.contains("FAILED_PRECONDITION") ->
            "Database memerlukan indeks. Hubungi administrator untuk membuat indeks yang diperlukan."
        msg.contains("DEADLINE_EXCEEDED") ->
            "Permintaan timeout. Koneksi internet mungkin lambat."
        msg.contains("ABORTED") ->
            "Operasi dibatalkan karena konflik. Coba lagi."
        else -> msg.ifBlank { "Terjadi kesalahan tidak dikenal" }
    }
}
