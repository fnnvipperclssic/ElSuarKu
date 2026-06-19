package com.example.elsuarku.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.elsuarku.utils.Constants
import java.io.ByteArrayOutputStream

/**
 * Free alternative to Firebase Storage.
 *
 * Stores images as compressed Base64 strings directly in Firestore documents.
 * Suitable for profile photos and candidate images (< 1MB after compression).
 *
 * Firestore document limit: 1 MiB (1,048,576 bytes).
 * Our compressed images target ~50-200KB, well within limits.
 *
 * If Cloudinary or another free service becomes preferred in the future,
 * swap this implementation — the interface stays the same.
 */
class ImageStorage(private val context: Context) {

    /**
     * Convert an image URI to a compressed Base64 string ready for Firestore storage.
     *
     * @param uri Local image URI
     * @return Base64-encoded JPEG string, or null on failure
     */
    fun uriToBase64(uri: Uri): String? {
        return try {
            // First, decode only bounds to check image size
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            // Calculate sample size to avoid OOM on large images
            val sampleSize = calculateInSampleSize(options.outWidth, options.outHeight, Constants.MAX_IMAGE_WIDTH, Constants.MAX_IMAGE_HEIGHT)

            // Decode with sampling
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            bitmap?.let { bitmapToBase64(it) }
        } catch (e: OutOfMemoryError) {
            System.gc()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Calculate optimal sample size for image decoding.
     */
    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Convert a Bitmap to a compressed Base64 string.
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        // Resize to max dimensions while maintaining aspect ratio
        val resized = resizeBitmap(bitmap, Constants.MAX_IMAGE_WIDTH, Constants.MAX_IMAGE_HEIGHT)

        val outputStream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, Constants.JPEG_QUALITY, outputStream)
        val bytes = outputStream.toByteArray()

        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Decode a Base64 string back to a Bitmap for display.
     */
    fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Check if a Base64 image string fits within Firestore's 1 MiB document limit.
     */
    fun isWithinSizeLimit(base64: String): Boolean {
        // Base64 is ~4/3 the size of raw bytes; check against ~900KB to be safe
        return base64.length < 900_000
    }

    /**
     * Resize bitmap maintaining aspect ratio.
     */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) return bitmap

        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
