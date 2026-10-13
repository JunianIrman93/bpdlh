package id.lapor.lh.feature.report.ui

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import id.lapor.lh.R
import id.lapor.lh.feature.report.domain.AddAttachmentResult
import id.lapor.lh.feature.report.domain.Attachment
import id.lapor.lh.feature.report.domain.AttachmentKind
import id.lapor.lh.feature.report.domain.Category
import id.lapor.lh.feature.report.domain.LocationProvider
import id.lapor.lh.feature.report.domain.LocationResult
import id.lapor.lh.feature.report.domain.ReportDraft
import id.lapor.lh.feature.report.domain.ReportError
import id.lapor.lh.feature.report.domain.ReportField
import id.lapor.lh.feature.report.domain.ReportRepository
import id.lapor.lh.feature.report.domain.SyncState
import id.lapor.lh.navigation.ReportFormDestination
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class LocationProblem {
    DENIED,
    DISABLED,
    TIMEOUT,
}

data class ReportFormUiState(
    val draft: ReportDraft,
    val attachments: List<Attachment> = emptyList(),
    val isNew: Boolean = true,
    val isExisting: Boolean = false,
    val isLoaded: Boolean = false,
    val errors: Map<ReportField, ReportError> = emptyMap(),
    val isLocating: Boolean = false,
    val locationProblem: LocationProblem? = null,
    val isAddingAttachment: Boolean = false,
    val isBusy: Boolean = false,
)

sealed interface ReportFormEvent {
    data object Exit : ReportFormEvent

    data class Message(
        @StringRes val res: Int,
    ) : ReportFormEvent
}

private const val CAMERA_URI_KEY = "camera_uri"

@HiltViewModel
class ReportFormViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val repository: ReportRepository,
        private val locationProvider: LocationProvider,
    ) : ViewModel() {
        private val reportId = savedStateHandle.toRoute<ReportFormDestination>().reportId
        private val _uiState = MutableStateFlow(ReportFormUiState(draft = ReportDraft(id = reportId)))
        val uiState: StateFlow<ReportFormUiState> = _uiState.asStateFlow()

        private val events = Channel<ReportFormEvent>(Channel.BUFFERED)
        val eventFlow: Flow<ReportFormEvent> = events.receiveAsFlow()

        private var deleted = false

        init {
            viewModelScope.launch {
                val existing = repository.getReport(reportId)
                if (existing != null && existing.syncState != SyncState.DRAFT) {
                    events.send(ReportFormEvent.Exit)
                    return@launch
                }
                _uiState.update {
                    it.copy(draft = existing?.draft ?: it.draft, isNew = existing == null, isExisting = existing != null, isLoaded = true)
                }
                repository.observeReport(reportId).collect { report ->
                    _uiState.update { it.copy(attachments = report?.attachments.orEmpty(), isExisting = report != null) }
                }
            }
        }

        fun onTitleChange(value: String) = edit(ReportField.TITLE) { copy(title = value) }

        fun onDescriptionChange(value: String) = edit(ReportField.DESCRIPTION) { copy(description = value) }

        fun onCategoryChange(value: Category) = edit(ReportField.CATEGORY) { copy(category = value) }

        fun onActivityDateChange(value: LocalDate) = edit(ReportField.ACTIVITY_DATE) { copy(activityDate = value) }

        fun captureLocation() {
            if (_uiState.value.isLocating) return
            _uiState.update { it.copy(isLocating = true, locationProblem = null) }
            viewModelScope.launch {
                val result = locationProvider.currentLocation()
                _uiState.update { state ->
                    when (result) {
                        is LocationResult.Found -> {
                            state.copy(
                                isLocating = false,
                                draft = state.draft.copy(location = result.location),
                                errors = state.errors - ReportField.LOCATION,
                            )
                        }

                        LocationResult.PermissionDenied -> {
                            state.copy(isLocating = false, locationProblem = LocationProblem.DENIED)
                        }

                        LocationResult.Disabled -> {
                            state.copy(isLocating = false, locationProblem = LocationProblem.DISABLED)
                        }

                        LocationResult.Timeout -> {
                            state.copy(isLocating = false, locationProblem = LocationProblem.TIMEOUT)
                        }
                    }
                }
            }
        }

        fun onLocationPermissionDenied() {
            _uiState.update { it.copy(locationProblem = LocationProblem.DENIED) }
        }

        fun newCameraUri(): String = repository.newCameraCaptureUri().also { savedStateHandle[CAMERA_URI_KEY] = it }

        fun onPhotoCaptured(success: Boolean) {
            val uri = savedStateHandle.get<String>(CAMERA_URI_KEY) ?: return
            savedStateHandle.remove<String>(CAMERA_URI_KEY)
            if (success) addAttachment(AttachmentKind.PHOTO, uri)
        }

        fun addAttachment(
            kind: AttachmentKind,
            uri: String,
        ) {
            _uiState.update { it.copy(isAddingAttachment = true) }
            viewModelScope.launch {
                val result = repository.addAttachment(_uiState.value.draft, kind, uri)
                _uiState.update { state ->
                    state.copy(
                        isAddingAttachment = false,
                        errors = if (result is AddAttachmentResult.Added) state.errors - ReportField.ATTACHMENTS else state.errors,
                    )
                }
                when (result) {
                    AddAttachmentResult.Added -> Unit
                    is AddAttachmentResult.Rejected -> events.send(ReportFormEvent.Message(result.error.messageRes()))
                    AddAttachmentResult.Failed -> events.send(ReportFormEvent.Message(R.string.form_attachment_failed))
                }
            }
        }

        fun removeAttachment(attachmentId: String) {
            viewModelScope.launch { repository.removeAttachment(reportId, attachmentId) }
        }

        fun saveDraft() {
            if (deleted || !_uiState.value.hasContent()) return
            viewModelScope.launch { repository.saveDraft(_uiState.value.draft) }
        }

        fun saveAndExit() {
            if (_uiState.value.isBusy) return
            _uiState.update { it.copy(isBusy = true) }
            viewModelScope.launch {
                if (!deleted && _uiState.value.hasContent()) repository.saveDraft(_uiState.value.draft)
                events.send(ReportFormEvent.Exit)
            }
        }

        fun submit() {
            if (_uiState.value.isBusy) return
            _uiState.update { it.copy(isBusy = true) }
            viewModelScope.launch {
                val errors = repository.submit(_uiState.value.draft)
                _uiState.update { it.copy(isBusy = false, errors = errors) }
                if (errors.isEmpty()) {
                    events.send(ReportFormEvent.Exit)
                } else {
                    events.send(ReportFormEvent.Message(R.string.form_fix_errors))
                }
            }
        }

        fun deleteDraft() {
            if (_uiState.value.isBusy) return
            deleted = true
            _uiState.update { it.copy(isBusy = true) }
            viewModelScope.launch {
                repository.deleteDraft(reportId)
                events.send(ReportFormEvent.Exit)
            }
        }

        private fun edit(
            field: ReportField,
            change: ReportDraft.() -> ReportDraft,
        ) {
            _uiState.update { it.copy(draft = it.draft.change(), errors = it.errors - field) }
        }
    }

private fun ReportFormUiState.hasContent(): Boolean =
    isExisting ||
        attachments.isNotEmpty() ||
        draft.title.isNotBlank() ||
        draft.description.isNotBlank() ||
        draft.category != null ||
        draft.activityDate != null ||
        draft.location != null
