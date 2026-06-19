package com.example.elsuarku.security

import android.app.Activity
import android.view.WindowManager

/**
 * Screen protection for e-voting security.
 *
 * - FLAG_SECURE: Prevents screenshots and screen recording
 * - Prevents content from appearing in the app switcher thumbnail
 *
 * Applied to sensitive screens: voting, candidate details, admin panels.
 */
object ScreenProtection {

    /**
     * Enable screen protection — blocks screenshots and screen recording.
     * Call from onCreate() in sensitive activities/screens.
     */
    fun enable(activity: Activity) {
        activity.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    /**
     * Disable screen protection — allows screenshots again.
     * Call when leaving sensitive screens.
     */
    fun disable(activity: Activity) {
        activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    /**
     * Apply screen protection for the duration of [block].
     */
    fun <T> withProtection(activity: Activity, block: () -> T): T {
        enable(activity)
        return try {
            block()
        } finally {
            disable(activity)
        }
    }
}

/**
 * Composable-safe wrapper for screen protection.
 * Use from Compose screens via LocalContext.
 */
fun android.app.Activity.secureScreen() {
    ScreenProtection.enable(this)
}

fun android.app.Activity.unsecureScreen() {
    ScreenProtection.disable(this)
}
