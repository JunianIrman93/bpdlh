package id.lapor.lh.feature.report.domain

import kotlinx.coroutines.flow.Flow

sealed interface AddAttachmentResult {
    data object Added : AddAttachmentResult

    data class Rejected(
        val error: AttachmentError,
    ) : AddAttachmentResult

    data object Failed : AddAttachmentResult
}

enum class DetailLoadResult {
    LOADED,
    NETWORK_ERROR,
    FAILED,
}

interface ReportRepository {
    fun observeReports(): Flow<List<Report>>

    fun observeReport(id: String): Flow<Report?>

    suspend fun getReport(id: String): Report?

    suspend fun loadServerDetail(id: String): DetailLoadResult

    suspend fun saveDraft(draft: ReportDraft)

    fun newCameraCaptureUri(): String

    suspend fun addAttachment(
        draft: ReportDraft,
        kind: AttachmentKind,
        sourceUri: String,
    ): AddAttachmentResult

    suspend fun removeAttachment(
        reportId: String,
        attachmentId: String,
    )

    suspend fun submit(draft: ReportDraft): Map<ReportField, ReportError>

    suspend fun cancelQueued(id: String)

    suspend fun retryFailed(id: String)

    suspend fun editFailed(id: String)

    suspend fun deleteDraft(id: String)

    suspend fun clearLocalData()
}
