package com.example.elsuarku.utils

import com.example.elsuarku.data.model.Election
import com.example.elsuarku.data.model.Vote

/**
 * Tracks voter turnout statistics for elections.
 *
 * Computes:
 *  - Overall turnout percentage
 *  - Hourly vote rate
 *  - Peak voting hour
 *  - Demographic breakdown (by department/division if available)
 *
 * All calculations are pure functions — no side effects, no caching.
 */
object VoterTurnoutTracker {

    /** Turnout breakdown for a single election */
    data class TurnoutReport(
        val electionId: String,
        val electionName: String,
        val totalEligibleVoters: Int,
        val totalVotesCasted: Int,
        val turnoutPercentage: Float,                   // 0..100
        val hourlyBreakdown: List<HourlySnapshot>,
        val peakHour: HourlySnapshot?,
        val votesPerDay: List<DailySnapshot>
    )

    data class HourlySnapshot(
        val hour: Int,          // 0..23
        val count: Int,
        val accumulatedTotal: Int
    )

    data class DailySnapshot(
        val dayIndex: Int,      // 0 = first day of election
        val count: Int
    )

    /**
     * Compute turnout report from election metadata and vote timestamps.
     *
     * @param election The election being analyzed
     * @param votes All confirmed votes for this election
     * @param totalEligibleVoters Number of voters eligible for this election
     */
    fun computeTurnout(
        election: Election,
        votes: List<Vote>,
        totalEligibleVoters: Int
    ): TurnoutReport {
        val totalVotes = votes.size
        val turnoutPct = if (totalEligibleVoters > 0)
            (totalVotes.toFloat() / totalEligibleVoters * 100f) else 0f

        // Hourly breakdown
        val hourCounts = IntArray(24)
        val startEpochDay = election.startDate / 86_400_000L // days since epoch
        val dayCounts = mutableMapOf<Int, Int>()

        votes.forEach { vote ->
            val voteDate = java.util.Date(vote.timestamp)
            val hour = voteDate.hours  // java.util.Date.getHours() deprecated but works on API 36
            hourCounts[hour]++

            val voteEpochDay = vote.timestamp / 86_400_000L
            val dayIndex = (voteEpochDay - startEpochDay).toInt().coerceAtLeast(0)
            dayCounts[dayIndex] = (dayCounts[dayIndex] ?: 0) + 1
        }

        // Build hourly snapshots with accumulated count
        var accumulated = 0
        val hourlySnapshots = hourCounts.mapIndexed { hour, count ->
            accumulated += count
            HourlySnapshot(hour, count, accumulated)
        }

        val peakHour = hourlySnapshots.maxByOrNull { it.count }

        val dailySnapshots = (0..(dayCounts.keys.maxOrNull() ?: 0)).map { day ->
            DailySnapshot(day, dayCounts[day] ?: 0)
        }

        return TurnoutReport(
            electionId = election.id,
            electionName = election.title,
            totalEligibleVoters = totalEligibleVoters,
            totalVotesCasted = totalVotes,
            turnoutPercentage = turnoutPct,
            hourlyBreakdown = hourlySnapshots,
            peakHour = peakHour,
            votesPerDay = dailySnapshots
        )
    }

    /**
     * Estimate voter turnout trend — whether it's accelerating or decelerating.
     * Returns hourly growth rate as a percentage (positive = accelerating).
     */
    fun computeTrend(snapshots: List<HourlySnapshot>): Float {
        if (snapshots.size < 2) return 0f
        val recent = snapshots.takeLast(3).map { it.count }.filter { it > 0 }
        val earlier = snapshots.dropLast(3).takeLast(3).map { it.count }.filter { it > 0 }
        if (recent.isEmpty() || earlier.isEmpty()) return 0f
        val recentAvg = recent.average().toFloat()
        val earlierAvg = earlier.average().toFloat()
        if (earlierAvg == 0f) return 100f
        return ((recentAvg - earlierAvg) / earlierAvg * 100f)
    }
}
