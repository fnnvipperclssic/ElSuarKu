package com.example.elsuarku.domain.repository

import com.example.elsuarku.data.model.Candidate
import com.example.elsuarku.utils.Resource
import kotlinx.coroutines.flow.Flow

interface ICandidateRepository {
    suspend fun createCandidate(candidate: Candidate): Resource<Candidate>
    suspend fun updateCandidate(candidate: Candidate): Resource<Candidate>
    suspend fun deleteCandidate(candidateId: String): Resource<Unit>
    suspend fun getCandidate(candidateId: String): Resource<Candidate>
    suspend fun incrementVoteCount(candidateId: String): Resource<Unit>
    suspend fun getCandidateCount(): Resource<Int>
    suspend fun getCandidates(electionId: String): Resource<List<Candidate>>
    fun observeCandidates(electionId: String): Flow<Resource<List<Candidate>>>
}
