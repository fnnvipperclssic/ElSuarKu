package com.example.elsuarku.presentation.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.fragment.app.FragmentActivity
import com.example.elsuarku.data.model.AuditAction
import com.example.elsuarku.data.model.UserRole
import com.example.elsuarku.data.repository.AuthRepository
import com.example.elsuarku.domain.repository.IAuditRepository
import com.example.elsuarku.domain.repository.IAuthRepository
import com.example.elsuarku.security.BiometricPromptManager
import com.example.elsuarku.security.BiometricResult
import com.example.elsuarku.security.PinManager
import com.example.elsuarku.security.SessionManager
import com.example.elsuarku.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: IAuthRepository,
    private val sessionManager: SessionManager,
    private val auditRepository: IAuditRepository,
    private val pinManager: PinManager
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
        private const val MAX_PIN_ATTEMPTS = 3
    }

    // PIN attempt tracking (transient, resets on process death)
    private var failedPinAttempts = 0
    private var isPinLocked = false

    // Cast to access concrete AuthRepository methods not exposed on IAuthRepository
    private val authRepo get() = authRepository as AuthRepository

    // CRITICAL: Prevent observeAuthState from processing during active sign-in
    // Firebase AuthStateListener fires during signInWithEmailAndPassword().await()
    // BEFORE our signIn method can set the correct role. This flag blocks that.
    @Volatile
    private var isSigningIn = false

    init { observeAuthState() }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepo.observeAuthState().collect { firebaseUser ->
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

                    when (val result = authRepo.getOrCreateUserPublic(firebaseUser.uid, name, email)) {
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
                        is Resource.Cached<*> -> {}
                        is Resource.Empty -> {}
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
        // Guard against double-submit: if already loading, ignore
        if (_loginState.value is LoginUiState.Loading) return
        viewModelScope.launch {
            isSigningIn = true  // BLOCK observeAuthState
            _loginState.value = LoginUiState.Loading
            Log.d(TAG, "signInWithEmail: starting for $email")

            when (val result = authRepo.signInWithEmail(email, password)) {
                is Resource.Success -> {
                    val user = result.data
                    sessionManager.saveUserInfo(user.uid, user.role, user.name, user.email)
                    pinManager.cacheUserProfile(user.uid, user.name, user.email, user.role.name)
                    auditRepository.log(user.uid, user.name, user.role.name, AuditAction.LOGIN, user.uid, user.name)
                    // Register device fingerprint for concurrent session detection
                    val fp = sessionManager.getOrCreateDeviceFingerprint()
                    authRepository.registerDeviceFingerprint(user.uid, fp)
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
                is Resource.Cached<*> -> {}
                is Resource.Empty -> {}
            }
            isSigningIn = false // UNBLOCK observeAuthState
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            isSigningIn = true
            _loginState.value = LoginUiState.Loading
            when (val result = authRepo.signInWithGoogle(idToken)) {
                is Resource.Success -> {
                    val user = result.data
                    sessionManager.saveUserInfo(user.uid, user.role, user.name, user.email)
                    pinManager.cacheUserProfile(user.uid, user.name, user.email, user.role.name)
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
                is Resource.Cached<*> -> {}
                is Resource.Empty -> {}
            }
            isSigningIn = false
        }
    }

    fun register(name: String, email: String, password: String) {
        // Guard against double-submit
        if (_loginState.value is LoginUiState.Loading) return
        viewModelScope.launch {
            isSigningIn = true  // Prevent observeAuthState from interfering
            _loginState.value = LoginUiState.Loading
            when (val result = authRepo.registerWithEmail(name, email, password)) {
                is Resource.Success -> {
                    // Auto-login after successful registration
                    val user = result.data
                    sessionManager.saveUserInfo(user.uid, user.role, user.name, user.email)
                    pinManager.cacheUserProfile(user.uid, user.name, user.email, user.role.name)
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
                is Resource.Cached<*> -> {}
                is Resource.Empty -> {}
            }
            isSigningIn = false
        }
    }

    fun logout() {
        val uid = sessionManager.getUserId() ?: ""
        val name = sessionManager.getUserName()
        val role = sessionManager.getUserRole()?.name ?: ""
        isSigningIn = false
        viewModelScope.launch {
            auditRepository.log(uid, name, role, AuditAction.LOGOUT)
            authRepository.signOut()
        }
        sessionManager.clearSession()
        _authState.value = AuthUiState.NotAuthenticated
        _loginState.value = LoginUiState.Idle
    }

    fun resetLoginState() { _loginState.value = LoginUiState.Idle }

    // ── Biometric Login ──

    /**
     * Initiate biometric authentication for quick re-login.
     * Uses the existing [BiometricPromptManager] for crypto-backed biometric prompt.
     * On success, restores session from cached user profile.
     *
     * @param activity The calling FragmentActivity (required for BiometricPrompt UI)
     * @param onSuccess Called with the user's role on successful authentication
     * @param onError Called with a user-friendly error message on failure
     */
    fun loginWithBiometric(
        activity: FragmentActivity,
        onSuccess: (UserRole) -> Unit,
        onError: (String) -> Unit
    ) {
        val bioManager = BiometricPromptManager()
        val status = bioManager.canAuthenticate(activity)

        when (status) {
            is BiometricResult.Available -> {
                bioManager.authenticate(
                    activity = activity,
                    title = "Login Biometrik",
                    subtitle = "Autentikasi untuk mengakses akun Anda",
                    onSuccess = { restoreCachedSession(onSuccess, onError) },
                    onError = { errorMsg ->
                        Log.w(TAG, "Biometric login error: $errorMsg")
                        onError(errorMsg)
                    },
                    onCancel = { /* user cancelled — do nothing */ }
                )
            }
            else -> {
                val msg = "Biometrik tidak tersedia: ${status.userFriendlyMessage}"
                Log.w(TAG, "Biometric unavailable: $msg")
                onError(msg)
            }
        }
    }

    // ── PIN Login ──

    /**
     * Verify PIN for quick re-login.
     * Tracks failed attempts and locks out after [MAX_PIN_ATTEMPTS].
     */
    fun loginWithPin(
        pin: String,
        onSuccess: (UserRole) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isPinLocked) {
            onError("Terlalu banyak percobaan. Akun terkunci. Silakan login dengan email.")
            return
        }

        if (!pinManager.isPinSet()) {
            onError("PIN belum dibuat. Silakan setup PIN terlebih dahulu.")
            return
        }

        if (pinManager.verifyPin(pin)) {
            failedPinAttempts = 0
            restoreCachedSession(onSuccess, onError)
        } else {
            failedPinAttempts++
            if (failedPinAttempts >= MAX_PIN_ATTEMPTS) {
                isPinLocked = true
                onError("PIN salah. Akun terkunci setelah $MAX_PIN_ATTEMPTS percobaan. Silakan login dengan email.")
            } else {
                val remaining = MAX_PIN_ATTEMPTS - failedPinAttempts
                onError("PIN salah. $remaining percobaan tersisa.")
            }
        }
    }

    /**
     * Reset PIN lockout (called when user switches to email login).
     */
    fun resetPinLockout() {
        failedPinAttempts = 0
        isPinLocked = false
    }

    val pinAttemptState: Pair<Int, Boolean>
        get() = failedPinAttempts to isPinLocked

    // ── PIN Setup ──

    /**
     * Set up a new 4-digit PIN.
     */
    fun setupPin(pin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (pinManager.setPin(pin)) {
            Log.i(TAG, "PIN setup successful")
            onSuccess()
        } else {
            onError("PIN harus 4 digit angka")
        }
    }

    // ── Session Restore ──

    /**
     * Restore session from cached user profile (for biometric/PIN quick login).
     * Reads the persistently cached profile from [PinManager] and sets auth state.
     */
    private fun restoreCachedSession(
        onSuccess: (UserRole) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = pinManager.getCachedUserId()
        val name = pinManager.getCachedUserName()
        val email = pinManager.getCachedUserEmail()
        val roleStr = pinManager.getCachedUserRole()

        if (uid != null && roleStr != null) {
            val role = try {
                UserRole.valueOf(roleStr)
            } catch (_: IllegalArgumentException) {
                Log.e(TAG, "Invalid cached role: $roleStr")
                onError("Data pengguna tidak valid. Silakan login ulang.")
                return
            }

            sessionManager.saveUserInfo(uid, role, name, email)
            sessionManager.refreshSession()
            _authState.value = AuthUiState.Authenticated(uid, name, email, role)
            _loginState.value = LoginUiState.Success(role)
            Log.i(TAG, "Quick re-login successful: uid=${uid.take(8)}... role=$role")
            onSuccess(role)
        } else {
            Log.w(TAG, "No cached profile for quick re-login")
            onError("Session tidak ditemukan. Silakan login dengan email terlebih dahulu.")
        }
    }
}
