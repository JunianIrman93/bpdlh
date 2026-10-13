package id.lapor.lh.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.lapor.lh.feature.report.data.AndroidLocationProvider
import id.lapor.lh.feature.report.data.ReportRepositoryImpl
import id.lapor.lh.feature.report.domain.LocationProvider
import id.lapor.lh.feature.report.domain.ReportRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReportModule {
    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    @Binds
    abstract fun bindLocationProvider(impl: AndroidLocationProvider): LocationProvider
}
