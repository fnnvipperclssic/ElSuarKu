package com.example.elsuarku.data.repository

import com.example.elsuarku.data.model.Election
import com.example.elsuarku.data.model.ElectionStatus
import com.example.elsuarku.data.model.ElectionTemplate
import com.example.elsuarku.domain.repository.IElectionTemplateRepository
import com.example.elsuarku.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ElectionTemplateRepository(
    private val firestore: FirebaseFirestore
) : IElectionTemplateRepository {

    companion object {
        private const val COLLECTION = "electionTemplates"
    }

    override suspend fun createTemplate(template: ElectionTemplate): Resource<ElectionTemplate> {
        return try {
            val docRef = firestore.collection(COLLECTION).document()
            val newTemplate = template.copy(
                id = docRef.id,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            docRef.set(newTemplate).await()
            Resource.Success(newTemplate)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Gagal membuat template")
        }
    }

    override suspend fun updateTemplate(template: ElectionTemplate): Resource<ElectionTemplate> {
        return try {
            val updated = template.copy(updatedAt = System.currentTimeMillis())
            firestore.collection(COLLECTION).document(template.id)
                .set(updated).await()
            Resource.Success(updated)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Gagal mengupdate template")
        }
    }

    override suspend fun deleteTemplate(templateId: String): Resource<Unit> {
        return try {
            firestore.collection(COLLECTION).document(templateId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Gagal menghapus template")
        }
    }

    override suspend fun getTemplate(templateId: String): Resource<ElectionTemplate> {
        return try {
            val doc = firestore.collection(COLLECTION).document(templateId).get().await()
            val template = doc.toObject(ElectionTemplate::class.java)
            if (template != null) Resource.Success(template)
            else Resource.Error("Template tidak ditemukan")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Gagal mengambil template")
        }
    }

    override suspend fun getAllTemplates(): Resource<List<ElectionTemplate>> {
        return try {
            val snapshot = firestore.collection(COLLECTION)
                .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            val templates = snapshot.toObjects(ElectionTemplate::class.java)
            Resource.Success(templates)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Gagal mengambil daftar template")
        }
    }

    override suspend fun getTemplateCount(): Resource<Int> {
        return try {
            val snapshot = firestore.collection(COLLECTION).get().await()
            Resource.Success(snapshot.size())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Gagal menghitung template")
        }
    }

    override suspend fun applyTemplate(
        templateId: String,
        name: String,
        startDate: Long,
        endDate: Long
    ): Resource<Election> {
        return try {
            val tmplResult = getTemplate(templateId)
            if (tmplResult !is Resource.Success) {
                return Resource.Error("Template tidak ditemukan")
            }
            val template = tmplResult.data

            val election = Election(
                id = UUID.randomUUID().toString(),
                title = name,
                description = template.description,
                status = ElectionStatus.DRAFT,
                startDate = startDate,
                endDate = endDate,
                votedCount = 0
            )

            firestore.collection("elections").document(election.id)
                .set(election).await()

            Resource.Success(election)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Gagal menerapkan template")
        }
    }
}
