package com.example.elsuarku.utils

/**
 * Application-wide constants for ElSuarKu.
 */
object Constants {

    // Firestore Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_ELECTIONS = "elections"
    const val COLLECTION_CANDIDATES = "candidates"
    const val COLLECTION_VOTES = "votes"
    const val COLLECTION_AUDIT_LOGS = "audit_logs"

    // SharedPreferences / DataStore keys
    const val PREF_SESSION_TOKEN = "session_token"
    const val PREF_USER_ROLE = "user_role"
    const val PREF_USER_UID = "user_uid"
    const val PREF_USER_NAME = "user_name"
    const val PREF_LAST_LOGIN = "last_login"
    const val PREF_BIOMETRIC_ENABLED = "biometric_enabled"

    // Security
    const val SESSION_TIMEOUT_MINUTES = 30L
    const val MAX_LOGIN_ATTEMPTS = 5
    const val ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding"
    const val KEY_ALIAS = "elsuarku_master_key"

    // Image
    const val MAX_IMAGE_WIDTH = 512
    const val MAX_IMAGE_HEIGHT = 512
    const val JPEG_QUALITY = 75

    // App Check
    const val APP_CHECK_DEBUG_TOKEN = "" // Fill from Firebase Console for debug builds

    // Defaults
    const val DEFAULT_ELECTION_DURATION_DAYS = 7L
}
