package com.example.elsuarku.data.repository

import com.example.elsuarku.data.model.AuditAction
import com.example.elsuarku.data.model.AuditLog
import com.example.elsuarku.data.model.AuditSeverity
import com.example.elsuarku.utils.Constants
import com.example.elsuarku.utils.Resource
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
) {

    private val collection = firestore.collection(Constants.COLLECTION_AUDIT_LOGS)

    /**
     * Write an audit log entry.
     */
    suspend fun log(
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
    fun observeRecentLogs(limit: Long = 100): Flow<Resource<List<AuditLog>>> = callbackFlow {
        val listener = collection
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.error(error.localizedMessage ?: "Gagal memuat log", error))
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
    suspend fun getLogsInRange(startTime: Long, endTime: Long): Resource<List<AuditLog>> {
        return try {
            val snapshot = collection
                .whereGreaterThanOrEqualTo("timestamp", startTime)
                .whereLessThanOrEqualTo("timestamp", endTime)
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
    suspend fun getLogsBySeverity(severity: AuditSeverity, limit: Long = 50): Resource<List<AuditLog>> {
        return try {
            val snapshot = collection
                .whereEqualTo("severity", severity.name)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
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
