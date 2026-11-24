package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NoteAlt
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.NoteEntity
import com.example.data.db.NoteFolderEntity
import com.example.python.PythonSyntaxHighlighter
import com.example.python.PythonVisualTransformation
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.viewmodel.NoteEditorDraft
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.StudyBookViewModel

private enum class NotesSubSection {
    NOTES,
    KITOBCHA,
}

@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    studyBookViewModel: StudyBookViewModel,
    onCopyCodeToEditor: (title: String, code: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val editingNote by viewModel.editingNote.collectAsStateWithLifecycle()

    var subSection by rememberSaveable { mutableStateOf(NotesSubSection.NOTES) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    if (subSection == NotesSubSection.NOTES && editingNote != null) {
        BackHandler { viewModel.closeNoteEditor() }
        NoteDetailEditor(
            draft = editingNote!!,
            folders = folders,
            onBack = { viewModel.closeNoteEditor() },
            onUpdateDraft = viewModel::updateDraft,
            onCopyCodeToEditor = {
                onCopyCodeToEditor(editingNote!!.title, editingNote!!.codeSnippet)
            },
            onDelete = { viewModel.deleteNote(editingNote!!.id) },
            modifier = modifier,
        )
        return
    }

    if (subSection == NotesSubSection.KITOBCHA) {
        StudyBookScreen(
            viewModel = studyBookViewModel,
            onCopyCodeToEditor = onCopyCodeToEditor,
            modifier = modifier,
            topHeaderContent = {
                NotesTopHeader(
                    subSection = subSection,
                    onSelectSubSection = { subSection = it },
                    notesCount = notes.size,
                    topicsCount = studyBookViewModel.topics.collectAsStateWithLifecycle().value.size,
                    onCreateFolderClick = { showCreateFolderDialog = true },
                )
            },
        )
        if (showCreateFolderDialog) {
            CreateFolderDialog(
                onDismiss = { showCreateFolderDialog = false },
                onCreate = { name ->
                    viewModel.createFolder(name)
                    showCreateFolderDialog = false
                },
            )
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgCanvas),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NotesTopHeader(
                subSection = subSection,
                onSelectSubSection = { subSection = it },
                notesCount = notes.size,
                topicsCount = studyBookViewModel.topics.collectAsStateWithLifecycle().value.size,
                onCreateFolderClick = { showCreateFolderDialog = true },
            )

            // Search Notes
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = { Text("Search notes by title or code...", fontSize = 13.sp, color = TextMuted) },
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
                    .testTag("notes_search_input"),
            )

            // Folder / Category Chips
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
                    label = { Text("All") },
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

            if (notes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.NoteAlt, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("No notes found", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        Text("Tap + to save a Python algorithm, formula, or snippet.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize().testTag("notes_list"),
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onOpen = { viewModel.openNoteForEdit(note) },
                            onTogglePin = { viewModel.togglePin(note) },
                            onCopyToEditor = { onCopyCodeToEditor(note.title, note.codeSnippet) },
                            onDelete = { viewModel.deleteNote(note.id) },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.createNewNote() },
            containerColor = AccentCyan,
            contentColor = BgCanvas,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("notes_fab_add"),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Create Note")
        }
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreate = { folderName ->
                viewModel.createFolder(folderName)
                showCreateFolderDialog = false
            },
        )
    }
}

@Composable
private fun NotesTopHeader(
    subSection: NotesSubSection,
    onSelectSubSection: (NotesSubSection) -> Unit,
    notesCount: Int,
    topicsCount: Int,
    onCreateFolderClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSurface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("My Notes", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(
                    text = if (subSection == NotesSubSection.NOTES) {
                        "$notesCount ta qayd • Avtomatik saqlanadi"
                    } else {
                        "$topicsCount ta mavzu • Kitobcha offline saqlanadi"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            IconButton(
                onClick = onCreateFolderClick,
                modifier = Modifier.testTag("notes_add_folder_button"),
            ) {
                Icon(Icons.Filled.CreateNewFolder, contentDescription = "New Folder", tint = AccentCyan)
            }
        }

        // Segmented Pill Switcher: Qaydlar | Kitobcha
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgCanvas, RoundedCornerShape(12.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            NotesSegmentedTab(
                selected = subSection == NotesSubSection.NOTES,
                label = "Qaydlar",
                icon = Icons.Filled.NoteAlt,
                tag = "notes_mode_notes",
                onClick = { onSelectSubSection(NotesSubSection.NOTES) },
                modifier = Modifier.weight(1f),
            )
            NotesSegmentedTab(
                selected = subSection == NotesSubSection.KITOBCHA,
                label = "Kitobcha",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                tag = "notes_mode_kitobcha",
                onClick = { onSelectSubSection(NotesSubSection.KITOBCHA) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NotesSegmentedTab(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (selected) AccentCyan.copy(alpha = 0.18f) else BgCanvas,
        shape = RoundedCornerShape(9.dp),
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(9.dp))
            .then(
                if (selected) Modifier.border(1.dp, AccentCyan.copy(alpha = 0.6f), RoundedCornerShape(9.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .testTag(tag),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) AccentCyan else TextSecondary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) AccentCyan else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var folderName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgSurface,
        title = { Text("Create Note Folder", color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                placeholder = { Text("e.g. Dynamic Programming", color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
                modifier = Modifier.fillMaxWidth().testTag("new_folder_input"),
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(folderName) },
                enabled = folderName.isNotBlank(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
    )
}

@Composable
private fun NoteCard(
    note: NoteEntity,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    onCopyToEditor: () -> Unit,
    onDelete: () -> Unit,
) {
    val highlightedPreview = remember(note.codeSnippet) {
        val preview = note.codeSnippet.lines().take(5).joinToString("\n")
        PythonSyntaxHighlighter.highlight(preview)
    }

    Surface(
        color = BgSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (note.isPinned) AccentAmber.copy(alpha = 0.5f) else BorderSubtle,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onOpen)
            .testTag("note_card_${note.id}"),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        color = AccentCyan.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(5.dp),
                    ) {
                        Text(
                            text = note.category,
                            color = AccentCyan,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                IconButton(onClick = onTogglePin, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (note.isPinned) "Unpin Note" else "Pin Note",
                        tint = if (note.isPinned) AccentAmber else TextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete Note",
                        tint = AccentCoral,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (note.description.isNotBlank()) {
                Text(
                    text = note.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (note.codeSnippet.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgCanvas, RoundedCornerShape(8.dp))
                        .border(0.5.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                ) {
                    Text(
                        text = highlightedPreview,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCopyToEditor) {
                    Icon(Icons.Filled.Code, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Send to Editor", color = AccentEmerald, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun NoteDetailEditor(
    draft: NoteEditorDraft,
    folders: List<NoteFolderEntity>,
    onBack: () -> Unit,
    onUpdateDraft: (title: String?, description: String?, codeSnippet: String?, category: String?, isPinned: Boolean?) -> Unit,
    onCopyCodeToEditor: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visualTransformation = remember { PythonVisualTransformation() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgCanvas),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("note_editor_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Notes", tint = TextPrimary)
            }
            Text(
                text = draft.lastSavedLabel.ifEmpty { "Auto-saved" },
                style = MaterialTheme.typography.labelMedium,
                color = if (draft.isSaving) AccentAmber else AccentEmerald,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { onUpdateDraft(null, null, null, null, !draft.isPinned) },
                modifier = Modifier.testTag("note_editor_pin"),
            ) {
                Icon(
                    imageVector = if (draft.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "Pin Note",
                    tint = if (draft.isPinned) AccentAmber else TextSecondary,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.testTag("note_editor_delete")) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete Note", tint = AccentCoral)
            }
            Button(
                onClick = onCopyCodeToEditor,
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = BgCanvas),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp).testTag("note_send_to_editor"),
            ) {
                Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("To Editor", style = MaterialTheme.typography.labelMedium)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = draft.title,
                onValueChange = { onUpdateDraft(it, null, null, null, null) },
                label = { Text("Note Title") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
                modifier = Modifier.fillMaxWidth().testTag("note_title_input"),
            )

            // Category picker
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                folders.forEach { folder ->
                    FilterChip(
                        selected = draft.category == folder.name,
                        onClick = { onUpdateDraft(null, null, null, folder.name, null) },
                        label = { Text(folder.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                            selectedLabelColor = AccentCyan,
                        ),
                    )
                }
            }

            OutlinedTextField(
                value = draft.description,
                onValueChange = { onUpdateDraft(null, it, null, null, null) },
                label = { Text("Notes / Explanation") },
                minLines = 2,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
                modifier = Modifier.fillMaxWidth().testTag("note_description_input"),
            )

            Text("Python Code Snippet", style = MaterialTheme.typography.labelLarge, color = AccentCyan)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp)
                    .background(BgSurface, RoundedCornerShape(10.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                    .padding(12.dp),
            ) {
                BasicTextField(
                    value = draft.codeSnippet,
                    onValueChange = { onUpdateDraft(null, null, it, null, null) },
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = TextPrimary,
                    ),
                    cursorBrush = SolidColor(AccentCyan),
                    visualTransformation = visualTransformation,
                    modifier = Modifier.fillMaxWidth().testTag("note_code_input"),
                )
            }
        }
    }
}
