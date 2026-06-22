package com.example.elsuarku.data.repository

import com.example.elsuarku.data.model.Candidate
import com.example.elsuarku.data.model.CandidateStatus
import com.example.elsuarku.utils.Constants
import com.example.elsuarku.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Manages Candidate CRUD operations on Firestore.
 * Candidate photos are stored as Base64 strings within the document.
 */
class CandidateRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val collection = firestore.collection(Constants.COLLECTION_CANDIDATES)

    /**
     * Create a new candidate (Admin only).
     */
    suspend fun createCandidate(candidate: Candidate): Resource<Candidate> {
        return try {
            val docRef = if (candidate.id.isBlank()) collection.document() else collection.document(candidate.id)
            val newCandidate = candidate.copy(id = docRef.id)
            docRef.set(newCandidate).await()
            Resource.success(newCandidate)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal membuat kandidat", e)
        }
    }

    /**
     * Get candidates for a specific election in real-time.
     * Sorts by nomorUrut in Kotlin to avoid requiring a composite index on Firestore.
     */
    fun observeCandidates(electionId: String): Flow<Resource<List<Candidate>>> = callbackFlow {
        val listener = collection
            .whereEqualTo("electionId", electionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.error(error.localizedMessage ?: "Gagal memuat kandidat", error))
                    return@addSnapshotListener
                }
                val candidates = snapshot?.toObjects(Candidate::class.java)
                    ?.filter { it.status == CandidateStatus.ACTIVE }
                    ?.sortedBy { it.nomorUrut }
                    ?: emptyList()
                trySend(Resource.success(candidates))
            }
        awaitClose { listener.remove() }
    }

    /**
     * Get all candidates for an election (one-shot).
     * Does NOT use orderBy — sorts by nomorUrut in Kotlin to avoid requiring
     * a composite Firestore index on (electionId, nomorUrut).
     */
    suspend fun getCandidates(electionId: String): Resource<List<Candidate>> {
        return try {
            val snapshot = collection
                .whereEqualTo("electionId", electionId)
                .get()
                .await()
            val candidates = snapshot.toObjects(Candidate::class.java)
                .filter { it.status == CandidateStatus.ACTIVE }
                .sortedBy { it.nomorUrut }
            Resource.success(candidates)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memuat kandidat. Periksa koneksi dan izin Firestore.", e)
        }
    }

    /**
     * Update a candidate (Admin only).
     */
    suspend fun updateCandidate(candidate: Candidate): Resource<Unit> {
        return try {
            collection.document(candidate.id).set(candidate).await()
            Resource.success(Unit)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memperbarui kandidat", e)
        }
    }

    /**
     * Delete a candidate (Admin only).
     */
    suspend fun deleteCandidate(id: String): Resource<Unit> {
        return try {
            // Soft delete — mark as inactive
            collection.document(id).update("status", CandidateStatus.INACTIVE.name).await()
            Resource.success(Unit)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal menghapus kandidat", e)
        }
    }

    /**
     * Increment vote count for a candidate.
     * Uses Firestore atomic increment to prevent race conditions.
     */
    suspend fun incrementVoteCount(candidateId: String): Resource<Unit> {
        return try {
            collection.document(candidateId)
                .update("voteCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            Resource.success(Unit)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memperbarui hitungan suara", e)
        }
    }

    /**
     * Get candidate by ID.
     */
    suspend fun getCandidate(id: String): Resource<Candidate> {
        return try {
            val snapshot = collection.document(id).get().await()
            val candidate = snapshot.toObject(Candidate::class.java)
            if (candidate != null) Resource.success(candidate)
            else Resource.error("Kandidat tidak ditemukan")
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memuat kandidat", e)
        }
    }
}
