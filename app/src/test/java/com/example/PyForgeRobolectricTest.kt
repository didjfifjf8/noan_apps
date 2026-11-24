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
import com.example.data.db.StudyBlockEntity
import com.example.data.db.StudyBlockType
import com.example.data.db.StudyBookRepository
import com.example.data.db.StudyJsonCodec
import com.example.data.settings.SettingsRepository
import com.example.data.workspace.WorkspaceRepository
import com.example.python.CompletionItem
import com.example.python.ExecutionOutputCallback
import com.example.python.HighlightTextUtils
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
import com.example.ui.viewmodel.StudyBookViewModel
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
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
    fun `compose navigation across all 5 ide tabs and kitobcha switch`() {
        val fakeRuntime = FakePythonRuntime()
        val notesRepo = NotesRepository(db.noteDao())
        val studyRepo = StudyBookRepository(context, db.studyDao())
        val workspaceRepo = WorkspaceRepository(context, db.projectMetaDao())
        val settingsRepo = SettingsRepository(context)
        runBlocking {
            notesRepo.seedDefaultsIfEmpty()
            studyRepo.seedDefaultTopicIfEmpty()
            workspaceRepo.ensureStarterWorkspace()
        }

        val editorVm = EditorViewModel(fakeRuntime, workspaceRepo, settingsRepo)
        val librariesVm = LibrariesViewModel(fakeRuntime, workspaceRepo)
        val notesVm = NotesViewModel(notesRepo)
        val studyVm = StudyBookViewModel(studyRepo, notesRepo)
        val projectsVm = ProjectsViewModel(workspaceRepo)
        val settingsVm = SettingsViewModel(settingsRepo, fakeRuntime, workspaceRepo)

        composeRule.setContent {
            PyForgeTheme {
                PyForgeAppShell(
                    editorViewModel = editorVm,
                    librariesViewModel = librariesVm,
                    notesViewModel = notesVm,
                    studyBookViewModel = studyVm,
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

        // Switch into Kitobcha inside My Notes
        composeRule.onNodeWithTag("notes_mode_kitobcha").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("study_book_search_input").assertIsDisplayed()
        composeRule.onNodeWithTag("study_book_fab_add").assertIsDisplayed()
        composeRule.onNodeWithText("Dinamik Dasturlash (DP)").assertIsDisplayed()

        // Open new topic in Kitobcha
        composeRule.onNodeWithTag("study_book_fab_add").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("study_topic_title_input").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("study_topic_title_input").assertIsDisplayed()
        composeRule.onNodeWithTag("study_add_diagram_block").assertExists()

        composeRule.onNodeWithTag("nav_projects").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("projects_new_file_button").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("toggle_auto_save").assertIsDisplayed()
    }

    @Test
    fun `study book repository blocks ordering and persistence`() = runBlocking {
        val studyRepo = StudyBookRepository(context, db.studyDao())
        studyRepo.seedDefaultTopicIfEmpty()

        val topics = studyRepo.searchTopics("All", "").first()
        assertEquals(1, topics.size)
        assertEquals("Dinamik Dasturlash (DP)", topics.first().title)

        val dpBlocks = studyRepo.getBlocks(topics.first().id)
        assertEquals(3, dpBlocks.size)
        assertEquals(StudyBlockType.TEXT.raw, dpBlocks[0].blockType)
        assertEquals(StudyBlockType.CODE_NOTE.raw, dpBlocks[1].blockType)
        assertEquals(StudyBlockType.DIAGRAM.raw, dpBlocks[2].blockType)

        val topicId = studyRepo.createTopic("Graflar nazariyasi", "Algorithms")
        val codeId = studyRepo.addBlock(
            topicId,
            StudyBlockEntity(
                topicId = topicId,
                orderIndex = 0,
                blockType = StudyBlockType.CODE_NOTE.raw,
                primaryText = "def bfs(g): pass",
                secondaryText = "Kenglik bo'yicha qidiruv",
            ),
        )
        assertTrue(codeId > 0)

        var blocks = studyRepo.getBlocks(topicId)
        assertEquals(2, blocks.size)
        assertEquals(StudyBlockType.TEXT.raw, blocks[0].blockType)
        assertEquals(StudyBlockType.CODE_NOTE.raw, blocks[1].blockType)

        studyRepo.moveBlock(topicId, codeId, -1)
        blocks = studyRepo.getBlocks(topicId)
        assertEquals(StudyBlockType.CODE_NOTE.raw, blocks[0].blockType)
        assertEquals(0, blocks[0].orderIndex)
        assertEquals(1, blocks[1].orderIndex)

        studyRepo.deleteTopic(topicId)
        assertEquals(null, studyRepo.getTopicById(topicId))
    }

    @Test
    fun `highlighter spans and linear diagram json codec work accurately`() {
        val sample = "Dinamik dasturlash usuli"
        val word = HighlightTextUtils.wordRangeAt(sample, 10)
        assertEquals(8 until 18, word)

        val added = HighlightTextUtils.addHighlight(emptyList(), 0, 7, "yellow")
        val second = HighlightTextUtils.addHighlight(added, 5, 18, "green")
        assertEquals(2, second.size)
        assertEquals(0, second[0].start)
        assertEquals(5, second[0].end)
        assertEquals("yellow", second[0].colorKey)
        assertEquals(5, second[1].start)
        assertEquals(18, second[1].end)
        assertEquals("green", second[1].colorKey)

        val shifted = HighlightTextUtils.adjustSpansForTextEdit(
            oldText = "Salom dunyo",
            newText = "Salom katta dunyo",
            spans = HighlightTextUtils.addHighlight(emptyList(), 6, 11, "cyan"),
        )
        assertEquals(1, shifted.size)
        assertEquals(12, shifted[0].start)
        assertEquals(17, shifted[0].end)

        val cleared = HighlightTextUtils.removeHighlightInRange(second, 6, 10)
        assertEquals(1, cleared.size)
        assertEquals("yellow", cleared[0].colorKey)

        val preset = StudyJsonCodec.linearPreset()
        val encoded = StudyJsonCodec.encodeDiagram(preset)
        val decoded = StudyJsonCodec.parseDiagram(encoded)
        assertEquals(preset.nodes.size, decoded.nodes.size)
        assertEquals(preset.edges.size, decoded.edges.size)
        assertEquals("Boshlash", decoded.nodes.first().label)
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
