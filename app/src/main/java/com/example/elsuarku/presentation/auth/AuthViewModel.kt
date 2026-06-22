package com.example.elsuarku.presentation.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elsuarku.data.model.AuditAction
import com.example.elsuarku.data.model.UserRole
import com.example.elsuarku.data.repository.AuditRepository
import com.example.elsuarku.data.repository.AuthRepository
import com.example.elsuarku.security.SessionManager
import com.example.elsuarku.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val auditRepository: AuditRepository
) : ViewModel() {

    sealed class AuthUiState {
        data object Loading : AuthUiState()
        data class Authenticated(val uid: String, val name: String, val email: String, val role: UserRole) : AuthUiState()
        data object NotAuthenticated : AuthUiState()
    }

    sealed class LoginUiState {
        data object Idle : LoginUiState()
        data object Loading : LoginUiState()
        data class Success(val role: UserRole) : LoginUiState()
        data class Error(val message: String) : LoginUiState()
    }

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    companion object { private const val TAG = "AuthViewModel" }

    // CRITICAL: Prevent observeAuthState from processing during active sign-in
    // Firebase AuthStateListener fires during signInWithEmailAndPassword().await()
    // BEFORE our signIn method can set the correct role. This flag blocks that.
    @Volatile
    private var isSigningIn = false

    init { observeAuthState() }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { firebaseUser ->
                if (firebaseUser != null) {
                    // SKIP if currently signing in — signIn methods handle auth state directly
                    if (isSigningIn) {
                        Log.d(TAG, "observeAuthState: skipping (sign-in in progress)")
                        return@collect
                    }
                    // SKIP if already authenticated with role
                    if (_authState.value is AuthUiState.Authenticated) {
                        Log.d(TAG, "observeAuthState: already authenticated, skipping")
                        return@collect
                    }

                    val name = firebaseUser.displayName ?: ""
                    val email = firebaseUser.email ?: ""
                    Log.d(TAG, "observeAuthState: restoring session for uid=${firebaseUser.uid}")

                    when (val result = authRepository.getOrCreateUserPublic(firebaseUser.uid, name, email)) {
                        is Resource.Success -> {
                            val user = result.data
                            sessionManager.saveUserInfo(user.uid, user.role, user.name, user.email)
                            Log.i(TAG, "observeAuthState: restored role=${user.role} for ${user.email}")
                            _authState.value = AuthUiState.Authenticated(user.uid, user.name, user.email, user.role)
                        }
                        is Resource.Error -> {
                            val cachedRole = sessionManager.getUserRole()
                            if (cachedRole != null) {
                                Log.w(TAG, "observeAuthState: Firestore failed, using cached role=$cachedRole")
                                _authState.value = AuthUiState.Authenticated(firebaseUser.uid, name, email, cachedRole)
                            } else {
                                Log.w(TAG, "observeAuthState: no cached role, showing not authenticated")
                                _authState.value = AuthUiState.NotAuthenticated
                            }
                        }
                        is Resource.Loading -> {}
                    }
                } else {
                    if (!isSigningIn) {
                        _authState.value = AuthUiState.NotAuthenticated
                    }
                }
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            isSigningIn = true  // BLOCK observeAuthState
            _loginState.value = LoginUiState.Loading
            Log.d(TAG, "signInWithEmail: starting for $email")

            when (val result = authRepository.signInWithEmail(email, password)) {
                is Resource.Success -> {
                    val user = result.data
                    sessionManager.saveUserInfo(user.uid, user.role, user.name, user.email)
                    auditRepository.log(user.uid, user.name, user.role.name, AuditAction.LOGIN, user.uid, user.name)
                    Log.i(TAG, "signInWithEmail: SUCCESS uid=${user.uid} role=${user.role} name=${user.name}")
                    _authState.value = AuthUiState.Authenticated(user.uid, user.name, user.email, user.role)
                    _loginState.value = LoginUiState.Success(user.role)
                }
                is Resource.Error -> {
                    Log.e(TAG, "signInWithEmail: FAILED ${result.message}")
                    _loginState.value = LoginUiState.Error(result.message)
                    _authState.value = AuthUiState.NotAuthenticated
                }
                is Resource.Loading -> {}
            }
            isSigningIn = false // UNBLOCK observeAuthState
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            isSigningIn = true
            _loginState.value = LoginUiState.Loading
            when (val result = authRepository.signInWithGoogle(idToken)) {
                is Resource.Success -> {
                    val user = result.data
                    sessionManager.saveUserInfo(user.uid, user.role, user.name, user.email)
                    auditRepository.log(user.uid, user.name, user.role.name, AuditAction.LOGIN, user.uid, user.name)
                    Log.i(TAG, "signInWithGoogle: SUCCESS role=${user.role}")
                    _authState.value = AuthUiState.Authenticated(user.uid, user.name, user.email, user.role)
                    _loginState.value = LoginUiState.Success(user.role)
                }
                is Resource.Error -> {
                    Log.e(TAG, "signInWithGoogle: FAILED ${result.message}")
                    _loginState.value = LoginUiState.Error(result.message)
                    _authState.value = AuthUiState.NotAuthenticated
                }
                is Resource.Loading -> {}
            }
            isSigningIn = false
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            isSigningIn = true  // Prevent observeAuthState from interfering
            _loginState.value = LoginUiState.Loading
            when (val result = authRepository.registerWithEmail(name, email, password)) {
                is Resource.Success -> {
                    // Auto-login after successful registration
                    val user = result.data
                    sessionManager.saveUserInfo(user.uid, user.role, user.name, user.email)
                    auditRepository.log(user.uid, user.name, user.role.name, AuditAction.REGISTER, user.uid, user.name)
                    Log.i(TAG, "register: SUCCESS — auto-login uid=${user.uid} role=${user.role}")
                    _authState.value = AuthUiState.Authenticated(user.uid, user.name, user.email, user.role)
                    _loginState.value = LoginUiState.Success(user.role)
                }
                is Resource.Error -> {
                    Log.e(TAG, "register: FAILED ${result.message}")
                    _loginState.value = LoginUiState.Error(result.message)
                    _authState.value = AuthUiState.NotAuthenticated
                }
                is Resource.Loading -> {}
            }
            isSigningIn = false
        }
    }

    fun logout() {
        val uid = sessionManager.getUserId() ?: ""
        val name = sessionManager.getUserName()
        val role = sessionManager.getUserRole()?.name ?: ""
        viewModelScope.launch { auditRepository.log(uid, name, role, AuditAction.LOGOUT) }
        isSigningIn = false
        authRepository.signOut()
        sessionManager.clearSession()
        _authState.value = AuthUiState.NotAuthenticated
        _loginState.value = LoginUiState.Idle
    }

    fun resetLoginState() { _loginState.value = LoginUiState.Idle }
}
