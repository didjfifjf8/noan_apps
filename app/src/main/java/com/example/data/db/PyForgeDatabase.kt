package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [NoteEntity::class, NoteFolderEntity::class, ProjectMetaEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PyForgeDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun projectMetaDao(): ProjectMetaDao

    companion object {
        @Volatile
        private var INSTANCE: PyForgeDatabase? = null

        fun getInstance(context: Context): PyForgeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PyForgeDatabase::class.java,
                    "pyforge.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
