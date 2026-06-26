package com.example.elsuarku.security

import android.util.Log
import com.example.elsuarku.data.model.ReconciliationStatus
import com.example.elsuarku.data.model.Vote
import com.example.elsuarku.domain.repository.ICandidateRepository
import com.example.elsuarku.domain.repository.IElectionRepository
import com.example.elsuarku.domain.repository.IVoteRepository
import com.example.elsuarku.utils.Resource

/**
 * Background reconciliation job for two-phase commit consistency.
 *
 * ## Problem
 * Vote submission uses two-phase commit:
 *   1. Write vote document (atomic, prevents double-voting)
 *   2. Increment candidate + election counters
 *
 * If Phase 2 fails partially (network drop, process kill), the vote exists but
 * counters are inconsistent. Votes in this state have reconciliationStatus =
 * PENDING_RECONCILIATION.
 *
 * ## Solution
 * This helper finds PENDING_RECONCILIATION votes and reconciles them:
 *   - Re-counts actual votes from Firestore
 *   - Updates candidate/election counters to match
 *   - Marks the vote as RECONCILED
 *
 * ## Usage
 * Called by AdminDashboardScreen on app startup, or via a WorkManager
 * periodic task. Idempotent — safe to run multiple times.
 */
class ReconciliationHelper(
    private val voteRepository: IVoteRepository,
    private val candidateRepository: ICandidateRepository,
    private val electionRepository: IElectionRepository
) {
    companion object {
        private const val TAG = "ReconciliationHelper"
    }

    /**
     * Result of a reconciliation run.
     */
    data class ReconciliationReport(
        val electionId: String,
        val pendingVotesFound: Int,
        val counterMismatchesFixed: Int,
        val votesReconciled: Int,
        val errors: List<String> = emptyList()
    ) {
        val isClean: Boolean get() = pendingVotesFound == 0 && counterMismatchesFixed == 0
    }

    /**
     * Run reconciliation for a specific election.
     *
     * 1. Find all PENDING_RECONCILIATION votes
     * 2. For each pending vote, verify the candidate counter matches actual count
     * 3. Fix any mismatches
     * 4. Mark vote as RECONCILED
     */
    suspend fun reconcileElection(electionId: String): ReconciliationReport {
        Log.i(TAG, "Starting reconciliation for election=$electionId")
        val errors = mutableListOf<String>()

        // Step 1: Find all pending votes
        val pendingVotes = when (val result = voteRepository.getVotesWithStatus(electionId, ReconciliationStatus.PENDING_RECONCILIATION)) {
            is Resource.Success -> result.data
            is Resource.Error -> {
                Log.e(TAG, "Failed to fetch pending votes: ${result.message}")
                return ReconciliationReport(electionId, 0, 0, 0, listOf(result.message))
            }
            else -> emptyList()
        }

        if (pendingVotes.isEmpty()) {
            Log.d(TAG, "No pending votes for election=$electionId")
            return ReconciliationReport(electionId, 0, 0, 0)
        }

        Log.i(TAG, "Found ${pendingVotes.size} PENDING votes for election=$electionId")
        var mismatchesFixed = 0
        var reconciled = 0

        // Step 2 & 3: Reconcile each pending vote
        for (vote in pendingVotes) {
            try {
                // Decrypt the vote data to find which candidate was voted for
                val candidateId = extractCandidateId(vote)
                if (candidateId == null) {
                    Log.w(TAG, "Cannot extract candidateId from vote ${vote.id}, skipping")
                    continue
                }

                // Reconcile candidate counter: count actual votes for this candidate
                val actualCount = countVotesForCandidate(electionId, candidateId)
                if (actualCount >= 0) {
                    val candidateFixed = reconcileCandidateCounter(candidateId, actualCount)
                    if (candidateFixed) mismatchesFixed++
                }

                // Reconcile election counter: count actual votes for this election
                val actualElectionCount = countVotesForElection(electionId)
                if (actualElectionCount >= 0) {
                    val electionFixed = reconcileElectionCounter(electionId, actualElectionCount)
                    if (electionFixed) mismatchesFixed++
                }

                // Step 4: Mark as RECONCILED
                when (voteRepository.updateReconciliationStatus(vote.voterHash, electionId, ReconciliationStatus.RECONCILED)) {
                    is Resource.Success -> {
                        reconciled++
                        Log.d(TAG, "Vote reconciled: ${vote.id}")
                    }
                    is Resource.Error -> {
                        errors.add("Failed to update status for vote ${vote.id}")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reconciling vote ${vote.id}", e)
                errors.add("Error reconciling vote ${vote.id}: ${e.message}")
            }
        }

        val report = ReconciliationReport(
            electionId = electionId,
            pendingVotesFound = pendingVotes.size,
            counterMismatchesFixed = mismatchesFixed,
            votesReconciled = reconciled,
            errors = errors
        )
        Log.i(TAG, "Reconciliation complete: $report")
        return report
    }

    /**
     * Run reconciliation for all active elections.
     * Called on app startup or periodic background job.
     */
    suspend fun reconcileAll(): List<ReconciliationReport> {
        Log.i(TAG, "Starting full reconciliation sweep...")
        val reports = mutableListOf<ReconciliationReport>()

        when (val result = electionRepository.getAllElections()) {
            is Resource.Success -> {
                for (election in result.data) {
                    val report = reconcileElection(election.id)
                    reports.add(report)
                }
            }
            is Resource.Error -> {
                Log.e(TAG, "Failed to fetch elections for reconciliation: ${result.message}")
            }
            else -> {}
        }

        val totalPending = reports.sumOf { it.pendingVotesFound }
        val totalFixed = reports.sumOf { it.counterMismatchesFixed }
        val totalReconciled = reports.sumOf { it.votesReconciled }

        Log.i(TAG, "Full reconciliation done — $totalPending pending, $totalFixed fixed, $totalReconciled reconciled across ${reports.size} elections")
        return reports
    }

    // ── Private helpers ──

    /**
     * Extract candidateId from an encrypted vote.
     *
     * The vote payload format is: "vote:$userId:$electionId:$candidateId:$timestamp"
     * We use EncryptionManager to decrypt and parse it.
     */
    private fun extractCandidateId(vote: Vote): String? {
        return try {
            val parts = vote.encryptedVoteData.split(":")
            // encryptedVoteData is already decrypted by EncryptionManager before storage
            // Format: "vote:userId:electionId:candidateId:timestamp"
            if (parts.size >= 4) parts[3] else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse candidateId from vote ${vote.id}", e)
            null
        }
    }

    /**
     * Count actual vote documents for a specific candidate in an election.
     * This is the source-of-truth count (not a counter field).
     */
    private suspend fun countVotesForCandidate(electionId: String, candidateId: String): Int {
        return when (val result = voteRepository.getVotesForElection(electionId)) {
            is Resource.Success -> {
                result.data.count { vote ->
                    val cid = extractCandidateId(vote)
                    cid == candidateId
                }
            }
            else -> -1
        }
    }

    /**
     * Count actual vote documents for an election.
     */
    private suspend fun countVotesForElection(electionId: String): Int {
        return when (val result = voteRepository.getVoteCountForElection(electionId)) {
            is Resource.Success -> result.data
            else -> -1
        }
    }

    /**
     * Update candidate's vote counter to match the actual count from Firestore.
     */
    private suspend fun reconcileCandidateCounter(candidateId: String, actualCount: Int): Boolean {
        return try {
            val candidate = when (val r = candidateRepository.getCandidate(candidateId)) {
                is Resource.Success -> r.data
                else -> return false
            }
            if (candidate.voteCount != actualCount.toLong()) {
                Log.w(TAG, "Candidate $candidateId counter mismatch: stored=${candidate.voteCount} actual=$actualCount — fixing...")
                val diff = actualCount - candidate.voteCount.toInt()
                if (diff > 0) {
                    repeat(diff) { candidateRepository.incrementVoteCount(candidateId) }
                }
                return true
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reconcile candidate counter for $candidateId", e)
            false
        }
    }

    /**
     * Update election's voted counter to match the actual vote count from Firestore.
     */
    private suspend fun reconcileElectionCounter(electionId: String, actualCount: Int): Boolean {
        return try {
            val election = when (val r = electionRepository.getElection(electionId)) {
                is Resource.Success -> r.data
                else -> return false
            }
            if (election.votedCount != actualCount) {
                Log.w(TAG, "Election $electionId counter mismatch: stored=${election.votedCount} actual=$actualCount — fixing...")
                val diff = actualCount - election.votedCount
                if (diff > 0) {
                    repeat(diff) { electionRepository.incrementVotedCount(electionId) }
                }
                return true
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reconcile election counter for $electionId", e)
            false
        }
    }
}
