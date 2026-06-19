package com.example.elsuarku.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages biometric authentication (fingerprint, face, iris) for secure voting.
 *
 * Uses AndroidX Biometric library with a crypto-backed prompt
 * to ensure the user is physically present before casting a vote.
 */
class BiometricPromptManager(private val context: Context) {

    companion object {
        private const val BIOMETRIC_KEY_ALIAS = "elsuarku_biometric_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
    }

    /**
     * Check if the device supports biometric authentication.
     */
    fun canAuthenticate(): BiometricResult {
        val biometricManager = BiometricManager.from(context)
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
     * The cipher is initialized for encryption — if biometric auth succeeds,
     * the cipher can be used to encrypt a verification token.
     */
    fun getBiometricCipher(): Cipher? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

            // Create key if not exists
            if (!keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
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
                        .setUserAuthenticationValidityDurationSeconds(-1) // Require auth every time
                        .build()
                )
                keyGenerator.generateKey()
            }

            val secretKey = (keyStore.getEntry(BIOMETRIC_KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            cipher
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Show the biometric prompt and call [onSuccess] on successful authentication.
     *
     * @param activity The FragmentActivity to host the prompt
     * @param title Title shown in the biometric dialog
     * @param subtitle Subtitle shown in the biometric dialog
     * @param onSuccess Called when biometric auth succeeds
     * @param onError Called with a user-friendly error message when auth fails
     * @param onCancel Called when user cancels the prompt
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Verifikasi Biometrik",
        subtitle: String = "Konfirmasikan identitas Anda untuk melanjutkan voting",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        val cipher = getBiometricCipher()
        if (cipher == null) {
            onError("Gagal menginisialisasi keamanan biometrik. Coba verifikasi alternatif.")
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Batal")
            .setConfirmationRequired(true)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Biometric auth succeeded — cipher is now unlocked
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> onCancel()
                        else -> onError("Autentikasi gagal: $errString")
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Verifikasi biometrik gagal. Coba lagi.")
                }
            }
        )

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
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
    val userFriendlyMessage: String get() = when (this) {
        Available -> "Biometrik tersedia"
        NoHardware -> "Perangkat tidak mendukung biometrik"
        HardwareUnavailable -> "Sensor biometrik tidak tersedia"
        NotEnrolled -> "Belum mendaftarkan biometrik di perangkat"
        SecurityUpdateRequired -> "Diperlukan pembaruan keamanan"
        Unknown -> "Status biometrik tidak diketahui"
    }
}
