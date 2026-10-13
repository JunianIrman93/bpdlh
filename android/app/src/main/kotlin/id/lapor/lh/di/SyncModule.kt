package id.lapor.lh.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.lapor.lh.feature.sync.data.WorkManagerSyncRepository
import id.lapor.lh.feature.sync.domain.SyncRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: WorkManagerSyncRepository): SyncRepository
}
