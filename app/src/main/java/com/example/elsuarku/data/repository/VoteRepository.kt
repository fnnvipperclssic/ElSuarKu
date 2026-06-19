package com.example.elsuarku.data.repository

import com.example.elsuarku.data.model.Vote
import com.example.elsuarku.utils.Constants
import com.example.elsuarku.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Manages Vote operations on Firestore.
 * All votes are encrypted before storage and include integrity hashes.
 *
 * Rules enforced:
 * - One User = One Vote per election
 * - Vote Cannot Be Modified
 * - Vote Cannot Be Deleted
 */
class VoteRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val collection = firestore.collection(Constants.COLLECTION_VOTES)

    /**
     * Submit a vote. Fails if user has already voted in this election.
     */
    suspend fun submitVote(vote: Vote): Resource<Vote> {
        return try {
            // Check if user already voted in this election
            val existing = checkUserHasVoted(vote.userId, vote.electionId)
            if (existing is Resource.Success && existing.data) {
                return Resource.error("Anda sudah memberikan suara dalam pemilihan ini")
            }

            val docRef = collection.document()
            val newVote = vote.copy(id = docRef.id, timestamp = System.currentTimeMillis())
            docRef.set(newVote).await()
            Resource.success(newVote)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal mengirim suara", e)
        }
    }

    /**
     * Check if a user has already voted in a specific election.
     */
    suspend fun checkUserHasVoted(userId: String, electionId: String): Resource<Boolean> {
        return try {
            val snapshot = collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("electionId", electionId)
                .limit(1)
                .get()
                .await()
            Resource.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memeriksa status voting", e)
        }
    }

    /**
     * Get a user's vote for a specific election (for receipt/verification).
     */
    suspend fun getUserVoteForElection(userId: String, electionId: String): Resource<Vote?> {
        return try {
            val snapshot = collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("electionId", electionId)
                .limit(1)
                .get()
                .await()
            val vote = snapshot.documents.firstOrNull()?.toObject(Vote::class.java)
            Resource.success(vote)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memuat data suara", e)
        }
    }

    /**
     * Get vote count for an election (for statistics).
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
     * Note: Vote contents are encrypted.
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
