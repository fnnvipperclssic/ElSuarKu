package com.example.elsuarku.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Haptic feedback utility for tactile confirmation of key actions:
 *  - Vote submission
 *  - Biometric auth success
 *  - Error states
 *  - Button press confirmation
 *
 * Falls back gracefully on devices without vibrator hardware.
 */
object HapticFeedback {

    private var vibrator: Vibrator? = null

    fun init(context: Context) {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /** Light tap — button press confirmation */
    fun lightTap(view: View? = null) {
        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            ?: vibrate(VibrationEffect.EFFECT_TICK)
    }

    /** Medium confirmation — successful action (e.g., vote cast) */
    fun confirmSubmit(view: View? = null) {
        view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            ?: vibrate(VibrationEffect.EFFECT_HEAVY_CLICK)
    }

    /** Strong double-pulse — significant event (e.g., vote success) */
    fun voteSuccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 60, 120, 60)
            val effect = VibrationEffect.createWaveform(pattern, -1)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 60, 120, 60), -1)
        }
    }

    /** Error buzz — operation failed */
    fun error() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 100, 50, 100, 50, 100)
            val effect = VibrationEffect.createWaveform(pattern, -1)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 100, 50, 100, 50, 100), -1)
        }
    }

    /** Biometric auth success — subtle feedback */
    fun biometricSuccess() {
        vibrate(VibrationEffect.EFFECT_CLICK)
    }

    private fun vibrate(effectId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(effectId))
        }
    }
}
