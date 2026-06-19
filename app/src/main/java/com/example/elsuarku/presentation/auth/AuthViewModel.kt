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

    companion object {
        private const val TAG = "AuthViewModel"
    }

    init {
        observeAuthState()
    }

    /**
     * Observe Firebase Auth state for auto-login (e.g., app restart with cached session).
     * NOT used for fresh sign-in — signIn methods directly set authState to avoid race conditions.
     */
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { firebaseUser ->
                if (firebaseUser != null) {
                    // Only process if we don't already have a valid auth state with role
                    val currentState = _authState.value
                    if (currentState is AuthUiState.Authenticated) {
                        return@collect // Already authenticated — skip
                    }

                    val name = firebaseUser.displayName ?: ""
                    val email = firebaseUser.email ?: ""

                    // Try to get role from Firestore
                    when (val result = authRepository.getOrCreateUserPublic(firebaseUser.uid, name, email)) {
                        is Resource.Success -> {
                            val user = result.data
                            sessionManager.saveUserInfo(user.uid, user.role, user.name)
                            Log.i(TAG, "Auth restored from Firestore: uid=${user.uid}, role=${user.role}")
                            setAuthenticatedState(user.uid, user.name, user.email, user.role)
                        }
                        is Resource.Error -> {
                            // Firestore failed — try session cache
                            val cachedRole = sessionManager.getUserRole()
                            if (cachedRole != null) {
                                Log.w(TAG, "Firestore read failed, using cached role: $cachedRole. Error: ${result.message}")
                                setAuthenticatedState(firebaseUser.uid, name, email, cachedRole)
                            } else {
                                Log.w(TAG, "Firestore read failed and no cached role. Error: ${result.message}")
                                // Don't auto-login if we can't determine role
                                _authState.value = AuthUiState.NotAuthenticated
                            }
                        }
                        is Resource.Loading -> {}
                    }
                } else {
                    _authState.value = AuthUiState.NotAuthenticated
                }
            }
        }
    }

    private fun setAuthenticatedState(uid: String, name: String, email: String, role: UserRole) {
        _authState.value = AuthUiState.Authenticated(
            uid = uid, name = name, email = email, role = role
        )
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            when (val result = authRepository.signInWithGoogle(idToken)) {
                is Resource.Success -> {
                    val user = result.data
                    // Save session FIRST
                    sessionManager.saveUserInfo(user.uid, user.role, user.name)
                    // Log audit
                    auditRepository.log(user.uid, user.name, user.role.name, AuditAction.LOGIN, user.uid, user.name)
                    // DIRECTLY set auth state — bypass observeAuthState to avoid race
                    Log.i(TAG, "Google sign-in success: uid=${user.uid}, role=${user.role}")
                    setAuthenticatedState(user.uid, user.name, user.email, user.role)
                    _loginState.value = LoginUiState.Success(user.role)
                }
                is Resource.Error -> {
                    Log.e(TAG, "Google sign-in failed: ${result.message}")
                    _loginState.value = LoginUiState.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            when (val result = authRepository.signInWithEmail(email, password)) {
                is Resource.Success -> {
                    val user = result.data
                    // Save session FIRST — before any state update
                    sessionManager.saveUserInfo(user.uid, user.role, user.name)
                    // Log audit
                    auditRepository.log(user.uid, user.name, user.role.name, AuditAction.LOGIN, user.uid, user.name)
                    // DIRECTLY set auth state with correct role — bypass observeAuthState race
                    Log.i(TAG, "Email sign-in success: uid=${user.uid}, role=${user.role}, name=${user.name}")
                    setAuthenticatedState(user.uid, user.name, user.email, user.role)
                    _loginState.value = LoginUiState.Success(user.role)
                }
                is Resource.Error -> {
                    Log.e(TAG, "Email sign-in failed: ${result.message}")
                    _loginState.value = LoginUiState.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            when (val result = authRepository.registerWithEmail(name, email, password)) {
                is Resource.Success -> {
                    _loginState.value = LoginUiState.Success(UserRole.PEMILIH)
                }
                is Resource.Error -> {
                    _loginState.value = LoginUiState.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun logout() {
        val uid = sessionManager.getUserId() ?: ""
        val role = sessionManager.getUserRole()?.name ?: ""
        val name = sessionManager.getUserName()
        viewModelScope.launch {
            auditRepository.log(uid, name, role, AuditAction.LOGOUT)
        }
        authRepository.signOut()
        sessionManager.clearSession()
        _authState.value = AuthUiState.NotAuthenticated
        _loginState.value = LoginUiState.Idle
    }

    fun resetLoginState() {
        _loginState.value = LoginUiState.Idle
    }
}
