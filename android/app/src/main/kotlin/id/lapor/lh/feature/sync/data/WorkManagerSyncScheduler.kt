package id.lapor.lh.feature.sync.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import id.lapor.lh.feature.sync.domain.SyncScheduler
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject

const val SYNC_WORK = "sync"
private const val PERIODIC_SYNC_WORK = "sync-periodic"
private const val BACKOFF_SECONDS = 30L
private const val PERIODIC_HOURS = 6L

class WorkManagerSyncScheduler
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : SyncScheduler {
        private val workManager = WorkManager.getInstance(context)
        private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        override suspend fun requestSync() {
            val states = workManager.getWorkInfosForUniqueWorkFlow(SYNC_WORK).first().map { it.state }
            val running = WorkInfo.State.RUNNING in states
            val waiting = WorkInfo.State.ENQUEUED in states || WorkInfo.State.BLOCKED in states
            val policy =
                when {
                    running && waiting -> return
                    running -> ExistingWorkPolicy.APPEND_OR_REPLACE
                    else -> ExistingWorkPolicy.REPLACE
                }
            val request =
                OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                    .build()
            workManager.enqueueUniqueWork(SYNC_WORK, policy, request)
        }

        override fun schedulePeriodicSync() {
            val request =
                PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_HOURS, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                    .build()
            workManager.enqueueUniquePeriodicWork(PERIODIC_SYNC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
