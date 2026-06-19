package com.example.elsuarku.data.model

import kotlinx.serialization.Serializable

/**
 * User document — stored in Firestore "users" collection.
 */
@Serializable
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.PEMILIH,
    val status: UserStatus = UserStatus.ACTIVE,
    val lastLogin: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val photoUrl: String? = null,
    val phoneNumber: String? = null
)

@Serializable
enum class UserRole {
    PEMILIH,      // Voter
    ADMIN,        // Administrator
    MONITOR       // Auditor/Monitor
}

@Serializable
enum class UserStatus {
    ACTIVE,
    SUSPENDED,
    BANNED
}
