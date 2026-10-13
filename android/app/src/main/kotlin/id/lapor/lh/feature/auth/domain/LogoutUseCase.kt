package id.lapor.lh.feature.auth.domain

import id.lapor.lh.feature.report.domain.ReportRepository
import id.lapor.lh.feature.sync.domain.SyncRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LogoutUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val reportRepository: ReportRepository,
        private val syncRepository: SyncRepository,
    ) {
        suspend operator fun invoke() =
            withContext(NonCancellable) {
                authRepository.revokeDevice()
                syncRepository.withSyncStopped { reportRepository.clearLocalData() }
                authRepository.clearLocalSession()
            }
    }
