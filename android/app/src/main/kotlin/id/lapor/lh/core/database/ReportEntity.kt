package id.lapor.lh.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "reports", indices = [Index("owner_id", "updated_at")])
data class ReportEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "owner_id") val ownerId: String,
    val title: String,
    val description: String,
    val category: String?,
    @ColumnInfo(name = "activity_date") val activityDate: String?,
    val latitude: Double?,
    val longitude: Double?,
    @ColumnInfo(name = "accuracy_m") val accuracyM: Float?,
    @ColumnInfo(name = "location_captured_at") val locationCapturedAt: Long?,
    @ColumnInfo(name = "is_mock_location") val isMockLocation: Boolean?,
    @ColumnInfo(name = "sync_state") val syncState: String,
    @ColumnInfo(name = "sync_attempts") val syncAttempts: Int,
    @ColumnInfo(name = "last_error") val lastError: String?,
    @ColumnInfo(name = "server_status") val serverStatus: String?,
    @ColumnInfo(name = "review_note") val reviewNote: String?,
    @ColumnInfo(name = "server_version") val serverVersion: Int?,
    @ColumnInfo(name = "client_created_at") val clientCreatedAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
