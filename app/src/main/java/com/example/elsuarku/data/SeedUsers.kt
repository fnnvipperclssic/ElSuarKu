package com.example.elsuarku.data

import com.example.elsuarku.data.model.User
import com.example.elsuarku.data.model.UserRole
import com.example.elsuarku.data.model.UserStatus
import com.example.elsuarku.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * One-time seeder untuk membuat test users (Admin, Pemilih, Monitor).
 *
 * KREDENSIAL TEST:
 * Admin   : admin@elsuarku.id    / Admin123!
 * Pemilih : pemilih@elsuarku.id  / Pemilih123!
 * Monitor : monitor@elsuarku.id  / Monitor123!
 */
object SeedUsers {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    data class SeedAccount(val email: String, val password: String, val name: String, val role: UserRole)

    val ACCOUNTS = listOf(
        SeedAccount("admin@elsuarku.id", "Admin123!", "Admin ElSuarKu", UserRole.ADMIN),
        SeedAccount("pemilih@elsuarku.id", "Pemilih123!", "Pemilih Demo", UserRole.PEMILIH),
        SeedAccount("monitor@elsuarku.id", "Monitor123!", "Monitor ElSuarKu", UserRole.MONITOR)
    )

    suspend fun seed(): List<String> {
        val results = mutableListOf<String>()
        for (account in ACCOUNTS) {
            try {
                // Always sign out first
                try { auth.signOut() } catch (_: Exception) {}

                var uid: String? = null

                // Try to sign in to check if user exists
                try {
                    val result = auth.signInWithEmailAndPassword(account.email, account.password).await()
                    uid = result.user?.uid
                    results.add("FOUND: ${account.email} sudah ada di Auth (uid=$uid)")
                } catch (e: Exception) {
                    // User doesn't exist in Auth — create new
                    try {
                        val newUser = auth.createUserWithEmailAndPassword(account.email, account.password).await()
                        uid = newUser.user?.uid
                        results.add("CREATED: ${account.email} dibuat di Auth (uid=$uid)")
                    } catch (e2: Exception) {
                        results.add("GAGAL Auth: ${account.email} — ${e2.message}")
                        continue
                    }
                }

                // ENSURE Firestore document exists with correct role
                if (uid != null) {
                    val docRef = firestore.collection(Constants.COLLECTION_USERS).document(uid)
                    val existingDoc = docRef.get().await()
                    val existingUser = existingDoc.toObject(User::class.java)

                    if (existingUser != null && existingUser.role == account.role) {
                        results.add("  └ Firestore: role ${account.role.name} — OK (unchanged)")
                    } else {
                        val user = User(
                            uid = uid,
                            name = account.name,
                            email = account.email,
                            role = account.role,
                            status = UserStatus.ACTIVE,
                            createdAt = existingUser?.createdAt ?: System.currentTimeMillis(),
                            lastLogin = System.currentTimeMillis()
                        )
                        docRef.set(user).await()
                        results.add("  └ Firestore: role ${account.role.name} — ${if (existingUser != null) "UPDATED" else "CREATED"}")
                    }
                }
            } catch (e: Exception) {
                results.add("GAGAL: ${account.email} — ${e.message}")
                try { auth.signOut() } catch (_: Exception) {}
            }
        }

        try { auth.signOut() } catch (_: Exception) {}
        return results
    }
}
