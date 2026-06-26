package com.example.elsuarku.testutil

import com.example.elsuarku.data.model.*

/**
 * Test data factories for unit testing.
 *
 * Each factory produces a valid default instance with all required fields,
 * allowing tests to override only the fields they care about.
 *
 * Usage:
 * ```kotlin
 * val election = TestElectionFactory.create(id = "test-1")
 * val expiredElection = TestElectionFactory.create(
 *     endDate = System.currentTimeMillis() - 86_400_000
 * )
 * ```
 */

object TestElectionFactory {
    fun create(
        id: String = "election-test-001",
        title: String = "Test Election",
        description: String = "A test election for unit testing",
        status: ElectionStatus = ElectionStatus.ACTIVE,
        startDate: Long = System.currentTimeMillis() - 3_600_000L, // 1 hour ago
        endDate: Long = System.currentTimeMillis() + 86_400_000L,  // 1 day from now
        votedCount: Int = 0,
        createdAt: Long = System.currentTimeMillis() - 7_200_000L,
        updatedAt: Long = System.currentTimeMillis()
    ) = Election(
        id = id,
        title = title,
        description = description,
        status = status,
        startDate = startDate,
        endDate = endDate,
        votedCount = votedCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun active() = create(status = ElectionStatus.ACTIVE)
    fun completed() = create(status = ElectionStatus.COMPLETED)
    fun expired() = create(
        endDate = System.currentTimeMillis() - 3_600_000L
    )
    fun notStarted() = create(
        startDate = System.currentTimeMillis() + 3_600_000L
    )
}

object TestCandidateFactory {
    fun create(
        id: String = "candidate-test-001",
        electionId: String = "election-test-001",
        name: String = "Test Candidate",
        description: String = "A test candidate",
        photoBase64: String = "",
        voteCount: Long = 0,
        nomorUrut: Int = 0
    ) = Candidate(
        id = id,
        electionId = electionId,
        name = name,
        description = description,
        photoBase64 = photoBase64,
        voteCount = voteCount,
        nomorUrut = nomorUrut
    )
}

object TestVoteFactory {
    fun create(
        id: String = "",
        electionId: String = "election-test-001",
        voterHash: String = "test-voter-hash-abc123",
        encryptedVoteData: String = "encrypted:vote:user1:election-test-001:candidate-test-001:1234567890",
        hash: String = "integrity-hash-123",
        hmac: String = "hmac-123",
        timestamp: Long = System.currentTimeMillis(),
        verificationToken: String = "verify-token-123",
        reconciliationStatus: ReconciliationStatus = ReconciliationStatus.CONFIRMED
    ) = Vote(
        id = id,
        electionId = electionId,
        voterHash = voterHash,
        encryptedVoteData = encryptedVoteData,
        hash = hash,
        hmac = hmac,
        timestamp = timestamp,
        verificationToken = verificationToken,
        reconciliationStatus = reconciliationStatus
    )

    fun pending() = create(reconciliationStatus = ReconciliationStatus.PENDING_RECONCILIATION)
    fun reconciled() = create(reconciliationStatus = ReconciliationStatus.RECONCILED)
}

object TestUserFactory {
    fun create(
        uid: String = "user-test-001",
        name: String = "Test User",
        email: String = "test@example.com",
        role: UserRole = UserRole.PEMILIH,
        status: UserStatus = UserStatus.ACTIVE,
        photoUrl: String? = null,
        lastLogin: Long = System.currentTimeMillis(),
        createdAt: Long = System.currentTimeMillis() - 86_400_000L
    ) = User(
        uid = uid,
        name = name,
        email = email,
        role = role,
        status = status,
        photoUrl = photoUrl,
        lastLogin = lastLogin,
        createdAt = createdAt
    )

    fun admin() = create(
        uid = "admin-001",
        name = "Admin User",
        email = "admin@example.com",
        role = UserRole.ADMIN
    )

    fun monitor() = create(
        uid = "monitor-001",
        name = "Monitor User",
        email = "monitor@example.com",
        role = UserRole.MONITOR
    )

    fun voter() = create(
        uid = "voter-001",
        name = "Voter User",
        email = "voter@example.com",
        role = UserRole.PEMILIH
    )
}

object TestAuditLogFactory {
    fun create(
        id: String = "audit-test-001",
        actorId: String = "user-001",
        actorName: String = "Test User",
        actorRole: String = "VOTER",
        action: AuditAction = AuditAction.LOGIN,
        target: String = "",
        targetName: String = "",
        metadata: Map<String, String> = emptyMap(),
        timestamp: Long = System.currentTimeMillis(),
        severity: AuditSeverity = AuditSeverity.INFO
    ) = AuditLog(
        id = id,
        actorId = actorId,
        actorName = actorName,
        actorRole = actorRole,
        action = action,
        target = target,
        targetName = targetName,
        metadata = metadata,
        timestamp = timestamp,
        severity = severity
    )
}
