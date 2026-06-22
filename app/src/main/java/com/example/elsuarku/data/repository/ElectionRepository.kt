package com.example.elsuarku.data.repository

import com.example.elsuarku.data.model.Election
import com.example.elsuarku.data.model.ElectionStatus
import com.example.elsuarku.utils.Constants
import com.example.elsuarku.utils.Resource
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
) {

    private val collection = firestore.collection(Constants.COLLECTION_ELECTIONS)

    /**
     * Create a new election (Admin only).
     */
    suspend fun createElection(election: Election): Resource<Election> {
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
    suspend fun getElection(id: String): Resource<Election> {
        return try {
            val snapshot = collection.document(id).get().await()
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
    fun observeActiveElections(): Flow<Resource<List<Election>>> = callbackFlow {
        val listener = collection
            .orderBy("startDate")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.error(error.localizedMessage ?: "Gagal memuat pemilihan", error))
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
    fun observeAllElections(): Flow<Resource<List<Election>>> = callbackFlow {
        val listener = collection
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.error(error.localizedMessage ?: "Gagal memuat pemilihan", error))
                    return@addSnapshotListener
                }
                val elections = snapshot?.toObjects(Election::class.java) ?: emptyList()
                trySend(Resource.success(elections))
            }
        awaitClose { listener.remove() }
    }

    /**
     * Get all active elections as a one-shot query.
     * No composite index needed — filter ACTIVE in code.
     */
    suspend fun getActiveElections(): Resource<List<Election>> {
        return try {
            val snapshot = collection
                .orderBy("startDate")
                .get()
                .await()
            val elections = snapshot.toObjects(Election::class.java)
                .filter { it.status == ElectionStatus.ACTIVE }
            Resource.success(elections)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memuat pemilihan", e)
        }
    }

    /**
     * Get ALL elections regardless of status (Admin view).
     */
    suspend fun getAllElections(): Resource<List<Election>> {
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
     */
    suspend fun updateElection(election: Election): Resource<Unit> {
        return try {
            collection.document(election.id)
                .set(election.copy(updatedAt = System.currentTimeMillis()))
                .await()
            Resource.success(Unit)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memperbarui pemilihan", e)
        }
    }

    /**
     * Update election status.
     */
    suspend fun updateElectionStatus(id: String, status: ElectionStatus): Resource<Unit> {
        return try {
            collection.document(id).update(
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
    suspend fun deleteElection(id: String): Resource<Unit> {
        return try {
            collection.document(id).delete().await()
            Resource.success(Unit)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal menghapus pemilihan", e)
        }
    }

    /**
     * Increment the voted count for an election.
     */
    suspend fun incrementVotedCount(electionId: String): Resource<Unit> {
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
