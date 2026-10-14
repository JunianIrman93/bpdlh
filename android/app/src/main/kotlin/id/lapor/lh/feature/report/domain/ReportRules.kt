package id.lapor.lh.feature.report.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class ReportField {
    TITLE,
    DESCRIPTION,
    CATEGORY,
    ACTIVITY_DATE,
    LOCATION,
    ATTACHMENTS,
}

enum class ReportError {
    TITLE_LENGTH,
    DESCRIPTION_LENGTH,
    CATEGORY_REQUIRED,
    DATE_REQUIRED,
    DATE_IN_FUTURE,
    DATE_TOO_OLD,
    LOCATION_REQUIRED,
    PHOTO_REQUIRED,
    TOO_MANY_ATTACHMENTS,
    TOTAL_TOO_LARGE,
}

enum class AttachmentError {
    TOO_MANY,
    UNSUPPORTED_TYPE,
    FILE_TOO_LARGE,
    TOTAL_TOO_LARGE,
}

object ReportRules {
    const val TITLE_MIN = 5
    const val TITLE_MAX = 100
    const val DESCRIPTION_MIN = 20
    const val DESCRIPTION_MAX = 500
    const val MAX_ACTIVITY_AGE_DAYS = 90L
    const val MAX_ATTACHMENTS = 10
    const val MAX_FILE_BYTES = 2L * 1024 * 1024
    const val MAX_TOTAL_BYTES = 30L * 1024 * 1024
    const val POOR_ACCURACY_M = 100f

    val zone: ZoneId = ZoneId.of("Asia/Jakarta")

    private val imageTypes = setOf("image/jpeg", "image/png", "image/webp")

    val mimeTypesByKind: Map<AttachmentKind, Set<String>> =
        mapOf(
            AttachmentKind.PHOTO to imageTypes,
            AttachmentKind.IMAGE to imageTypes,
            AttachmentKind.DOCUMENT to
                setOf(
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                ),
            AttachmentKind.FILE to setOf("text/plain", "text/csv"),
        )

    val documentMimeTypes: Set<String> =
        mimeTypesByKind.getValue(AttachmentKind.DOCUMENT) + mimeTypesByKind.getValue(AttachmentKind.FILE)

    fun documentKindOf(mimeType: String): AttachmentKind? =
        listOf(AttachmentKind.DOCUMENT, AttachmentKind.FILE).firstOrNull { mimeType in mimeTypesByKind.getValue(it) }

    fun today(nowMillis: Long): LocalDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()

    fun earliestActivityDate(today: LocalDate): LocalDate = today.minusDays(MAX_ACTIVITY_AGE_DAYS)

    fun checkNewAttachment(
        existing: List<Attachment>,
        kind: AttachmentKind,
        mimeType: String,
        sizeBytes: Long,
    ): AttachmentError? =
        when {
            existing.size >= MAX_ATTACHMENTS -> AttachmentError.TOO_MANY
            mimeType !in mimeTypesByKind.getValue(kind) -> AttachmentError.UNSUPPORTED_TYPE
            sizeBytes > MAX_FILE_BYTES -> AttachmentError.FILE_TOO_LARGE
            existing.sumOf { it.sizeBytes } + sizeBytes > MAX_TOTAL_BYTES -> AttachmentError.TOTAL_TOO_LARGE
            else -> null
        }

    fun validateForSubmit(
        draft: ReportDraft,
        attachments: List<Attachment>,
        today: LocalDate,
    ): Map<ReportField, ReportError> {
        val errors = mutableMapOf<ReportField, ReportError>()
        if (draft.title.trim().length !in TITLE_MIN..TITLE_MAX) errors[ReportField.TITLE] = ReportError.TITLE_LENGTH
        if (draft.description.trim().length !in DESCRIPTION_MIN..DESCRIPTION_MAX) {
            errors[ReportField.DESCRIPTION] = ReportError.DESCRIPTION_LENGTH
        }
        if (draft.category == null) errors[ReportField.CATEGORY] = ReportError.CATEGORY_REQUIRED
        val date = draft.activityDate
        when {
            date == null -> errors[ReportField.ACTIVITY_DATE] = ReportError.DATE_REQUIRED
            date.isAfter(today) -> errors[ReportField.ACTIVITY_DATE] = ReportError.DATE_IN_FUTURE
            date.isBefore(earliestActivityDate(today)) -> errors[ReportField.ACTIVITY_DATE] = ReportError.DATE_TOO_OLD
        }
        if (draft.location == null) errors[ReportField.LOCATION] = ReportError.LOCATION_REQUIRED
        when {
            attachments.size > MAX_ATTACHMENTS -> {
                errors[ReportField.ATTACHMENTS] = ReportError.TOO_MANY_ATTACHMENTS
            }

            attachments.sumOf { it.sizeBytes } > MAX_TOTAL_BYTES -> {
                errors[ReportField.ATTACHMENTS] = ReportError.TOTAL_TOO_LARGE
            }

            attachments.none { it.kind == AttachmentKind.PHOTO || it.kind == AttachmentKind.IMAGE } -> {
                errors[ReportField.ATTACHMENTS] = ReportError.PHOTO_REQUIRED
            }
        }
        return errors
    }
}
