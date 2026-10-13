package id.lapor.lh.feature.sync.data

import androidx.room.withTransaction
import id.lapor.lh.core.database.AppDatabase
import id.lapor.lh.core.database.AttachmentEntity
import id.lapor.lh.core.database.ReportEntity
import id.lapor.lh.core.database.ReportWithAttachments
import id.lapor.lh.core.database.SyncMetaEntity
import id.lapor.lh.core.network.ApiException
import id.lapor.lh.core.network.ReportApi
import id.lapor.lh.core.network.apiCall
import id.lapor.lh.core.network.dto.AttachmentManifestDto
import id.lapor.lh.core.network.dto.AttachmentReceiptDto
import id.lapor.lh.core.network.dto.LocationDto
import id.lapor.lh.core.network.dto.ReportInputDto
import id.lapor.lh.core.network.dto.ReportReceiptDto
import id.lapor.lh.core.network.dto.ReportSummaryDto
import id.lapor.lh.feature.auth.data.AuthLocalStore
import id.lapor.lh.feature.auth.data.TokenRefresher
import id.lapor.lh.feature.report.domain.SyncState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private const val PAGE_SIZE = 100
private const val PULL_OVERLAP_SECONDS = 60L
private const val LAST_PULL_KEY = "last_pull_server_time"
private const val LAST_SYNC_KEY = "last_sync_at"
private const val UPLOADED = "UPLOADED"

fun lastSyncKey(ownerId: String) = "$LAST_SYNC_KEY:$ownerId"

enum class SyncOutcome {
    DONE,
    RETRY,
}

@Singleton
class SyncEngine
    @Inject
    constructor(
        private val database: AppDatabase,
        private val api: ReportApi,
        private val authLocalStore: AuthLocalStore,
        private val tokenRefresher: TokenRefresher,
        private val json: Json,
    ) {
        private val dao = database.reportDao()
        private val metaDao = database.syncMetaDao()
        private val mutex = Mutex()

        suspend fun run(): SyncOutcome =
            mutex.withLock {
                val ownerId = authLocalStore.currentUser()?.id ?: return@withLock SyncOutcome.DONE
                dao.requeueInterrupted(ownerId)
                try {
                    tokenRefresher.ensureFresh()
                    var retry = false
                    for (row in dao.getQueued(ownerId)) {
                        if (push(ownerId, row) == FailureKind.TRANSIENT) retry = true
                    }
                    pull(ownerId)
                    metaDao.put(SyncMetaEntity(lastSyncKey(ownerId), System.currentTimeMillis().toString()))
                    if (retry) SyncOutcome.RETRY else SyncOutcome.DONE
                } catch (e: ApiException) {
                    if (kindOf(e) == FailureKind.TRANSIENT) SyncOutcome.RETRY else SyncOutcome.DONE
                }
            }

        suspend fun <T> exclusive(block: suspend () -> T): T = mutex.withLock { block() }

        private suspend fun push(
            ownerId: String,
            row: ReportWithAttachments,
        ): FailureKind? {
            val report = row.report
            if (dao.claim(report.id, ownerId) == 0) return null
            try {
                send(ownerId, row, resendAllowed = true)
                return null
            } catch (e: CancellationException) {
                withContext(NonCancellable) { release(ownerId, report, SyncState.QUEUED, report.syncAttempts, report.lastError) }
                throw e
            } catch (e: ApiException) {
                val kind = kindOf(e)
                when (kind) {
                    FailureKind.SESSION -> {
                        release(ownerId, report, SyncState.QUEUED, report.syncAttempts, report.lastError)
                        throw e
                    }

                    FailureKind.PERMANENT -> {
                        release(ownerId, report, SyncState.FAILED, report.syncAttempts + 1, e.code)
                    }

                    FailureKind.TRANSIENT -> {
                        val attempts = report.syncAttempts + 1
                        val next = if (attempts >= MAX_SYNC_ATTEMPTS) SyncState.FAILED else SyncState.QUEUED
                        release(ownerId, report, next, attempts, e.code)
                    }
                }
                return kind
            }
        }

        private suspend fun send(
            ownerId: String,
            row: ReportWithAttachments,
            resendAllowed: Boolean,
        ) {
            val reportId = row.report.id
            val receipt = apiCall(json) { api.upsert(reportId, row.toInputDto()) }
            val onServer = receipt.uploadedIds()
            if (onServer.isNotEmpty()) dao.markAttachmentsUploaded(reportId, onServer.toList())

            var status = receipt.status
            for (attachment in row.attachments.sortedBy { it.createdAt }) {
                if (attachment.id in onServer) continue
                val uploaded =
                    try {
                        upload(reportId, attachment)
                    } catch (e: ApiException) {
                        if (e.code == REPORT_NOT_FOUND && resendAllowed) return send(ownerId, row, resendAllowed = false)
                        throw e
                    }
                dao.markAttachmentsUploaded(reportId, listOf(attachment.id))
                status = uploaded.reportStatus
            }
            dao.markSynced(reportId, ownerId, status, receipt.version, System.currentTimeMillis())
        }

        private suspend fun upload(
            reportId: String,
            attachment: AttachmentEntity,
        ): AttachmentReceiptDto {
            val file = File(attachment.localPath)
            if (!file.isFile) throw ApiException(FILE_MISSING, "local file missing")
            val part = MultipartBody.Part.createFormData("file", attachment.id, file.asRequestBody(attachment.mimeType.toMediaType()))
            val call = suspend { apiCall(json) { api.uploadAttachment(reportId, attachment.id, attachment.sha256, part) } }
            return try {
                call()
            } catch (e: ApiException) {
                if (e.code != CHECKSUM_MISMATCH) throw e
                call()
            }
        }

        private suspend fun pull(ownerId: String) {
            val key = "$LAST_PULL_KEY:$ownerId"
            val since = metaDao.get(key)
            var page = apiCall(json) { api.list(since, null, PAGE_SIZE) }
            val serverTime = Instant.parse(page.serverTime)
            while (true) {
                val items = page.items
                database.withTransaction { items.forEach { apply(ownerId, it) } }
                val cursor = page.nextCursor ?: break
                page = apiCall(json) { api.list(since, cursor, PAGE_SIZE) }
            }
            metaDao.put(SyncMetaEntity(key, serverTime.minusSeconds(PULL_OVERLAP_SECONDS).toString()))
        }

        private suspend fun apply(
            ownerId: String,
            summary: ReportSummaryDto,
        ) {
            val updatedAt = Instant.parse(summary.updatedAt).toEpochMilli()
            val changed = dao.applyServerStatus(summary.id, ownerId, summary.status, summary.reviewNote, summary.version, updatedAt)
            if (changed == 0) dao.insertReportIfAbsent(summary.toEntity(ownerId, updatedAt))
        }

        private suspend fun release(
            ownerId: String,
            report: ReportEntity,
            to: SyncState,
            attempts: Int,
            lastError: String?,
        ) {
            dao.releaseSyncing(report.id, ownerId, to.name, attempts, lastError, System.currentTimeMillis())
        }

        private suspend fun kindOf(e: ApiException): FailureKind =
            if (authLocalStore.currentUser() == null) FailureKind.SESSION else e.failureKind()
    }

private fun ReportReceiptDto.uploadedIds(): Set<String> = attachments.filter { it.state == UPLOADED }.map { it.id }.toSet()

private fun ReportWithAttachments.toInputDto(): ReportInputDto {
    val r = report
    if (r.category == null || r.activityDate == null || r.latitude == null || r.longitude == null) {
        throw ApiException("VALIDATION_ERROR", "incomplete report")
    }
    return ReportInputDto(
        title = r.title,
        description = r.description,
        category = r.category,
        activityDate = r.activityDate,
        location =
            LocationDto(
                latitude = r.latitude,
                longitude = r.longitude,
                accuracyM = r.accuracyM ?: 0f,
                capturedAt = Instant.ofEpochMilli(r.locationCapturedAt ?: r.clientCreatedAt).toString(),
                isMock = r.isMockLocation ?: false,
            ),
        clientCreatedAt = Instant.ofEpochMilli(r.clientCreatedAt).toString(),
        attachments =
            attachments.sortedBy { it.createdAt }.map {
                AttachmentManifestDto(
                    id = it.id,
                    kind = it.kind,
                    fileName = it.fileName,
                    mimeType = it.mimeType,
                    sizeBytes = it.sizeBytes,
                    sha256 = it.sha256,
                )
            },
    )
}

private fun ReportSummaryDto.toEntity(
    ownerId: String,
    updatedAt: Long,
) = ReportEntity(
    id = id,
    ownerId = ownerId,
    title = title,
    description = "",
    category = category,
    activityDate = activityDate,
    latitude = null,
    longitude = null,
    accuracyM = null,
    locationCapturedAt = null,
    isMockLocation = null,
    syncState = SyncState.SYNCED.name,
    syncAttempts = 0,
    lastError = null,
    serverStatus = status,
    reviewNote = reviewNote,
    serverVersion = version,
    clientCreatedAt = updatedAt,
    updatedAt = updatedAt,
)
