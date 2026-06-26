package com.example.elsuarku.security

import org.junit.Assert.*
import org.junit.Test

class IntegrityVerifierTest {

    // ═══════════════════════════════════════════════════════════════
    // SHA-256
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `sha256 produces deterministic output`() {
        val hash1 = IntegrityVerifier.sha256("hello world")
        val hash2 = IntegrityVerifier.sha256("hello world")
        assertEquals(hash1, hash2)
    }

    @Test
    fun `sha256 different inputs produce different hashes`() {
        val hash1 = IntegrityVerifier.sha256("hello world")
        val hash2 = IntegrityVerifier.sha256("hello world!")
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `sha256 output is 64 hex characters`() {
        val hash = IntegrityVerifier.sha256("test")
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("^[0-9a-f]+$")))
    }

    @Test
    fun `sha256 produces known hash for fixed input`() {
        // SHA-256 of "test" = 9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08
        val hash = IntegrityVerifier.sha256("test")
        assertEquals(
            "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
            hash
        )
    }

    @Test
    fun `sha256 byte array produces expected hash`() {
        val hash = IntegrityVerifier.sha256("test".toByteArray())
        assertEquals(64, hash.length)
    }

    // ═══════════════════════════════════════════════════════════════
    // HMAC-SHA256
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `hmacSha256 produces deterministic output`() {
        val hmac1 = IntegrityVerifier.hmacSha256("data")
        val hmac2 = IntegrityVerifier.hmacSha256("data")
        assertEquals(hmac1, hmac2)
    }

    @Test
    fun `hmacSha256 different inputs produce different outputs`() {
        val hmac1 = IntegrityVerifier.hmacSha256("data1")
        val hmac2 = IntegrityVerifier.hmacSha256("data2")
        assertNotEquals(hmac1, hmac2)
    }

    @Test
    fun `verifyHmac returns true for matching HMAC`() {
        val input = "vote:user1:e1:c1:123456"
        val hmac = IntegrityVerifier.hmacSha256(input)
        assertTrue(IntegrityVerifier.verifyHmac(input, hmac))
    }

    @Test
    fun `verifyHmac returns false for tampered input`() {
        val input = "vote:user1:e1:c1:123456"
        val hmac = IntegrityVerifier.hmacSha256(input)
        assertFalse(IntegrityVerifier.verifyHmac("tampered-input", hmac))
    }

    @Test
    fun `verifyHmac returns false for wrong HMAC`() {
        val hmac = IntegrityVerifier.hmacSha256("data1")
        assertFalse(IntegrityVerifier.verifyHmac("data1", "wrong-hmac"))
    }

    // ═══════════════════════════════════════════════════════════════
    // Vote Signature
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `generateVoteSignature returns all three components`() {
        val sig = IntegrityVerifier.generateVoteSignature("u1", "e1", "c1", 1000L)
        assertTrue(sig.hash.isNotBlank())
        assertTrue(sig.hmac.isNotBlank())
        assertTrue(sig.verificationToken.isNotBlank())
        assertEquals(32, sig.verificationToken.length)
    }

    @Test
    fun `generateVoteSignature is deterministic`() {
        val sig1 = IntegrityVerifier.generateVoteSignature("u1", "e1", "c1", 1000L)
        val sig2 = IntegrityVerifier.generateVoteSignature("u1", "e1", "c1", 1000L)
        assertEquals(sig1.hash, sig2.hash)
        assertEquals(sig1.hmac, sig2.hmac)
        assertEquals(sig1.verificationToken, sig2.verificationToken)
    }

    @Test
    fun `verifyVoteSignature detects tampered candidate`() {
        val sig = IntegrityVerifier.generateVoteSignature("u1", "e1", "c1", 1000L)
        assertFalse(
            IntegrityVerifier.verifyVoteSignature("u1", "e1", "c2", 1000L, sig.hash, sig.hmac)
        )
    }

    @Test
    fun `verifyVoteSignature detects tampered timestamp`() {
        val sig = IntegrityVerifier.generateVoteSignature("u1", "e1", "c1", 1000L)
        assertFalse(
            IntegrityVerifier.verifyVoteSignature("u1", "e1", "c1", 2000L, sig.hash, sig.hmac)
        )
    }

    @Test
    fun `verifyVoteSignature passes for matching data`() {
        val sig = IntegrityVerifier.generateVoteSignature("u1", "e1", "c1", 1000L)
        assertTrue(
            IntegrityVerifier.verifyVoteSignature("u1", "e1", "c1", 1000L, sig.hash, sig.hmac)
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // Data Hash
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `generateDataHash with multiple fields`() {
        val hash = IntegrityVerifier.generateDataHash("a", "b", "c")
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("^[0-9a-f]+$")))
    }

    @Test
    fun `verifyDataHash succeeds`() {
        val hash = IntegrityVerifier.generateDataHash("a", "b", "c")
        assertTrue(IntegrityVerifier.verifyDataHash(hash, "a", "b", "c"))
    }

    @Test
    fun `verifyDataHash fails on tampered field`() {
        val hash = IntegrityVerifier.generateDataHash("a", "b", "c")
        assertFalse(IntegrityVerifier.verifyDataHash(hash, "a", "b", "d"))
    }

    // ═══════════════════════════════════════════════════════════════
    // Secure Token
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `generateSecureToken default length is 32`() {
        assertEquals(32, IntegrityVerifier.generateSecureToken().length)
    }

    @Test
    fun `generateSecureToken custom length`() {
        assertEquals(16, IntegrityVerifier.generateSecureToken(16).length)
        assertEquals(64, IntegrityVerifier.generateSecureToken(64).length)
    }

    @Test
    fun `generateSecureToken produces unique tokens`() {
        val tokens = (1..100).map { IntegrityVerifier.generateSecureToken() }
        assertEquals(100, tokens.toSet().size)
    }

    // ═══════════════════════════════════════════════════════════════
    // Voter Hash
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `computeVoterHash is deterministic`() {
        val h1 = IntegrityVerifier.computeVoterHash("user-1", "election-1")
        val h2 = IntegrityVerifier.computeVoterHash("user-1", "election-1")
        assertEquals(h1, h2)
    }

    @Test
    fun `computeVoterHash different users produce different hashes`() {
        val h1 = IntegrityVerifier.computeVoterHash("user-1", "election-1")
        val h2 = IntegrityVerifier.computeVoterHash("user-2", "election-1")
        assertNotEquals(h1, h2)
    }

    @Test
    fun `computeVoterHash different elections produce different hashes`() {
        val h1 = IntegrityVerifier.computeVoterHash("user-1", "election-1")
        val h2 = IntegrityVerifier.computeVoterHash("user-1", "election-2")
        assertNotEquals(h1, h2)
    }

    @Test
    fun `computeVoterHash output is 64 hex chars`() {
        val hash = IntegrityVerifier.computeVoterHash("user-1", "election-1")
        assertEquals(64, hash.length)
    }
}
