package com.example.elsuarku.data.model

import kotlinx.serialization.Serializable

/**
 * Vote document — stored in Firestore "votes" collection.
 *
 * Security guarantees:
 *  - voterHash = SHA256("voter:$userId:$electionId") — anonymous, irreversible, deterministic
 *    → enables duplicate detection WITHOUT storing plaintext userId
 *  - encryptedVoteData = AES-256-GCM(userId:electionId:candidateId:timestamp) — hardware-backed key
 *  - hash = SHA-256 integrity hash of the vote payload
 *  - hmac = HMAC-SHA256 tamper-detection signature (verified on read)
 *  - verificationToken = unique receipt token for the voter
 *  - Document ID = "vote_{electionId}_{voterHash}" → Firestore enforces uniqueness atomically
 *
 * No plaintext userId or candidateId is stored outside the encrypted blob.
 */
@Serializable
data class Vote(
    val id: String = "",
    val electionId: String = "",              // public; needed for group-by-election queries
    val voterHash: String = "",               // SHA256("voter:$userId:$electionId") — anonymous
    val encryptedVoteData: String = "",       // AES-256-GCM encrypted payload
    val hash: String = "",                    // SHA-256 integrity hash
    val hmac: String = "",                    // HMAC-SHA256 tamper-detection signature
    val timestamp: Long = 0L,
    val verificationToken: String = ""        // Unique receipt token for voter verification
)
