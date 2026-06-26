package com.example.elsuarku.testutil

import com.example.elsuarku.data.model.*
import com.example.elsuarku.domain.repository.*
import com.example.elsuarku.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake repositories for unit testing ViewModels.
 *
 * Each fake implements the repository interface and stores data in mutable collections.
 * This avoids Firebase dependencies in unit tests — only the ViewModel logic is tested.
 *
 * All fakes are designed to be deterministic: the same operation always produces
 * the same result for the same state. No randomness, no network delays.
 *
 * ## Usage
 * ```kotlin
 * val fakeElections = FakeElectionRepository()
 * fakeElections.setElections(listOf(TestElectionFactory.active()))
 * val viewModel = DashboardViewModel(fakeElections, fakeVotes, fakeAuth, ...)
 * ```
 */

class FakeElectionRepository : IElectionRepository {
    private val elections = mutableListOf<Election>()
    private val electionsFlow = MutableStateFlow<Resource<List<Election>>>(Resource.success(emptyList()))

    fun setElections(list: List<Election>) {
        elections.clear(); elections.addAll(list)
        electionsFlow.value = Resource.success(elections.toList())
    }

    override suspend fun createElection(election: Election): Resource<Election> {
        elections.add(election)
        electionsFlow.value = Resource.success(elections.toList())
        return Resource.success(election)
    }

    override suspend fun updateElection(election: Election): Resource<Election> {
        val idx = elections.indexOfFirst { it.id == election.id }
        if (idx >= 0) elections[idx] = election
        electionsFlow.value = Resource.success(elections.toList())
        return Resource.success(election)
    }

    override suspend fun deleteElection(electionId: String): Resource<Unit> {
        elections.removeAll { it.id == electionId }
        electionsFlow.value = Resource.success(elections.toList())
        return Resource.success(Unit)
    }

    override suspend fun getElection(electionId: String): Resource<Election> {
        return elections.find { it.id == electionId }
            ?.let { Resource.success(it) }
            ?: Resource.error("Election not found: $electionId")
    }

    override suspend fun getAllElections(): Resource<List<Election>> =
        Resource.success(elections.toList())

    override suspend fun updateElectionStatus(electionId: String, status: ElectionStatus): Resource<Unit> {
        val idx = elections.indexOfFirst { it.id == electionId }
        if (idx >= 0) elections[idx] = elections[idx].copy(status = status)
        return Resource.success(Unit)
    }

    override suspend fun incrementVotedCount(electionId: String): Resource<Unit> {
        val idx = elections.indexOfFirst { it.id == electionId }
        if (idx >= 0) elections[idx] = elections[idx].copy(votedCount = elections[idx].votedCount + 1)
        return Resource.success(Unit)
    }

    override suspend fun getElectionCount(): Resource<Int> =
        Resource.success(elections.size)

    override fun observeActiveElections(): Flow<Resource<List<Election>>> =
        electionsFlow.map { result ->
            if (result is Resource.Success) {
                Resource.success(result.data.filter { it.status == ElectionStatus.ACTIVE })
            } else result
        }

    override fun observeAllElections(): Flow<Resource<List<Election>>> = electionsFlow

    override suspend fun getActiveElections(): Resource<List<Election>> =
        Resource.success(elections.filter { it.status == ElectionStatus.ACTIVE || it.status == ElectionStatus.DRAFT })

    // Test-helpers
    fun simulateError(message: String = "Simulated error") {
        electionsFlow.value = Resource.error(message)
    }
}

class FakeVoteRepository : IVoteRepository {
    private val votes = mutableListOf<Vote>()

    var submitVoteShouldFail: Boolean = false
    var submitVoteErrorMsg: String = "Vote submission failed"

    override suspend fun submitVote(vote: Vote): Resource<Vote> {
        if (submitVoteShouldFail) return Resource.error(submitVoteErrorMsg)
        val docId = "vote_${vote.electionId}_${vote.voterHash}"
        val saved = vote.copy(id = docId, timestamp = System.currentTimeMillis())
        votes.add(saved)
        return Resource.success(saved)
    }

    override suspend fun checkUserHasVoted(voterHash: String, electionId: String): Resource<Boolean> {
        return Resource.success(
            votes.any { it.voterHash == voterHash && it.electionId == electionId }
        )
    }

    override suspend fun getUserVoteForElection(voterHash: String, electionId: String): Resource<Vote?> {
        return Resource.success(
            votes.find { it.voterHash == voterHash && it.electionId == electionId }
        )
    }

    override suspend fun getVoteCountForElection(electionId: String): Resource<Int> {
        return Resource.success(votes.count { it.electionId == electionId })
    }

    override suspend fun getVotesForElection(electionId: String): Resource<List<Vote>> {
        return Resource.success(votes.filter { it.electionId == electionId })
    }

    override suspend fun getTotalVotesCount(): Resource<Int> =
        Resource.success(votes.size)

    override suspend fun updateReconciliationStatus(
        voterHash: String, electionId: String, status: ReconciliationStatus
    ): Resource<Unit> {
        val idx = votes.indexOfFirst { it.voterHash == voterHash && it.electionId == electionId }
        if (idx >= 0) votes[idx] = votes[idx].copy(reconciliationStatus = status)
        return Resource.success(Unit)
    }

    override suspend fun getVotesWithStatus(
        electionId: String, status: ReconciliationStatus
    ): Resource<List<Vote>> {
        return Resource.success(
            votes.filter { it.electionId == electionId && it.reconciliationStatus == status }
        )
    }

    override fun observeUserVoteStatus(userId: String, electionIds: List<String>): Flow<Resource<Set<String>>> {
        val votedEids = votes
            .filter { it.electionId in electionIds }
            .map { it.electionId }
            .toSet()
        return MutableStateFlow(Resource.success(votedEids))
    }

    override fun observeUserVoteForElection(voterHash: String, electionId: String): Flow<Resource<Vote?>> {
        val vote = votes.find { it.voterHash == voterHash && it.electionId == electionId }
        return MutableStateFlow(Resource.success(vote))
    }

    // Test-helpers
    fun addVote(vote: Vote) { votes.add(vote) }
    fun clear() { votes.clear() }
}

class FakeCandidateRepository : ICandidateRepository {
    private val candidates = mutableListOf<Candidate>()
    private val candidatesFlow = MutableStateFlow<Resource<List<Candidate>>>(
        Resource.success(emptyList())
    )

    fun setCandidates(list: List<Candidate>) {
        candidates.clear(); candidates.addAll(list)
        candidatesFlow.value = Resource.success(candidates.toList())
    }

    override suspend fun createCandidate(candidate: Candidate): Resource<Candidate> {
        candidates.add(candidate)
        candidatesFlow.value = Resource.success(candidates.toList())
        return Resource.success(candidate)
    }

    override suspend fun updateCandidate(candidate: Candidate): Resource<Candidate> {
        val idx = candidates.indexOfFirst { it.id == candidate.id }
        if (idx >= 0) candidates[idx] = candidate
        return Resource.success(candidate)
    }

    override suspend fun deleteCandidate(candidateId: String): Resource<Unit> {
        candidates.removeAll { it.id == candidateId }
        return Resource.success(Unit)
    }

    override suspend fun getCandidate(candidateId: String): Resource<Candidate> {
        return candidates.find { it.id == candidateId }
            ?.let { Resource.success(it) }
            ?: Resource.error("Candidate not found: $candidateId")
    }

    override suspend fun incrementVoteCount(candidateId: String): Resource<Unit> {
        val idx = candidates.indexOfFirst { it.id == candidateId }
        if (idx >= 0) {
            candidates[idx] = candidates[idx].copy(voteCount = candidates[idx].voteCount + 1)
        }
        return Resource.success(Unit)
    }

    override suspend fun getCandidateCount(): Resource<Int> =
        Resource.success(candidates.size)

    override suspend fun getCandidates(electionId: String): Resource<List<Candidate>> =
        Resource.success(candidates.filter { it.electionId == electionId && it.status == CandidateStatus.ACTIVE })

    override fun observeCandidates(electionId: String): Flow<Resource<List<Candidate>>> =
        candidatesFlow.map { result ->
            if (result is Resource.Success) {
                Resource.success(result.data.filter { it.electionId == electionId })
            } else result
        }
}

class FakeAuthRepository : IAuthRepository {
    private val users = mutableListOf<User>()
    private val devicePrints = mutableMapOf<String, MutableMap<String, Long>>()

    var loginShouldSucceed: Boolean = true
    var loginErrorMsg: String = "Invalid credentials"
    var loginUid: String = "user-test-001"

    fun setUsers(list: List<User>) { users.clear(); users.addAll(list) }

    override suspend fun login(email: String, password: String): Resource<String> {
        return if (loginShouldSucceed) Resource.success(loginUid)
        else Resource.error(loginErrorMsg)
    }

    override suspend fun register(
        email: String, password: String, name: String, role: String
    ): Resource<String> = Resource.success("new-user-uid")

    override suspend fun signOut(): Resource<Unit> = Resource.success(Unit)

    override suspend fun getUser(userId: String): Resource<User> {
        return users.find { it.uid == userId }
            ?.let { Resource.success(it) }
            ?: Resource.error("User not found: $userId")
    }

    override suspend fun getUserPhoto(userId: String): String? = null

    override suspend fun updateProfilePhoto(userId: String, photoBase64: String): Boolean = true

    override suspend fun registerDeviceFingerprint(userId: String, fingerprint: String): Resource<Unit> {
        devicePrints.getOrPut(userId) { mutableMapOf() }
            .put(fingerprint, System.currentTimeMillis())
        return Resource.success(Unit)
    }

    override suspend fun checkConcurrentSessions(
        userId: String, fingerprint: String
    ): Resource<List<String>> {
        val threshold = System.currentTimeMillis() - 30 * 60 * 1000L
        return Resource.success(
            devicePrints[userId]
                ?.filterKeys { it != fingerprint }
                ?.filter { it.value > threshold }
                ?.keys?.toList()
                ?: emptyList()
        )
    }

    override suspend fun getAllUsers(): Resource<List<User>> =
        Resource.success(users.toList())

    override fun observeAllUsers(): Flow<Resource<List<User>>> =
        MutableStateFlow(Resource.success(users.toList()))
}

class FakeAuditRepository : IAuditRepository {
    private val logs = mutableListOf<AuditLog>()

    override suspend fun log(
        actorId: String, actorName: String?, actorRole: String,
        action: AuditAction, target: String, targetName: String, detail: String
    ): Resource<Unit> {
        logs.add(
            AuditLog(
                actorId = actorId,
                actorName = actorName ?: "",
                actorRole = actorRole,
                action = action,
                target = target,
                targetName = targetName,
                metadata = if (detail.isNotBlank()) mapOf("detail" to detail) else emptyMap(),
                timestamp = System.currentTimeMillis()
            )
        )
        return Resource.success(Unit)
    }

    override suspend fun getLogsInRange(start: Long, end: Long): Resource<List<AuditLog>> {
        return Resource.success(logs.filter { it.timestamp in start..end })
    }

    override fun observeRecentLogs(limit: Long): Flow<Resource<List<AuditLog>>> {
        return MutableStateFlow(Resource.success(logs.takeLast(limit.toInt())))
    }

    override suspend fun getLogsBySeverity(severity: AuditSeverity, limit: Int): Resource<List<AuditLog>> {
        return Resource.success(
            logs.filter { it.severity == severity }.take(limit)
        )
    }

    // Test-helpers
    fun getLogs(): List<AuditLog> = logs.toList()
    fun clear() { logs.clear() }
}
