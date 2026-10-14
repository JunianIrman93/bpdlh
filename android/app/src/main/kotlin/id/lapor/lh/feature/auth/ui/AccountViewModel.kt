package id.lapor.lh.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.lapor.lh.core.settings.ThemeSettings
import id.lapor.lh.core.ui.ThemeMode
import id.lapor.lh.feature.auth.domain.AuthRepository
import id.lapor.lh.feature.auth.domain.LogoutUseCase
import id.lapor.lh.feature.auth.domain.User
import id.lapor.lh.feature.report.domain.ReportRepository
import id.lapor.lh.feature.report.domain.ReportStatus
import id.lapor.lh.feature.sync.domain.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val unsentStatuses =
    setOf(ReportStatus.DRAFT, ReportStatus.WAITING_NETWORK, ReportStatus.SYNCING, ReportStatus.FAILED)

data class AccountUiState(
    val user: User? = null,
    val unsentCount: Int = 0,
    val lastSyncAt: Long? = null,
    val isLoggingOut: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

@HiltViewModel
class AccountViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
        reportRepository: ReportRepository,
        syncRepository: SyncRepository,
        private val logoutUseCase: LogoutUseCase,
        private val themeSettings: ThemeSettings,
    ) : ViewModel() {
        private val loggingOut = MutableStateFlow(false)

        val uiState: StateFlow<AccountUiState> =
            combine(
                authRepository.observeCurrentUser(),
                reportRepository.observeReports(),
                syncRepository.observeLastSyncAt(),
                loggingOut,
                themeSettings.themeMode,
            ) { user, reports, lastSyncAt, isLoggingOut, themeMode ->
                AccountUiState(
                    user = user,
                    unsentCount = reports.count { it.status in unsentStatuses },
                    lastSyncAt = lastSyncAt,
                    isLoggingOut = isLoggingOut,
                    themeMode = themeMode,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountUiState())

        fun setThemeMode(mode: ThemeMode) {
            viewModelScope.launch { themeSettings.setThemeMode(mode) }
        }

        fun logout() {
            if (loggingOut.value) return
            loggingOut.value = true
            viewModelScope.launch { logoutUseCase() }
        }
    }
