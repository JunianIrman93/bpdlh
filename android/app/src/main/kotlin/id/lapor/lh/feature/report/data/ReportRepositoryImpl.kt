package id.lapor.lh.feature.report.data

import android.net.Uri
import androidx.room.withTransaction
import id.lapor.lh.core.database.AppDatabase
import id.lapor.lh.core.database.AttachmentEntity
import id.lapor.lh.core.database.ReportEntity
import id.lapor.lh.core.files.AttachmentStorage
import id.lapor.lh.core.files.StoredFile
import id.lapor.lh.feature.auth.domain.AuthRepository
import id.lapor.lh.feature.report.domain.AddAttachmentResult
import id.lapor.lh.feature.report.domain.AttachmentError
import id.lapor.lh.feature.report.domain.AttachmentKind
import id.lapor.lh.feature.report.domain.Report
import id.lapor.lh.feature.report.domain.ReportDraft
import id.lapor.lh.feature.report.domain.ReportError
import id.lapor.lh.feature.report.domain.ReportField
import id.lapor.lh.feature.report.domain.ReportRepository
import id.lapor.lh.feature.report.domain.ReportRules
import id.lapor.lh.feature.report.domain.SyncState
import id.lapor.lh.feature.sync.domain.SyncScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

private val timestampFormat = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ReportRules.zone)

private val extensionByMime =
    mapOf(
        "application/pdf" to "pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to "docx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" to "xlsx",
        "text/plain" to "txt",
        "text/csv" to "csv",
    )

private const val MAX_FILE_NAME = 255

@OptIn(ExperimentalCoroutinesApi::class)
class ReportRepositoryImpl
    @Inject
    constructor(
        private val database: AppDatabase,
        private val storage: AttachmentStorage,
        private val authRepository: AuthRepository,
        private val syncScheduler: SyncScheduler,
    ) : ReportRepository {
        private val dao = database.reportDao()

        override fun observeReports(): Flow<List<Report>> =
            authRepository.observeCurrentUser().flatMapLatest { user ->
                if (user == null) {
                    flowOf(emptyList())
                } else {
                    dao.observeReports(user.id).map { rows -> rows.map { it.toDomain() } }
                }
            }

        override fun observeReport(id: String): Flow<Report?> =
            authRepository.observeCurrentUser().flatMapLatest { user ->
                if (user == null) flowOf(null) else dao.observeReport(id, user.id).map { it?.toDomain() }
            }

        override suspend fun getReport(id: String): Report? {
            val ownerId = ownerId() ?: return null
            return dao.getReport(id, ownerId)?.toDomain()
        }

        override suspend fun saveDraft(draft: ReportDraft) {
            val ownerId = ownerId() ?: return
            val now = System.currentTimeMillis()
            database.withTransaction {
                val updated =
                    dao.updateDraft(
                        id = draft.id,
                        ownerId = ownerId,
                        title = draft.title.trim(),
                        description = draft.description.trim(),
                        category = draft.category?.name,
                        activityDate = draft.activityDate?.toString(),
                        latitude = draft.location?.latitude,
                        longitude = draft.location?.longitude,
                        accuracyM = draft.location?.accuracyM,
                        locationCapturedAt = draft.location?.capturedAt,
                        isMockLocation = draft.location?.isMock,
                        updatedAt = now,
                    )
                if (updated == 0 && dao.getReport(draft.id, ownerId) == null) {
                    dao.insertReport(draft.toNewEntity(ownerId, now))
                }
            }
        }

        override fun newCameraCaptureUri(): String = storage.newCameraUri().toString()

        override suspend fun addAttachment(
            draft: ReportDraft,
            kind: AttachmentKind,
            sourceUri: String,
        ): AddAttachmentResult {
            val ownerId = ownerId() ?: return AddAttachmentResult.Failed
            saveDraft(draft)
            val current = dao.getReport(draft.id, ownerId)?.toDomain() ?: return AddAttachmentResult.Failed
            if (current.syncState != SyncState.DRAFT) return AddAttachmentResult.Failed
            if (current.attachments.size >= ReportRules.MAX_ATTACHMENTS) {
                return AddAttachmentResult.Rejected(AttachmentError.TOO_MANY)
            }

            val attachmentId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val imported =
                try {
                    import(Uri.parse(sourceUri), kind, draft.id, attachmentId, now)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    storage.deleteAttachmentFiles(draft.id, attachmentId)
                    return AddAttachmentResult.Failed
                } finally {
                    if (kind == AttachmentKind.PHOTO) storage.discardCameraFiles()
                }
            val stored =
                when (imported) {
                    is Imported.Rejected -> return AddAttachmentResult.Rejected(imported.error)
                    is Imported.Stored -> imported
                }

            val error = ReportRules.checkNewAttachment(current.attachments, kind, stored.mimeType, stored.file.sizeBytes)
            if (error != null) {
                storage.delete(stored.file.path)
                return AddAttachmentResult.Rejected(error)
            }

            val inserted =
                database.withTransaction {
                    val stillDraft = dao.getReport(draft.id, ownerId)?.report?.syncState == SyncState.DRAFT.name
                    if (stillDraft) {
                        dao.insertAttachment(
                            AttachmentEntity(
                                id = attachmentId,
                                reportId = draft.id,
                                kind = kind.name,
                                fileName = stored.fileName.take(MAX_FILE_NAME),
                                mimeType = stored.mimeType,
                                sizeBytes = stored.file.sizeBytes,
                                sha256 = stored.file.sha256,
                                localPath = stored.file.path,
                                uploadState = "PENDING",
                                createdAt = now,
                            ),
                        )
                    }
                    stillDraft
                }
            if (!inserted) {
                storage.delete(stored.file.path)
                return AddAttachmentResult.Failed
            }
            return AddAttachmentResult.Added
        }

        override suspend fun removeAttachment(
            reportId: String,
            attachmentId: String,
        ) {
            val ownerId = ownerId() ?: return
            val path =
                dao
                    .getReport(reportId, ownerId)
                    ?.attachments
                    ?.firstOrNull { it.id == attachmentId }
                    ?.localPath ?: return
            if (dao.deleteDraftAttachment(reportId, attachmentId, ownerId) == 1) storage.delete(path)
        }

        override suspend fun submit(draft: ReportDraft): Map<ReportField, ReportError> {
            val ownerId = ownerId() ?: return emptyMap()
            saveDraft(draft)
            val now = System.currentTimeMillis()
            val errors =
                database.withTransaction {
                    val report = dao.getReport(draft.id, ownerId)?.toDomain()
                    if (report == null || report.syncState != SyncState.DRAFT) {
                        emptyMap()
                    } else {
                        val errors = ReportRules.validateForSubmit(report.draft, report.attachments, ReportRules.today(now))
                        if (errors.isEmpty()) dao.transition(draft.id, ownerId, SyncState.DRAFT.name, SyncState.QUEUED.name, now)
                        errors
                    }
                }
            if (errors.isEmpty()) syncScheduler.requestSync()
            return errors
        }

        override suspend fun cancelQueued(id: String) = transition(id, SyncState.QUEUED, SyncState.DRAFT)

        override suspend fun retryFailed(id: String) {
            transition(id, SyncState.FAILED, SyncState.QUEUED)
            syncScheduler.requestSync()
        }

        override suspend fun editFailed(id: String) = transition(id, SyncState.FAILED, SyncState.DRAFT)

        override suspend fun deleteDraft(id: String) {
            val ownerId = ownerId() ?: return
            if (dao.deleteDraft(id, ownerId) == 1) storage.deleteReport(id)
        }

        private suspend fun transition(
            id: String,
            from: SyncState,
            to: SyncState,
        ) {
            val ownerId = ownerId() ?: return
            dao.transition(id, ownerId, from.name, to.name, System.currentTimeMillis())
        }

        private suspend fun ownerId(): String? = authRepository.observeCurrentUser().first()?.id

        private suspend fun import(
            source: Uri,
            kind: AttachmentKind,
            reportId: String,
            attachmentId: String,
            now: Long,
        ): Imported {
            val stamp = timestampFormat.format(Instant.ofEpochMilli(now))
            return when (kind) {
                AttachmentKind.PHOTO -> {
                    val file = storage.storeCompressedImage(source, reportId, attachmentId)
                    Imported.Stored(file, "image/jpeg", "foto-$stamp.jpg")
                }

                AttachmentKind.IMAGE -> {
                    val baseName =
                        storage
                            .describe(source)
                            .displayName
                            ?.substringBeforeLast('.')
                            ?.takeIf { it.isNotBlank() }
                    val file = storage.storeCompressedImage(source, reportId, attachmentId)
                    Imported.Stored(file, "image/jpeg", "${baseName ?: "gambar-$stamp"}.jpg")
                }

                AttachmentKind.DOCUMENT, AttachmentKind.FILE -> {
                    val info = storage.describe(source)
                    val mime = info.mimeType
                    if (mime == null || mime !in ReportRules.mimeTypesByKind.getValue(kind)) {
                        return Imported.Rejected(AttachmentError.UNSUPPORTED_TYPE)
                    }
                    val extension = extensionByMime.getValue(mime)
                    val file =
                        storage.storeCopy(source, reportId, attachmentId, extension, ReportRules.MAX_FILE_BYTES)
                            ?: return Imported.Rejected(AttachmentError.FILE_TOO_LARGE)
                    val fallback = if (kind == AttachmentKind.DOCUMENT) "dokumen-$stamp.$extension" else "berkas-$stamp.$extension"
                    Imported.Stored(file, mime, info.displayName ?: fallback)
                }
            }
        }
    }

private sealed interface Imported {
    data class Stored(
        val file: StoredFile,
        val mimeType: String,
        val fileName: String,
    ) : Imported

    data class Rejected(
        val error: AttachmentError,
    ) : Imported
}

private fun ReportDraft.toNewEntity(
    ownerId: String,
    now: Long,
) = ReportEntity(
    id = id,
    ownerId = ownerId,
    title = title.trim(),
    description = description.trim(),
    category = category?.name,
    activityDate = activityDate?.toString(),
    latitude = location?.latitude,
    longitude = location?.longitude,
    accuracyM = location?.accuracyM,
    locationCapturedAt = location?.capturedAt,
    isMockLocation = location?.isMock,
    syncState = SyncState.DRAFT.name,
    syncAttempts = 0,
    lastError = null,
    serverStatus = null,
    reviewNote = null,
    serverVersion = null,
    clientCreatedAt = now,
    updatedAt = now,
)
