package com.example.elsuarku.data.repository

import android.util.Log
import com.example.elsuarku.data.model.ReconciliationStatus
import com.example.elsuarku.data.model.Vote
import com.example.elsuarku.domain.repository.IVoteRepository
import com.example.elsuarku.security.IntegrityVerifier
import com.example.elsuarku.utils.Constants
import com.example.elsuarku.utils.Resource
import com.example.elsuarku.utils.toFirestoreErrorMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Manages Vote operations on Firestore with cryptographic integrity.
 *
 * Security architecture:
 *  - Document ID = "vote_{electionId}_{voterHash}" → Firestore atomic uniqueness
 *  - All writes use runTransaction() → TOCTOU-proof (no double-voting race condition)
 *  - voterHash = SHA256("voter:userId:electionId") → anonymous, irreversible
 *  - userId + candidateId + timestamp encrypted inside encryptedVoteData (AES-256-GCM)
 *  - HMAC-SHA256 signature stored for tamper detection
 *
 * Rules enforced:
 *  - One Vote per voter per election (atomic, Firestore-transaction-guaranteed)
 *  - Vote cannot be modified after creation
 *  - Vote cannot be deleted
 */
class VoteRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IVoteRepository {

    private val collection = firestore.collection(Constants.COLLECTION_VOTES)

    companion object {
        private const val TAG = "VoteRepository"
    }

    /**
     * Submit a vote atomically using Firestore transaction.
     *
     * Uses deterministic document ID ("vote_{electionId}_{voterHash}") so that
     * Firestore's own uniqueness constraint + transaction read-then-write guarantee
     * exactly-once semantics — no TOCTOU double-voting is possible.
     */
    override suspend fun submitVote(vote: Vote): Resource<Vote> {
        val docId = "vote_${vote.electionId}_${vote.voterHash}"

        return try {
            val docRef = collection.document(docId)

            val result = firestore.runTransaction<Vote> { transaction ->
                // Atomic read: check if this voter already has a vote document
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    Log.w(TAG, "Transaction ABORTED: voter already voted — doc=$docId")
                    throw FirebaseFirestoreException(
                        "Voter already cast a vote in this election",
                        FirebaseFirestoreException.Code.ABORTED
                    )
                }

                val newVote = vote.copy(id = docRef.id, timestamp = System.currentTimeMillis())
                transaction.set(docRef, newVote)
                Log.i(TAG, "Transaction COMMITTED: vote stored — doc=$docId")
                newVote
            }.await()

            Resource.success(result)
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.ABORTED) {
                Log.w(TAG, "Vote rejected: already voted")
                Resource.error("Anda sudah memberikan suara dalam pemilihan ini")
            } else {
                Log.e(TAG, "Firestore transaction failed", e)
                Resource.error(e.localizedMessage ?: "Gagal mengirim suara", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vote submission failed", e)
            Resource.error(e.localizedMessage ?: "Gagal mengirim suara", e)
        }
    }

    /**
     * Check if a voter has already voted in a specific election.
     * Uses voterHash (SHA256 of userId:electionId) — anonymous, no PII exposed.
     */
    override suspend fun checkUserHasVoted(voterHash: String, electionId: String): Resource<Boolean> {
        return try {
            val docId = "vote_${electionId}_${voterHash}"
            val snapshot = collection.document(docId).get().await()
            Resource.success(snapshot.exists())
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memeriksa status voting", e)
        }
    }

    /**
     * Get a user's vote for a specific election (for receipt/verification).
     * Uses voterHash — anonymous, deterministic lookup.
     */
    override suspend fun getUserVoteForElection(voterHash: String, electionId: String): Resource<Vote?> {
        return try {
            val docId = "vote_${electionId}_${voterHash}"
            val snapshot = collection.document(docId).get().await()
            val vote = snapshot.toObject(Vote::class.java)
            Resource.success(vote)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memuat data suara", e)
        }
    }

    /**
     * Get vote count for an election (for statistics).
     * Election ID is public metadata, not PII.
     */
    override suspend fun getVoteCountForElection(electionId: String): Resource<Int> {
        return try {
            val snapshot = collection
                .whereEqualTo("electionId", electionId)
                .get()
                .await()
            Resource.success(snapshot.size())
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal menghitung suara", e)
        }
    }

    /**
     * Get all votes for an election (Monitor only, for audit).
     * Note: Vote contents are encrypted — only metadata (electionId, timestamp) is visible.
     */
    override suspend fun getVotesForElection(electionId: String): Resource<List<Vote>> {
        return try {
            val snapshot = collection
                .whereEqualTo("electionId", electionId)
                .get()
                .await()
            Resource.success(snapshot.toObjects(Vote::class.java))
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memuat data suara", e)
        }
    }

    /**
     * Check total votes cast across all elections (for admin/monitor stats).
     */
    override suspend fun getTotalVotesCount(): Resource<Int> {
        return try {
            val snapshot = collection.get().await()
            Resource.success(snapshot.size())
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal menghitung total suara", e)
        }
    }

    /**
     * Real-time observer: track which elections a user has voted in.
     *
     * Pre-computes voterHash for each election ID, then listens to the votes
     * collection filtered by those election IDs. Matching is done via voterHash
     * (anonymous, no plaintext userId in queries).
     *
     * Firestore whereIn supports up to 10 values; for >10 elections we fall back
     * to a full-collection listener with Kotlin-side filtering.
     *
     * @param userId The current user's UID
     * @param electionIds List of election IDs to monitor
     * @return Flow emitting the set of election IDs the user has voted in
     */
    override fun observeUserVoteStatus(userId: String, electionIds: List<String>): Flow<Resource<Set<String>>> = callbackFlow {
        if (electionIds.isEmpty()) {
            trySend(Resource.success(emptySet()))
            awaitClose {}
            return@callbackFlow
        }

        // Pre-compute anonymous voter hashes for all monitored elections
        val hashToElectionId = electionIds.associate { eid ->
            IntegrityVerifier.computeVoterHash(userId, eid) to eid
        }
        val voterHashes = hashToElectionId.keys

        // Helper: extract voted election IDs from a snapshot
        fun extractVoted(snapshot: com.google.firebase.firestore.QuerySnapshot?): Set<String> {
            val votes = snapshot?.toObjects(Vote::class.java) ?: emptyList()
            return votes
                .filter { it.voterHash in voterHashes }
                .mapNotNull { v -> hashToElectionId[v.voterHash] }
                .toSet()
        }

        val listener = if (electionIds.size <= 10) {
            // Optimized: filter server-side with whereIn
            collection
                .whereIn("electionId", electionIds)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Resource.error(error.toFirestoreErrorMessage(), error))
                        return@addSnapshotListener
                    }
                    trySend(Resource.success(extractVoted(snapshot)))
                }
        } else {
            // Fallback: full-collection listener, filter in Kotlin
            Log.w(TAG, "observeUserVoteStatus: ${electionIds.size} elections — using full-collection fallback")
            collection
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Resource.error(error.toFirestoreErrorMessage(), error))
                        return@addSnapshotListener
                    }
                    trySend(Resource.success(extractVoted(snapshot)))
                }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Update the reconciliation status of a vote document.
     *
     * Called after two-phase commit completes to mark vote as CONFIRMED,
     * or by the reconciliation job to mark fixed votes as RECONCILED.
     */
    override suspend fun updateReconciliationStatus(voterHash: String, electionId: String, status: ReconciliationStatus): Resource<Unit> {
        val docId = "vote_${electionId}_${voterHash}"
        return try {
            collection.document(docId)
                .update("reconciliationStatus", status.name)
                .await()
            Log.d(TAG, "Reconciliation status updated: $docId → $status")
            Resource.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update reconciliation status for $docId", e)
            Resource.error(e.localizedMessage ?: "Gagal memperbarui status rekonsiliasi", e)
        }
    }

    /**
     * Retrieve all votes for an election with a specific reconciliation status.
     * Used by the reconciliation job to find pending votes that need counter fixes.
     */
    override suspend fun getVotesWithStatus(electionId: String, status: ReconciliationStatus): Resource<List<Vote>> {
        return try {
            val snapshot = collection
                .whereEqualTo("electionId", electionId)
                .whereEqualTo("reconciliationStatus", status.name)
                .get()
                .await()
            Resource.success(snapshot.toObjects(Vote::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get votes with status $status for election $electionId", e)
            Resource.error(e.localizedMessage ?: "Gagal memuat data suara", e)
        }
    }

    /**
     * Real-time observer: watch a single vote document for a specific (user, election) pair.
     * Useful for the vote confirmation flow — detects when the vote is persisted.
     *
     * @param voterHash SHA256("voter:$userId:$electionId")
     * @param electionId The election being voted on
     * @return Flow emitting the Vote document if it exists, null otherwise
     */
    override fun observeUserVoteForElection(voterHash: String, electionId: String): Flow<Resource<Vote?>> = callbackFlow {
        val docId = "vote_${electionId}_${voterHash}"
        val listener = collection.document(docId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.error(error.localizedMessage ?: "Gagal memantau status voting", error))
                    return@addSnapshotListener
                }
                val vote = if (snapshot?.exists() == true) snapshot.toObject(Vote::class.java) else null
                trySend(Resource.success(vote))
            }
        awaitClose { listener.remove() }
    }
}
