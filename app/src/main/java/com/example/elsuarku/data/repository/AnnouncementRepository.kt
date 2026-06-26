package com.example.elsuarku.data.repository

import com.example.elsuarku.data.model.Announcement
import com.example.elsuarku.domain.repository.IAnnouncementRepository
import com.example.elsuarku.utils.Constants
import com.example.elsuarku.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AnnouncementRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IAnnouncementRepository {

    private val collection = firestore.collection(Constants.COLLECTION_ANNOUNCEMENTS)

    override suspend fun createAnnouncement(announcement: Announcement): Resource<Announcement> {
        return try {
            val docRef = if (announcement.id.isBlank()) collection.document() else collection.document(announcement.id)
            val newAnnouncement = announcement.copy(id = docRef.id)
            docRef.set(newAnnouncement).await()
            Resource.success(newAnnouncement)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal membuat pengumuman", e)
        }
    }

    override suspend fun updateAnnouncement(announcement: Announcement): Resource<Announcement> {
        return try {
            collection.document(announcement.id).set(announcement).await()
            Resource.success(announcement)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memperbarui pengumuman", e)
        }
    }

    override suspend fun deleteAnnouncement(announcementId: String): Resource<Unit> {
        return try {
            collection.document(announcementId).delete().await()
            Resource.success(Unit)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal menghapus pengumuman", e)
        }
    }

    override suspend fun getActiveAnnouncements(): Resource<List<Announcement>> {
        return try {
            val now = System.currentTimeMillis()
            val snapshot = collection
                .whereEqualTo("isActive", true)
                .get()
                .await()
            val announcements = snapshot.toObjects(Announcement::class.java)
                .filter { it.expiresAt == 0L || it.expiresAt > now }
                .sortedByDescending { it.priority }
            Resource.success(announcements)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memuat pengumuman", e)
        }
    }

    override suspend fun getAllAnnouncements(): Resource<List<Announcement>> {
        return try {
            val snapshot = collection
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            Resource.success(snapshot.toObjects(Announcement::class.java))
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memuat pengumuman", e)
        }
    }

    override fun observeActiveAnnouncements(): Flow<Resource<List<Announcement>>> = callbackFlow {
        val now = System.currentTimeMillis()
        val listener = collection
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.error(error.localizedMessage ?: "Gagal memuat pengumuman", error))
                    return@addSnapshotListener
                }
                val announcements = snapshot?.toObjects(Announcement::class.java)
                    ?.filter { it.expiresAt == 0L || it.expiresAt > now }
                    ?.sortedByDescending { it.priority }
                    ?: emptyList()
                trySend(Resource.success(announcements))
            }
        awaitClose { listener.remove() }
    }
}
