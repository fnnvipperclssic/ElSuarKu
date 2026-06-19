package com.example.elsuarku.data.model

import kotlinx.serialization.Serializable

/**
 * Election document — stored in Firestore "elections" collection.
 */
@Serializable
data class Election(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val startDate: Long = 0L,       // epoch millis
    val endDate: Long = 0L,          // epoch millis
    val status: ElectionStatus = ElectionStatus.DRAFT,
    val createdBy: String = "",      // admin UID
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val totalVoters: Int = 0,
    val votedCount: Int = 0
)

@Serializable
enum class ElectionStatus {
    DRAFT,
    ACTIVE,
    COMPLETED,
    CANCELLED
}
