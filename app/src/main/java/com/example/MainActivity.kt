package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.LibrariesScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BgCanvas
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BgSurfaceHover
import com.example.ui.theme.PyForgeTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.EditorViewModel
import com.example.ui.viewmodel.LibrariesViewModel
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.ProjectsViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.StudyBookViewModel

enum class MainNavDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
) {
    EDITOR("Editor", Icons.Filled.Code, Icons.Outlined.Code, "nav_editor"),
    LIBRARIES("Libraries", Icons.Filled.Extension, Icons.Outlined.Extension, "nav_libraries"),
    NOTES("My Notes", Icons.Filled.NoteAlt, Icons.Outlined.NoteAlt, "nav_notes"),
    PROJECTS("Projects", Icons.Filled.Folder, Icons.Outlined.Folder, "nav_projects"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings"),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PyForgeTheme {
                PyForgeRoot()
            }
        }
    }
}

@Composable
fun PyForgeRoot() {
    val app = LocalContext.current.applicationContext as PyForgeApp

    val editorViewModel = remember {
        EditorViewModel(app.pythonRuntime, app.workspaceRepository, app.settingsRepository)
    }
    val librariesViewModel = remember {
        LibrariesViewModel(app.pythonRuntime, app.workspaceRepository)
    }
    val notesViewModel = remember {
        NotesViewModel(app.notesRepository)
    }
    val studyBookViewModel = remember {
        StudyBookViewModel(app.studyBookRepository, app.notesRepository)
    }
    val projectsViewModel = remember {
        ProjectsViewModel(app.workspaceRepository)
    }
    val settingsViewModel = remember {
        SettingsViewModel(app.settingsRepository, app.pythonRuntime, app.workspaceRepository)
    }

    PyForgeAppShell(
        editorViewModel = editorViewModel,
        librariesViewModel = librariesViewModel,
        notesViewModel = notesViewModel,
        studyBookViewModel = studyBookViewModel,
        projectsViewModel = projectsViewModel,
        settingsViewModel = settingsViewModel,
    )
}

@Composable
fun PyForgeAppShell(
    editorViewModel: EditorViewModel,
    librariesViewModel: LibrariesViewModel,
    notesViewModel: NotesViewModel,
    studyBookViewModel: StudyBookViewModel,
    projectsViewModel: ProjectsViewModel,
    settingsViewModel: SettingsViewModel,
) {
    var currentTab by rememberSaveable { mutableStateOf(MainNavDestination.EDITOR) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = BgCanvas,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = BgSurface,
                        tonalElevation = 0.dp,
                    ) {
                        MainNavDestination.entries.forEach { dest ->
                            val selected = currentTab == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = dest },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label,
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AccentCyan,
                                    selectedTextColor = AccentCyan,
                                    indicatorColor = BgSurfaceHover,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                ),
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = BgSurface,
                    ) {
                        MainNavDestination.entries.forEach { dest ->
                            val selected = currentTab == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = dest },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label,
                                    )
                                },
                                label = { Text(dest.label, fontSize = 11.sp, maxLines = 1) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = AccentCyan,
                                    selectedTextColor = AccentCyan,
                                    indicatorColor = BgSurfaceHover,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                ),
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                }

                when (currentTab) {
                    MainNavDestination.EDITOR -> EditorScreen(
                        viewModel = editorViewModel,
                        modifier = Modifier.weight(1f),
                    )
                    MainNavDestination.LIBRARIES -> LibrariesScreen(
                        viewModel = librariesViewModel,
                        onInsertImportToEditor = { importName ->
                            editorViewModel.insertQuickText("import $importName\n")
                            currentTab = MainNavDestination.EDITOR
                        },
                        modifier = Modifier.weight(1f),
                    )
                    MainNavDestination.NOTES -> NotesScreen(
                        viewModel = notesViewModel,
                        studyBookViewModel = studyBookViewModel,
                        onCopyCodeToEditor = { title, code ->
                            editorViewModel.insertCodeFromNote(title, code)
                            currentTab = MainNavDestination.EDITOR
                        },
                        modifier = Modifier.weight(1f),
                    )
                    MainNavDestination.PROJECTS -> ProjectsScreen(
                        viewModel = projectsViewModel,
                        onOpenFileInEditor = { filePath ->
                            editorViewModel.openFile(filePath)
                            currentTab = MainNavDestination.EDITOR
                        },
                        onFileRenamed = { oldPath, newFile ->
                            editorViewModel.updateRenamedTab(oldPath, newFile)
                        },
                        onPathDeleted = { deletedPath ->
                            editorViewModel.closeDeletedPathTabs(deletedPath)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    MainNavDestination.SETTINGS -> SettingsScreen(
                        viewModel = settingsViewModel,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
