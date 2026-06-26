package com.example.elsuarku.data.model

import kotlinx.serialization.Serializable

/**
 * In-app announcement posted by admin to all users.
 *
 * Stored in Firestore: announcements/{id}
 * Displayed in AnnouncementBanner on user dashboards.
 */
@Serializable
data class Announcement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val priority: String = "NORMAL",          // HIGH / NORMAL / LOW
    val isActive: Boolean = true,
    val expiresAt: Long = 0,                   // epoch millis, 0 = never
    val createdBy: String = "",                // admin userId
    val createdAt: Long = System.currentTimeMillis()
)

/** In-memory priority enum matching Firestore string values */
enum class AnnouncementPriority(val firestoreValue: String) {
    HIGH("HIGH"),
    NORMAL("NORMAL"),
    LOW("LOW");

    companion object {
        fun fromString(value: String): AnnouncementPriority =
            entries.find { it.firestoreValue == value } ?: NORMAL
    }
}
