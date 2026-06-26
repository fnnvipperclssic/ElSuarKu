package com.example.elsuarku.data.repository

import com.example.elsuarku.data.model.Election
import com.example.elsuarku.data.model.ElectionStatus
import com.example.elsuarku.domain.repository.IElectionRepository
import com.example.elsuarku.utils.Constants
import com.example.elsuarku.utils.Resource
import com.example.elsuarku.utils.toFirestoreErrorMessage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Manages Election CRUD operations on Firestore.
 */
class ElectionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IElectionRepository {

    private val collection = firestore.collection(Constants.COLLECTION_ELECTIONS)

    /**
     * Create a new election (Admin only).
     */
    override suspend fun createElection(election: Election): Resource<Election> {
        return try {
            val docRef = if (election.id.isBlank()) collection.document() else collection.document(election.id)
            val newElection = election.copy(id = docRef.id)
            docRef.set(newElection).await()
            Resource.success(newElection)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal membuat pemilihan", e)
        }
    }

    /**
     * Get a single election by ID.
     */
    override suspend fun getElection(electionId: String): Resource<Election> {
        return try {
            val snapshot = collection.document(electionId).get().await()
            val election = snapshot.toObject(Election::class.java)
            if (election != null) Resource.success(election)
            else Resource.error("Pemilihan tidak ditemukan")
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memuat pemilihan", e)
        }
    }

    /**
     * Stream active elections in real-time.
     * No composite index needed — filter ACTIVE in code.
     */
    override fun observeActiveElections(): Flow<Resource<List<Election>>> = callbackFlow {
        val listener = collection
            .orderBy("startDate")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.error(error.toFirestoreErrorMessage(), error))
                    return@addSnapshotListener
                }
                val elections = snapshot?.toObjects(Election::class.java)
                    ?.filter { it.status == ElectionStatus.ACTIVE } ?: emptyList()
                trySend(Resource.success(elections))
            }
        awaitClose { listener.remove() }
    }

    /**
     * Stream all elections (for admin).
     */
    override fun observeAllElections(): Flow<Resource<List<Election>>> = callbackFlow {
        val listener = collection
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.error(error.toFirestoreErrorMessage(), error))
                    return@addSnapshotListener
                }
                val elections = snapshot?.toObjects(Election::class.java) ?: emptyList()
                trySend(Resource.success(elections))
            }
        awaitClose { listener.remove() }
    }

    /**
     * Get all active elections as a one-shot query.
     * Includes both DRAFT and ACTIVE statuses for the monitor dashboard.
     */
    override suspend fun getActiveElections(): Resource<List<Election>> {
        return try {
            val snapshot = collection
                .whereIn("status", listOf(ElectionStatus.DRAFT.name, ElectionStatus.ACTIVE.name))
                .get().await()
            Resource.success(snapshot.toObjects(Election::class.java))
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal mengambil pemilihan aktif", e)
        }
    }

    /**
     * Get ALL elections regardless of status (Admin view).
     */
    override suspend fun getAllElections(): Resource<List<Election>> {
        return try {
            val snapshot = collection
                .orderBy("startDate")
                .get()
                .await()
            val elections = snapshot.toObjects(Election::class.java)
            Resource.success(elections)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memuat pemilihan", e)
        }
    }

    /**
     * Update an election (Admin only).
     * Uses partial update() — preserves votedCount, totalVoters, createdAt,
     * and other fields that must never be reset by an edit operation.
     */
    override suspend fun updateElection(election: Election): Resource<Election> {
        return try {
            val updates = mapOf(
                "title" to election.title,
                "description" to election.description,
                "startDate" to election.startDate,
                "endDate" to election.endDate,
                "status" to election.status.name,
                "updatedAt" to System.currentTimeMillis()
            )
            collection.document(election.id).update(updates).await()
            Resource.success(election)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memperbarui pemilihan", e)
        }
    }

    /**
     * Update election status.
     */
    override suspend fun updateElectionStatus(electionId: String, status: ElectionStatus): Resource<Unit> {
        return try {
            collection.document(electionId).update(
                mapOf(
                    "status" to status.name,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Resource.success(Unit)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal mengubah status", e)
        }
    }

    /**
     * Delete an election (Admin only).
     */
    override suspend fun deleteElection(electionId: String): Resource<Unit> {
        return try {
            collection.document(electionId).delete().await()
            Resource.success(Unit)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal menghapus pemilihan", e)
        }
    }

    /**
     * Get total election count across all statuses.
     */
    override suspend fun getElectionCount(): Resource<Int> {
        return try {
            val snapshot = collection.get().await()
            Resource.success(snapshot.size())
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal menghitung pemilihan", e)
        }
    }

    /**
     * Increment the voted count for an election.
     */
    override suspend fun incrementVotedCount(electionId: String): Resource<Unit> {
        return try {
            collection.document(electionId)
                .update("votedCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            Resource.success(Unit)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memperbarui hitungan", e)
        }
    }
}
