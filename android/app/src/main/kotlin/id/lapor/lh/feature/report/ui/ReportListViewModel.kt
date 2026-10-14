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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

private const val SYNC_START_TIMEOUT_MS = 5_000L
private const val SYNC_FINISH_TIMEOUT_MS = 60_000L
private const val PAGE_SIZE = 10

data class ReportListUiState(
    val userName: String? = null,
    val reports: List<Report> = emptyList(),
    val hasMore: Boolean = false,
    val filter: ReportStatus? = null,
    val hasAnyReport: Boolean = false,
    val pendingCount: Int = 0,
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoaded: Boolean = false,
    val isInitialLoad: Boolean = false,
)

private data class LoadSignals(
    val sync: SyncStatus,
    val lastSyncAt: Long?,
    val awaitingFirstSync: Boolean,
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
        private val limit = MutableStateFlow(PAGE_SIZE)
        private val window = combine(filter, limit, ::Pair)
        private val refreshing = MutableStateFlow(false)
        private val awaitingFirstSync = MutableStateFlow(true)
        private val syncStatus =
            syncRepository
                .observeStatus()
                .stateIn(viewModelScope, SharingStarted.Eagerly, SyncStatus(isOnline = true, isSyncing = false))
        private val loadSignals =
            combine(syncStatus, syncRepository.observeLastSyncAt(), awaitingFirstSync) { sync, lastSyncAt, awaiting ->
                LoadSignals(sync, lastSyncAt, awaiting)
            }

        init {
            viewModelScope.launch {
                val started = withTimeoutOrNull(SYNC_START_TIMEOUT_MS) { syncStatus.first { it.isSyncing } }
                if (started != null) syncStatus.first { !it.isSyncing }
                awaitingFirstSync.value = false
            }
        }

        val uiState: StateFlow<ReportListUiState> =
            combine(
                reportRepository.observeReports(),
                authRepository.observeCurrentUser(),
                window,
                loadSignals,
                refreshing,
            ) { reports, user, (selected, shownCount), signals, isRefreshing ->
                val sync = signals.sync
                val filtered = if (selected == null) reports else reports.filter { it.status == selected }
                ReportListUiState(
                    userName = user?.fullName,
                    reports = filtered.take(shownCount),
                    hasMore = filtered.size > shownCount,
                    filter = selected,
                    hasAnyReport = reports.isNotEmpty(),
                    pendingCount = reports.count { it.status == ReportStatus.WAITING_NETWORK || it.status == ReportStatus.SYNCING },
                    isOnline = sync.isOnline,
                    isSyncing = sync.isSyncing,
                    isRefreshing = isRefreshing,
                    isLoaded = true,
                    isInitialLoad =
                        reports.isEmpty() &&
                            signals.lastSyncAt == null &&
                            sync.isOnline &&
                            (sync.isSyncing || signals.awaitingFirstSync),
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportListUiState())

        fun onFilterChange(status: ReportStatus?) {
            filter.value = status
            limit.value = PAGE_SIZE
        }

        fun loadMore() {
            if (uiState.value.hasMore) limit.update { it + PAGE_SIZE }
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
