package com.example.elsuarku.data.model

import kotlinx.serialization.Serializable

/**
 * Candidate document — stored in Firestore "candidates" collection.
 * Photo is stored as Base64 string (free alternative to Firebase Storage).
 */
@Serializable
data class Candidate(
    val id: String = "",
    val electionId: String = "",
    val name: String = "",
    val photoBase64: String = "",   // Base64-encoded compressed JPEG (free alternative to Firebase Storage)
    val description: String = "",
    val visi: String = "",           // Vision statement
    val misi: String = "",           // Mission statement
    val nomorUrut: Int = 0,          // Candidate number
    val voteCount: Long = 0,
    val status: CandidateStatus = CandidateStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class CandidateStatus {
    ACTIVE,
    INACTIVE,
    DISQUALIFIED
}
