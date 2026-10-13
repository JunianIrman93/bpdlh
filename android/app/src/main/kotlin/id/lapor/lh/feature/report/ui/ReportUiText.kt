package id.lapor.lh.feature.report.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.lapor.lh.R
import id.lapor.lh.feature.report.domain.AttachmentError
import id.lapor.lh.feature.report.domain.Category
import id.lapor.lh.feature.report.domain.FailureReason
import id.lapor.lh.feature.report.domain.Report
import id.lapor.lh.feature.report.domain.ReportError
import id.lapor.lh.feature.report.domain.ReportRules
import id.lapor.lh.feature.report.domain.ReportStatus
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val indonesian: Locale = Locale.forLanguageTag("id-ID")
private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", indonesian)
private val dateTimeFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH.mm 'WIB'", indonesian).withZone(ReportRules.zone)

fun LocalDate.display(): String = dateFormat.format(this)

fun Long.displayDateTime(): String = dateTimeFormat.format(Instant.ofEpochMilli(this))

@StringRes
fun Category.labelRes(): Int =
    when (this) {
        Category.REHABILITASI_HUTAN_LAHAN -> R.string.category_rehabilitasi_hutan_lahan
        Category.MANGROVE_PESISIR -> R.string.category_mangrove_pesisir
        Category.PENGELOLAAN_SAMPAH -> R.string.category_pengelolaan_sampah
        Category.ENERGI_TERBARUKAN -> R.string.category_energi_terbarukan
        Category.PEMBERDAYAAN_MASYARAKAT -> R.string.category_pemberdayaan_masyarakat
        Category.LAINNYA -> R.string.category_lainnya
    }

@StringRes
fun ReportStatus.labelRes(): Int =
    when (this) {
        ReportStatus.DRAFT -> R.string.status_draft
        ReportStatus.WAITING_NETWORK -> R.string.status_waiting_network
        ReportStatus.SYNCING -> R.string.status_syncing
        ReportStatus.FAILED -> R.string.status_failed
        ReportStatus.SENT -> R.string.status_sent
        ReportStatus.VERIFIED -> R.string.status_verified
        ReportStatus.REJECTED -> R.string.status_rejected
    }

@StringRes
fun ReportError.messageRes(): Int =
    when (this) {
        ReportError.TITLE_LENGTH -> R.string.form_error_title
        ReportError.DESCRIPTION_LENGTH -> R.string.form_error_description
        ReportError.CATEGORY_REQUIRED -> R.string.form_error_category
        ReportError.DATE_REQUIRED -> R.string.form_error_date_required
        ReportError.DATE_IN_FUTURE -> R.string.form_error_date_future
        ReportError.DATE_TOO_OLD -> R.string.form_error_date_too_old
        ReportError.LOCATION_REQUIRED -> R.string.form_error_location
        ReportError.PHOTO_REQUIRED -> R.string.form_error_photo
        ReportError.TOO_MANY_ATTACHMENTS -> R.string.form_attachment_too_many
        ReportError.TOTAL_TOO_LARGE -> R.string.form_attachment_total_too_large
    }

@StringRes
fun AttachmentError.messageRes(): Int =
    when (this) {
        AttachmentError.TOO_MANY -> R.string.form_attachment_too_many
        AttachmentError.UNSUPPORTED_TYPE -> R.string.form_attachment_unsupported
        AttachmentError.FILE_TOO_LARGE -> R.string.form_attachment_too_large
        AttachmentError.TOTAL_TOO_LARGE -> R.string.form_attachment_total_too_large
    }

@StringRes
fun FailureReason.messageRes(): Int =
    when (this) {
        FailureReason.NETWORK -> R.string.failure_network
        FailureReason.SERVER -> R.string.failure_server
        FailureReason.INVALID_CONTENT -> R.string.failure_invalid_content
        FailureReason.LOCKED -> R.string.failure_locked
        FailureReason.ATTACHMENT -> R.string.failure_attachment
        FailureReason.NOT_ALLOWED -> R.string.failure_not_allowed
    }

@Composable
fun UploadProgress(
    report: Report,
    modifier: Modifier = Modifier,
) {
    val total = report.attachments.size
    if (total == 0) return
    val uploaded = report.uploadedCount
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.sync_attachment_progress, uploaded, total),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(progress = { uploaded.toFloat() / total }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun StatusBadge(
    status: ReportStatus,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (container, content) =
        when (status) {
            ReportStatus.DRAFT -> colors.surfaceVariant to colors.onSurfaceVariant
            ReportStatus.WAITING_NETWORK, ReportStatus.SYNCING -> colors.secondaryContainer to colors.onSecondaryContainer
            ReportStatus.FAILED, ReportStatus.REJECTED -> colors.errorContainer to colors.onErrorContainer
            ReportStatus.SENT -> colors.primaryContainer to colors.onPrimaryContainer
            ReportStatus.VERIFIED -> colors.tertiaryContainer to colors.onTertiaryContainer
        }
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(
            text = stringResource(status.labelRes()),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
