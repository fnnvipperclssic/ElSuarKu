package com.example.elsuarku.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure PIN management using EncryptedSharedPreferences.
 *
 * Stores a 4-digit PIN encrypted with AES-256-GCM via Android KeyStore-backed
 * MasterKey. Also caches user profile for quick biometric/PIN re-login
 * without requiring full Firebase re-authentication.
 *
 * Falls back to regular SharedPreferences if EncryptedSharedPreferences
 * initialization fails (rare — typically only on broken KeyStore implementations).
 *
 * Security:
 * - PIN stored in EncryptedSharedPreferences (AES-256-SIV key encryption,
 *   AES-256-GCM value encryption) when KeyStore is available
 * - MasterKey backed by Android KeyStore (TEE/StrongBox where available)
 * - User profile cached separately from session — survives logout
 * - PIN validation with lockout tracking (handled by AuthViewModel)
 */
class PinManager(private val context: Context) {

    private val prefs: SharedPreferences
    private val isSecure: Boolean

    init {
        val result = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "elsuarku_secure_pin",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).also {
                Log.i(TAG, "EncryptedSharedPreferences initialized successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "EncryptedSharedPreferences init failed, falling back to regular prefs: ${e.message}", e)
            null
        }

        if (result != null) {
            prefs = result
            isSecure = true
        } else {
            // Fallback to regular SharedPreferences (not hardware-backed but functional)
            prefs = context.getSharedPreferences("elsuarku_pin_fallback", Context.MODE_PRIVATE)
            isSecure = false
            Log.w(TAG, "Using fallback SharedPreferences — PIN storage is NOT hardware-backed")
        }
    }

    companion object {
        private const val TAG = "PinManager"
        private const val KEY_PIN = "user_pin"
        private const val KEY_IS_PIN_SET = "is_pin_set"
        private const val KEY_CACHED_UID = "cached_uid"
        private const val KEY_CACHED_NAME = "cached_name"
        private const val KEY_CACHED_EMAIL = "cached_email"
        private const val KEY_CACHED_ROLE = "cached_role"
    }

    // ── PIN Management ──

    /**
     * Set a new 4-digit numeric PIN.
     * Returns true if PIN is valid and stored successfully.
     */
    fun setPin(pin: String): Boolean {
        return if (pin.length == 4 && pin.all { it.isDigit() }) {
            prefs.edit()
                .putString(KEY_PIN, pin)
                .putBoolean(KEY_IS_PIN_SET, true)
                .apply()
            Log.i(TAG, "PIN set successfully (secure=$isSecure)")
            true
        } else {
            Log.w(TAG, "Invalid PIN format: must be 4 digits")
            false
        }
    }

    /**
     * Verify a PIN against the stored value.
     * Returns true if PIN matches.
     */
    fun verifyPin(pin: String): Boolean {
        val storedPin = prefs.getString(KEY_PIN, null)
        return storedPin != null && storedPin == pin
    }

    /**
     * Check if a PIN has been set up.
     */
    fun isPinSet(): Boolean = prefs.getBoolean(KEY_IS_PIN_SET, false)

    /**
     * Clear the stored PIN (on reset or full logout).
     */
    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN)
            .putBoolean(KEY_IS_PIN_SET, false)
            .apply()
        Log.i(TAG, "PIN cleared")
    }

    // ── Persistent User Profile Cache ──

    /**
     * Cache user profile for quick biometric/PIN re-login.
     * This persists across sessions and survives logout,
     * unlike SessionManager which is session-scoped.
     */
    fun cacheUserProfile(uid: String, name: String, email: String, role: String) {
        prefs.edit()
            .putString(KEY_CACHED_UID, uid)
            .putString(KEY_CACHED_NAME, name)
            .putString(KEY_CACHED_EMAIL, email)
            .putString(KEY_CACHED_ROLE, role)
            .apply()
        Log.d(TAG, "User profile cached: uid=${uid.take(8)}... role=$role")
    }

    fun getCachedUserId(): String? = prefs.getString(KEY_CACHED_UID, null)

    fun getCachedUserName(): String = prefs.getString(KEY_CACHED_NAME, "") ?: ""

    fun getCachedUserEmail(): String = prefs.getString(KEY_CACHED_EMAIL, "") ?: ""

    fun getCachedUserRole(): String? = prefs.getString(KEY_CACHED_ROLE, null)

    /**
     * Returns true if a cached user profile exists (user has logged in before).
     */
    fun hasCachedProfile(): Boolean = getCachedUserId() != null

    /**
     * Clear cached user profile (on explicit full logout).
     */
    fun clearUserProfile() {
        prefs.edit()
            .remove(KEY_CACHED_UID)
            .remove(KEY_CACHED_NAME)
            .remove(KEY_CACHED_EMAIL)
            .remove(KEY_CACHED_ROLE)
            .apply()
        Log.i(TAG, "User profile cache cleared")
    }

    /**
     * Clear all data — PIN + profile (factory reset).
     */
    fun clearAll() {
        prefs.edit().clear().apply()
        Log.i(TAG, "All PIN data cleared")
    }
}
