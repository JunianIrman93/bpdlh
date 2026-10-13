package id.lapor.lh.feature.report.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import id.lapor.lh.feature.report.domain.DetailLoadResult
import id.lapor.lh.feature.report.domain.Report
import id.lapor.lh.feature.report.domain.ReportRepository
import id.lapor.lh.navigation.ReportDetailDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DetailLoadState {
    IDLE,
    LOADING,
    NETWORK_ERROR,
    FAILED,
}

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

        private val _detailLoad = MutableStateFlow(DetailLoadState.IDLE)
        val detailLoad: StateFlow<DetailLoadState> = _detailLoad.asStateFlow()

        init {
            viewModelScope.launch {
                if (repository.getReport(reportId)?.isSummaryOnly == true) loadDetail()
            }
        }

        fun loadDetail() {
            if (_detailLoad.value == DetailLoadState.LOADING) return
            _detailLoad.value = DetailLoadState.LOADING
            viewModelScope.launch {
                _detailLoad.value =
                    when (repository.loadServerDetail(reportId)) {
                        DetailLoadResult.LOADED -> DetailLoadState.IDLE
                        DetailLoadResult.NETWORK_ERROR -> DetailLoadState.NETWORK_ERROR
                        DetailLoadResult.FAILED -> DetailLoadState.FAILED
                    }
            }
        }

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
