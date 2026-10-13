package id.lapor.lh.feature.sync.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun syncEngine(): SyncEngine
    }

    override suspend fun doWork(): Result {
        val engine = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java).syncEngine()
        return when (engine.run()) {
            SyncOutcome.DONE -> Result.success()
            SyncOutcome.RETRY -> Result.retry()
        }
    }
}
