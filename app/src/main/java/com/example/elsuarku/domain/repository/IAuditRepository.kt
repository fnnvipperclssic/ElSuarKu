package com.example.elsuarku.domain.repository

import com.example.elsuarku.data.model.AuditAction
import com.example.elsuarku.data.model.AuditLog
import com.example.elsuarku.data.model.AuditSeverity
import com.example.elsuarku.utils.Resource
import kotlinx.coroutines.flow.Flow

interface IAuditRepository {
    suspend fun log(
        actorId: String,
        actorName: String?,
        actorRole: String,
        action: AuditAction,
        target: String = "",
        targetName: String = "",
        detail: String = ""
    ): Resource<Unit>

    suspend fun getLogsInRange(start: Long, end: Long): Resource<List<AuditLog>>
    suspend fun getLogsBySeverity(severity: AuditSeverity, limit: Int): Resource<List<AuditLog>>
    fun observeRecentLogs(limit: Long = 50): Flow<Resource<List<AuditLog>>>
}
