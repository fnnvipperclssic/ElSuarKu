package com.example.elsuarku.data.model

import kotlinx.serialization.Serializable

/**
 * Vote document — stored in Firestore "votes" collection.
 * Vote data is encrypted (AES-256-GCM) and stored with integrity hash.
 */
@Serializable
data class Vote(
    val id: String = "",
    val userId: String = "",
    val electionId: String = "",
    val candidateId: String = "",
    val encryptedVoteData: String = "",     // AES-256-GCM encrypted
    val hash: String = "",                   // SHA-256 integrity hash
    val timestamp: Long = System.currentTimeMillis(),
    val verificationToken: String = ""       // Unique token for voter to verify their vote
)
