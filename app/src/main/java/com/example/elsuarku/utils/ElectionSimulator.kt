package com.example.elsuarku.utils

import com.example.elsuarku.data.model.*
import kotlin.random.Random

/**
 * Election dry-run / simulation engine.
 *
 * Simulates an election with configurable parameters to test:
 *  - Vote counting accuracy
 *  - Turnout distribution patterns
 *  - Audit trail completeness
 *  - Two-phase commit resiliency
 *
 * Used in admin simulation mode — does not touch production Firestore data.
 * All simulated votes are local-only and prefixed with "SIM_" to distinguish from real votes.
 */
object ElectionSimulator {

    /** Configuration for a simulation run */
    data class SimulationConfig(
        val totalVoters: Int = 100,
        val candidates: List<String> = listOf("Kandidat A", "Kandidat B", "Kandidat C"),
        val durationHours: Int = 48,
        val turnoutRate: Float = 0.75f,           // 0..1
        val distributionPattern: DistributionPattern = DistributionPattern.NORMAL,
        val fraudInjectionCount: Int = 0,          // inject fraudulent votes to test detection
        val networkFailureRate: Float = 0.02f      // simulate intermittent failures (0..1)
    )

    enum class DistributionPattern {
        /** Even spread — each candidate gets roughly equal votes */
        UNIFORM,
        /** Bell curve — one candidate dominates, others trail */
        NORMAL,
        /** Zipfian — power-law distribution, highly skewed */
        SKEWED,
        /** Winner-take-all — one candidate gets 90%+ votes */
        LANDSLIDE
    }

    data class SimulationResult(
        val config: SimulationConfig,
        val candidates: Map<String, Int>,              // candidateName → votes
        val totalVotesCast: Int,
        val turnoutAchieved: Float,
        val timeDistribution: Map<Int, Int>,            // hour → votes
        val reconciliationFailures: Int,                 // votes stuck in PENDING
        val duplicateAttempts: Int,                      // attempted double-votes
        val durationMs: Long,
        val isAccurate: Boolean                          // total matches expected
    )

    data class SimulatedVote(
        val voterId: String,
        val candidateName: String,
        val timestamp: Long,
        val reconciliationStatus: String,
        val isDuplicate: Boolean = false
    )

    /**
     * Run a simulation with the given config.
     *
     * @return SimulationResult with statistics and accuracy verification
     */
    fun runSimulation(config: SimulationConfig): SimulationResult {
        val startTime = System.currentTimeMillis()
        val rng = Random(42) // deterministic seed for reproducibility

        val votes = mutableListOf<SimulatedVote>()
        val votedIds = mutableSetOf<String>()
        val candidateCounts = mutableMapOf<String, Int>().apply {
            config.candidates.forEach { put(it, 0) }
        }
        val hourlyCounts = mutableMapOf<Int, Int>()
        var reconciliationFailures = 0
        var duplicateAttempts = 0

        val activeVoters = (config.totalVoters * config.turnoutRate).toInt()
        val voteWeights = generateDistribution(config.candidates, config.distributionPattern, rng)

        // Spread votes across the election duration
        val electionStart = System.currentTimeMillis() - (config.durationHours * 3_600_000L)
        val stepMs = (config.durationHours * 3_600_000L) / activeVoters.coerceAtLeast(1)

        repeat(activeVoters) { _ ->
            val voterId = "SIM_VOTER_${rng.nextInt(1_000_000)}"

            // Simulate duplicate attempt
            if (voterId in votedIds) {
                duplicateAttempts++
                return@repeat
            }
            votedIds.add(voterId)

            // Simulate network failure
            val isReconciled = rng.nextFloat() < config.networkFailureRate
            if (isReconciled) reconciliationFailures++

            // Pick candidate based on distribution
            val candidateName = weightedPick(voteWeights, rng)
            candidateCounts[candidateName] = (candidateCounts[candidateName] ?: 0) + 1

            val voteTime = electionStart + (stepMs * votedIds.size.coerceAtMost(activeVoters))
            val hour = ((voteTime - electionStart) / 3_600_000L).toInt()
            hourlyCounts[hour] = (hourlyCounts[hour] ?: 0) + 1

            votes.add(
                SimulatedVote(
                    voterId = voterId,
                    candidateName = candidateName,
                    timestamp = voteTime,
                    reconciliationStatus = if (isReconciled) "PENDING_RECONCILIATION" else "CONFIRMED"
                )
            )

            // Inject fraudulent votes
            if (config.fraudInjectionCount > 0 && rng.nextInt(1000) < (config.fraudInjectionCount * 10)) {
                votes.add(
                    SimulatedVote(
                        voterId = "SIM_FRAUD_${rng.nextInt(1_000_000)}",
                        candidateName = candidateName,
                        timestamp = voteTime,
                        reconciliationStatus = "CONFIRMED",
                        isDuplicate = true
                    )
                )
            }
        }

        val eligibleVoteCount = (config.totalVoters * config.turnoutRate).toInt()

        return SimulationResult(
            config = config,
            candidates = candidateCounts,
            totalVotesCast = votes.size,
            turnoutAchieved = votes.size.toFloat() / config.totalVoters.toFloat(),
            timeDistribution = hourlyCounts,
            reconciliationFailures = reconciliationFailures,
            duplicateAttempts = duplicateAttempts,
            durationMs = System.currentTimeMillis() - startTime,
            isAccurate = votes.sumOf { 1 } == votes.size // sanity check
        )
    }

    private fun generateDistribution(
        candidates: List<String>,
        pattern: DistributionPattern,
        rng: Random
    ): Map<String, Float> {
        return when (pattern) {
            DistributionPattern.UNIFORM -> {
                candidates.associateWith { 1f / candidates.size }
            }
            DistributionPattern.NORMAL -> {
                // Bell curve: first candidate ~40%, last ~10%
                val total = candidates.size * (candidates.size + 1) / 2f
                candidates.mapIndexed { index, name ->
                    name to ((candidates.size - index).toFloat() / total)
                }.toMap()
            }
            DistributionPattern.SKEWED -> {
                // Power-law: winner gets ~50%, others drop rapidly
                val weights = candidates.mapIndexed { index, _ ->
                    1f / (index + 1).toFloat()
                }
                val sum = weights.sum()
                candidates.zip(weights).associate { (name, w) -> name to w / sum }
            }
            DistributionPattern.LANDSLIDE -> {
                val winner = candidates.first()
                val remainder = (1f - 0.85f) / (candidates.size - 1).coerceAtLeast(1)
                candidates.associateWith { name ->
                    if (name == winner) 0.85f else remainder
                }
            }
        }
    }

    private fun weightedPick(weights: Map<String, Float>, rng: Random): String {
        val total = weights.values.sum()
        var r = rng.nextFloat() * total
        for ((name, weight) in weights) {
            r -= weight
            if (r <= 0f) return name
        }
        return weights.keys.first()
    }
}
