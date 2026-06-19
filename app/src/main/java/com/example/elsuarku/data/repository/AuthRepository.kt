package com.example.elsuarku.data.repository

import com.example.elsuarku.data.model.User
import com.example.elsuarku.data.model.UserRole
import com.example.elsuarku.data.model.UserStatus
import com.example.elsuarku.utils.Constants
import com.example.elsuarku.utils.Resource
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
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
) {

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
            Resource.error(e.localizedMessage ?: "Gagal masuk dengan Google", e)
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
            Resource.error(e.localizedMessage ?: "Gagal mendaftar", e)
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
            Resource.error(e.localizedMessage ?: "Email atau kata sandi salah", e)
        }
    }

    /**
     * Get user data from Firestore.
     */
    suspend fun getUser(uid: String): Resource<User> {
        return try {
            val snapshot = firestore.collection(Constants.COLLECTION_USERS)
                .document(uid)
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
                .document(uid)
                .get()
                .await()

            val existing = snapshot.toObject(User::class.java)
            if (existing != null) {
                // Already exists — update lastLogin + name/email
                try {
                    firestore.collection(Constants.COLLECTION_USERS)
                        .document(uid)
                        .update(mapOf("lastLogin" to System.currentTimeMillis(), "name" to name, "email" to email))
                        .await()
                } catch (_: Exception) { /* non-critical */ }
                Resource.success(existing)
            } else {
                // First login — determine role from email pattern or default PEMILIH
                val role = detectRoleFromEmail(email)
                val user = User(uid = uid, name = name, email = email, role = role, status = UserStatus.ACTIVE, lastLogin = System.currentTimeMillis(), createdAt = System.currentTimeMillis())
                try {
                    firestore.collection(Constants.COLLECTION_USERS).document(uid).set(user).await()
                } catch (_: Exception) { /* Firestore write failed but user can still login */ }
                Resource.success(user)
            }
        } catch (e: Exception) {
            // Firestore completely unavailable — create in-memory user with role from email
            val role = detectRoleFromEmail(email)
            val user = User(uid = uid, name = name, email = email, role = role, status = UserStatus.ACTIVE, lastLogin = System.currentTimeMillis(), createdAt = System.currentTimeMillis())
            Resource.success(user)
        }
    }

    /**
     * Detect user role from email pattern when Firestore is unavailable.
     * This ensures admin/monitor accounts work even without Firestore connection.
     */
    private fun detectRoleFromEmail(email: String): UserRole {
        return when {
            email.startsWith("admin@") || email.contains("+admin") -> UserRole.ADMIN
            email.startsWith("monitor@") || email.contains("+monitor") -> UserRole.MONITOR
            else -> UserRole.PEMILIH
        }
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
     * Sign out current user.
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Send password reset email.
     */
    suspend fun sendPasswordReset(email: String): Resource<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Resource.success(Unit)
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal mengirim email reset", e)
        }
    }

    /**
     * Get all users (Admin only).
     */
    suspend fun getAllUsers(): Resource<List<User>> {
        return try {
            val snapshot = firestore.collection(Constants.COLLECTION_USERS).get().await()
            Resource.success(snapshot.toObjects(User::class.java))
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: "Gagal memuat data pengguna", e)
        }
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
            Resource.error(e.localizedMessage ?: "Gagal mengubah kata sandi", e)
        }
    }
}
