package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE (:category = 'All' OR category = :category)
        AND (:query = '' OR title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR codeSnippet LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, updatedAt DESC
        """
    )
    fun searchNotes(category: String, query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun countNotes(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("UPDATE notes SET isPinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM note_folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<NoteFolderEntity>>

    @Query("SELECT COUNT(*) FROM note_folders")
    suspend fun countFolders(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolder(folder: NoteFolderEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolders(folders: List<NoteFolderEntity>)

    @Query("DELETE FROM note_folders WHERE name = :name")
    suspend fun deleteFolderByName(name: String)

    @Query("UPDATE notes SET category = 'Personal Notes' WHERE category = :oldCategory")
    suspend fun moveNotesToPersonal(oldCategory: String)
}

@Dao
interface ProjectMetaDao {
    @Query("SELECT * FROM project_metadata ORDER BY lastOpenedAt DESC")
    fun getAllProjectMeta(): Flow<List<ProjectMetaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: ProjectMetaEntity)

    @Query("DELETE FROM project_metadata WHERE relativePath = :relativePath")
    suspend fun deleteByPath(relativePath: String)
}
