package com.example.elsuarku.data.model

import kotlinx.serialization.Serializable

/**
 * Election configuration template for quick election creation.
 *
 * Admin can save/load templates to avoid re-entering the same
 * configuration for recurring elections (annual, semester, etc.).
 *
 * Stored in Firestore: electionTemplates/{id}
 */
@Serializable
data class ElectionTemplate(
    val id: String = "",
    val name: String = "",                       // e.g. "Pemilu Tahunan 2026"
    val description: String = "",
    val defaultDurationDays: Int = 7,            // default election duration
    val maxCandidates: Int = 10,                 // max candidates per election
    val isAnonymous: Boolean = true,              // voter anonymity
    val requireBiometric: Boolean = false,        // biometric verification gate
    val allowMonitorAccess: Boolean = true,       // real-time monitor access
    val customFields: List<TemplateField> = emptyList(),
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Additional custom field definition for election templates.
 */
@Serializable
data class TemplateField(
    val key: String,         // e.g. "department", "position"
    val label: String,       // e.g. "Departemen", "Jabatan"
    val type: String = "TEXT", // TEXT / NUMBER / SELECT
    val options: List<String> = emptyList(),  // for SELECT type
    val required: Boolean = false
)
