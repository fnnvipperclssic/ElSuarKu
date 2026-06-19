package com.example.elsuarku.security

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HMAC-SHA256 + SHA-256 data integrity verification.
 *
 * Ensures:
 * - Vote data has not been tampered with
 * - Firestore documents can be verified for integrity
 * - All critical operations have verifiable integrity hashes
 */
object IntegrityVerifier {

    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val HASH_ALGORITHM = "SHA-256"

    // Secret key for HMAC — in production, derive from Android Keystore
    private val HMAC_SECRET = "ElSuarKu_Integrity_Secret_2026".toByteArray(Charsets.UTF_8)

    /**
     * Compute SHA-256 hash of input string.
     */
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Compute SHA-256 hash of byte array.
     */
    fun sha256(input: ByteArray): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val hashBytes = digest.digest(input)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Compute HMAC-SHA256 of input string.
     * Provides keyed-hash message authentication for tamper detection.
     */
    fun hmacSha256(input: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        val keySpec = SecretKeySpec(HMAC_SECRET, HMAC_ALGORITHM)
        mac.init(keySpec)
        val hmacBytes = mac.doFinal(input.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hmacBytes, Base64.NO_WRAP)
    }

    /**
     * Verify HMAC-SHA256 — returns true if the computed HMAC matches expected.
     */
    fun verifyHmac(input: String, expectedHmac: String): Boolean {
        return try {
            val computed = hmacSha256(input)
            MessageDigest.isEqual(computed.toByteArray(), expectedHmac.toByteArray())
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generate a combined integrity signature for vote data.
     * Format: SHA256(userId:electionId:candidateId:timestamp:hmac)
     */
    fun generateVoteSignature(userId: String, electionId: String, candidateId: String, timestamp: Long): VoteIntegrity {
        val payload = "$userId:$electionId:$candidateId:$timestamp"
        val hash = sha256(payload)
        val hmac = hmacSha256(payload)
        val verificationToken = sha256("$payload:$hmac").take(16)
        return VoteIntegrity(hash, hmac, verificationToken)
    }

    /**
     * Verify a vote's integrity.
     */
    fun verifyVoteSignature(userId: String, electionId: String, candidateId: String, timestamp: Long, expectedHash: String, expectedHmac: String): Boolean {
        val payload = "$userId:$electionId:$candidateId:$timestamp"
        val computedHash = sha256(payload)
        if (!MessageDigest.isEqual(computedHash.toByteArray(), expectedHash.toByteArray())) return false
        return verifyHmac(payload, expectedHmac)
    }

    /**
     * Generate a data integrity hash for any object.
     * Used to verify Firestore documents haven't been tampered with.
     */
    fun generateDataHash(vararg fields: String): String {
        return sha256(fields.joinToString("|"))
    }

    /**
     * Verify data integrity hash.
     */
    fun verifyDataHash(expectedHash: String, vararg fields: String): Boolean {
        val computed = generateDataHash(*fields)
        return MessageDigest.isEqual(computed.toByteArray(), expectedHash.toByteArray())
    }

    /**
     * Generate a secure random token (for verification tokens, session IDs, etc.).
     */
    fun generateSecureToken(length: Int = 32): String {
        val bytes = ByteArray(length)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    data class VoteIntegrity(
        val hash: String,
        val hmac: String,
        val verificationToken: String
    )
}
