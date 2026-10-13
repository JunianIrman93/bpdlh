package id.lapor.lh

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import id.lapor.lh.feature.sync.domain.SyncScheduler
import javax.inject.Inject

@HiltAndroidApp
class LaporLhApplication : Application() {
    @Inject
    lateinit var syncScheduler: SyncScheduler

    override fun onCreate() {
        super.onCreate()
        syncScheduler.schedulePeriodicSync()
    }
}
