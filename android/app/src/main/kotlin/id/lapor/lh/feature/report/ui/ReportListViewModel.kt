package id.lapor.lh.feature.report.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.lapor.lh.feature.auth.domain.AuthRepository
import id.lapor.lh.feature.report.domain.Report
import id.lapor.lh.feature.report.domain.ReportRepository
import id.lapor.lh.feature.report.domain.ReportStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ReportListUiState(
    val userName: String? = null,
    val reports: List<Report> = emptyList(),
    val filter: ReportStatus? = null,
    val hasAnyReport: Boolean = false,
    val isLoaded: Boolean = false,
)

@HiltViewModel
class ReportListViewModel
    @Inject
    constructor(
        reportRepository: ReportRepository,
        authRepository: AuthRepository,
    ) : ViewModel() {
        private val filter = MutableStateFlow<ReportStatus?>(null)

        val uiState: StateFlow<ReportListUiState> =
            combine(reportRepository.observeReports(), authRepository.observeCurrentUser(), filter) { reports, user, selected ->
                ReportListUiState(
                    userName = user?.fullName,
                    reports = if (selected == null) reports else reports.filter { it.status == selected },
                    filter = selected,
                    hasAnyReport = reports.isNotEmpty(),
                    isLoaded = true,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportListUiState())

        fun onFilterChange(status: ReportStatus?) {
            filter.value = status
        }
    }
