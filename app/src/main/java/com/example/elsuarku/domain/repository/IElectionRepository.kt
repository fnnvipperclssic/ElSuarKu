package com.example.elsuarku.domain.repository

import com.example.elsuarku.data.model.Election
import com.example.elsuarku.data.model.ElectionStatus
import com.example.elsuarku.utils.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Election operations.
 * Abstracts Firestore implementation behind a contract for testability and DIP compliance.
 */
interface IElectionRepository {
    suspend fun createElection(election: Election): Resource<Election>
    suspend fun updateElection(election: Election): Resource<Election>
    suspend fun deleteElection(electionId: String): Resource<Unit>
    suspend fun getElection(electionId: String): Resource<Election>
    suspend fun getAllElections(): Resource<List<Election>>
    suspend fun updateElectionStatus(electionId: String, status: ElectionStatus): Resource<Unit>
    suspend fun incrementVotedCount(electionId: String): Resource<Unit>
    suspend fun getElectionCount(): Resource<Int>
    suspend fun getActiveElections(): Resource<List<Election>>
    fun observeActiveElections(): Flow<Resource<List<Election>>>
    fun observeAllElections(): Flow<Resource<List<Election>>>
}
