package com.example.elsuarku

import android.app.Application
import android.os.StrictMode
import android.util.Log
import com.example.elsuarku.utils.Constants
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * ElSuarKu Application class.
 *
 * Initializes Firebase, App Check (Play Integrity), and Firestore persistence
 * before any component accesses Firebase services.
 */
class ElSuarKuApp : Application() {

    companion object {
        lateinit var instance: ElSuarKuApp
            private set
        private const val TAG = "ElSuarKuApp"

        @Volatile
        var firebaseInitialized: Boolean = false
            private set

        @Volatile
        var appCheckInitialized: Boolean = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // ── LeakCanary (debug only) ──
        if (com.example.elsuarku.BuildConfig.DEBUG) {
            try {
                leakcanary.LeakCanary.config = leakcanary.LeakCanary.config.copy(
                    dumpHeap = true,
                    retainedVisibleThreshold = 5
                )
                Log.i(TAG, "LeakCanary initialized")
            } catch (e: Exception) {
                Log.w(TAG, "LeakCanary not available", e)
            }
        }

        // ── StrictMode (debug only) — detect disk/network on main thread ──
        if (com.example.elsuarku.BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .detectUnsafeIntentLaunch()
                    .penaltyLog()
                    .build()
            )
            Log.i(TAG, "StrictMode enabled")
        }

        // ── Crashlytics + Performance Monitoring ──
        try {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            // Set custom keys for all crash reports
            FirebaseCrashlytics.getInstance().setCustomKey("app_version", BuildConfig.VERSION_NAME)
            Log.i(TAG, "Crashlytics initialized")
        } catch (e: Exception) {
            Log.w(TAG, "Crashlytics initialization failed: ${e.message}")
        }

        // Performance Monitoring auto-initializes via Firebase BoM content provider
        // Collection enabled by default via manifest meta-data

        // ── Firebase Core ──
        try {
            FirebaseApp.initializeApp(this)
            Log.i(TAG, "Firebase initialized successfully")
            firebaseInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed: ${e.message}", e)
            return
        }

        // ── App Check (Play Integrity) ──
        // Without App Check, the Firestore rules may reject ALL requests with
        // PERMISSION_DENIED if App Check enforcement is enabled in the Firebase Console.
        initializeAppCheck()

        // ── Firestore Settings ──
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                        .build()
                )
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
            Log.i(TAG, "Firestore configured with persistent cache")
        } catch (e: Exception) {
            Log.e(TAG, "Firestore configuration failed: ${e.message}", e)
        }
    }

    /**
     * Initialize Firebase App Check with Play Integrity provider.
     *
     * Play Integrity is the recommended provider for Android apps using
     * Google Play Services. In debug builds, a debug token from the Firebase
     * Console can be configured via Constants.APP_CHECK_DEBUG_TOKEN.
     *
     * If App Check is enforced in the Firebase Console, skipping this initialization
     * causes EVERY Firestore request to fail with PERMISSION_DENIED.
     */
    private fun initializeAppCheck() {
        try {
            val appCheck = FirebaseAppCheck.getInstance()

            if (com.example.elsuarku.BuildConfig.DEBUG || android.os.Build.FINGERPRINT.contains("generic")) {
                // Debug / emulator: use debug provider with token from Firebase Console
                val debugToken = Constants.APP_CHECK_DEBUG_TOKEN
                if (debugToken.isNotBlank()) {
                    appCheck.installAppCheckProviderFactory(
                        com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
                    )
                    Log.i(TAG, "App Check initialized — DEBUG mode (token configured)")
                } else {
                    // No debug token configured — use Play Integrity anyway for dev
                    // (will use default Play Integrity behavior)
                    appCheck.installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance()
                    )
                    Log.i(TAG, "App Check initialized — Play Integrity (debug build, no debug token)")
                }
            } else {
                // Release build: use Play Integrity
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
                Log.i(TAG, "App Check initialized — Play Integrity (release)")
            }

            appCheckInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "App Check initialization failed: ${e.message}", e)
            // Try debug fallback — useful for emulator/testing
            try {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
                )
                appCheckInitialized = true
                Log.w(TAG, "App Check fallback: using debug provider")
            } catch (e2: Exception) {
                Log.e(TAG, "App Check fallback also failed: ${e2.message}", e2)
            }
        }
    }
}
