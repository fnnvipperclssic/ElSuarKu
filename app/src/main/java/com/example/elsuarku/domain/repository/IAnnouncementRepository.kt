package com.example.elsuarku.domain.repository

import com.example.elsuarku.data.model.Announcement
import com.example.elsuarku.utils.Resource
import kotlinx.coroutines.flow.Flow

interface IAnnouncementRepository {
    suspend fun createAnnouncement(announcement: Announcement): Resource<Announcement>
    suspend fun updateAnnouncement(announcement: Announcement): Resource<Announcement>
    suspend fun deleteAnnouncement(announcementId: String): Resource<Unit>
    suspend fun getActiveAnnouncements(): Resource<List<Announcement>>
    suspend fun getAllAnnouncements(): Resource<List<Announcement>>
    fun observeActiveAnnouncements(): Flow<Resource<List<Announcement>>>
}
