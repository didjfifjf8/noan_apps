package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.HighlightSpan
import com.example.data.db.HighlighterColor
import com.example.data.db.StudyBlockEntity
import com.example.data.db.StudyJsonCodec
import com.example.python.HighlightTextUtils
import com.example.python.HighlightVisualTransformation
import com.example.python.PythonSyntaxHighlighter
import com.example.python.PythonVisualTransformation
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.BgCanvas
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BgSurfaceElevated
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StudyBlockHeader(
    label: String,
    accentColor: Color,
    isReadMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    if (isReadMode) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = accentColor.copy(alpha = 0.14f),
            shape = RoundedCornerShape(6.dp),
        ) {
            Text(
                text = label,
                color = accentColor,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = "Yuqoriga",
                tint = if (canMoveUp) TextSecondary else TextMuted.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = "Pastga",
                tint = if (canMoveDown) TextSecondary else TextMuted.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.DeleteOutline,
                contentDescription = "Blokni o'chirish",
                tint = AccentCoral,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
fun HighlighterBar(
    onApplyColor: (HighlighterColor) -> Unit,
    onClearHighlight: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Brush,
            contentDescription = "Flomaster",
            tint = AccentAmber,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = "Flomaster:",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
        HighlighterColor.entries.forEach { hc ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(hc.color.copy(alpha = 0.7f))
                    .border(1.dp, hc.color, CircleShape)
                    .clickable { onApplyColor(hc) }
                    .testTag("highlight_color_${hc.key}"),
            )
        }
        Surface(
            color = BgSurfaceElevated,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                .clickable(onClick = onClearHighlight)
                .testTag("highlight_clear_button"),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Icon(Icons.Filled.FormatColorReset, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("Tozalash", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }
        }
    }
}

@Composable
fun TextHighlightBlockCard(
    block: StudyBlockEntity,
    isReadMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onUpdateBlock: (StudyBlockEntity) -> Unit,
) {
    var textValue by remember(block.id) { mutableStateOf(TextFieldValue(block.primaryText)) }
    var spans by remember(block.id) { mutableStateOf(StudyJsonCodec.parseHighlights(block.highlightsJson)) }

    if (textValue.text != block.primaryText && block.primaryText.isNotEmpty() && textValue.text.isEmpty()) {
        textValue = TextFieldValue(block.primaryText)
        spans = StudyJsonCodec.parseHighlights(block.highlightsJson)
    }

    val highlightVisual = remember(spans) { HighlightVisualTransformation(spans) }

    Surface(
        color = BgSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .testTag("study_text_block_${block.id}"),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StudyBlockHeader(
                label = "Matn va Konspekt",
                accentColor = AccentCyan,
                isReadMode = isReadMode,
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onDelete = onDelete,
            )

            if (isReadMode) {
                val annotated = remember(textValue.text, spans) {
                    HighlightTextUtils.buildHighlightedText(textValue.text, spans)
                }
                Text(
                    text = annotated,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    lineHeight = 24.sp,
                )
            } else {
                HighlighterBar(
                    onApplyColor = { hc ->
                        val sel = textValue.selection
                        val range = if (sel.length > 0) {
                            minOf(sel.start, sel.end) until maxOf(sel.start, sel.end)
                        } else {
                            HighlightTextUtils.wordRangeAt(textValue.text, sel.start)
                        }
                        if (range != null) {
                            val nextSpans = HighlightTextUtils.addHighlight(spans, range.first, range.last + 1, hc.key)
                            spans = nextSpans
                            onUpdateBlock(
                                block.copy(
                                    primaryText = textValue.text,
                                    highlightsJson = StudyJsonCodec.encodeHighlights(nextSpans),
                                )
                            )
                        }
                    },
                    onClearHighlight = {
                        val sel = textValue.selection
                        val nextSpans = HighlightTextUtils.removeHighlightInRange(spans, sel.start, sel.end)
                        spans = nextSpans
                        onUpdateBlock(
                            block.copy(
                                primaryText = textValue.text,
                                highlightsJson = StudyJsonCodec.encodeHighlights(nextSpans),
                            )
                        )
                    },
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 84.dp)
                        .background(BgCanvas, RoundedCornerShape(10.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                ) {
                    if (textValue.text.isEmpty()) {
                        Text(
                            text = "Mavzu tushuntirishini yozing... (so'zni belgilab Flomaster bilan bo'yashingiz mumkin)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                        )
                    }
                    BasicTextField(
                        value = textValue,
                        onValueChange = { incoming ->
                            val nextSpans = if (incoming.text != textValue.text) {
                                HighlightTextUtils.adjustSpansForTextEdit(textValue.text, incoming.text, spans)
                            } else {
                                spans
                            }
                            textValue = incoming
                            if (nextSpans != spans || incoming.text != block.primaryText) {
                                spans = nextSpans
                                onUpdateBlock(
                                    block.copy(
                                        primaryText = incoming.text,
                                        highlightsJson = StudyJsonCodec.encodeHighlights(nextSpans),
                                    )
                                )
                            }
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = TextPrimary,
                            lineHeight = 24.sp,
                        ),
                        cursorBrush = SolidColor(AccentCyan),
                        visualTransformation = highlightVisual,
                        modifier = Modifier.fillMaxWidth().testTag("study_text_input_${block.id}"),
                    )
                }
            }
        }
    }
}

@Composable
fun CodeNoteBlockCard(
    block: StudyBlockEntity,
    isReadMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onSendCodeToEditor: (String) -> Unit,
    onUpdateBlock: (StudyBlockEntity) -> Unit,
) {
    var codeValue by remember(block.id) { mutableStateOf(TextFieldValue(block.primaryText)) }
    var noteValue by remember(block.id) { mutableStateOf(TextFieldValue(block.secondaryText)) }
    var noteSpans by remember(block.id) {
        mutableStateOf(StudyJsonCodec.parseHighlights(block.secondaryHighlightsJson))
    }

    val pythonVisual = remember { PythonVisualTransformation() }
    val noteHighlightVisual = remember(noteSpans) { HighlightVisualTransformation(noteSpans) }

    Surface(
        color = BgSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .testTag("study_code_block_${block.id}"),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StudyBlockHeader(
                label = "Kod va Vazifasi",
                accentColor = AccentEmerald,
                isReadMode = isReadMode,
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onDelete = onDelete,
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val sideBySide = maxWidth >= 540.dp

                val codeSection: @Composable (Modifier) -> Unit = { mod ->
                    Column(modifier = mod, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Python kodi",
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentEmerald,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = { onSendCodeToEditor(codeValue.text) },
                                modifier = Modifier.testTag("study_send_code_${block.id}"),
                            ) {
                                Icon(Icons.Filled.Code, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Editor'ga yuborish", color = AccentEmerald, style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 90.dp)
                                .background(BgCanvas, RoundedCornerShape(10.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                        ) {
                            if (isReadMode) {
                                val highlighted = remember(codeValue.text) {
                                    PythonSyntaxHighlighter.highlight(codeValue.text)
                                }
                                Text(
                                    text = highlighted,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                )
                            } else {
                                BasicTextField(
                                    value = codeValue,
                                    onValueChange = { incoming ->
                                        codeValue = incoming
                                        onUpdateBlock(
                                            block.copy(
                                                primaryText = incoming.text,
                                                secondaryText = noteValue.text,
                                                secondaryHighlightsJson = StudyJsonCodec.encodeHighlights(noteSpans),
                                            )
                                        )
                                    },
                                    textStyle = TextStyle(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = TextPrimary,
                                    ),
                                    cursorBrush = SolidColor(AccentCyan),
                                    visualTransformation = pythonVisual,
                                    modifier = Modifier.fillMaxWidth().testTag("study_code_input_${block.id}"),
                                )
                            }
                        }
                    }
                }

                val explanationSection: @Composable (Modifier) -> Unit = { mod ->
                    Column(modifier = mod, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Vazifasi va tushuntirish",
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentCyan,
                        )
                        if (!isReadMode) {
                            HighlighterBar(
                                onApplyColor = { hc ->
                                    val sel = noteValue.selection
                                    val range = if (sel.length > 0) {
                                        minOf(sel.start, sel.end) until maxOf(sel.start, sel.end)
                                    } else {
                                        HighlightTextUtils.wordRangeAt(noteValue.text, sel.start)
                                    }
                                    if (range != null) {
                                        val nextSpans = HighlightTextUtils.addHighlight(noteSpans, range.first, range.last + 1, hc.key)
                                        noteSpans = nextSpans
                                        onUpdateBlock(
                                            block.copy(
                                                primaryText = codeValue.text,
                                                secondaryText = noteValue.text,
                                                secondaryHighlightsJson = StudyJsonCodec.encodeHighlights(nextSpans),
                                            )
                                        )
                                    }
                                },
                                onClearHighlight = {
                                    val sel = noteValue.selection
                                    val nextSpans = HighlightTextUtils.removeHighlightInRange(noteSpans, sel.start, sel.end)
                                    noteSpans = nextSpans
                                    onUpdateBlock(
                                        block.copy(
                                            primaryText = codeValue.text,
                                            secondaryText = noteValue.text,
                                            secondaryHighlightsJson = StudyJsonCodec.encodeHighlights(nextSpans),
                                        )
                                    )
                                },
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 76.dp)
                                .background(BgSurfaceElevated, RoundedCornerShape(10.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                        ) {
                            if (isReadMode) {
                                val annotated = remember(noteValue.text, noteSpans) {
                                    HighlightTextUtils.buildHighlightedText(noteValue.text, noteSpans)
                                }
                                if (annotated.isEmpty()) {
                                    Text(
                                        text = "Izoh kiritilmagan",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMuted,
                                        lineHeight = 20.sp,
                                    )
                                } else {
                                    Text(
                                        text = annotated,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        lineHeight = 20.sp,
                                    )
                                }
                            } else {
                                if (noteValue.text.isEmpty()) {
                                    Text(
                                        text = "Bu kod qanday vazifa bajarishini yozing...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                    )
                                }
                                BasicTextField(
                                    value = noteValue,
                                    onValueChange = { incoming ->
                                        val nextSpans = if (incoming.text != noteValue.text) {
                                            HighlightTextUtils.adjustSpansForTextEdit(noteValue.text, incoming.text, noteSpans)
                                        } else {
                                            noteSpans
                                        }
                                        noteValue = incoming
                                        noteSpans = nextSpans
                                        onUpdateBlock(
                                            block.copy(
                                                primaryText = codeValue.text,
                                                secondaryText = incoming.text,
                                                secondaryHighlightsJson = StudyJsonCodec.encodeHighlights(nextSpans),
                                            )
                                        )
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextPrimary,
                                        lineHeight = 20.sp,
                                    ),
                                    cursorBrush = SolidColor(AccentCyan),
                                    visualTransformation = noteHighlightVisual,
                                    modifier = Modifier.fillMaxWidth().testTag("study_code_note_input_${block.id}"),
                                )
                            }
                        }
                    }
                }

                if (sideBySide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        codeSection(Modifier.weight(1.1f))
                        explanationSection(Modifier.weight(0.9f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        codeSection(Modifier.fillMaxWidth())
                        explanationSection(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
fun ImageBlockCard(
    block: StudyBlockEntity,
    isReadMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onUpdateBlock: (StudyBlockEntity) -> Unit,
) {
    var offsetX by remember(block.id) { mutableFloatStateOf(block.imageOffsetX) }
    var offsetY by remember(block.id) { mutableFloatStateOf(block.imageOffsetY) }
    var scale by remember(block.id) { mutableFloatStateOf(block.imageScale.coerceIn(0.5f, 3.0f)) }
    var heightDp by remember(block.id) { mutableStateOf(block.imageHeightDp.coerceIn(140, 360)) }
    var caption by remember(block.id) { mutableStateOf(block.primaryText) }
    var dragEnabled by remember(block.id) { mutableStateOf(false) }

    val imageBitmap: ImageBitmap? = remember(block.imagePath) {
        decodeSampledImage(block.imagePath, maxDim = 1280)
    }

    fun persistTransform(
        nx: Float = offsetX,
        ny: Float = offsetY,
        ns: Float = scale,
        nh: Int = heightDp,
        cap: String = caption,
    ) {
        offsetX = nx.coerceIn(-400f, 400f)
        offsetY = ny.coerceIn(-400f, 400f)
        scale = ns.coerceIn(0.5f, 3.0f)
        heightDp = nh.coerceIn(140, 360)
        caption = cap
        onUpdateBlock(
            block.copy(
                primaryText = caption,
                imageOffsetX = offsetX,
                imageOffsetY = offsetY,
                imageScale = scale,
                imageHeightDp = heightDp,
            )
        )
    }

    Surface(
        color = BgSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .testTag("study_image_block_${block.id}"),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StudyBlockHeader(
                label = "Rasm",
                accentColor = AccentViolet,
                isReadMode = isReadMode,
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onDelete = onDelete,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightDp.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clipToBounds()
                    .background(BgCanvas)
                    .border(
                        width = 1.dp,
                        color = if (dragEnabled && !isReadMode) AccentCyan else BorderSubtle,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .then(
                        if (!isReadMode && dragEnabled) {
                            Modifier.pointerInput(block.id) {
                                detectDragGestures(
                                    onDragEnd = { persistTransform() },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        offsetX = (offsetX + dragAmount.x).coerceIn(-400f, 400f)
                                        offsetY = (offsetY + dragAmount.y).coerceIn(-400f, 400f)
                                    },
                                )
                            }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = caption.ifBlank { "Mavzu rasmi" },
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                translationX = offsetX,
                                translationY = offsetY,
                                scaleX = scale,
                                scaleY = scale,
                            ),
                    )
                } else {
                    Text("Rasm topilmadi", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (!isReadMode) {
                // Image move (left, right, up, down) & zoom controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = if (dragEnabled) AccentCyan.copy(alpha = 0.2f) else BgSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(1.dp, if (dragEnabled) AccentCyan else BorderSubtle, RoundedCornerShape(8.dp))
                            .clickable { dragEnabled = !dragEnabled }
                            .testTag("image_drag_toggle_${block.id}"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        ) {
                            Icon(Icons.Filled.OpenWith, contentDescription = null, tint = if (dragEnabled) AccentCyan else TextSecondary, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (dragEnabled) "Surish yoqilgan" else "Barmoq bilan surish",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (dragEnabled) AccentCyan else TextPrimary,
                            )
                        }
                    }

                    SmallNudgeButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Chapga") {
                        persistTransform(nx = offsetX - 24f)
                    }
                    SmallNudgeButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "O'ngga") {
                        persistTransform(nx = offsetX + 24f)
                    }
                    SmallNudgeButton(Icons.Filled.KeyboardArrowUp, "Tepaga") {
                        persistTransform(ny = offsetY - 24f)
                    }
                    SmallNudgeButton(Icons.Filled.KeyboardArrowDown, "Pastga") {
                        persistTransform(ny = offsetY + 24f)
                    }
                    SmallNudgeButton(Icons.Filled.ZoomOut, "Kichraytirish") {
                        persistTransform(ns = scale - 0.15f)
                    }
                    SmallNudgeButton(Icons.Filled.ZoomIn, "Kattalashtirish") {
                        persistTransform(ns = scale + 0.15f)
                    }
                    SmallNudgeButton(Icons.Filled.CenterFocusStrong, "Tiklash") {
                        persistTransform(nx = 0f, ny = 0f, ns = 1f, nh = 220)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Ramka balandligi:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    listOf(160 to "Kichik", 220 to "O'rta", 320 to "Katta").forEach { (hVal, label) ->
                        Surface(
                            color = if (heightDp == hVal) AccentViolet.copy(alpha = 0.2f) else BgSurfaceElevated,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .border(1.dp, if (heightDp == hVal) AccentViolet else BorderSubtle, RoundedCornerShape(6.dp))
                                .clickable { persistTransform(nh = hVal) },
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (heightDp == hVal) AccentViolet else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = caption,
                    onValueChange = { persistTransform(cap = it) },
                    placeholder = { Text("Rasm ostiga izoh (ixtiyoriy)...", fontSize = 12.sp, color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (caption.isNotBlank()) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun SmallNudgeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        color = BgSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .size(34.dp)
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = TextPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

private fun decodeSampledImage(path: String, maxDim: Int): ImageBitmap? {
    if (path.isBlank()) return null
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        var sample = 1
        while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) {
            sample *= 2
        }
        val loadOpts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeFile(path, loadOpts)?.asImageBitmap()
    } catch (_: Throwable) {
        null
    }
}
