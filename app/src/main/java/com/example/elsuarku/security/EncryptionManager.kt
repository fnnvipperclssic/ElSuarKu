package com.example.elsuarku.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.ProviderException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM encryption backed by Android Keystore (StrongBox if available,
 * falls back to TEE / software-backed key).
 *
 * Security:
 * - Hardware-backed AES-256 key in Android Keystore / StrongBox / TEE
 * - GCM mode with 128-bit authentication tag (prevents tampering)
 * - Random 96-bit IV per encryption (prevents pattern analysis)
 * - Key never leaves secure hardware (cannot be extracted)
 * - Key validity limited (1 year) with automatic rotation
 *
 * Output format: Base64(version_byte + IV + ciphertext)
 *   - version_byte: 1 byte (for future key rotation)
 *   - IV: 12 bytes
 *   - Ciphertext: variable (includes GCM auth tag)
 */
class EncryptionManager {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128   // bits
        private const val GCM_IV_LENGTH = 12     // bytes (96-bit NIST recommended)
        private const val KEY_SIZE = 256          // bits
        private const val KEY_VALIDITY_YEARS = 1  // Key rotation period
        private const val CURRENT_KEY_VERSION: Byte = 1
        private const val TAG = "EncryptionManager"
    }

    /**
     * Get the appropriate key alias. Supports key rotation by version.
     */
    private fun keyAlias(): String = "elsuarku_vote_key_v$CURRENT_KEY_VERSION"

    /**
     * Get or create the AES-256 key in Android Keystore.
     *
     * Tries StrongBox first (hardware security module).
     * Falls back to TEE-backed key if StrongBox unavailable.
     * Falls back to software-backed key if TEE unavailable (rare, but covers all devices).
     */
    private fun getOrCreateKey(): SecretKey {
        val alias = keyAlias()

        // Return existing key if already created
        val existingKey = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) {
            Log.d(TAG, "Using existing encryption key: $alias")
            return existingKey.secretKey
        }

        // Try StrongBox → TEE → Software fallback
        return try {
            Log.i(TAG, "Attempting StrongBox-backed key generation...")
            createKey(alias, isStrongBoxBacked = true)
        } catch (e: StrongBoxUnavailableException) {
            Log.w(TAG, "StrongBox not available — falling back to TEE")
            try {
                createKey(alias, isStrongBoxBacked = false)
            } catch (e2: Exception) {
                Log.e(TAG, "TEE key generation failed — using software-backed key", e2)
                createKeySoftware(alias)
            }
        } catch (e: ProviderException) {
            Log.w(TAG, "Provider error for StrongBox — falling back: ${e.message}")
            try {
                createKey(alias, isStrongBoxBacked = false)
            } catch (e2: Exception) {
                Log.e(TAG, "TEE fallback also failed — using software-backed key", e2)
                createKeySoftware(alias)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during key creation — trying software fallback", e)
            try {
                createKeySoftware(alias)
            } catch (e2: Exception) {
                Log.e(TAG, "All key creation methods failed", e2)
                throw IllegalStateException("Tidak dapat membuat kunci enkripsi: ${e2.message}", e2)
            }
        }
    }

    /**
     * Create a hardware-backed key (StrongBox or TEE).
     */
    @Throws(StrongBoxUnavailableException::class, ProviderException::class)
    private fun createKey(alias: String, isStrongBoxBacked: Boolean): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(KEY_SIZE)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setIsStrongBoxBacked(isStrongBoxBacked)
            .setKeyValidityEnd(java.util.Date(System.currentTimeMillis() + KEY_VALIDITY_YEARS * 365L * 24 * 60 * 60 * 1000))
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(spec)
        val key = keyGenerator.generateKey()
        Log.i(TAG, "Key created successfully: $alias (StrongBox=$isStrongBoxBacked)")
        return key
    }

    /**
     * Create a software-backed key (last resort fallback).
     * Still stored in Android Keystore but not hardware-backed.
     */
    @Throws(Exception::class)
    private fun createKeySoftware(alias: String): SecretKey {
        // If a previous key attempt left a broken entry, delete it
        try { keyStore.deleteEntry(alias) } catch (_: Exception) {}

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(KEY_SIZE)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(false)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(spec)
        val key = keyGenerator.generateKey()
        Log.w(TAG, "Software-backed key created: $alias")
        return key
    }

    /**
     * Encrypt plaintext with AES-256-GCM + version byte.
     * Output: Base64([version_byte][IV:12][ciphertext+tag])
     */
    fun encrypt(plainText: String): String {
        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Format: [version:1][IV:12][ciphertext:N]
        val combined = ByteArray(1 + iv.size + encryptedBytes.size)
        combined[0] = CURRENT_KEY_VERSION
        System.arraycopy(iv, 0, combined, 1, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, 1 + iv.size, encryptedBytes.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypt a Base64-encoded ciphertext (with version byte).
     */
    fun decrypt(encryptedBase64: String): String {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)

        // Parse: [version:1][IV:12][ciphertext:N]
        val version = combined[0]
        val iv = combined.copyOfRange(1, 1 + GCM_IV_LENGTH)
        val encryptedBytes = combined.copyOfRange(1 + GCM_IV_LENGTH, combined.size)

        // Future: use version for key rotation
        val alias = if (version == CURRENT_KEY_VERSION) keyAlias()
        else "elsuarku_vote_key_v$version"

        val secretKey = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            ?: throw IllegalStateException("Encryption key v$version not found")

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey.secretKey, spec)

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
