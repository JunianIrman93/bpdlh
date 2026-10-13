package id.lapor.lh.core.database

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
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
            is_mock_location = :isMockLocation, updated_at = :updatedAt
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
        updatedAt: Long,
    ): Int

    @Query(
        """
        UPDATE reports SET sync_state = :to, updated_at = :updatedAt
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
}
