package id.lapor.lh.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ReportEntity::class, AttachmentEntity::class, SyncMetaEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao

    abstract fun syncMetaDao(): SyncMetaDao
}
