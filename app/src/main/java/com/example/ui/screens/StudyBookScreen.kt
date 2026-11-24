package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.NoteFolderEntity
import com.example.data.db.StudyBlockEntity
import com.example.data.db.StudyBlockType
import com.example.data.db.StudyJsonCodec
import com.example.data.db.StudyTopicEntity
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.BgCanvas
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BgSurfaceElevated
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.StudyBookViewModel

@Composable
fun StudyBookScreen(
    viewModel: StudyBookViewModel,
    onCopyCodeToEditor: (title: String, code: String) -> Unit,
    modifier: Modifier = Modifier,
    topHeaderContent: (@Composable () -> Unit)? = null,
) {
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val activeTopic by viewModel.activeTopic.collectAsStateWithLifecycle()
    val activeBlocks by viewModel.activeBlocks.collectAsStateWithLifecycle()
    val isReadMode by viewModel.isReadMode.collectAsStateWithLifecycle()
    val saveStatus by viewModel.saveStatus.collectAsStateWithLifecycle()
    val localTitleDraft by viewModel.localTitleDraft.collectAsStateWithLifecycle()

    if (activeTopic != null) {
        BackHandler { viewModel.closeTopic() }
        StudyTopicDetailScreen(
            topic = activeTopic!!,
            titleText = localTitleDraft ?: activeTopic!!.title,
            blocks = activeBlocks,
            folders = folders,
            isReadMode = isReadMode,
            saveStatus = saveStatus,
            onBack = { viewModel.closeTopic() },
            onToggleReadMode = { viewModel.toggleReadMode() },
            onTitleChange = viewModel::updateTopicTitle,
            onCategorySelect = viewModel::updateTopicCategory,
            onTogglePin = { viewModel.togglePin(activeTopic!!) },
            onDeleteTopic = { viewModel.deleteTopic(activeTopic!!.id) },
            onAddTextBlock = { viewModel.addTextBlock() },
            onAddCodeNoteBlock = { viewModel.addCodeNoteBlock() },
            onPickImageUri = { uri -> viewModel.addImageFromUri(uri) },
            onAddDiagramBlock = { viewModel.addDiagramBlock(withPreset = true) },
            onMoveBlock = { blockId, delta -> viewModel.moveBlock(blockId, delta) },
            onDeleteBlock = { block -> viewModel.deleteBlock(block) },
            onUpdateBlockDebounced = viewModel::updateBlockDebounced,
            onUpdateBlockImmediate = viewModel::updateBlockImmediate,
            onCopyCodeToEditor = { code ->
                onCopyCodeToEditor(localTitleDraft ?: activeTopic!!.title, code)
            },
            modifier = modifier,
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgCanvas),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            topHeaderContent?.invoke()

            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = { Text("Mavzularni qidirish...", fontSize = 13.sp, color = TextMuted) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("study_book_search_input"),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedCategory == "All",
                    onClick = { viewModel.selectCategory("All") },
                    label = { Text("Barchasi") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                        selectedLabelColor = AccentCyan,
                    ),
                )
                folders.forEach { folder ->
                    FilterChip(
                        selected = selectedCategory == folder.name,
                        onClick = { viewModel.selectCategory(folder.name) },
                        label = { Text(folder.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                            selectedLabelColor = AccentCyan,
                        ),
                    )
                }
            }

            if (topics.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Kitobchada mavzular yo'q",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Yangi mavzu qo'shish uchun + tugmasini bosing (kodlar, izohlar, rasmlar va diagrammalar).",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("study_book_list"),
                ) {
                    itemsIndexed(topics, key = { _, item -> item.id }) { index, topic ->
                        StudyTopicBookCard(
                            topic = topic,
                            index = index,
                            onOpen = { viewModel.openTopic(topic) },
                            onTogglePin = { viewModel.togglePin(topic) },
                            onDelete = { viewModel.deleteTopic(topic.id) },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.createNewTopic("Yangi mavzu") },
            containerColor = AccentCyan,
            contentColor = BgCanvas,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("study_book_fab_add"),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Yangi mavzu qo'shish")
        }
    }
}

@Composable
private fun StudyTopicBookCard(
    topic: StudyTopicEntity,
    index: Int,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {
    val spineColors = listOf(AccentCyan, AccentEmerald, AccentAmber, AccentViolet, AccentCoral)
    val spineColor = spineColors[index % spineColors.size]

    Surface(
        color = BgSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (topic.isPinned) AccentAmber.copy(alpha = 0.55f) else BorderSubtle,
                shape = RoundedCornerShape(14.dp),
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .testTag("study_topic_card_${topic.id}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Book spine accent stripe
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(88.dp)
                    .background(spineColor),
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = spineColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(42.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.AutoStories,
                            contentDescription = null,
                            tint = spineColor,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            color = spineColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(5.dp),
                        ) {
                            Text(
                                text = topic.category,
                                color = spineColor,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        Text(
                            text = "Ochish →",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted,
                        )
                    }
                }

                IconButton(onClick = onTogglePin) {
                    Icon(
                        imageVector = if (topic.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (topic.isPinned) "Mahkamlashni olish" else "Mahkamlash",
                        tint = if (topic.isPinned) AccentAmber else TextMuted,
                        modifier = Modifier.size(19.dp),
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Mavzuni o'chirish",
                        tint = AccentCoral,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyTopicDetailScreen(
    topic: StudyTopicEntity,
    titleText: String,
    blocks: List<StudyBlockEntity>,
    folders: List<NoteFolderEntity>,
    isReadMode: Boolean,
    saveStatus: String,
    onBack: () -> Unit,
    onToggleReadMode: () -> Unit,
    onTitleChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onTogglePin: () -> Unit,
    onDeleteTopic: () -> Unit,
    onAddTextBlock: () -> Unit,
    onAddCodeNoteBlock: () -> Unit,
    onPickImageUri: (android.net.Uri) -> Unit,
    onAddDiagramBlock: () -> Unit,
    onMoveBlock: (Long, Int) -> Unit,
    onDeleteBlock: (StudyBlockEntity) -> Unit,
    onUpdateBlockDebounced: (StudyBlockEntity) -> Unit,
    onUpdateBlockImmediate: (StudyBlockEntity) -> Unit,
    onCopyCodeToEditor: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDeleteDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onPickImageUri(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgCanvas),
    ) {
        // Top App Bar for Topic
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgSurface)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("study_topic_back"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kitobchaga qaytish",
                    tint = TextPrimary,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isReadMode) "O'qish rejimi" else "Kitobcha mavzusi",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = saveStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (saveStatus.contains("Saqlanmoqda") || saveStatus.contains("Rasm")) AccentAmber else AccentEmerald,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Surface(
                color = if (isReadMode) AccentEmerald.copy(alpha = 0.2f) else BgSurfaceElevated,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = if (isReadMode) AccentEmerald else BorderSubtle,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable(onClick = onToggleReadMode)
                    .testTag("study_read_mode_toggle"),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = if (isReadMode) Icons.Filled.Edit else Icons.Filled.AutoStories,
                        contentDescription = null,
                        tint = if (isReadMode) AccentEmerald else AccentCyan,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (isReadMode) "Tahrirlash" else "O'qish",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isReadMode) AccentEmerald else TextPrimary,
                    )
                }
            }

            IconButton(
                onClick = onTogglePin,
                modifier = Modifier.testTag("study_topic_pin"),
            ) {
                Icon(
                    imageVector = if (topic.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "Mavzuni mahkamlash",
                    tint = if (topic.isPinned) AccentAmber else TextSecondary,
                )
            }

            IconButton(
                onClick = { confirmDeleteDialog = true },
                modifier = Modifier.testTag("study_topic_delete"),
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Mavzuni o'chirish",
                    tint = AccentCoral,
                )
            }
        }

        // Scrollable Topic Content (Title + Category + Ordered Blocks)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "topic_header") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isReadMode) {
                        Text(
                            text = titleText.ifBlank { "Yangi mavzu" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Surface(
                            color = AccentCyan.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = topic.category,
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = titleText,
                            onValueChange = onTitleChange,
                            label = { Text("Mavzu nomi (masalan: Dinamik Dasturlash)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("study_topic_title_input"),
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            folders.forEach { folder ->
                                FilterChip(
                                    selected = topic.category == folder.name,
                                    onClick = { onCategorySelect(folder.name) },
                                    label = { Text(folder.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = AccentCyan,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            if (blocks.isEmpty()) {
                item(key = "empty_blocks_hint") {
                    Surface(
                        color = BgSurface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
                    ) {
                        Text(
                            text = "Pastdagi tugmalar orqali Matn, Kod va Izoh, Rasm yoki Chiziqli algoritm Diagrammasi qo'shing.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            itemsIndexed(blocks, key = { _, block -> block.id }) { idx, block ->
                val canUp = idx > 0
                val canDown = idx < blocks.lastIndex
                when (StudyBlockType.fromRaw(block.blockType)) {
                    StudyBlockType.TEXT -> TextHighlightBlockCard(
                        block = block,
                        isReadMode = isReadMode,
                        canMoveUp = canUp,
                        canMoveDown = canDown,
                        onMoveUp = { onMoveBlock(block.id, -1) },
                        onMoveDown = { onMoveBlock(block.id, +1) },
                        onDelete = { onDeleteBlock(block) },
                        onUpdateBlock = onUpdateBlockDebounced,
                    )

                    StudyBlockType.CODE_NOTE -> CodeNoteBlockCard(
                        block = block,
                        isReadMode = isReadMode,
                        canMoveUp = canUp,
                        canMoveDown = canDown,
                        onMoveUp = { onMoveBlock(block.id, -1) },
                        onMoveDown = { onMoveBlock(block.id, +1) },
                        onDelete = { onDeleteBlock(block) },
                        onSendCodeToEditor = onCopyCodeToEditor,
                        onUpdateBlock = onUpdateBlockDebounced,
                    )

                    StudyBlockType.IMAGE -> ImageBlockCard(
                        block = block,
                        isReadMode = isReadMode,
                        canMoveUp = canUp,
                        canMoveDown = canDown,
                        onMoveUp = { onMoveBlock(block.id, -1) },
                        onMoveDown = { onMoveBlock(block.id, +1) },
                        onDelete = { onDeleteBlock(block) },
                        onUpdateBlock = onUpdateBlockImmediate,
                    )

                    StudyBlockType.DIAGRAM -> DiagramBlockCard(
                        block = block,
                        isReadMode = isReadMode,
                        canMoveUp = canUp,
                        canMoveDown = canDown,
                        onMoveUp = { onMoveBlock(block.id, -1) },
                        onMoveDown = { onMoveBlock(block.id, +1) },
                        onDelete = { onDeleteBlock(block) },
                        onUpdateTitle = { newTitle ->
                            onUpdateBlockDebounced(block.copy(primaryText = newTitle))
                        },
                        onUpdateDiagram = { newDiagram ->
                            onUpdateBlockImmediate(
                                block.copy(diagramJson = StudyJsonCodec.encodeDiagram(newDiagram))
                            )
                        },
                    )
                }
            }
        }

        // Bottom Add-Block Bar (Edit mode only)
        if (!isReadMode) {
            Surface(
                color = BgSurface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 0.5.dp, color = BorderSubtle),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AddBlockChip(
                        icon = Icons.AutoMirrored.Filled.Notes,
                        label = "+ Matn",
                        accentColor = AccentCyan,
                        tag = "study_add_text_block",
                        onClick = onAddTextBlock,
                    )
                    AddBlockChip(
                        icon = Icons.Filled.Code,
                        label = "+ Kod va Izoh",
                        accentColor = AccentEmerald,
                        tag = "study_add_code_block",
                        onClick = onAddCodeNoteBlock,
                    )
                    AddBlockChip(
                        icon = Icons.Filled.Image,
                        label = "+ Rasm",
                        accentColor = AccentViolet,
                        tag = "study_add_image_block",
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    )
                    AddBlockChip(
                        icon = Icons.Filled.AccountTree,
                        label = "+ Diagramma",
                        accentColor = AccentAmber,
                        tag = "study_add_diagram_block",
                        onClick = onAddDiagramBlock,
                    )
                }
            }
        }
    }

    if (confirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { confirmDeleteDialog = false },
            containerColor = BgSurface,
            title = { Text("Mavzuni o'chirish", color = TextPrimary) },
            text = {
                Text(
                    "\"${titleText.ifBlank { topic.title }}\" mavzusi va uning barcha bloklari o'chirilsinmi?",
                    color = TextSecondary,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteDialog = false
                        onDeleteTopic()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCoral, contentColor = BgCanvas),
                ) {
                    Text("O'chirish")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteDialog = false }) {
                    Text("Bekor", color = TextSecondary)
                }
            },
        )
    }
}

@Composable
private fun AddBlockChip(
    icon: ImageVector,
    label: String,
    accentColor: androidx.compose.ui.graphics.Color,
    tag: String,
    onClick: () -> Unit,
) {
    Surface(
        color = BgSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
            )
        }
    }
}
