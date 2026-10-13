package id.lapor.lh.feature.report.data

import id.lapor.lh.core.database.AttachmentEntity
import id.lapor.lh.core.database.ReportWithAttachments
import id.lapor.lh.feature.report.domain.Attachment
import id.lapor.lh.feature.report.domain.AttachmentKind
import id.lapor.lh.feature.report.domain.Category
import id.lapor.lh.feature.report.domain.Report
import id.lapor.lh.feature.report.domain.ReportDraft
import id.lapor.lh.feature.report.domain.ReportLocation
import id.lapor.lh.feature.report.domain.ServerStatus
import id.lapor.lh.feature.report.domain.SyncState
import java.time.LocalDate

internal fun ReportWithAttachments.toDomain(): Report {
    val latitude = report.latitude
    val longitude = report.longitude
    val location =
        if (latitude != null && longitude != null) {
            ReportLocation(
                latitude = latitude,
                longitude = longitude,
                accuracyM = report.accuracyM ?: 0f,
                capturedAt = report.locationCapturedAt ?: 0L,
                isMock = report.isMockLocation ?: false,
            )
        } else {
            null
        }
    return Report(
        draft =
            ReportDraft(
                id = report.id,
                title = report.title,
                description = report.description,
                category = report.category?.let(Category::valueOf),
                activityDate = report.activityDate?.let(LocalDate::parse),
                location = location,
            ),
        attachments = attachments.sortedBy { it.createdAt }.map { it.toDomain() },
        syncState = SyncState.valueOf(report.syncState),
        serverStatus = report.serverStatus?.let(ServerStatus::valueOf),
        reviewNote = report.reviewNote,
        lastError = report.lastError,
        updatedAt = report.updatedAt,
    )
}

private fun AttachmentEntity.toDomain() =
    Attachment(
        id = id,
        kind = AttachmentKind.valueOf(kind),
        fileName = fileName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        localPath = localPath,
    )
