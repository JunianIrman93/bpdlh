package id.lapor.lh.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ReportEntity::class, AttachmentEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
}
