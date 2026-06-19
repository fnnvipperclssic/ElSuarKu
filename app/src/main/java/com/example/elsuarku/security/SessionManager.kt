package com.example.elsuarku.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.elsuarku.data.model.UserRole
import com.example.elsuarku.utils.Constants

/**
 * Secure session management using EncryptedSharedPreferences.
 *
 * Manages:
 * - Session tokens
 * - User role caching
 * - Auto-logout timeout (30 minutes)
 * - Biometric preference
 */
class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "elsuarku_secure_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ---- Session Token ----

    fun saveSessionToken(token: String) {
        prefs.edit().putString(Constants.PREF_SESSION_TOKEN, token).apply()
    }

    fun getSessionToken(): String? {
        return prefs.getString(Constants.PREF_SESSION_TOKEN, null)
    }

    // ---- User Info ----

    fun saveUserInfo(uid: String, role: UserRole, name: String = "") {
        prefs.edit()
            .putString(Constants.PREF_USER_UID, uid)
            .putString(Constants.PREF_USER_ROLE, role.name)
            .putString(Constants.PREF_USER_NAME, name)
            .putLong(Constants.PREF_LAST_LOGIN, System.currentTimeMillis())
            .apply()
    }

    fun getUserId(): String? = prefs.getString(Constants.PREF_USER_UID, null)

    fun getUserName(): String = prefs.getString(Constants.PREF_USER_NAME, "") ?: ""

    fun getUserRole(): UserRole? {
        val roleStr = prefs.getString(Constants.PREF_USER_ROLE, null) ?: return null
        return try {
            UserRole.valueOf(roleStr)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    // ---- Session Timeout ----

    /**
     * Check if the session has expired (30-minute inactivity timeout).
     */
    fun isSessionExpired(): Boolean {
        val lastLogin = prefs.getLong(Constants.PREF_LAST_LOGIN, 0L)
        if (lastLogin == 0L) return true

        val elapsed = System.currentTimeMillis() - lastLogin
        val timeoutMillis = Constants.SESSION_TIMEOUT_MINUTES * 60 * 1000
        return elapsed > timeoutMillis
    }

    /**
     * Refresh the session timestamp (call on user interaction).
     */
    fun refreshSession() {
        prefs.edit().putLong(Constants.PREF_LAST_LOGIN, System.currentTimeMillis()).apply()
    }

    // ---- Biometric ----

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(Constants.PREF_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Constants.PREF_BIOMETRIC_ENABLED, enabled).apply()
    }

    // ---- Clear ----

    /**
     * Clear all session data (on logout or timeout).
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
