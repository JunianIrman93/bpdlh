package id.lapor.lh.core.database

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class ReportWithAttachments(
    @Embedded val report: ReportEntity,
    @Relation(parentColumn = "id", entityColumn = "report_id")
    val attachments: List<AttachmentEntity>,
)

@Dao
interface ReportDao {
    @Transaction
    @Query("SELECT * FROM reports WHERE owner_id = :ownerId ORDER BY updated_at DESC")
    fun observeReports(ownerId: String): Flow<List<ReportWithAttachments>>

    @Transaction
    @Query("SELECT * FROM reports WHERE id = :id AND owner_id = :ownerId")
    fun observeReport(
        id: String,
        ownerId: String,
    ): Flow<ReportWithAttachments?>

    @Transaction
    @Query("SELECT * FROM reports WHERE id = :id AND owner_id = :ownerId")
    suspend fun getReport(
        id: String,
        ownerId: String,
    ): ReportWithAttachments?

    @Insert
    suspend fun insertReport(report: ReportEntity)

    @Query(
        """
        UPDATE reports SET title = :title, description = :description, category = :category,
            activity_date = :activityDate, latitude = :latitude, longitude = :longitude,
            accuracy_m = :accuracyM, location_captured_at = :locationCapturedAt,
            is_mock_location = :isMockLocation, location_address = :locationAddress, updated_at = :updatedAt
        WHERE id = :id AND owner_id = :ownerId AND sync_state = 'DRAFT'
        """,
    )
    suspend fun updateDraft(
        id: String,
        ownerId: String,
        title: String,
        description: String,
        category: String?,
        activityDate: String?,
        latitude: Double?,
        longitude: Double?,
        accuracyM: Float?,
        locationCapturedAt: Long?,
        isMockLocation: Boolean?,
        locationAddress: String?,
        updatedAt: Long,
    ): Int

    @Query(
        """
        UPDATE reports SET sync_state = :to, sync_attempts = 0, last_error = NULL, updated_at = :updatedAt
        WHERE id = :id AND owner_id = :ownerId AND sync_state = :from
        """,
    )
    suspend fun transition(
        id: String,
        ownerId: String,
        from: String,
        to: String,
        updatedAt: Long,
    ): Int

    @Query("DELETE FROM reports WHERE id = :id AND owner_id = :ownerId AND sync_state = 'DRAFT'")
    suspend fun deleteDraft(
        id: String,
        ownerId: String,
    ): Int

    @Insert
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Query(
        """
        DELETE FROM attachments WHERE id = :attachmentId AND report_id IN
            (SELECT id FROM reports WHERE id = :reportId AND owner_id = :ownerId AND sync_state = 'DRAFT')
        """,
    )
    suspend fun deleteDraftAttachment(
        reportId: String,
        attachmentId: String,
        ownerId: String,
    ): Int

    @Transaction
    @Query("SELECT * FROM reports WHERE owner_id = :ownerId AND sync_state = 'QUEUED' ORDER BY client_created_at ASC")
    suspend fun getQueued(ownerId: String): List<ReportWithAttachments>

    @Query("UPDATE reports SET sync_state = 'SYNCING' WHERE id = :id AND owner_id = :ownerId AND sync_state = 'QUEUED'")
    suspend fun claim(
        id: String,
        ownerId: String,
    ): Int

    @Query("UPDATE reports SET sync_state = 'QUEUED' WHERE owner_id = :ownerId AND sync_state = 'SYNCING'")
    suspend fun requeueInterrupted(ownerId: String): Int

    @Query("UPDATE attachments SET upload_state = 'UPLOADED' WHERE report_id = :reportId AND id IN (:ids)")
    suspend fun markAttachmentsUploaded(
        reportId: String,
        ids: List<String>,
    )

    @Query(
        """
        UPDATE reports SET sync_state = 'SYNCED', sync_attempts = 0, last_error = NULL,
            server_status = :serverStatus, server_version = :serverVersion, updated_at = :updatedAt
        WHERE id = :id AND owner_id = :ownerId AND sync_state = 'SYNCING'
        """,
    )
    suspend fun markSynced(
        id: String,
        ownerId: String,
        serverStatus: String,
        serverVersion: Int,
        updatedAt: Long,
    ): Int

    @Query(
        """
        UPDATE reports SET sync_state = :to, sync_attempts = :attempts, last_error = :lastError, updated_at = :updatedAt
        WHERE id = :id AND owner_id = :ownerId AND sync_state = 'SYNCING'
        """,
    )
    suspend fun releaseSyncing(
        id: String,
        ownerId: String,
        to: String,
        attempts: Int,
        lastError: String?,
        updatedAt: Long,
    ): Int

    @Query(
        """
        UPDATE reports SET server_status = :serverStatus, review_note = :reviewNote, server_version = :serverVersion,
            updated_at = MAX(updated_at, :updatedAt)
        WHERE id = :id AND owner_id = :ownerId
        """,
    )
    suspend fun applyServerStatus(
        id: String,
        ownerId: String,
        serverStatus: String,
        reviewNote: String?,
        serverVersion: Int,
        updatedAt: Long,
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReportIfAbsent(report: ReportEntity)

    @Query(
        """
        UPDATE reports SET description = :description, latitude = :latitude, longitude = :longitude,
            accuracy_m = :accuracyM, location_captured_at = :locationCapturedAt, is_mock_location = :isMockLocation,
            location_address = :locationAddress, server_status = :serverStatus, review_note = :reviewNote, server_version = :serverVersion,
            client_created_at = :clientCreatedAt, updated_at = MAX(updated_at, :updatedAt)
        WHERE id = :id AND owner_id = :ownerId AND sync_state = 'SYNCED' AND latitude IS NULL
        """,
    )
    suspend fun applyServerDetail(
        id: String,
        ownerId: String,
        description: String,
        latitude: Double,
        longitude: Double,
        accuracyM: Float,
        locationCapturedAt: Long,
        isMockLocation: Boolean,
        locationAddress: String?,
        serverStatus: String,
        reviewNote: String?,
        serverVersion: Int,
        clientCreatedAt: Long,
        updatedAt: Long,
    ): Int
}
