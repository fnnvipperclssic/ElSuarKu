package com.example.elsuarku.domain.repository

import com.example.elsuarku.data.model.ReconciliationStatus
import com.example.elsuarku.data.model.Vote
import com.example.elsuarku.utils.Resource
import kotlinx.coroutines.flow.Flow

interface IVoteRepository {
    suspend fun submitVote(vote: Vote): Resource<Vote>
    suspend fun checkUserHasVoted(voterHash: String, electionId: String): Resource<Boolean>
    suspend fun getUserVoteForElection(voterHash: String, electionId: String): Resource<Vote?>
    suspend fun getVoteCountForElection(electionId: String): Resource<Int>
    suspend fun getVotesForElection(electionId: String): Resource<List<Vote>>
    suspend fun getTotalVotesCount(): Resource<Int>
    suspend fun updateReconciliationStatus(voterHash: String, electionId: String, status: ReconciliationStatus): Resource<Unit>
    suspend fun getVotesWithStatus(electionId: String, status: ReconciliationStatus): Resource<List<Vote>>
    fun observeUserVoteStatus(userId: String, electionIds: List<String>): Flow<Resource<Set<String>>>
    fun observeUserVoteForElection(voterHash: String, electionId: String): Flow<Resource<Vote?>>
}
