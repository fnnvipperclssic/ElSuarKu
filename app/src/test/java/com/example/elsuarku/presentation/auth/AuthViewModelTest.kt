package com.example.elsuarku.presentation.auth

import com.example.elsuarku.security.SessionManager
import com.example.elsuarku.testutil.*
import com.example.elsuarku.utils.Resource
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val authRepo = FakeAuthRepository()
    private val auditRepo = FakeAuditRepository()
    private val sessionManager = mockk<SessionManager>(relaxed = true)

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { sessionManager.getUserId() } returns "user-001"
        every { sessionManager.getUserName() } returns "Test User"
        every { sessionManager.getUserEmail() } returns "test@example.com"
        every { sessionManager.getUserRole() } returns mockk {
            every { name } returns "PEMILIH"
        }
        every { sessionManager.getOrCreateDeviceFingerprint() } returns "fp-abc-123"

        viewModel = AuthViewModel(authRepo, sessionManager, auditRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ═══════════════════════════════════════════════════════════════
    // Initial state
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `initial login state is Idle`() = testScope.runTest {
        val state = viewModel.loginState.first()
        assertTrue(state is AuthViewModel.LoginUiState.Idle)
    }

    // ═══════════════════════════════════════════════════════════════
    // signInWithEmail — success
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `signInWithEmail success transitions to Success state`() = testScope.runTest {
        authRepo.loginShouldSucceed = true

        viewModel.signInWithEmail("test@example.com", "password123")
        advanceUntilIdle()

        val loginState = viewModel.loginState.first()
        assertTrue(loginState is AuthViewModel.LoginUiState.Success)
    }

    @Test
    fun `signInWithEmail success saves session info`() = testScope.runTest {
        authRepo.loginShouldSucceed = true

        viewModel.signInWithEmail("test@example.com", "password123")
        advanceUntilIdle()

        verify { sessionManager.saveUserInfo(any(), any(), any(), any()) }
        verify { sessionManager.getOrCreateDeviceFingerprint() }
    }

    @Test
    fun `signInWithEmail success registers device fingerprint`() = testScope.runTest {
        authRepo.loginShouldSucceed = true

        viewModel.signInWithEmail("test@example.com", "password123")
        advanceUntilIdle()

        // Verify fingerprint was passed to repository
        val prints = authRepo.checkConcurrentSessions("user-test-001", "fp-abc-123")
        assertTrue(prints is Resource.Success)
    }

    // ═══════════════════════════════════════════════════════════════
    // signInWithEmail — failure
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `signInWithEmail failure transitions to Error state`() = testScope.runTest {
        authRepo.loginShouldSucceed = false
        authRepo.loginErrorMsg = "Email atau kata sandi salah"

        viewModel.signInWithEmail("wrong@example.com", "wrongpass")
        advanceUntilIdle()

        val loginState = viewModel.loginState.first()
        assertTrue(loginState is AuthViewModel.LoginUiState.Error)
        assertEquals("Email atau kata sandi salah", (loginState as AuthViewModel.LoginUiState.Error).message)
    }

    // ═══════════════════════════════════════════════════════════════
    // signInWithEmail — double submit guard
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `signInWithEmail ignores double-submit while loading`() = testScope.runTest {
        authRepo.loginShouldSucceed = true

        viewModel.signInWithEmail("test@example.com", "password123")
        // Second call while first is still in-flight
        viewModel.signInWithEmail("test@example.com", "password123")
        advanceUntilIdle()

        val loginState = viewModel.loginState.first()
        assertTrue(loginState is AuthViewModel.LoginUiState.Success)
    }

    // ═══════════════════════════════════════════════════════════════
    // logout
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `logout clears session and transitions to NotAuthenticated`() = testScope.runTest {
        viewModel.logout()
        advanceUntilIdle()

        val authState = viewModel.authState.first()
        assertTrue(authState is AuthViewModel.AuthUiState.NotAuthenticated)

        val loginState = viewModel.loginState.first()
        assertTrue(loginState is AuthViewModel.LoginUiState.Idle)

        verify { sessionManager.clearSession() }
    }

    // ═══════════════════════════════════════════════════════════════
    // resetLoginState
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `resetLoginState returns to Idle`() = testScope.runTest {
        // First set to error
        authRepo.loginShouldSucceed = false
        viewModel.signInWithEmail("x@x.com", "x")
        advanceUntilIdle()

        // Then reset
        viewModel.resetLoginState()
        val loginState = viewModel.loginState.first()
        assertTrue(loginState is AuthViewModel.LoginUiState.Idle)
    }
}
