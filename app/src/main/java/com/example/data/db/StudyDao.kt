package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class StudyTopicSummary(
    val topic: StudyTopicEntity,
    val blockCount: Int,
)

@Dao
interface StudyDao {
    @Query(
        """
        SELECT * FROM study_topics
        WHERE (:category = 'All' OR category = :category)
        AND (:query = '' OR title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, updatedAt DESC
        """
    )
    fun searchTopics(category: String, query: String): Flow<List<StudyTopicEntity>>

    @Query("SELECT * FROM study_topics WHERE id = :topicId")
    fun observeTopic(topicId: Long): Flow<StudyTopicEntity?>

    @Query("SELECT * FROM study_topics WHERE id = :topicId")
    suspend fun getTopicById(topicId: Long): StudyTopicEntity?

    @Query("SELECT COUNT(*) FROM study_topics")
    suspend fun countTopics(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: StudyTopicEntity): Long

    @Update
    suspend fun updateTopic(topic: StudyTopicEntity)

    @Query("DELETE FROM study_topics WHERE id = :topicId")
    suspend fun deleteTopicById(topicId: Long)

    @Query("UPDATE study_topics SET isPinned = :pinned, updatedAt = :updatedAt WHERE id = :topicId")
    suspend fun setTopicPinned(topicId: Long, pinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE study_topics SET updatedAt = :updatedAt WHERE id = :topicId")
    suspend fun touchTopic(topicId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM study_blocks WHERE topicId = :topicId ORDER BY orderIndex ASC, id ASC")
    fun observeBlocksForTopic(topicId: Long): Flow<List<StudyBlockEntity>>

    @Query("SELECT * FROM study_blocks WHERE topicId = :topicId ORDER BY orderIndex ASC, id ASC")
    suspend fun getBlocksForTopic(topicId: Long): List<StudyBlockEntity>

    @Query("SELECT COUNT(*) FROM study_blocks WHERE topicId = :topicId")
    suspend fun countBlocks(topicId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: StudyBlockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<StudyBlockEntity>)

    @Update
    suspend fun updateBlock(block: StudyBlockEntity)

    @Query("DELETE FROM study_blocks WHERE id = :blockId")
    suspend fun deleteBlockById(blockId: Long)

    @Transaction
    suspend fun replaceBlockOrders(blocks: List<StudyBlockEntity>) {
        blocks.forEachIndexed { index, block ->
            updateBlock(block.copy(orderIndex = index))
        }
    }
}
