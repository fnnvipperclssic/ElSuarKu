package com.example.elsuarku.utils

import android.content.Context
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility extension functions for ElSuarKu.
 */

// ---- Date Formatting ----

fun Long.toFormattedDate(pattern: String = "dd MMM yyyy"): String {
    val fmt = SimpleDateFormat(pattern, Locale("id"))
    return fmt.format(Date(this))
}

fun Long.toFormattedDateTime(): String {
    val fmt = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id"))
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
