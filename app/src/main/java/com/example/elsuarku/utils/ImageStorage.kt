package com.example.elsuarku.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Firebase Storage manager for photos — replaces Base64 photo storage in Firestore.
 *
 * ## Why Storage instead of Firestore Base64
 * | Aspect          | Firestore Base64     | Firebase Storage       |
 * |-----------------|---------------------|------------------------|
 * | Doc size limit  | 1 MiB (Base64 bloats)| Unlimited per file     |
 * | Cost model      | Read/write per doc   | Download bandwidth     |
 * | Cache           | Manual               | Native CDN + disk      |
 * | Resize          | Manual               | Cloud Functions resize |
 * | Migration       | N/A                  | Auto-migrate from B64  |
 *
 * ## Usage
 * ```kotlin
 * val url = ImageStorage.uploadUserPhoto(context, userId, bitmap)
 * val url = ImageStorage.uploadCandidatePhoto(context, electionId, candidateId, bitmap)
 * ```
 */
class ImageStorage(context: Context) {

    private val storage = FirebaseStorage.getInstance()

    companion object {
        private const val TAG = "ImageStorage"

        // Max dimensions for uploaded images (server-side resizing not available
        // without Cloud Functions extension; we resize client-side)
        private const val MAX_PHOTO_WIDTH = 512
        private const val MAX_PHOTO_HEIGHT = 512
        private const val JPEG_QUALITY = 75
    }

    /**
     * Upload a user profile photo to Firebase Storage.
     *
     * Path: `users/{userId}/profile.jpg`
     * Returns the download URL (HTTPS) on success.
     */
    suspend fun uploadUserPhoto(userId: String, bitmap: Bitmap): String? {
        val ref = storage.reference.child("users/$userId/profile.jpg")
        return uploadPhoto(ref, bitmap)
    }

    /**
     * Upload a candidate photo to Firebase Storage.
     *
     * Path: `candidates/{electionId}/{candidateId}.jpg`
     * Returns the download URL (HTTPS) on success.
     */
    suspend fun uploadCandidatePhoto(electionId: String, candidateId: String, bitmap: Bitmap): String? {
        val ref = storage.reference.child("candidates/$electionId/$candidateId.jpg")
        return uploadPhoto(ref, bitmap)
    }

    /**
     * Get the download URL for a user's profile photo.
     * Returns null if no photo exists.
     */
    suspend fun getUserPhotoUrl(userId: String): String? {
        return try {
            val ref = storage.reference.child("users/$userId/profile.jpg")
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.d(TAG, "No photo for user $userId: ${e.message}")
            null
        }
    }

    /**
     * Get the download URL for a candidate photo.
     */
    suspend fun getCandidatePhotoUrl(electionId: String, candidateId: String): String? {
        return try {
            val ref = storage.reference.child("candidates/$electionId/$candidateId.jpg")
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.d(TAG, "No photo for candidate $candidateId: ${e.message}")
            null
        }
    }

    /**
     * Delete a user's profile photo from Storage.
     */
    suspend fun deleteUserPhoto(userId: String) {
        try {
            storage.reference.child("users/$userId/profile.jpg").delete().await()
            Log.d(TAG, "Deleted photo for user $userId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete photo for user $userId: ${e.message}")
        }
    }

    /**
     * Migrate a Base64 photo to Firebase Storage.
     *
     * Decodes the Base64 string to a Bitmap, resizes it, and uploads to Storage.
     * Returns the new download URL, or null if migration fails.
     *
     * After migration, the Base64 field in Firestore should be cleared to save space.
     */
    suspend fun migrateFromBase64(
        base64Data: String,
        storagePath: String
    ): String? {
        if (base64Data.isBlank()) return null

        return withContext(Dispatchers.IO) {
            try {
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: return@withContext null

                val resized = resizeIfNeeded(bitmap)
                val jpegBytes = compressToJpeg(resized)
                val ref = storage.reference.child(storagePath)

                val metadata = StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .setCacheControl("public, max-age=86400") // 24h CDN cache
                    .build()

                ref.putBytes(jpegBytes, metadata).await()
                val url = ref.downloadUrl.await().toString()

                // Clean up
                bitmap.recycle()
                if (resized != bitmap) resized.recycle()

                Log.i(TAG, "Base64 → Storage migrated: $storagePath (${jpegBytes.size} bytes)")
                url
            } catch (e: Exception) {
                Log.e(TAG, "Base64 migration failed for $storagePath: ${e.message}", e)
                null
            }
        }
    }

    // ── Private helpers ──

    private suspend fun uploadPhoto(ref: com.google.firebase.storage.StorageReference, bitmap: Bitmap): String? {
        return withContext(Dispatchers.IO) {
            try {
                val resized = resizeIfNeeded(bitmap)
                val jpegBytes = compressToJpeg(resized)

                val metadata = StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .setCacheControl("public, max-age=86400")
                    .build()

                ref.putBytes(jpegBytes, metadata).await()
                val url = ref.downloadUrl.await().toString()

                // Clean up
                bitmap.recycle()
                if (resized != bitmap) resized.recycle()

                Log.i(TAG, "Photo uploaded: ${ref.path} (${jpegBytes.size} bytes)")
                url
            } catch (e: Exception) {
                Log.e(TAG, "Photo upload failed for ${ref.path}: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Resize bitmap to max dimensions if needed.
     * Prevents storing unnecessarily large images in Storage.
     */
    private fun resizeIfNeeded(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= MAX_PHOTO_WIDTH && height <= MAX_PHOTO_HEIGHT) {
            return bitmap
        }

        val ratio = minOf(
            MAX_PHOTO_WIDTH.toFloat() / width,
            MAX_PHOTO_HEIGHT.toFloat() / height
        )
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Compress bitmap to JPEG bytes.
     */
    private fun compressToJpeg(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        return stream.toByteArray()
    }
}
