package id.lapor.lh.feature.report.domain

import java.time.LocalDate

enum class Category {
    REHABILITASI_HUTAN_LAHAN,
    MANGROVE_PESISIR,
    PENGELOLAAN_SAMPAH,
    ENERGI_TERBARUKAN,
    PEMBERDAYAAN_MASYARAKAT,
    LAINNYA,
}

enum class AttachmentKind {
    PHOTO,
    IMAGE,
    DOCUMENT,
    FILE,
}

enum class SyncState {
    DRAFT,
    QUEUED,
    SYNCING,
    SYNCED,
    FAILED,
}

enum class ServerStatus {
    UPLOADING,
    SUBMITTED,
    VERIFIED,
    REJECTED,
}

enum class ReportStatus {
    DRAFT,
    WAITING_NETWORK,
    SYNCING,
    FAILED,
    SENT,
    VERIFIED,
    REJECTED,
}

enum class FailureReason {
    NETWORK,
    SERVER,
    INVALID_CONTENT,
    LOCKED,
    ATTACHMENT,
    NOT_ALLOWED,
}

data class ReportLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Float,
    val capturedAt: Long,
    val isMock: Boolean,
)

data class Attachment(
    val id: String,
    val kind: AttachmentKind,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localPath: String,
    val isUploaded: Boolean,
)

data class ReportDraft(
    val id: String,
    val title: String = "",
    val description: String = "",
    val category: Category? = null,
    val activityDate: LocalDate? = null,
    val location: ReportLocation? = null,
)

data class Report(
    val draft: ReportDraft,
    val attachments: List<Attachment>,
    val syncState: SyncState,
    val serverStatus: ServerStatus?,
    val reviewNote: String?,
    val failure: FailureReason?,
    val updatedAt: Long,
) {
    val id: String get() = draft.id

    val uploadedCount: Int get() = attachments.count { it.isUploaded }

    val status: ReportStatus
        get() =
            when (syncState) {
                SyncState.DRAFT -> {
                    ReportStatus.DRAFT
                }

                SyncState.QUEUED -> {
                    ReportStatus.WAITING_NETWORK
                }

                SyncState.SYNCING -> {
                    ReportStatus.SYNCING
                }

                SyncState.FAILED -> {
                    ReportStatus.FAILED
                }

                SyncState.SYNCED -> {
                    when (serverStatus) {
                        ServerStatus.VERIFIED -> ReportStatus.VERIFIED
                        ServerStatus.REJECTED -> ReportStatus.REJECTED
                        else -> ReportStatus.SENT
                    }
                }
            }
}
