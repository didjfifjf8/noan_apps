package com.example.data.db

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class StudyBookRepository(
    private val context: Context,
    private val studyDao: StudyDao,
) {
    val imagesDir: File
        get() = File(context.filesDir, "study_images").apply { mkdirs() }

    fun searchTopics(category: String, query: String): Flow<List<StudyTopicEntity>> =
        studyDao.searchTopics(category, query)

    fun observeTopic(topicId: Long): Flow<StudyTopicEntity?> =
        studyDao.observeTopic(topicId)

    fun observeBlocks(topicId: Long): Flow<List<StudyBlockEntity>> =
        studyDao.observeBlocksForTopic(topicId)

    suspend fun getTopicById(topicId: Long): StudyTopicEntity? =
        studyDao.getTopicById(topicId)

    suspend fun getBlocks(topicId: Long): List<StudyBlockEntity> =
        studyDao.getBlocksForTopic(topicId)

    suspend fun createTopic(title: String, category: String): Long {
        val now = System.currentTimeMillis()
        val topicId = studyDao.insertTopic(
            StudyTopicEntity(
                title = title.ifBlank { "Yangi mavzu" },
                category = category,
                isPinned = false,
                createdAt = now,
                updatedAt = now,
            )
        )
        studyDao.insertBlock(
            StudyBlockEntity(
                topicId = topicId,
                orderIndex = 0,
                blockType = StudyBlockType.TEXT.raw,
                primaryText = "Bu mavzuda o'rgangan asosiy tushunchalar va qoidalarni yozing.",
            )
        )
        return topicId
    }

    suspend fun updateTopic(topic: StudyTopicEntity) {
        studyDao.updateTopic(topic.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun togglePin(topicId: Long, currentlyPinned: Boolean) {
        studyDao.setTopicPinned(topicId, !currentlyPinned)
    }

    suspend fun deleteTopic(topicId: Long) {
        val blocks = studyDao.getBlocksForTopic(topicId)
        withContext(Dispatchers.IO) {
            blocks.filter { it.blockType == StudyBlockType.IMAGE.raw && it.imagePath.isNotBlank() }
                .forEach { runCatching { File(it.imagePath).delete() } }
        }
        studyDao.deleteTopicById(topicId)
    }

    suspend fun addBlock(topicId: Long, block: StudyBlockEntity): Long {
        val nextIndex = studyDao.countBlocks(topicId)
        val id = studyDao.insertBlock(block.copy(topicId = topicId, orderIndex = nextIndex))
        studyDao.touchTopic(topicId)
        return id
    }

    suspend fun updateBlock(block: StudyBlockEntity) {
        studyDao.updateBlock(block)
        studyDao.touchTopic(block.topicId)
    }

    suspend fun deleteBlock(block: StudyBlockEntity) {
        if (block.blockType == StudyBlockType.IMAGE.raw && block.imagePath.isNotBlank()) {
            withContext(Dispatchers.IO) {
                runCatching { File(block.imagePath).delete() }
            }
        }
        studyDao.deleteBlockById(block.id)
        val remaining = studyDao.getBlocksForTopic(block.topicId)
        studyDao.replaceBlockOrders(remaining)
        studyDao.touchTopic(block.topicId)
    }

    suspend fun moveBlock(topicId: Long, blockId: Long, delta: Int) {
        val blocks = studyDao.getBlocksForTopic(topicId).toMutableList()
        val idx = blocks.indexOfFirst { it.id == blockId }
        val target = idx + delta
        if (idx < 0 || target !in blocks.indices) return
        val item = blocks.removeAt(idx)
        blocks.add(target, item)
        studyDao.replaceBlockOrders(blocks)
        studyDao.touchTopic(topicId)
    }

    suspend fun copyPickedImageToInternal(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val outFile = File(imagesDir, "study_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@runCatching null
            outFile.absolutePath
        }.getOrNull()
    }

    suspend fun seedDefaultTopicIfEmpty() {
        if (studyDao.countTopics() > 0) return
        val now = System.currentTimeMillis()
        val topicId = studyDao.insertTopic(
            StudyTopicEntity(
                title = "Dinamik Dasturlash (DP)",
                category = "Algorithms",
                isPinned = true,
                createdAt = now,
                updatedAt = now,
            )
        )
        val introText =
            "Dinamik dasturlash katta masalani kichik ost-masalalarga bo'lib, ularning natijalarini xotirada (jadvalda) saqlab qayta ishlatish usulidir. Bu usul takroriy hisoblashlarni yo'qotib, algoritmni keskin tezlashtiradi."
        val highlights = StudyJsonCodec.encodeHighlights(
            listOf(
                HighlightSpan(0, 19, HighlighterColor.YELLOW.key),
                HighlightSpan(68, 97, HighlighterColor.CYAN.key),
                HighlightSpan(123, 155, HighlighterColor.GREEN.key),
            )
        )
        studyDao.insertBlocks(
            listOf(
                StudyBlockEntity(
                    topicId = topicId,
                    orderIndex = 0,
                    blockType = StudyBlockType.TEXT.raw,
                    primaryText = introText,
                    highlightsJson = highlights,
                ),
                StudyBlockEntity(
                    topicId = topicId,
                    orderIndex = 1,
                    blockType = StudyBlockType.CODE_NOTE.raw,
                    primaryText = """def fib_dp(n: int) -> int:
    if n <= 1:
        return n
    dp = [0] * (n + 1)
    dp[1] = 1
    for i in range(2, n + 1):
        dp[i] = dp[i - 1] + dp[i - 2]
    return dp[n]

print("Fib(10) =", fib_dp(10))""",
                    secondaryText = "dp[i] ro'yxati 0 dan n gacha har bir Fibonachchi sonini bir marta hisoblab saqlaydi. O(n) vaqt va O(n) xotira sarflaydi.",
                    secondaryHighlightsJson = StudyJsonCodec.encodeHighlights(
                        listOf(HighlightSpan(94, 104, HighlighterColor.GREEN.key))
                    ),
                ),
                StudyBlockEntity(
                    topicId = topicId,
                    orderIndex = 2,
                    blockType = StudyBlockType.DIAGRAM.raw,
                    primaryText = "Fibonachchi DP Chiziqli Algoritmi",
                    diagramJson = StudyJsonCodec.encodeDiagram(StudyJsonCodec.linearPreset()),
                ),
            )
        )
    }
}
