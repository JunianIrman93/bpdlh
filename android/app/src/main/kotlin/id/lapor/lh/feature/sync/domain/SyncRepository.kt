package id.lapor.lh.feature.sync.domain

import kotlinx.coroutines.flow.Flow

data class SyncStatus(
    val isOnline: Boolean,
    val isSyncing: Boolean,
)

interface SyncRepository {
    suspend fun requestSync()

    fun schedulePeriodicSync()

    fun observeStatus(): Flow<SyncStatus>
}
