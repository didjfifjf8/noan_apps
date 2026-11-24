package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DiagramData
import com.example.data.db.DiagramEdge
import com.example.data.db.DiagramNode
import com.example.data.db.DiagramShape
import com.example.data.db.HighlighterColor
import com.example.data.db.StudyBlockEntity
import com.example.data.db.StudyJsonCodec
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.BgCanvas
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BgSurfaceElevated
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun DiagramBlockCard(
    block: StudyBlockEntity,
    isReadMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onUpdateTitle: (String) -> Unit,
    onUpdateDiagram: (DiagramData) -> Unit,
) {
    var titleText by remember(block.id) { mutableStateOf(block.primaryText) }
    var diagram by remember(block.id, block.diagramJson) {
        mutableStateOf(StudyJsonCodec.parseDiagram(block.diagramJson))
    }
    var selectedNodeId by remember(block.id) { mutableStateOf<String?>(null) }
    var connectSourceId by remember(block.id) { mutableStateOf<String?>(null) }
    var pendingEdgeLabel by remember(block.id) { mutableStateOf("") }

    val latestDiagram by rememberUpdatedState(diagram)
    val latestConnectSource by rememberUpdatedState(connectSourceId)
    val latestSelectedId by rememberUpdatedState(selectedNodeId)
    val latestEdgeLabel by rememberUpdatedState(pendingEdgeLabel)

    val textMeasurer = rememberTextMeasurer()

    fun commitDiagram(next: DiagramData) {
        diagram = next
        onUpdateDiagram(next)
    }

    Surface(
        color = BgSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .testTag("study_diagram_block_${block.id}"),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StudyBlockHeader(
                label = "Diagramma",
                accentColor = AccentAmber,
                isReadMode = isReadMode,
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onDelete = onDelete,
            )

            if (isReadMode) {
                if (titleText.isNotBlank()) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                }
            } else {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = {
                        titleText = it
                        onUpdateTitle(it)
                    },
                    placeholder = { Text("Diagramma nomi (masalan: Chiziqli algoritm)", fontSize = 12.sp, color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Shape palette bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DiagramShape.entries.forEach { shape ->
                        val defaultColor = when (shape) {
                            DiagramShape.START_END -> HighlighterColor.GREEN.key
                            DiagramShape.INPUT_OUTPUT -> HighlighterColor.CYAN.key
                            DiagramShape.PROCESS -> HighlighterColor.YELLOW.key
                            DiagramShape.DECISION -> HighlighterColor.VIOLET.key
                        }
                        Surface(
                            color = BgSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                .clickable {
                                    val newId = "n_${System.currentTimeMillis()}_${diagram.nodes.size}"
                                    val lastNode = diagram.nodes.lastOrNull()
                                    val newY = if (lastNode == null) 0.18f else (lastNode.y + 0.22f).coerceIn(0.14f, 0.88f)
                                    val newX = if (lastNode != null && lastNode.y > 0.78f) {
                                        ((lastNode.x + 0.28f) % 0.7f + 0.16f).coerceIn(0.16f, 0.84f)
                                    } else {
                                        lastNode?.x ?: 0.5f
                                    }
                                    val newNode = DiagramNode(
                                        id = newId,
                                        shape = shape,
                                        label = shape.label,
                                        x = newX,
                                        y = newY,
                                        colorKey = defaultColor,
                                    )
                                    val newEdges = if (lastNode != null) {
                                        diagram.edges + DiagramEdge(
                                            id = "e_${System.currentTimeMillis()}",
                                            fromId = lastNode.id,
                                            toId = newId,
                                        )
                                    } else {
                                        diagram.edges
                                    }
                                    selectedNodeId = newId
                                    connectSourceId = null
                                    commitDiagram(diagram.copy(nodes = diagram.nodes + newNode, edges = newEdges))
                                }
                                .testTag("diagram_add_${shape.raw.lowercase()}"),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(shape.label, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            selectedNodeId = null
                            connectSourceId = null
                            commitDiagram(StudyJsonCodec.linearPreset())
                        },
                    ) {
                        Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Shablon", color = AccentAmber, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (connectSourceId != null && !isReadMode) {
                Surface(
                    color = AccentCyan.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Endi ulanadigan keyingi blokni bosing (Strelka)",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentCyan,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { connectSourceId = null }) {
                            Text("Bekor", color = AccentCoral)
                        }
                    }
                }
            }

            // Interactive Flowchart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgCanvas)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
            ) {
                var draggingNodeId by remember { mutableStateOf<String?>(null) }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isReadMode) {
                                Modifier
                            } else {
                                Modifier
                                    .pointerInput(block.id) {
                                        detectTapGestures { offset ->
                                            val w = size.width.toFloat().coerceAtLeast(1f)
                                            val h = size.height.toFloat().coerceAtLeast(1f)
                                            val hit = findNodeAt(latestDiagram.nodes, offset, w, h)
                                            val src = latestConnectSource
                                            if (src != null && hit != null && hit.id != src) {
                                                val already = latestDiagram.edges.any { it.fromId == src && it.toId == hit.id }
                                                if (!already) {
                                                    val newEdge = DiagramEdge(
                                                        id = "e_${System.currentTimeMillis()}",
                                                        fromId = src,
                                                        toId = hit.id,
                                                        label = latestEdgeLabel,
                                                    )
                                                    commitDiagram(
                                                        latestDiagram.copy(edges = latestDiagram.edges + newEdge)
                                                    )
                                                }
                                                connectSourceId = null
                                                pendingEdgeLabel = ""
                                                selectedNodeId = hit.id
                                            } else {
                                                connectSourceId = null
                                                selectedNodeId = hit?.id
                                            }
                                        }
                                    }
                                    .pointerInput(block.id) {
                                        detectDragGestures(
                                            onDragStart = { startOffset ->
                                                val w = size.width.toFloat().coerceAtLeast(1f)
                                                val h = size.height.toFloat().coerceAtLeast(1f)
                                                val hit = findNodeAt(latestDiagram.nodes, startOffset, w, h)
                                                draggingNodeId = hit?.id ?: latestSelectedId
                                                if (hit != null) {
                                                    selectedNodeId = hit.id
                                                }
                                            },
                                            onDragEnd = {
                                                draggingNodeId = null
                                                onUpdateDiagram(latestDiagram)
                                            },
                                            onDragCancel = {
                                                draggingNodeId = null
                                            },
                                            onDrag = { change, dragAmount ->
                                                val targetId = draggingNodeId ?: return@detectDragGestures
                                                change.consume()
                                                val w = size.width.toFloat().coerceAtLeast(1f)
                                                val h = size.height.toFloat().coerceAtLeast(1f)
                                                val dx = dragAmount.x / w
                                                val dy = dragAmount.y / h
                                                diagram = latestDiagram.copy(
                                                    nodes = latestDiagram.nodes.map { n ->
                                                        if (n.id == targetId) {
                                                            n.copy(
                                                                x = (n.x + dx).coerceIn(0.12f, 0.88f),
                                                                y = (n.y + dy).coerceIn(0.10f, 0.90f),
                                                            )
                                                        } else n
                                                    }
                                                )
                                            },
                                        )
                                    }
                            }
                        ),
                ) {
                    val w = size.width
                    val h = size.height

                    // Subtle dot grid
                    val step = 24.dp.toPx()
                    var gx = step
                    while (gx < w) {
                        var gy = step
                        while (gy < h) {
                            drawCircle(
                                color = BorderSubtle.copy(alpha = 0.45f),
                                radius = 1.dp.toPx(),
                                center = Offset(gx, gy),
                            )
                            gy += step
                        }
                        gx += step
                    }

                    val nodeMap = diagram.nodes.associateBy { it.id }

                    // Draw arrows/edges first
                    diagram.edges.forEach { edge ->
                        val fromNode = nodeMap[edge.fromId] ?: return@forEach
                        val toNode = nodeMap[edge.toId] ?: return@forEach
                        val p1 = Offset(fromNode.x * w, fromNode.y * h)
                        val p2 = Offset(toNode.x * w, toNode.y * h)
                        val angle = atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble()).toFloat()

                        val halfW = 54.dp.toPx()
                        val halfH = 22.dp.toPx()
                        val endMarginX = cos(angle) * halfW
                        val endMarginY = sin(angle) * halfH
                        val endPt = Offset(p2.x - endMarginX, p2.y - endMarginY)
                        val startPt = Offset(p1.x + cos(angle) * (halfW * 0.65f), p1.y + sin(angle) * halfH)

                        val lineColor = when (edge.label) {
                            "Ha" -> AccentEmerald
                            "Yo'q" -> AccentCoral
                            else -> TextSecondary
                        }
                        drawLine(
                            color = lineColor,
                            start = startPt,
                            end = endPt,
                            strokeWidth = 2.dp.toPx(),
                        )

                        // Arrowhead
                        val arrowLen = 10.dp.toPx()
                        val a1 = angle + Math.toRadians(150.0).toFloat()
                        val a2 = angle - Math.toRadians(150.0).toFloat()
                        val arrowPath = Path().apply {
                            moveTo(endPt.x, endPt.y)
                            lineTo(endPt.x + cos(a1) * arrowLen, endPt.y + sin(a1) * arrowLen)
                            lineTo(endPt.x + cos(a2) * arrowLen, endPt.y + sin(a2) * arrowLen)
                            close()
                        }
                        drawPath(arrowPath, color = lineColor, style = Fill)

                        if (edge.label.isNotBlank()) {
                            val mid = Offset((startPt.x + endPt.x) / 2f, (startPt.y + endPt.y) / 2f)
                            val measured = textMeasurer.measure(
                                text = edge.label,
                                style = TextStyle(
                                    color = lineColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            drawRoundRect(
                                color = BgCanvas,
                                topLeft = Offset(mid.x - measured.size.width / 2f - 4f, mid.y - measured.size.height / 2f - 2f),
                                size = Size(measured.size.width + 8f, measured.size.height + 4f),
                                cornerRadius = CornerRadius(4f, 4f),
                            )
                            drawText(
                                textLayoutResult = measured,
                                topLeft = Offset(mid.x - measured.size.width / 2f, mid.y - measured.size.height / 2f),
                            )
                        }
                    }

                    // Draw flowchart nodes
                    val nodeW = 112.dp.toPx()
                    val nodeH = 42.dp.toPx()
                    diagram.nodes.forEach { node ->
                        val cx = node.x * w
                        val cy = node.y * h
                        val left = cx - nodeW / 2f
                        val top = cy - nodeH / 2f
                        val accent = HighlighterColor.fromKey(node.colorKey).color
                        val isSelected = !isReadMode && (node.id == selectedNodeId || node.id == connectSourceId)
                        val fillCol = BgSurfaceElevated
                        val strokeCol = if (isSelected) AccentCyan else accent
                        val strokeW = if (isSelected) 2.5.dp.toPx() else 1.5.dp.toPx()

                        when (node.shape) {
                            DiagramShape.START_END -> {
                                drawRoundRect(
                                    color = fillCol,
                                    topLeft = Offset(left, top),
                                    size = Size(nodeW, nodeH),
                                    cornerRadius = CornerRadius(nodeH / 2f, nodeH / 2f),
                                )
                                drawRoundRect(
                                    color = strokeCol,
                                    topLeft = Offset(left, top),
                                    size = Size(nodeW, nodeH),
                                    cornerRadius = CornerRadius(nodeH / 2f, nodeH / 2f),
                                    style = Stroke(width = strokeW),
                                )
                            }
                            DiagramShape.PROCESS -> {
                                drawRoundRect(
                                    color = fillCol,
                                    topLeft = Offset(left, top),
                                    size = Size(nodeW, nodeH),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                )
                                drawRoundRect(
                                    color = strokeCol,
                                    topLeft = Offset(left, top),
                                    size = Size(nodeW, nodeH),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                    style = Stroke(width = strokeW),
                                )
                            }
                            DiagramShape.INPUT_OUTPUT -> {
                                val skew = 14.dp.toPx()
                                val path = Path().apply {
                                    moveTo(left + skew, top)
                                    lineTo(left + nodeW, top)
                                    lineTo(left + nodeW - skew, top + nodeH)
                                    lineTo(left, top + nodeH)
                                    close()
                                }
                                drawPath(path, color = fillCol, style = Fill)
                                drawPath(path, color = strokeCol, style = Stroke(width = strokeW))
                            }
                            DiagramShape.DECISION -> {
                                val path = Path().apply {
                                    moveTo(cx, top - 4.dp.toPx())
                                    lineTo(left + nodeW + 6.dp.toPx(), cy)
                                    lineTo(cx, top + nodeH + 4.dp.toPx())
                                    lineTo(left - 6.dp.toPx(), cy)
                                    close()
                                }
                                drawPath(path, color = fillCol, style = Fill)
                                drawPath(path, color = strokeCol, style = Stroke(width = strokeW))
                            }
                        }

                        val measured = textMeasurer.measure(
                            text = node.label,
                            style = TextStyle(
                                color = TextPrimary,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            constraints = Constraints(maxWidth = (nodeW - 22.dp.toPx()).toInt().coerceAtLeast(40)),
                        )
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                x = cx - measured.size.width / 2f,
                                y = cy - measured.size.height / 2f,
                            ),
                        )
                    }
                }

                if (diagram.nodes.isEmpty()) {
                    Text(
                        text = "Yuqoridagi tugmalardan blok qo'shing yoki Shablon bosing",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    )
                }
            }

            // Selected Node Inspector (Edit mode)
            if (!isReadMode) {
                val selectedNode = diagram.nodes.firstOrNull { it.id == selectedNodeId }
                if (selectedNode != null) {
                    Surface(
                        color = BgSurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = selectedNode.label,
                                onValueChange = { nextLabel ->
                                    commitDiagram(
                                        diagram.copy(
                                            nodes = diagram.nodes.map {
                                                if (it.id == selectedNode.id) it.copy(label = nextLabel) else it
                                            }
                                        )
                                    )
                                },
                                label = { Text("Blok matni (${selectedNode.shape.label})") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                HighlighterColor.entries.forEach { hc ->
                                    val isCur = selectedNode.colorKey == hc.key
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(hc.color)
                                            .border(
                                                width = if (isCur) 2.5.dp else 1.dp,
                                                color = if (isCur) Color.White else BorderSubtle,
                                                shape = CircleShape,
                                            )
                                            .clickable {
                                                commitDiagram(
                                                    diagram.copy(
                                                        nodes = diagram.nodes.map {
                                                            if (it.id == selectedNode.id) it.copy(colorKey = hc.key) else it
                                                        }
                                                    )
                                                )
                                            }
                                    )
                                }

                                Spacer(Modifier.width(4.dp))

                                if (selectedNode.shape == DiagramShape.DECISION) {
                                    FilterChip(
                                        selected = connectSourceId == selectedNode.id && pendingEdgeLabel == "Ha",
                                        onClick = {
                                            connectSourceId = selectedNode.id
                                            pendingEdgeLabel = "Ha"
                                        },
                                        label = { Text("Strelka: Ha") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AccentEmerald.copy(alpha = 0.2f),
                                            selectedLabelColor = AccentEmerald,
                                        ),
                                    )
                                    FilterChip(
                                        selected = connectSourceId == selectedNode.id && pendingEdgeLabel == "Yo'q",
                                        onClick = {
                                            connectSourceId = selectedNode.id
                                            pendingEdgeLabel = "Yo'q"
                                        },
                                        label = { Text("Strelka: Yo'q") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AccentCoral.copy(alpha = 0.2f),
                                            selectedLabelColor = AccentCoral,
                                        ),
                                    )
                                } else {
                                    FilterChip(
                                        selected = connectSourceId == selectedNode.id,
                                        onClick = {
                                            connectSourceId = selectedNode.id
                                            pendingEdgeLabel = ""
                                        },
                                        leadingIcon = {
                                            Icon(Icons.AutoMirrored.Filled.TrendingFlat, contentDescription = null, modifier = Modifier.size(15.dp))
                                        },
                                        label = { Text("Strelka ulash") },
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        commitDiagram(
                                            diagram.copy(
                                                edges = diagram.edges.filterNot {
                                                    it.fromId == selectedNode.id || it.toId == selectedNode.id
                                                }
                                            )
                                        )
                                    },
                                    modifier = Modifier.size(34.dp),
                                ) {
                                    Icon(Icons.Filled.LinkOff, contentDescription = "Strelkalarni uzish", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = {
                                        commitDiagram(
                                            diagram.copy(
                                                nodes = diagram.nodes.filterNot { it.id == selectedNode.id },
                                                edges = diagram.edges.filterNot {
                                                    it.fromId == selectedNode.id || it.toId == selectedNode.id
                                                },
                                            )
                                        )
                                        selectedNodeId = null
                                        connectSourceId = null
                                    },
                                    modifier = Modifier.size(34.dp),
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Blokni o'chirish", tint = AccentCoral, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                } else if (diagram.nodes.isNotEmpty()) {
                    Text(
                        text = "Blokni surish uchun ushlab torting; tahrirlash yoki strelka ulash uchun ustiga bosing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
            }
        }
    }
}

private fun findNodeAt(nodes: List<DiagramNode>, tap: Offset, width: Float, height: Float): DiagramNode? {
    val hitRadiusX = 64f
    val hitRadiusY = 34f
    var best: DiagramNode? = null
    var bestDist = Float.MAX_VALUE
    for (node in nodes.asReversed()) {
        val nx = node.x * width
        val ny = node.y * height
        if (kotlin.math.abs(tap.x - nx) <= hitRadiusX && kotlin.math.abs(tap.y - ny) <= hitRadiusY) {
            val d = hypot((tap.x - nx).toDouble(), (tap.y - ny).toDouble()).toFloat()
            if (d < bestDist) {
                bestDist = d
                best = node
            }
        }
    }
    return best
}
