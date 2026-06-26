package com.example.elsuarku.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import java.util.concurrent.Executor
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Manages biometric authentication (fingerprint, face, iris) for secure voting.
 *
 * Uses AndroidX Biometric library for consistent behavior across all supported devices.
 * Crypto-backed prompt ensures the user is physically present before casting a vote.
 *
 * Key features:
 * - Automatic recovery from key invalidation (user added/removed biometrics)
 * - Device credential fallback (PIN/pattern/password) when biometric not available
 * - Clear Indonesian error messages for all failure modes
 */
class BiometricPromptManager {

    companion object {
        private const val BIOMETRIC_KEY_ALIAS = "elsuarku_biometric_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG = "BiometricPromptMgr"
    }

    /**
     * Check if the device supports biometric authentication.
     * Uses AndroidX BiometricManager for accurate device capability detection.
     */
    fun canAuthenticate(activity: FragmentActivity): BiometricResult {
        val biometricManager = BiometricManager.from(activity)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricResult.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricResult.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricResult.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricResult.NotEnrolled
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricResult.SecurityUpdateRequired
            else -> BiometricResult.Unknown
        }
    }

    /**
     * Get a crypto-backed Cipher for biometric authentication.
     *
     * If the key was permanently invalidated (user changed biometrics),
     * automatically deletes the old key and creates a new one.
     *
     * Returns null if the device has no enrolled biometrics — the caller
     * should fall back to device-credential-only (PIN/password) authentication.
     */
    fun getBiometricCipher(): Cipher? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

            // Check if key exists and is still valid
            if (keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
                try {
                    val secretKey = (keyStore.getEntry(BIOMETRIC_KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
                    val cipher = Cipher.getInstance(TRANSFORMATION)
                    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                    return cipher
                } catch (e: KeyPermanentlyInvalidatedException) {
                    Log.w(TAG, "Biometric key permanently invalidated — recreating")
                    keyStore.deleteEntry(BIOMETRIC_KEY_ALIAS)
                }
            }

            // Create new key
            createNewBiometricKey()
            val secretKey = (keyStore.getEntry(BIOMETRIC_KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            cipher
        } catch (e: java.security.InvalidAlgorithmParameterException) {
            // No biometric enrolled — key generation fails on emulator/devices without biometrics
            Log.w(TAG, "Biometric key not available (no biometric enrolled): ${e.message}")
            null
        } catch (e: java.lang.IllegalStateException) {
            // Emulator or device without any biometric enrollment
            Log.w(TAG, "Biometric key not available: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize biometric cipher: ${e.message}", e)
            null
        }
    }

    /**
     * Create a new biometric-backed key in the Android Keystore.
     */
    private fun createNewBiometricKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                BIOMETRIC_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true) // Invalidate if biometrics change
                .build()
        )
        keyGenerator.generateKey()
        Log.i(TAG, "New biometric key created successfully")
    }

    /**
     * Show the biometric prompt and call [onSuccess] on successful authentication.
     *
     * Uses AndroidX BiometricPrompt which provides:
     * - Consistent behavior across Android versions
     * - Device credential fallback (PIN/pattern/password)
     * - Clear error codes for all failure modes
     *
     * @param activity The calling Activity (required for the prompt UI)
     * @param title Title shown in the biometric dialog
     * @param subtitle Subtitle shown in the biometric dialog
     * @param allowDeviceCredential If true, allows PIN/pattern/password as fallback
     * @param onSuccess Called when biometric auth succeeds
     * @param onError Called with a user-friendly error message when auth fails
     * @param onCancel Called when user cancels the prompt
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Verifikasi Biometrik",
        subtitle: String = "Konfirmasikan identitas Anda untuk melanjutkan voting",
        allowDeviceCredential: Boolean = true,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        // Validate activity is not finishing/destroyed
        if (activity.isFinishing || activity.isDestroyed) {
            onError("Aplikasi sedang ditutup. Silakan coba lagi.")
            return
        }

        val cipher = getBiometricCipher()
        val useCrypto = cipher != null

        // If no cipher and device credential fallback is NOT allowed → hard error
        if (!useCrypto && !allowDeviceCredential) {
            onError("Gagal menginisialisasi modul keamanan biometrik. " +
                    "Pastikan sidik jari/wajah sudah terdaftar di Pengaturan > Keamanan.")
            return
        }

        // If no cipher but device credential IS allowed → non-crypto prompt (PIN/pattern/password)
        // This covers emulator and devices without biometric enrollment.
        if (!useCrypto && allowDeviceCredential) {
            Log.w(TAG, "Biometric cipher unavailable — falling back to device credential only")
        }

        val executor: Executor = ContextCompat.getMainExecutor(activity)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setConfirmationRequired(true)
            .apply {
                if (useCrypto) {
                    // Crypto-backed: require biometric STRONG, allow device credential fallback
                    setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                } else {
                    // No biometric key available — device credential only
                    setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    setTitle("Verifikasi Perangkat")
                    setSubtitle("Konfirmasikan PIN/pola/sandi perangkat Anda untuk melanjutkan")
                }
            }
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.i(TAG, "Biometric/device authentication succeeded (crypto=${result.cryptoObject != null})")
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.w(TAG, "Biometric error: code=$errorCode msg=$errString")
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                            onCancel()
                        }
                        BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                            if (allowDeviceCredential) {
                                // Should not happen when DEVICE_CREDENTIAL is set, but handle gracefully
                                onError("Verifikasi biometrik tidak tersedia. " +
                                        "Gunakan PIN/pola/sandi perangkat untuk melanjutkan.")
                            } else {
                                onError("Tidak ada biometrik terdaftar di perangkat ini. " +
                                        "Daftarkan sidik jari atau wajah di Pengaturan > Keamanan.")
                            }
                        }
                        BiometricPrompt.ERROR_HW_UNAVAILABLE -> {
                            onError("Sensor biometrik sedang tidak tersedia. " +
                                    "Coba lagi nanti atau gunakan verifikasi alternatif.")
                        }
                        BiometricPrompt.ERROR_HW_NOT_PRESENT -> {
                            onError("Perangkat ini tidak memiliki sensor biometrik. " +
                                    "Gunakan PIN/pola/sandi perangkat untuk verifikasi.")
                        }
                        BiometricPrompt.ERROR_LOCKOUT -> {
                            onError("Terlalu banyak percobaan gagal. " +
                                    "Sensor biometrik terkunci. Tunggu 30 detik atau gunakan PIN perangkat.")
                        }
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            onError("Sensor biometrik terkunci permanen. " +
                                    "Buka kunci perangkat menggunakan PIN/pola, lalu coba lagi.")
                        }
                        BiometricPrompt.ERROR_TIMEOUT -> {
                            onError("Waktu verifikasi habis. Silakan coba lagi.")
                        }
                        BiometricPrompt.ERROR_VENDOR -> {
                            onError("Terjadi kesalahan pada sensor biometrik. Coba lagi.")
                        }
                        else -> {
                            onError("Autentikasi gagal: $errString")
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.d(TAG, "Biometric authentication failed (non-fatal)")
                }
            }
        )

        try {
            if (useCrypto && cipher != null) {
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            } else {
                biometricPrompt.authenticate(promptInfo)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch biometric prompt: ${e.message}", e)
            onError("Gagal membuka dialog biometrik: ${e.localizedMessage ?: "kesalahan sistem"}")
        }
    }
}

/**
 * Result of biometric availability check.
 */
sealed class BiometricResult {
    data object Available : BiometricResult()
    data object NoHardware : BiometricResult()
    data object HardwareUnavailable : BiometricResult()
    data object NotEnrolled : BiometricResult()
    data object SecurityUpdateRequired : BiometricResult()
    data object Unknown : BiometricResult()

    val isAvailable: Boolean get() = this is Available
    val isNotEnrolled: Boolean get() = this is NotEnrolled
    val userFriendlyMessage: String get() = when (this) {
        Available -> "Biometrik tersedia"
        NoHardware -> "Perangkat tidak memiliki sensor biometrik"
        HardwareUnavailable -> "Sensor biometrik sedang tidak tersedia"
        NotEnrolled -> "Belum mendaftarkan sidik jari/wajah di perangkat. Buka Pengaturan > Keamanan > Biometrik."
        SecurityUpdateRequired -> "Diperlukan pembaruan keamanan sistem"
        Unknown -> "Status biometrik tidak diketahui"
    }
}
