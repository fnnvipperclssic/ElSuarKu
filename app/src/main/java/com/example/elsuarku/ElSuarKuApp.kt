package com.example.elsuarku

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * ElSuarKu Application class.
 *
 * Explicitly initializes Firebase before any component accesses FirebaseAuth/Firestore.
 * The Google Services plugin ContentProvider also runs, but explicit init ensures ordering.
 */
class ElSuarKuApp : Application() {

    companion object {
        lateinit var instance: ElSuarKuApp
            private set
        private const val TAG = "ElSuarKuApp"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Explicit Firebase initialization — MUST run before any Firebase.getInstance() calls
        try {
            FirebaseApp.initializeApp(this)
            Log.i(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed: ${e.message}", e)
        }

        // Configure Firestore settings
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                        .build()
                )
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
            Log.i(TAG, "Firestore configured")
        } catch (e: Exception) {
            Log.e(TAG, "Firestore configuration failed: ${e.message}", e)
        }
    }
}
