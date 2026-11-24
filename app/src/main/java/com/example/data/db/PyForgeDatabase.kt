package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        NoteEntity::class,
        NoteFolderEntity::class,
        ProjectMetaEntity::class,
        StudyTopicEntity::class,
        StudyBlockEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class PyForgeDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun projectMetaDao(): ProjectMetaDao
    abstract fun studyDao(): StudyDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `study_topics` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `isPinned` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `study_blocks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `topicId` INTEGER NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        `blockType` TEXT NOT NULL,
                        `primaryText` TEXT NOT NULL,
                        `secondaryText` TEXT NOT NULL,
                        `highlightsJson` TEXT NOT NULL,
                        `secondaryHighlightsJson` TEXT NOT NULL,
                        `imagePath` TEXT NOT NULL,
                        `imageOffsetX` REAL NOT NULL,
                        `imageOffsetY` REAL NOT NULL,
                        `imageScale` REAL NOT NULL,
                        `imageHeightDp` INTEGER NOT NULL,
                        `diagramJson` TEXT NOT NULL,
                        FOREIGN KEY(`topicId`) REFERENCES `study_topics`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_study_blocks_topicId` ON `study_blocks` (`topicId`)"
                )
            }
        }

        @Volatile
        private var INSTANCE: PyForgeDatabase? = null

        fun getInstance(context: Context): PyForgeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PyForgeDatabase::class.java,
                    "pyforge.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
