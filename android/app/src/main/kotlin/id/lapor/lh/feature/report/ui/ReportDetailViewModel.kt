package id.lapor.lh.feature.report.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import id.lapor.lh.feature.report.domain.Report
import id.lapor.lh.feature.report.domain.ReportRepository
import id.lapor.lh.navigation.ReportDetailDestination
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: ReportRepository,
    ) : ViewModel() {
        private val reportId = savedStateHandle.toRoute<ReportDetailDestination>().reportId

        val report: StateFlow<Report?> =
            repository.observeReport(reportId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        fun cancelQueued() {
            viewModelScope.launch { repository.cancelQueued(reportId) }
        }

        fun retry() {
            viewModelScope.launch { repository.retryFailed(reportId) }
        }

        fun edit(onReady: () -> Unit) {
            viewModelScope.launch {
                repository.editFailed(reportId)
                onReady()
            }
        }
    }
