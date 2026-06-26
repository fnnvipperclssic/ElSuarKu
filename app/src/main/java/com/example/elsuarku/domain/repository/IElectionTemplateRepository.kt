package com.example.elsuarku.domain.repository

import com.example.elsuarku.data.model.ElectionTemplate
import com.example.elsuarku.utils.Resource

/**
 * Repository interface for Election Template operations.
 *
 * Templates allow admin to save/load election configurations
 * for recurring elections (annual, semester, etc.).
 */
interface IElectionTemplateRepository {
    suspend fun createTemplate(template: ElectionTemplate): Resource<ElectionTemplate>
    suspend fun updateTemplate(template: ElectionTemplate): Resource<ElectionTemplate>
    suspend fun deleteTemplate(templateId: String): Resource<Unit>
    suspend fun getTemplate(templateId: String): Resource<ElectionTemplate>
    suspend fun getAllTemplates(): Resource<List<ElectionTemplate>>
    suspend fun getTemplateCount(): Resource<Int>

    /** Apply a template to create a new election draft */
    suspend fun applyTemplate(templateId: String, name: String, startDate: Long, endDate: Long): Resource<com.example.elsuarku.data.model.Election>
}
