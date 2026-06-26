package com.example.elsuarku.security

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.elsuarku.data.model.ReconciliationStatus
import com.example.elsuarku.data.repository.AuditRepository
import com.example.elsuarku.data.repository.CandidateRepository
import com.example.elsuarku.data.repository.ElectionRepository
import com.example.elsuarku.data.repository.VoteRepository
import java.util.concurrent.TimeUnit

/**
 * WorkManager periodic worker for vote counter reconciliation.
 *
 * Runs every 6 hours (with flex) to detect and fix PENDING_RECONCILIATION votes.
 * This ensures vote counters are always consistent even if Phase 2 of the
 * two-phase commit fails due to transient network issues or process death.
 *
 * The worker is constrained to run only when the device has an unmetered
 * network connection and is idle (not actively in use).
 */
class ReconciliationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ReconciliationWorker"
        private const val UNIQUE_WORK_NAME = "elsuarku_vote_reconciliation"

        /**
         * Schedule periodic reconciliation. Idempotent — calling multiple
         * times won't create duplicate jobs.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<ReconciliationWorker>(
                6, TimeUnit.HOURS,
                1, TimeUnit.HOURS // flex interval
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .addTag("reconciliation")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Don't replace if already scheduled
                request
            )

            Log.i(TAG, "Periodic reconciliation scheduled (6h interval)")
        }

        /**
         * Cancel the periodic reconciliation job.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting vote reconciliation...")

        return try {
            val voteRepo = VoteRepository()
            val candidateRepo = CandidateRepository()
            val electionRepo = ElectionRepository()
            val reconciliationHelper = ReconciliationHelper(
                voteRepo, candidateRepo, electionRepo
            )

            // ReconciliationHelper.reconcileAll() fetches all elections,
            // then reconciles each one. Idempotent — safe to run multiple times.
            val reports = reconciliationHelper.reconcileAll()

            val totalPending = reports.sumOf { it.pendingVotesFound }
            val totalFixed = reports.sumOf { it.counterMismatchesFixed }

            if (totalPending == 0) {
                Log.i(TAG, "No pending reconciliation votes — all counters synced")
            } else {
                Log.i(TAG, "Reconciliation complete: $totalPending pending, $totalFixed fixed across ${reports.size} elections")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Reconciliation worker failed", e)
            Result.retry()
        }
    }
}
