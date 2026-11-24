package com.example.data.db

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "study_topics")
data class StudyTopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = "Python Basics",
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "study_blocks",
    foreignKeys = [
        ForeignKey(
            entity = StudyTopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["topicId"])],
)
data class StudyBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val orderIndex: Int,
    val blockType: String, // TEXT, CODE_NOTE, IMAGE, DIAGRAM
    val primaryText: String = "",
    val secondaryText: String = "",
    val highlightsJson: String = "[]",
    val secondaryHighlightsJson: String = "[]",
    val imagePath: String = "",
    val imageOffsetX: Float = 0f,
    val imageOffsetY: Float = 0f,
    val imageScale: Float = 1f,
    val imageHeightDp: Int = 220,
    val diagramJson: String = "",
)

enum class StudyBlockType(val raw: String, val displayLabel: String) {
    TEXT("TEXT", "Matn"),
    CODE_NOTE("CODE_NOTE", "Kod va Izoh"),
    IMAGE("IMAGE", "Rasm"),
    DIAGRAM("DIAGRAM", "Diagramma");

    companion object {
        fun fromRaw(raw: String): StudyBlockType =
            entries.firstOrNull { it.raw == raw } ?: TEXT
    }
}

enum class HighlighterColor(val key: String, val label: String, val color: Color) {
    YELLOW("yellow", "Sariq", Color(0xFFFACC15)),
    GREEN("green", "Yashil", Color(0xFF34D399)),
    CYAN("cyan", "Moviy", Color(0xFF38BDF8)),
    CORAL("coral", "Pushti", Color(0xFFFB7185)),
    VIOLET("violet", "Binafsha", Color(0xFFC084FC));

    companion object {
        fun fromKey(key: String): HighlighterColor =
            entries.firstOrNull { it.key == key } ?: YELLOW
    }
}

data class HighlightSpan(
    val start: Int,
    val end: Int,
    val colorKey: String,
)

enum class DiagramShape(val raw: String, val label: String) {
    START_END("START_END", "Boshlash"),
    INPUT_OUTPUT("INPUT_OUTPUT", "Kiritish"),
    PROCESS("PROCESS", "Amal"),
    DECISION("DECISION", "Shart");

    companion object {
        fun fromRaw(raw: String): DiagramShape =
            entries.firstOrNull { it.raw == raw } ?: PROCESS
    }
}

data class DiagramNode(
    val id: String,
    val shape: DiagramShape,
    val label: String,
    val x: Float, // normalized 0.08f .. 0.92f
    val y: Float, // normalized 0.08f .. 0.92f
    val colorKey: String = "cyan",
)

data class DiagramEdge(
    val id: String,
    val fromId: String,
    val toId: String,
    val label: String = "",
)

data class DiagramData(
    val nodes: List<DiagramNode> = emptyList(),
    val edges: List<DiagramEdge> = emptyList(),
)

object StudyJsonCodec {
    fun parseHighlights(json: String): List<HighlightSpan> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val s = o.optInt("start", 0)
                    val e = o.optInt("end", 0)
                    if (e > s) {
                        add(HighlightSpan(s, e, o.optString("color", "yellow")))
                    }
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun encodeHighlights(spans: List<HighlightSpan>): String {
        val arr = JSONArray()
        spans.filter { it.end > it.start }.forEach { span ->
            arr.put(
                JSONObject().apply {
                    put("start", span.start)
                    put("end", span.end)
                    put("color", span.colorKey)
                }
            )
        }
        return arr.toString()
    }

    fun parseDiagram(json: String): DiagramData {
        if (json.isBlank()) return DiagramData()
        return try {
            val obj = JSONObject(json)
            val nodesArr = obj.optJSONArray("nodes") ?: JSONArray()
            val edgesArr = obj.optJSONArray("edges") ?: JSONArray()
            val nodes = buildList {
                for (i in 0 until nodesArr.length()) {
                    val n = nodesArr.getJSONObject(i)
                    add(
                        DiagramNode(
                            id = n.optString("id"),
                            shape = DiagramShape.fromRaw(n.optString("shape")),
                            label = n.optString("label", "Blok"),
                            x = n.optDouble("x", 0.5).toFloat().coerceIn(0.08f, 0.92f),
                            y = n.optDouble("y", 0.5).toFloat().coerceIn(0.08f, 0.92f),
                            colorKey = n.optString("color", "cyan"),
                        )
                    )
                }
            }
            val edges = buildList {
                for (i in 0 until edgesArr.length()) {
                    val e = edgesArr.getJSONObject(i)
                    add(
                        DiagramEdge(
                            id = e.optString("id"),
                            fromId = e.optString("from"),
                            toId = e.optString("to"),
                            label = e.optString("label", ""),
                        )
                    )
                }
            }
            DiagramData(nodes = nodes, edges = edges)
        } catch (_: Throwable) {
            DiagramData()
        }
    }

    fun encodeDiagram(data: DiagramData): String {
        val nodesArr = JSONArray()
        data.nodes.forEach { n ->
            nodesArr.put(
                JSONObject().apply {
                    put("id", n.id)
                    put("shape", n.shape.raw)
                    put("label", n.label)
                    put("x", n.x.toDouble())
                    put("y", n.y.toDouble())
                    put("color", n.colorKey)
                }
            )
        }
        val edgesArr = JSONArray()
        data.edges.forEach { e ->
            edgesArr.put(
                JSONObject().apply {
                    put("id", e.id)
                    put("from", e.fromId)
                    put("to", e.toId)
                    put("label", e.label)
                }
            )
        }
        return JSONObject().apply {
            put("nodes", nodesArr)
            put("edges", edgesArr)
        }.toString()
    }

    fun linearPreset(): DiagramData = DiagramData(
        nodes = listOf(
            DiagramNode("n1", DiagramShape.START_END, "Boshlash", 0.5f, 0.14f, "green"),
            DiagramNode("n2", DiagramShape.INPUT_OUTPUT, "Kiritish: n", 0.5f, 0.38f, "cyan"),
            DiagramNode("n3", DiagramShape.PROCESS, "dp[i] = dp[i-1] + dp[i-2]", 0.5f, 0.62f, "yellow"),
            DiagramNode("n4", DiagramShape.START_END, "Natija: dp[n]", 0.5f, 0.86f, "coral"),
        ),
        edges = listOf(
            DiagramEdge("e1", "n1", "n2"),
            DiagramEdge("e2", "n2", "n3"),
            DiagramEdge("e3", "n3", "n4"),
        ),
    )
}
