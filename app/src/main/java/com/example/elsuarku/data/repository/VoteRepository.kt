package com.example.elsuarku.data.repository

import android.util.Log
import com.example.elsuarku.data.model.Vote
import com.example.elsuarku.utils.Constants
import com.example.elsuarku.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
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
) {

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
    suspend fun submitVote(vote: Vote): Resource<Vote> {
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
    suspend fun checkUserHasVoted(voterHash: String, electionId: String): Resource<Boolean> {
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
    suspend fun getUserVoteForElection(voterHash: String, electionId: String): Resource<Vote?> {
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
    suspend fun getVoteCountForElection(electionId: String): Resource<Int> {
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
    suspend fun getVotesForElection(electionId: String): Resource<List<Vote>> {
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
    suspend fun getTotalVotesCount(): Resource<Int> {
        return try {
            val snapshot = collection.get().await()
            Resource.success(snapshot.size())
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal menghitung total suara", e)
        }
    }
}
