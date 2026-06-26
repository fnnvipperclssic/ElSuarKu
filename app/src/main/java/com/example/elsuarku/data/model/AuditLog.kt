package com.example.elsuarku.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Audit Log document — stored in Firestore "audit_logs" collection.
 * Records all security-relevant actions for monitoring and compliance.
 */
@Immutable
@Serializable
data class AuditLog(
    val id: String = "",
    val actorId: String = "",           // User UID who performed the action
    val actorName: String = "",
    val actorRole: String = "",
    val action: AuditAction = AuditAction.OTHER,
    val target: String = "",            // Target resource (election ID, candidate ID, etc.)
    val targetName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap(),  // Additional context
    val ipAddress: String? = null,
    val deviceInfo: String? = null,
    val severity: AuditSeverity = AuditSeverity.INFO
)

@Serializable
enum class AuditAction {
    REGISTER,
    LOGIN,
    LOGOUT,
    VOTE_CAST,
    ELECTION_CREATED,
    ELECTION_UPDATED,
    ELECTION_DELETED,
    CANDIDATE_CREATED,
    CANDIDATE_UPDATED,
    CANDIDATE_DELETED,
    USER_SUSPENDED,
    USER_ACTIVATED,
    SECURITY_ALERT,
    SYSTEM_ERROR,
    EXPORT_PERFORMED,
    OTHER
}

@Serializable
enum class AuditSeverity {
    INFO,
    WARNING,
    CRITICAL
}
