package com.example.elsuarku.presentation.dashboard

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
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val electionRepo = FakeElectionRepository()
    private val voteRepo = FakeVoteRepository()
    private val authRepo = FakeAuthRepository()
    private val sessionManager = mockk<SessionManager>(relaxed = true)

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { sessionManager.getUserId() } returns "user-001"
        every { sessionManager.getUserName() } returns "Test User"
        every { sessionManager.getUserEmail() } returns "test@example.com"

        viewModel = DashboardViewModel(electionRepo, voteRepo, authRepo, sessionManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ═══════════════════════════════════════════════════════════════
    // Initial state
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `initial state shows user name and email`() = testScope.runTest {
        val state = viewModel.state.first()
        assertEquals("Test User", state.userName)
        assertEquals("test@example.com", state.userEmail)
        assertTrue(state.isLoading)
    }

    @Test
    fun `initial state without session shows empty name`() = testScope.runTest {
        every { sessionManager.getUserId() } returns null
        val vm = DashboardViewModel(electionRepo, voteRepo, authRepo, sessionManager)
        val state = vm.state.first()
        assertEquals("", state.userName)
        assertEquals("", state.userEmail)
    }

    // ═══════════════════════════════════════════════════════════════
    // loadDashboard — success
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `loadDashboard populates active elections`() = testScope.runTest {
        val elections = listOf(
            TestElectionFactory.active().copy(id = "e-001", title = "Election 1"),
            TestElectionFactory.active().copy(id = "e-002", title = "Election 2"),
            TestElectionFactory.completed().copy(id = "e-003", title = "Completed")
        )
        electionRepo.setElections(elections)

        viewModel.loadDashboard()
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(2, state.activeElections.size)
        assertEquals("Election 1", state.activeElections[0].title)
    }

    @Test
    fun `loadDashboard empty — shows zero elections`() = testScope.runTest {
        electionRepo.setElections(emptyList())

        viewModel.loadDashboard()
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertFalse(state.isLoading)
        assertEquals(0, state.activeElections.size)
    }

    // ═══════════════════════════════════════════════════════════════
    // loadDashboard — no session
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `loadDashboard with null userId shows error`() = testScope.runTest {
        every { sessionManager.getUserId() } returns null
        val vm = DashboardViewModel(electionRepo, voteRepo, authRepo, sessionManager)

        vm.loadDashboard()
        advanceUntilIdle()

        val state = vm.state.first()
        assertFalse(state.isLoading)
        assertTrue(state.error?.contains("Sesi tidak valid") == true)
    }

    // ═══════════════════════════════════════════════════════════════
    // loadDashboard — error
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `loadDashboard surfaces Firestore error`() = testScope.runTest {
        electionRepo.simulateError("Firestore unavailable")

        viewModel.loadDashboard()
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertFalse(state.isLoading)
        assertEquals("Firestore unavailable", state.error)
    }

    // ═══════════════════════════════════════════════════════════════
    // loadDashboard — idempotency
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `loadDashboard skips if already observing`() = testScope.runTest {
        electionRepo.setElections(listOf(TestElectionFactory.active().copy(id = "e-001")))

        viewModel.loadDashboard()
        advanceUntilIdle()

        // Second call should be no-op
        viewModel.loadDashboard()
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals(1, state.activeElections.size)
    }

    @Test
    fun `loadDashboard force reload resets observing`() = testScope.runTest {
        electionRepo.setElections(listOf(TestElectionFactory.active().copy(id = "e-001")))

        viewModel.loadDashboard()
        advanceUntilIdle()

        // Update elections and force reload
        electionRepo.setElections(listOf(
            TestElectionFactory.active().copy(id = "e-001"),
            TestElectionFactory.active().copy(id = "e-002")
        ))
        viewModel.loadDashboard(forceReload = true)
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals(2, state.activeElections.size)
    }

    // ═══════════════════════════════════════════════════════════════
    // refreshSession
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `refreshSession delegates to session manager`() {
        viewModel.refreshSession()
        verify(exactly = 1) { sessionManager.refreshSession() }
    }

    // ═══════════════════════════════════════════════════════════════
    // loadUserPhoto / updateProfilePhoto
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `loadUserPhoto returns null when no photo set`() = testScope.runTest {
        val result = viewModel.loadUserPhoto("user-001")
        assertNull(result)
    }

    @Test
    fun `updateProfilePhoto returns true on success`() = testScope.runTest {
        val result = viewModel.updateProfilePhoto("user-001", "base64data")
        assertTrue(result)
    }
}
