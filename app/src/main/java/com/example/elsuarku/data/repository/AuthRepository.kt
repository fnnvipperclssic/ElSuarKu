package com.example.elsuarku.data.repository

import android.util.Log
import com.example.elsuarku.data.model.User
import com.example.elsuarku.data.model.UserRole
import com.example.elsuarku.data.model.UserStatus
import com.example.elsuarku.domain.repository.IAuthRepository
import com.example.elsuarku.utils.Constants
import com.example.elsuarku.utils.Resource
import com.example.elsuarku.utils.toFirestoreErrorMessage
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Handles all authentication operations: Google Sign-In, Email/Password, session management.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IAuthRepository {

    /**
     * Login with email and password. Returns user UID on success.
     */
    override suspend fun login(email: String, password: String): Resource<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Resource.error("Gagal masuk")
            // Update last login
            firestore.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .update("lastLogin", System.currentTimeMillis())
            Resource.success(uid)
        } catch (e: Exception) {
            Resource.error(mapFirebaseAuthError(e), e)
        }
    }

    /**
     * Register with email and password. Returns user UID on success.
     */
    override suspend fun register(email: String, password: String, name: String, role: String): Resource<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Resource.error("Gagal membuat akun")
            val userRole = try { UserRole.valueOf(role) } catch (_: Exception) { UserRole.PEMILIH }
            val user = User(
                uid = uid, name = name, email = email,
                role = userRole, status = UserStatus.ACTIVE,
                lastLogin = System.currentTimeMillis(), createdAt = System.currentTimeMillis()
            )
            firestore.collection(Constants.COLLECTION_USERS).document(uid).set(user).await()
            Resource.success(uid)
        } catch (e: Exception) {
            Resource.error(mapFirebaseAuthError(e), e)
        }
    }

    override suspend fun signOut(): Resource<Unit> {
        auth.signOut()
        return Resource.success(Unit)
    }

    /**
     * Register this device fingerprint in the user's document.
     * Used for concurrent session detection — if multiple distinct fingerprints
     * are active simultaneously, the account may be compromised.
     *
     * Stores in a sub-map `deviceFingerprints/{fingerprint}: { lastSeen: timestamp }`
     * to track all devices that have logged into this account.
     */
    override suspend fun registerDeviceFingerprint(userId: String, fingerprint: String): Resource<Unit> {
        return try {
            val userRef = firestore.collection(Constants.COLLECTION_USERS).document(userId)
            userRef.update(
                "deviceFingerprints.${fingerprint}",
                mapOf("lastSeen" to System.currentTimeMillis())
            ).await()
            Log.d(TAG, "Device fingerprint registered for user $userId")
            Resource.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register device fingerprint", e)
            Resource.error(e.localizedMessage ?: "Gagal registrasi perangkat", e)
        }
    }

    /**
     * Check for concurrent sessions on different devices.
     * Returns a list of other device fingerprints active in the last 30 minutes.
     * If this returns non-empty, the user may have multiple concurrent sessions.
     */
    override suspend fun checkConcurrentSessions(userId: String, fingerprint: String): Resource<List<String>> {
        return try {
            val userDoc = firestore.collection(Constants.COLLECTION_USERS)
                .document(userId).get().await()
            val fingerprints = userDoc.get("deviceFingerprints") as? Map<String, Map<String, Any>>
            if (fingerprints == null) {
                return Resource.success(emptyList())
            }
            val threshold = System.currentTimeMillis() - 30 * 60 * 1000L // 30 minutes
            val active = fingerprints.filterKeys { it != fingerprint }
                .filter { (_, data) ->
                    val lastSeen = (data["lastSeen"] as? Long) ?: 0L
                    lastSeen > threshold
                }
                .keys.toList()
            if (active.isNotEmpty()) {
                Log.w(TAG, "Concurrent sessions detected: ${active.size} other device(s)")
            }
            Resource.success(active)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check concurrent sessions", e)
            Resource.error(e.localizedMessage ?: "Gagal memeriksa sesi", e)
        }
    }

    companion object {
        private const val TAG = "AuthRepository"

        /**
         * Maps Firebase Auth exceptions to user-friendly Indonesian error messages.
         * Distinguishes GMS/network failures from authentication failures.
         */
        fun mapFirebaseAuthError(e: Exception): String {
            return when {
                // ── Credential / Authentication Errors ──
                e is FirebaseAuthInvalidCredentialsException ||
                e is FirebaseAuthInvalidUserException -> {
                    val code = (e as? FirebaseAuthException)?.errorCode ?: ""
                    when (code) {
                        "ERROR_WRONG_PASSWORD" -> "Kata sandi salah. Silakan coba lagi."
                        "ERROR_USER_NOT_FOUND" -> "Akun tidak ditemukan. Periksa email Anda atau daftar terlebih dahulu."
                        "ERROR_USER_DISABLED" -> "Akun ini telah dinonaktifkan. Hubungi administrator."
                        "ERROR_INVALID_CREDENTIAL" -> "Kredensial tidak valid. Silakan coba lagi."
                        "ERROR_INVALID_EMAIL" -> "Format email tidak valid."
                        else -> e.localizedMessage ?: "Email atau kata sandi salah."
                    }
                }

                // ── Registration-Specific ──
                e is FirebaseAuthUserCollisionException ->
                    "Email ini sudah terdaftar. Gunakan email lain atau masuk ke akun yang sudah ada."
                e is FirebaseAuthWeakPasswordException ->
                    "Kata sandi terlalu lemah. Gunakan minimal 6 karakter dengan kombinasi huruf dan angka."

                // ── Network / Server Errors ──
                e is FirebaseNetworkException || (e as? FirebaseException)?.message?.contains("network") == true ->
                    "Gagal terhubung ke server. Periksa koneksi internet Anda dan coba lagi."
                e is FirebaseTooManyRequestsException ->
                    "Terlalu banyak percobaan. Silakan tunggu beberapa saat dan coba lagi."

                // ── GMS / Play Services Errors ──
                e is SecurityException || e is IllegalStateException -> {
                    val msg = e.localizedMessage ?: ""
                    when {
                        msg.contains("DEVELOPER_ERROR") || msg.contains("12501") ->
                            "Google Play Services tidak tersedia atau perlu diperbarui. " +
                            "Pastikan Google Play Services terinstal dan aktif di perangkat Anda."
                        msg.contains("SIGN_IN_REQUIRED") || msg.contains("12500") ->
                            "Perlu masuk ke akun Google terlebih dahulu."
                        else ->
                            "Layanan autentikasi tidak tersedia. Pastikan Google Play Services berfungsi dengan baik."
                    }
                }
                e is com.google.firebase.FirebaseApiNotAvailableException ->
                    "Layanan Firebase tidak tersedia saat ini. Coba lagi nanti."

                // ── Generic Fallback ──
                else -> e.localizedMessage ?: "Terjadi kesalahan. Silakan coba lagi."
            }
        }
    }

    /**
     * Get the currently signed-in user.
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Observe auth state changes as a Flow.
     */
    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { authState ->
            trySend(authState.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Sign in with Google ID token.
     * Auto-creates Firestore user document on first login if missing.
     */
    suspend fun signInWithGoogle(idToken: String): Resource<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: return Resource.error("Gagal mendapatkan data pengguna")

            // Get existing or create new Firestore doc
            val name = firebaseUser.displayName ?: ""
            val email = firebaseUser.email ?: ""
            getOrCreateUserPublic(firebaseUser.uid, name, email)
        } catch (e: Exception) {
            Resource.error(mapFirebaseAuthError(e), e)
        }
    }

    /**
     * Register with email and password.
     */
    suspend fun registerWithEmail(name: String, email: String, password: String): Resource<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Resource.error("Gagal membuat akun")

            val user = User(
                uid = firebaseUser.uid,
                name = name,
                email = email,
                role = UserRole.PEMILIH,
                status = UserStatus.ACTIVE,
                lastLogin = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )

            firestore.collection(Constants.COLLECTION_USERS)
                .document(user.uid)
                .set(user)
                .await()

            Resource.success(user)
        } catch (e: Exception) {
            Resource.error(mapFirebaseAuthError(e), e)
        }
    }

    /**
     * Sign in with email and password.
     * Auto-creates Firestore user document on first login if missing.
     */
    suspend fun signInWithEmail(email: String, password: String): Resource<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Resource.error("Gagal masuk")

            // Get existing Firestore doc, or create one on first login
            val user = getOrCreateUserPublic(firebaseUser.uid, firebaseUser.displayName ?: "", email)

            // Update last login
            if (user is Resource.Success<*>) {
                firestore.collection(Constants.COLLECTION_USERS)
                    .document(firebaseUser.uid)
                    .update("lastLogin", System.currentTimeMillis())
                    .await()
            }

            user
        } catch (e: Exception) {
            Resource.error(mapFirebaseAuthError(e), e)
        }
    }

    /**
     * Get user data from Firestore.
     */
    override suspend fun getUser(userId: String): Resource<User> {
        return try {
            val snapshot = firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            val user = snapshot.toObject(User::class.java)
            if (user != null) {
                Resource.success(user)
            } else {
                Resource.error("Pengguna tidak ditemukan")
            }
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memuat data pengguna", e)
        }
    }

    /**
     * Get existing Firestore user doc, or auto-create one on first login.
     * New users get PEMILIH role as default (safe). Admin promosi via console.
     */
    suspend fun getOrCreateUserPublic(uid: String, name: String, email: String): Resource<User> {
        return try {
            val snapshot = firestore.collection(Constants.COLLECTION_USERS)
                .document(uid).get().await()
            val existing = snapshot.toObject(User::class.java)
            // Determine role: email pattern is authoritative for known system accounts
            val emailRole = detectRoleFromEmail(email)

            if (existing != null) {
                // CORRECT role if Firestore has wrong role (e.g., from old/broken seeding)
                val correctedRole = if (existing.role == UserRole.PEMILIH && emailRole != UserRole.PEMILIH) emailRole else existing.role
                try {
                    val updates = mutableMapOf<String, Any>("lastLogin" to System.currentTimeMillis(), "name" to name, "email" to email)
                    if (correctedRole != existing.role) updates["role"] = correctedRole.name as String
                    firestore.collection(Constants.COLLECTION_USERS).document(uid).update(updates).await()
                } catch (e: Exception) { Log.w(TAG, "Failed to update user role", e) }
                Resource.success(existing.copy(role = correctedRole))
            } else {
                // First login — use email-based role detection
                val user = User(uid = uid, name = name, email = email, role = emailRole, status = UserStatus.ACTIVE, lastLogin = System.currentTimeMillis(), createdAt = System.currentTimeMillis())
                try { firestore.collection(Constants.COLLECTION_USERS).document(uid).set(user).await() } catch (e: Exception) { Log.w(TAG, "Failed to create user document", e) }
                Resource.success(user)
            }
        } catch (e: Exception) {
            // Firestore completely unavailable — use email-based role
            val role = detectRoleFromEmail(email)
            Resource.success(User(uid = uid, name = name, email = email, role = role, status = UserStatus.ACTIVE, lastLogin = System.currentTimeMillis(), createdAt = System.currentTimeMillis()))
        }
    }

    /**
     * Detect user role from email pattern.
     * admin@ → ADMIN, monitor@ → MONITOR, anything else → PEMILIH.
     */
    private fun detectRoleFromEmail(email: String): UserRole = when {
        email.startsWith("admin@") || email.contains("+admin") -> UserRole.ADMIN
        email.startsWith("monitor@") || email.contains("+monitor") -> UserRole.MONITOR
        else -> UserRole.PEMILIH
    }

    /**
     * Check if a user exists and get their role.
     */
    suspend fun getUserRole(uid: String): UserRole? {
        return when (val result = getUser(uid)) {
            is Resource.Success -> result.data.role
            else -> null
        }
    }

    /**
     * Send password reset email.
     */
    suspend fun sendPasswordReset(email: String): Resource<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Resource.success(Unit)
        } catch (e: Exception) {
            Resource.error(mapFirebaseAuthError(e), e)
        }
    }

    /**
     * Get all users (Admin only) — one-shot query ordered by creation date.
     */
    override suspend fun getAllUsers(): Resource<List<User>> {
        return try {
            val snapshot = firestore.collection(Constants.COLLECTION_USERS)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            Resource.success(snapshot.toObjects(User::class.java))
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal mengambil daftar pengguna", e)
        }
    }

    /**
     * Real-time observer for all users (Admin only).
     */
    override fun observeAllUsers(): Flow<Resource<List<User>>> = callbackFlow {
        trySend(Resource.loading())
        val listener = firestore.collection(Constants.COLLECTION_USERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.error(error.toFirestoreErrorMessage(), error))
                    return@addSnapshotListener
                }
                val users = snapshot?.toObjects(User::class.java) ?: emptyList()
                trySend(Resource.success(users))
            }
        awaitClose { listener.remove() }
    }

    /**
     * Update user role (Admin only).
     */
    suspend fun updateUserRole(uid: String, role: UserRole): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_USERS).document(uid)
                .update("role", role.name).await()
            Resource.success(Unit)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal mengubah role", e)
        }
    }

    /**
     * Update user status (Admin only) — suspend, activate, ban.
     */
    suspend fun updateUserStatus(uid: String, status: UserStatus): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_USERS).document(uid)
                .update("status", status.name).await()
            Resource.success(Unit)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal mengubah status", e)
        }
    }

    /**
     * Get user's profile photo (Base64) from Firestore.
     */
    override suspend fun getUserPhoto(userId: String): String? {
        return try {
            val snapshot = firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
            snapshot.getString("photoUrl")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Update user's profile photo in Firestore.
     * Returns true on success, false on failure.
     */
    override suspend fun updateProfilePhoto(userId: String, photoBase64: String): Boolean {
        return try {
            firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update("photoUrl", photoBase64)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Change user password (requires recent login).
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): Resource<Unit> {
        return try {
            val user = auth.currentUser ?: return Resource.error("Tidak ada sesi aktif")
            val email = user.email ?: return Resource.error("Email pengguna tidak tersedia")
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            Resource.success(Unit)
        } catch (e: Exception) {
            Resource.error(mapFirebaseAuthError(e), e)
        }
    }
}
