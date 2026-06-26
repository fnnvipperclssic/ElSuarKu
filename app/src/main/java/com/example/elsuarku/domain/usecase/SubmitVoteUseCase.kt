package com.example.elsuarku.domain.usecase

import com.example.elsuarku.data.model.Candidate
import com.example.elsuarku.data.model.Election
import com.example.elsuarku.data.model.Vote
import com.example.elsuarku.domain.error.AppError

/**
 * Orchestrates a secure vote submission with two-phase commit semantics.
 *
 * Phase 1 — Atomic write: Store the encrypted vote document in Firestore
 *           using a transaction with deterministic document ID (prevents double-voting).
 * Phase 2 — Counter reconciliation: Increment candidate + election vote counters.
 *           If counters fail, the vote is marked PENDING_RECONCILIATION for a
 *           background reconciliation job.
 *
 * This ensures the vote is never lost, even if counter increments fail due to
 * transient network issues or Firestore unavailability.
 */
data class SubmitVoteParams(
    val userId: String,
    val userName: String?,
    val userRole: String,
    val electionId: String,
    val candidateId: String,
    val voterHash: String,
    val encryptedVoteData: String,
    val hash: String,
    val hmac: String,
    val verificationToken: String
)

sealed class SubmitVoteResult {
    data class Success(val verificationToken: String) : SubmitVoteResult()
    data class Error(val error: AppError) : SubmitVoteResult()
}
