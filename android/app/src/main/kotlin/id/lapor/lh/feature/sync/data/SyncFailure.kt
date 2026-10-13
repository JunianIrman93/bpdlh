package id.lapor.lh.feature.sync.data

import id.lapor.lh.core.network.ApiException
import id.lapor.lh.feature.auth.data.DEVICE_KEY_INVALID

const val MAX_SYNC_ATTEMPTS = 8
const val FILE_MISSING = "FILE_MISSING"
const val REPORT_NOT_FOUND = "REPORT_NOT_FOUND"
const val CHECKSUM_MISMATCH = "CHECKSUM_MISMATCH"

enum class FailureKind {
    TRANSIENT,
    PERMANENT,
    SESSION,
}

private val permanentCodes =
    setOf(
        "VALIDATION_ERROR",
        "FORBIDDEN",
        "NOT_FOUND",
        REPORT_NOT_FOUND,
        "ATTACHMENT_NOT_FOUND",
        "REPORT_LOCKED",
        "INVALID_STATE",
        "PAYLOAD_TOO_LARGE",
        "UNSUPPORTED_MEDIA_TYPE",
        CHECKSUM_MISMATCH,
        FILE_MISSING,
    )

fun ApiException.failureKind(): FailureKind =
    when (code) {
        DEVICE_KEY_INVALID -> FailureKind.SESSION
        in permanentCodes -> FailureKind.PERMANENT
        else -> FailureKind.TRANSIENT
    }
