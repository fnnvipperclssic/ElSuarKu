package com.example.elsuarku.domain.repository

import com.example.elsuarku.data.model.User
import com.example.elsuarku.utils.Resource
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    suspend fun login(email: String, password: String): Resource<String>
    suspend fun register(email: String, password: String, name: String, role: String): Resource<String>
    suspend fun signOut(): Resource<Unit>
    suspend fun getUser(userId: String): Resource<User>
    suspend fun getUserPhoto(userId: String): String?
    suspend fun updateProfilePhoto(userId: String, photoBase64: String): Boolean
    suspend fun registerDeviceFingerprint(userId: String, fingerprint: String): Resource<Unit>
    suspend fun checkConcurrentSessions(userId: String, fingerprint: String): Resource<List<String>>
    suspend fun getAllUsers(): Resource<List<User>>
    fun observeAllUsers(): Flow<Resource<List<User>>>
}
