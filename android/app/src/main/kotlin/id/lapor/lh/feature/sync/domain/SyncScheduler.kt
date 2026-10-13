package id.lapor.lh.feature.sync.domain

interface SyncScheduler {
    suspend fun requestSync()

    fun schedulePeriodicSync()
}
