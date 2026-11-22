package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.NoteEntity
import com.example.data.db.NotesRepository
import com.example.data.db.PyForgeDatabase
import com.example.data.settings.SettingsRepository
import com.example.data.workspace.WorkspaceRepository
import com.example.python.CompletionItem
import com.example.python.ExecutionOutputCallback
import com.example.python.InputProvider
import com.example.python.InstallProgressCallback
import com.example.python.LibrariesCatalog
import com.example.python.ModuleDocResult
import com.example.python.PackageActionResult
import com.example.python.PyPiSearchResult
import com.example.python.PythonAutoIndent
import com.example.python.PythonRuntime
import com.example.python.RuntimeInfo
import com.example.python.SyntaxCheckResult
import com.example.python.UndoRedoManager
import com.example.python.VerifyImportResult
import com.example.ui.theme.PyForgeTheme
import com.example.ui.viewmodel.EditorViewModel
import com.example.ui.viewmodel.LibrariesViewModel
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.ProjectsViewModel
import com.example.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class PyForgeRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: PyForgeDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, PyForgeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `notes room persistence search pin and categories work offline`() = runBlocking {
        val repo = NotesRepository(db.noteDao())
        repo.seedDefaultsIfEmpty()

        val folders = repo.allFolders.first()
        assertTrue(folders.any { it.name == "Algorithms" })
        assertTrue(folders.any { it.name == "RoboContest" })

        val id = repo.insertNote(
            NoteEntity(
                title = "Dijkstra Shortest Path",
                description = "Priority queue graph algorithm",
                codeSnippet = "import heapq\ndef dijkstra(g, s): pass",
                category = "Algorithms",
                isPinned = false,
            )
        )
        assertTrue(id > 0)

        val found = repo.searchNotes("Algorithms", "heapq").first()
        assertEquals(1, found.size)
        assertEquals("Dijkstra Shortest Path", found.first().title)

        repo.togglePin(id, false)
        val updated = repo.getNoteById(id)
        assertNotNull(updated)
        assertTrue(updated!!.isPinned)

        repo.deleteNote(id)
        val afterDelete = repo.getNoteById(id)
        assertEquals(null, afterDelete)
    }

    @Test
    fun `workspace file manager create rename read write and delete`() = runBlocking {
        val workspace = WorkspaceRepository(context, db.projectMetaDao())
        val starter = workspace.ensureStarterWorkspace()
        assertTrue(starter.exists())

        val folderRes = workspace.createFolder(workspace.rootDir, "algo_lab")
        assertTrue(folderRes.isSuccess)
        val folder = folderRes.getOrThrow()

        val fileRes = workspace.createFile(folder, "solve", "print(42)\n")
        assertTrue(fileRes.isSuccess)
        val file = fileRes.getOrThrow()
        assertEquals("solve.py", file.name)
        assertEquals("print(42)\n", workspace.readText(file.absolutePath))

        val entries = workspace.listDirectory(folder)
        assertEquals(1, entries.size)
        val renamedRes = workspace.renameEntry(entries.first(), "solver_v2.py")
        assertTrue(renamedRes.isSuccess)
        assertEquals("solver_v2.py", renamedRes.getOrThrow().name)

        val updatedEntries = workspace.listDirectory(folder)
        assertEquals(1, updatedEntries.size)
        assertTrue(workspace.deleteEntry(updatedEntries.first()).isSuccess)
        assertTrue(workspace.listDirectory(folder).isEmpty())
    }

    @Test
    fun `editor undo redo and auto indentation`() {
        val mgr = UndoRedoManager(TextFieldValue("x = 1"))
        val v2 = TextFieldValue("x = 2")
        mgr.recordChange(v2)
        assertTrue(mgr.canUndo)
        assertEquals("x = 1", mgr.undo()?.text)
        assertTrue(mgr.canRedo)
        assertEquals("x = 2", mgr.redo()?.text)

        val prev = TextFieldValue("def hello():", TextRange(12))
        val typedNewline = TextFieldValue("def hello():\n", TextRange(13))
        val indented = PythonAutoIndent.applyOnNewline(prev, typedNewline, tabSize = 4)
        assertNotNull(indented)
        assertEquals("def hello():\n    ", indented!!.text)
        assertEquals(17, indented.selection.start)
    }

    @Test
    fun `compose navigation across all 5 ide tabs`() {
        val fakeRuntime = FakePythonRuntime()
        val notesRepo = NotesRepository(db.noteDao())
        val workspaceRepo = WorkspaceRepository(context, db.projectMetaDao())
        val settingsRepo = SettingsRepository(context)
        runBlocking {
            notesRepo.seedDefaultsIfEmpty()
            workspaceRepo.ensureStarterWorkspace()
        }

        val editorVm = EditorViewModel(fakeRuntime, workspaceRepo, settingsRepo)
        val librariesVm = LibrariesViewModel(fakeRuntime, workspaceRepo)
        val notesVm = NotesViewModel(notesRepo)
        val projectsVm = ProjectsViewModel(workspaceRepo)
        val settingsVm = SettingsViewModel(settingsRepo, fakeRuntime, workspaceRepo)

        composeRule.setContent {
            PyForgeTheme {
                PyForgeAppShell(
                    editorViewModel = editorVm,
                    librariesViewModel = librariesVm,
                    notesViewModel = notesVm,
                    projectsViewModel = projectsVm,
                    settingsViewModel = settingsVm,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("editor_run_button").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_libraries").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("libraries_search_input").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_notes").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notes_search_input").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_projects").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("projects_new_file_button").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("toggle_auto_save").assertIsDisplayed()
    }
}

private class FakePythonRuntime : PythonRuntime {
    override fun ensureStarted() {}
    override fun executeScript(
        filePath: String,
        code: String,
        workingDir: String,
        sitePackagesDir: String,
        plotOutputDir: String,
        inputProvider: InputProvider,
        outputCallback: ExecutionOutputCallback,
    ): Int {
        outputCallback.onStarted(1L)
        outputCallback.onStdout("Hello from Python\n")
        return 0
    }

    override fun interruptExecution(threadId: Long): Boolean = true
    override fun checkSyntax(code: String, filename: String): SyntaxCheckResult = SyntaxCheckResult(valid = true)
    override fun getCompletions(code: String, cursorPos: Int): List<CompletionItem> = emptyList()
    override fun getRuntimeInfo(sitePackagesDir: String): RuntimeInfo = RuntimeInfo(pythonVersion = "3.11.17")
    override fun listLibraries(sitePackagesDir: String): LibrariesCatalog = LibrariesCatalog()
    override fun verifyImport(importName: String, sitePackagesDir: String): VerifyImportResult =
        VerifyImportResult(ok = true, module = importName, version = "1.0")
    override fun getModuleDocs(moduleName: String, sitePackagesDir: String): ModuleDocResult =
        ModuleDocResult(ok = true, name = moduleName, doc = "Documentation")
    override fun searchPyPi(packageName: String, sitePackagesDir: String): PyPiSearchResult =
        PyPiSearchResult(ok = true, name = packageName, version = "1.0")
    override fun installPackage(
        packageName: String,
        sitePackagesDir: String,
        upgrade: Boolean,
        progressCallback: InstallProgressCallback,
    ): PackageActionResult = PackageActionResult(ok = true)
    override fun uninstallPackage(packageName: String, sitePackagesDir: String): PackageActionResult =
        PackageActionResult(ok = true)
}
