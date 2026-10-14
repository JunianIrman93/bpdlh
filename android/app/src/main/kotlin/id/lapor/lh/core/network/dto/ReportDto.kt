package id.lapor.lh.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReportInputDto(
    val title: String,
    val description: String,
    val category: String,
    @SerialName("activity_date") val activityDate: String,
    val location: LocationDto,
    @SerialName("client_created_at") val clientCreatedAt: String,
    val attachments: List<AttachmentManifestDto>,
)

@Serializable
data class LocationDto(
    val latitude: Double,
    val longitude: Double,
    @SerialName("accuracy_m") val accuracyM: Float,
    @SerialName("captured_at") val capturedAt: String,
    @SerialName("is_mock") val isMock: Boolean,
    val address: String? = null,
)

@Serializable
data class AttachmentManifestDto(
    val id: String,
    val kind: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    val sha256: String,
)

@Serializable
data class ReportReceiptDto(
    val id: String,
    val status: String,
    val version: Int,
    val attachments: List<AttachmentRefDto>,
)

@Serializable
data class AttachmentRefDto(
    val id: String,
    val state: String,
)

@Serializable
data class AttachmentReceiptDto(
    val id: String,
    val state: String,
    @SerialName("report_status") val reportStatus: String,
)

@Serializable
data class ReportListDto(
    val items: List<ReportSummaryDto>,
    @SerialName("next_cursor") val nextCursor: String?,
    @SerialName("server_time") val serverTime: String,
)

@Serializable
data class ReportSummaryDto(
    val id: String,
    val title: String,
    val category: String,
    @SerialName("activity_date") val activityDate: String,
    val status: String,
    @SerialName("review_note") val reviewNote: String? = null,
    val version: Int,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ReportDetailDto(
    val id: String,
    val status: String,
    @SerialName("review_note") val reviewNote: String? = null,
    val version: Int,
    val description: String,
    val location: LocationDto,
    @SerialName("client_created_at") val clientCreatedAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val attachments: List<AttachmentDetailDto>,
)

@Serializable
data class AttachmentDetailDto(
    val id: String,
    val kind: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    val sha256: String,
    val state: String,
)
