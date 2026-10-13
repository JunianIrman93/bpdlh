package id.lapor.lh

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import id.lapor.lh.feature.sync.domain.SyncRepository
import javax.inject.Inject

@HiltAndroidApp
class LaporLhApplication : Application() {
    @Inject
    lateinit var syncRepository: SyncRepository

    override fun onCreate() {
        super.onCreate()
        syncRepository.schedulePeriodicSync()
    }
}
