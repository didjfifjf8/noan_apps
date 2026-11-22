package com.example.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.workspace.WorkspaceEntry
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
import com.example.ui.viewmodel.ProjectsViewModel
import java.io.File

@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel,
    onOpenFileInEditor: (String) -> Unit,
    onFileRenamed: (oldPath: String, newFile: File) -> Unit,
    onPathDeleted: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<WorkspaceEntry?>(null) }
    var deleteTarget by remember { mutableStateOf<WorkspaceEntry?>(null) }

    BackHandler(enabled = !uiState.isAtRoot) {
        viewModel.navigateUp()
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            var fileName = "imported.py"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) {
                    fileName = cursor.getString(idx) ?: fileName
                }
            }
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            viewModel.importExternalFile(fileName, content) { importedFile ->
                onOpenFileInEditor(importedFile.absolutePath)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgCanvas),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgSurface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!uiState.isAtRoot) {
                IconButton(
                    onClick = { viewModel.navigateUp() },
                    modifier = Modifier.testTag("projects_up_button"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Parent Folder", tint = TextPrimary)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text("Projects", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text("Local Python workspace", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }

            IconButton(
                onClick = { showNewFileDialog = true },
                modifier = Modifier.testTag("projects_new_file_button"),
            ) {
                Icon(Icons.Filled.NoteAdd, contentDescription = "New Python File", tint = AccentEmerald)
            }
            IconButton(
                onClick = { showNewFolderDialog = true },
                modifier = Modifier.testTag("projects_new_folder_button"),
            ) {
                Icon(Icons.Filled.CreateNewFolder, contentDescription = "New Folder", tint = AccentCyan)
            }
            IconButton(
                onClick = { openDocumentLauncher.launch(arrayOf("text/*", "application/x-python-code", "*/*")) },
                modifier = Modifier.testTag("projects_import_button"),
            ) {
                Icon(Icons.Filled.FileOpen, contentDescription = "Import File", tint = AccentAmber)
            }
        }

        // Breadcrumbs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgSurfaceElevated)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            uiState.breadcrumbs.forEachIndexed { idx, (label, path) ->
                if (idx > 0) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = label,
                    color = if (idx == uiState.breadcrumbs.lastIndex) AccentCyan else TextSecondary,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable { viewModel.navigateTo(path) }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }

        // Status Banner
        AnimatedVisibility(visible = uiState.statusMessage != null) {
            uiState.statusMessage?.let { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AccentCyan.copy(alpha = 0.14f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(msg, color = AccentCyan, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.dismissStatus() }) {
                        Text("OK", color = AccentCyan)
                    }
                }
            }
        }

        if (uiState.entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Folder, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Folder is empty", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showNewFileDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = BgCanvas),
                        ) {
                            Text("+ New .py File")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().testTag("projects_list"),
            ) {
                items(uiState.entries, key = { it.absolutePath }) { entry ->
                    ProjectEntryRow(
                        entry = entry,
                        onClick = {
                            if (entry.isDirectory) {
                                viewModel.navigateTo(entry.absolutePath)
                            } else {
                                onOpenFileInEditor(entry.absolutePath)
                            }
                        },
                        onRename = { renameTarget = entry },
                        onDelete = { deleteTarget = entry },
                    )
                }
            }
        }
    }

    if (showNewFileDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            containerColor = BgSurface,
            title = { Text("New Python File", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. solver.py", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_new_file_input"),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createFile(name) { created ->
                            onOpenFileInEditor(created.absolutePath)
                        }
                        showNewFileDialog = false
                    },
                    enabled = name.isNotBlank(),
                ) {
                    Text("Create & Open")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    if (showNewFolderDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            containerColor = BgSurface,
            title = { Text("New Folder / Project", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. data_analysis", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_new_folder_input"),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createFolder(name)
                        showNewFolderDialog = false
                    },
                    enabled = name.isNotBlank(),
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    renameTarget?.let { target ->
        var newName by remember(target) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = BgSurface,
            title = { Text("Rename", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_rename_input"),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameEntry(target, newName) { renamed ->
                            onFileRenamed(target.absolutePath, renamed)
                        }
                        renameTarget = null
                    },
                    enabled = newName.isNotBlank(),
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = BgSurface,
            title = { Text("Delete '${target.name}'?", color = AccentCoral) },
            text = {
                Text(
                    text = if (target.isDirectory) "This will permanently delete the folder and all files inside it." else "This file will be permanently deleted.",
                    color = TextSecondary,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEntry(target) { deletedPath ->
                            onPathDeleted(deletedPath)
                        }
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCoral, contentColor = BgCanvas),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }
}

@Composable
private fun ProjectEntryRow(
    entry: WorkspaceEntry,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = BgSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("project_entry_${entry.name}"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (entry.isDirectory) Icons.Filled.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = if (entry.isDirectory) "Folder" else "File",
                tint = if (entry.isDirectory) AccentAmber else AccentCyan,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (entry.isDirectory) "Folder" else "${entry.sizeBytes} bytes",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
            IconButton(onClick = onRename, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = "Rename ${entry.name}", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete ${entry.name}", tint = AccentCoral, modifier = Modifier.size(18.dp))
            }
        }
    }
}
