package id.lapor.lh.feature.sync.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import dagger.hilt.android.qualifiers.ApplicationContext
import id.lapor.lh.core.database.AppDatabase
import id.lapor.lh.feature.auth.data.AuthLocalStore
import id.lapor.lh.feature.sync.domain.SyncRepository
import id.lapor.lh.feature.sync.domain.SyncStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val SYNC_WORK = "sync"
private const val PERIODIC_SYNC_WORK = "sync-periodic"
private const val BACKOFF_SECONDS = 30L
private const val PERIODIC_HOURS = 6L

@OptIn(ExperimentalCoroutinesApi::class)
class WorkManagerSyncRepository
    @Inject
    constructor(
        @ApplicationContext context: Context,
        database: AppDatabase,
        private val authLocalStore: AuthLocalStore,
        private val engine: SyncEngine,
    ) : SyncRepository {
        private val metaDao = database.syncMetaDao()
        private val workManager = WorkManager.getInstance(context)
        private val connectivity = context.getSystemService(ConnectivityManager::class.java)
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

        override fun observeStatus(): Flow<SyncStatus> =
            combine(observeOnline(), observeRunning(SYNC_WORK), observeRunning(PERIODIC_SYNC_WORK)) { online, sync, periodic ->
                SyncStatus(isOnline = online, isSyncing = sync || periodic)
            }.distinctUntilChanged()

        override fun observeLastSyncAt(): Flow<Long?> =
            authLocalStore.currentUser.flatMapLatest { user ->
                if (user == null) flowOf(null) else metaDao.observe(lastSyncKey(user.id)).map { it?.toLongOrNull() }
            }

        override suspend fun <T> withSyncStopped(block: suspend () -> T): T {
            workManager.cancelUniqueWork(SYNC_WORK).await()
            workManager.cancelUniqueWork(PERIODIC_SYNC_WORK).await()
            return engine.exclusive(block)
        }

        private fun observeRunning(name: String): Flow<Boolean> =
            workManager.getWorkInfosForUniqueWorkFlow(name).map { infos -> infos.any { it.state == WorkInfo.State.RUNNING } }

        private fun observeOnline(): Flow<Boolean> =
            callbackFlow {
                val callback =
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onCapabilitiesChanged(
                            network: Network,
                            capabilities: NetworkCapabilities,
                        ) {
                            trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                        }

                        override fun onLost(network: Network) {
                            trySend(false)
                        }
                    }
                trySend(connectivity.activeNetwork?.let(connectivity::getNetworkCapabilities).hasInternet())
                connectivity.registerDefaultNetworkCallback(callback)
                awaitClose { connectivity.unregisterNetworkCallback(callback) }
            }.distinctUntilChanged()
    }

private fun NetworkCapabilities?.hasInternet(): Boolean = this?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
