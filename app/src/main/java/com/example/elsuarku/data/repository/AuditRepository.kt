package com.example.elsuarku.data.repository

import com.example.elsuarku.data.model.AuditAction
import com.example.elsuarku.data.model.AuditLog
import com.example.elsuarku.data.model.AuditSeverity
import com.example.elsuarku.domain.repository.IAuditRepository
import com.example.elsuarku.utils.Constants
import com.example.elsuarku.utils.Resource
import com.example.elsuarku.utils.toFirestoreErrorMessage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Manages Audit Log operations for security monitoring and compliance.
 */
class AuditRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IAuditRepository {

    private val collection = firestore.collection(Constants.COLLECTION_AUDIT_LOGS)

    /**
     * Write an audit log entry (interface-compliant version).
     */
    override suspend fun log(
        actorId: String,
        actorName: String?,
        actorRole: String,
        action: AuditAction,
        target: String,
        targetName: String,
        detail: String
    ): Resource<Unit> {
        return logInternal(
            actorId = actorId,
            actorName = actorName ?: "",
            actorRole = actorRole,
            action = action,
            target = target,
            targetName = targetName,
            metadata = if (detail.isNotBlank()) mapOf("detail" to detail) else emptyMap(),
            severity = AuditSeverity.INFO
        )
    }

    /**
     * Write an audit log entry (internal version with full params).
     */
    suspend fun logInternal(
        actorId: String,
        actorName: String,
        actorRole: String,
        action: AuditAction,
        target: String = "",
        targetName: String = "",
        metadata: Map<String, String> = emptyMap(),
        severity: AuditSeverity = AuditSeverity.INFO
    ): Resource<Unit> {
        return try {
            val log = AuditLog(
                id = collection.document().id,
                actorId = actorId,
                actorName = actorName,
                actorRole = actorRole,
                action = action,
                target = target,
                targetName = targetName,
                timestamp = System.currentTimeMillis(),
                metadata = metadata,
                severity = severity
            )
            collection.document(log.id).set(log).await()
            Resource.success(Unit)
        } catch (e: Exception) {
            // Don't fail the main operation if logging fails
            Resource.error(e.localizedMessage ?: "Gagal menulis log audit", e)
        }
    }

    /**
     * Stream audit logs in real-time (for Monitor dashboard).
     */
    override fun observeRecentLogs(limit: Long): Flow<Resource<List<AuditLog>>> = callbackFlow {
        val listener = collection
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.error(error.toFirestoreErrorMessage(), error))
                    return@addSnapshotListener
                }
                val logs = snapshot?.toObjects(AuditLog::class.java) ?: emptyList()
                trySend(Resource.success(logs))
            }
        awaitClose { listener.remove() }
    }

    /**
     * Get logs for a specific date range (for reports).
     */
    override suspend fun getLogsInRange(start: Long, end: Long): Resource<List<AuditLog>> {
        return try {
            val snapshot = collection
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThanOrEqualTo("timestamp", end)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            Resource.success(snapshot.toObjects(AuditLog::class.java))
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memuat log", e)
        }
    }

    /**
     * Get logs filtered by severity (for security monitoring).
     */
    override suspend fun getLogsBySeverity(severity: AuditSeverity, limit: Int): Resource<List<AuditLog>> {
        return try {
            val snapshot = collection
                .whereEqualTo("severity", severity.name)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            Resource.success(snapshot.toObjects(AuditLog::class.java))
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memuat log", e)
        }
    }

    /**
     * Get logs for a specific actor (user).
     */
    suspend fun getLogsForUser(userId: String, limit: Long = 50): Resource<List<AuditLog>> {
        return try {
            val snapshot = collection
                .whereEqualTo("actorId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            Resource.success(snapshot.toObjects(AuditLog::class.java))
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memuat log pengguna", e)
        }
    }
}
