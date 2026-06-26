package com.example.elsuarku.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.elsuarku.data.model.UserRole
import com.example.elsuarku.utils.Constants
import java.security.MessageDigest
import java.util.UUID

/**
 * Secure session management using EncryptedSharedPreferences.
 *
 * Manages:
 * - Session tokens with 5-minute inactivity timeout
 * - User role caching
 * - Device fingerprinting for concurrent session detection
 * - Biometric preference
 * - Step-up re-authentication tracking
 */
class SessionManager(private val context: Context) {

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

    companion object {
        private const val TAG = "SessionManager"
    }

    // ---- Session Token ----

    fun saveSessionToken(token: String) {
        prefs.edit().putString(Constants.PREF_SESSION_TOKEN, token).apply()
    }

    fun getSessionToken(): String? {
        return prefs.getString(Constants.PREF_SESSION_TOKEN, null)
    }

    // ---- User Info ----

    fun saveUserInfo(uid: String, role: UserRole, name: String = "", email: String = "") {
        prefs.edit()
            .putString(Constants.PREF_USER_UID, uid)
            .putString(Constants.PREF_USER_ROLE, role.name)
            .putString(Constants.PREF_USER_NAME, name)
            .putString(Constants.PREF_USER_EMAIL, email)
            .putLong(Constants.PREF_LAST_LOGIN, System.currentTimeMillis())
            .putLong(Constants.PREF_LAST_ACTIVITY, System.currentTimeMillis())
            .apply()
    }

    fun getUserId(): String? = prefs.getString(Constants.PREF_USER_UID, null)

    fun getUserName(): String = prefs.getString(Constants.PREF_USER_NAME, "") ?: ""

    fun getUserEmail(): String = prefs.getString(Constants.PREF_USER_EMAIL, "") ?: ""

    fun getUserRole(): UserRole? {
        val roleStr = prefs.getString(Constants.PREF_USER_ROLE, null) ?: return null
        return try {
            UserRole.valueOf(roleStr)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    // ---- Session Timeout (5-minute inactivity) ----

    /**
     * Check if the session has expired (5-minute inactivity timeout).
     * Activity-based: resets on every user interaction via [touchSession].
     */
    fun isSessionExpired(): Boolean {
        val lastActivity = prefs.getLong(Constants.PREF_LAST_ACTIVITY, 0L)
        if (lastActivity == 0L) return true

        val elapsed = System.currentTimeMillis() - lastActivity
        val timeoutMillis = Constants.SESSION_TIMEOUT_MINUTES * 60 * 1000
        return elapsed > timeoutMillis
    }

    /**
     * Touch the session — call on any user interaction (tap, scroll, navigate).
     * Resets the inactivity timer.
     */
    fun touchSession() {
        prefs.edit().putLong(Constants.PREF_LAST_ACTIVITY, System.currentTimeMillis()).apply()
    }

    /**
     * Refresh the login session timestamp (call on explicit re-auth).
     */
    fun refreshSession() {
        prefs.edit()
            .putLong(Constants.PREF_LAST_LOGIN, System.currentTimeMillis())
            .putLong(Constants.PREF_LAST_ACTIVITY, System.currentTimeMillis())
            .apply()
    }

    // ---- Step-Up Authentication (vote requires fresh auth) ----

    /**
     * Records that step-up authentication (biometric/credential) has been completed
     * for the current voting session. This is separate from login auth and expires
     * after 2 minutes — forcing re-auth before vote submission.
     */
    fun setStepUpAuthCompleted() {
        prefs.edit()
            .putLong("step_up_auth_time", System.currentTimeMillis())
            .apply()
    }

    /**
     * Check if step-up authentication is still fresh (within 2 minutes).
     * If expired, user must re-authenticate before voting.
     */
    fun isStepUpAuthValid(): Boolean {
        val stepUpTime = prefs.getLong("step_up_auth_time", 0L)
        if (stepUpTime == 0L) return false
        val elapsed = System.currentTimeMillis() - stepUpTime
        return elapsed < 120_000L // 2 minutes
    }

    fun clearStepUpAuth() {
        prefs.edit().remove("step_up_auth_time").apply()
    }

    // ---- Device Fingerprinting ----

    /**
     * Generate a unique device fingerprint for concurrent session detection.
     * Uses a combination of:
     * - Android ID (Settings.Secure)
     * - Build fingerprint (hardware + OS signature)
     * - Encrypted random UUID (persistent)
     *
     * The fingerprint is stored in EncryptedSharedPreferences and used to
     * detect if a user is logged in from multiple devices.
     */
    fun getOrCreateDeviceFingerprint(): String {
        val existing = prefs.getString(Constants.PREF_DEVICE_FINGERPRINT, null)
        if (existing != null) return existing

        val deviceId = try {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown"
            val buildFp = "${Build.FINGERPRINT}${Build.MODEL}"
            val seed = "$androidId:$buildFp:${UUID.randomUUID()}"
            MessageDigest.getInstance("SHA-256")
                .digest(seed.toByteArray())
                .take(16)
                .joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to generate device fingerprint, using random", e)
            UUID.randomUUID().toString().take(16)
        }

        prefs.edit().putString(Constants.PREF_DEVICE_FINGERPRINT, deviceId).apply()
        Log.d(TAG, "Device fingerprint: ${deviceId.take(8)}...")
        return deviceId
    }

    // ---- Biometric ----

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(Constants.PREF_BIOMETRIC_ENABLED, true) // default ON — biometric wajib
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Constants.PREF_BIOMETRIC_ENABLED, enabled).apply()
    }

    // ---- Clear ----

    /**
     * Clear all session data (on logout or timeout).
     * Also clears step-up auth and device fingerprint.
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
