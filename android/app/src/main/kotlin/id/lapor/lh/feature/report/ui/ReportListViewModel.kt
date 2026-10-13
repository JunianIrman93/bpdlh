package id.lapor.lh.feature.report.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.lapor.lh.feature.auth.domain.AuthRepository
import id.lapor.lh.feature.report.domain.Report
import id.lapor.lh.feature.report.domain.ReportRepository
import id.lapor.lh.feature.report.domain.ReportStatus
import id.lapor.lh.feature.sync.domain.SyncRepository
import id.lapor.lh.feature.sync.domain.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

private const val SYNC_START_TIMEOUT_MS = 5_000L
private const val SYNC_FINISH_TIMEOUT_MS = 60_000L

data class ReportListUiState(
    val userName: String? = null,
    val reports: List<Report> = emptyList(),
    val filter: ReportStatus? = null,
    val hasAnyReport: Boolean = false,
    val pendingCount: Int = 0,
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoaded: Boolean = false,
)

@HiltViewModel
class ReportListViewModel
    @Inject
    constructor(
        reportRepository: ReportRepository,
        authRepository: AuthRepository,
        private val syncRepository: SyncRepository,
    ) : ViewModel() {
        private val filter = MutableStateFlow<ReportStatus?>(null)
        private val refreshing = MutableStateFlow(false)
        private val syncStatus =
            syncRepository
                .observeStatus()
                .stateIn(viewModelScope, SharingStarted.Eagerly, SyncStatus(isOnline = true, isSyncing = false))

        val uiState: StateFlow<ReportListUiState> =
            combine(
                reportRepository.observeReports(),
                authRepository.observeCurrentUser(),
                filter,
                syncStatus,
                refreshing,
            ) { reports, user, selected, sync, isRefreshing ->
                ReportListUiState(
                    userName = user?.fullName,
                    reports = if (selected == null) reports else reports.filter { it.status == selected },
                    filter = selected,
                    hasAnyReport = reports.isNotEmpty(),
                    pendingCount = reports.count { it.status == ReportStatus.WAITING_NETWORK || it.status == ReportStatus.SYNCING },
                    isOnline = sync.isOnline,
                    isSyncing = sync.isSyncing,
                    isRefreshing = isRefreshing,
                    isLoaded = true,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportListUiState())

        fun onFilterChange(status: ReportStatus?) {
            filter.value = status
        }

        fun refresh() {
            if (refreshing.value) return
            refreshing.value = true
            viewModelScope.launch {
                try {
                    syncRepository.requestSync()
                    if (syncStatus.value.isOnline) {
                        val started = withTimeoutOrNull(SYNC_START_TIMEOUT_MS) { syncStatus.first { it.isSyncing } }
                        if (started != null) withTimeoutOrNull(SYNC_FINISH_TIMEOUT_MS) { syncStatus.first { !it.isSyncing } }
                    }
                } finally {
                    refreshing.value = false
                }
            }
        }
    }
