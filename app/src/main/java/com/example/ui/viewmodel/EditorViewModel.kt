package com.example.ui.viewmodel

import android.os.SystemClock
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.settings.IdeSettings
import com.example.data.settings.SettingsRepository
import com.example.data.workspace.WorkspaceRepository
import com.example.python.CompletionItem
import com.example.python.ExecutionOutputCallback
import com.example.python.InputProvider
import com.example.python.PythonAutoIndent
import com.example.python.PythonRuntime
import com.example.python.SyntaxCheckResult
import com.example.python.UndoRedoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.LinkedBlockingQueue

data class EditorTab(
    val filePath: String,
    val fileName: String,
    val textValue: TextFieldValue,
    val isDirty: Boolean = false,
    val syntaxResult: SyntaxCheckResult = SyntaxCheckResult(valid = true),
    val undoRedo: UndoRedoManager = UndoRedoManager(textValue),
)

enum class ConsoleEntryType {
    STDOUT,
    STDERR,
    INPUT_ECHO,
    STATUS,
    PLOT,
}

data class ConsoleEntry(
    val id: Long,
    val type: ConsoleEntryType,
    val text: String,
)

data class FindReplaceState(
    val visible: Boolean = false,
    val findQuery: String = "",
    val replaceQuery: String = "",
    val caseSensitive: Boolean = false,
    val matchRanges: List<IntRange> = emptyList(),
    val activeMatchIndex: Int = -1,
)

data class EditorUiState(
    val tabs: List<EditorTab> = emptyList(),
    val activeTabIndex: Int = 0,
    val completions: List<CompletionItem> = emptyList(),
    val findReplace: FindReplaceState = FindReplaceState(),
    val isConsoleVisible: Boolean = false,
    val isRunning: Boolean = false,
    val isWaitingForInput: Boolean = false,
    val inputPrompt: String = "",
    val consoleEntries: List<ConsoleEntry> = emptyList(),
    val lastExitCode: Int? = null,
    val lastDurationMs: Long? = null,
    val statusBanner: String? = null,
) {
    val activeTab: EditorTab? get() = tabs.getOrNull(activeTabIndex)
}

private sealed interface InputSignal {
    data class Value(val text: String) : InputSignal
    data object Cancel : InputSignal
}

class EditorViewModel(
    private val pythonRuntime: PythonRuntime,
    private val workspaceRepository: WorkspaceRepository,
    settingsRepository: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val settings: StateFlow<IdeSettings> = settingsRepository.settings.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = IdeSettings(),
    )

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var saveJob: Job? = null
    private var lintJob: Job? = null
    private var completionJob: Job? = null
    private var timeoutJob: Job? = null
    private var executionThread: Thread? = null
    @Volatile
    private var activePythonThreadId: Long = 0L
    private var inputQueue = LinkedBlockingQueue<InputSignal>()
    private var consoleEntryId = 0L

    init {
        scope.launch {
            val starterFile = workspaceRepository.ensureStarterWorkspace()
            openFile(starterFile.absolutePath)
        }
    }

    fun openFile(filePath: String) {
        val currentTabs = _uiState.value.tabs
        val existingIdx = currentTabs.indexOfFirst { it.filePath == filePath }
        if (existingIdx >= 0) {
            _uiState.update { it.copy(activeTabIndex = existingIdx) }
            refreshFindMatches()
            return
        }

        scope.launch {
            val content = workspaceRepository.readText(filePath)
            val file = File(filePath)
            workspaceRepository.recordFileOpened(filePath)
            val tfv = TextFieldValue(content)
            val newTab = EditorTab(
                filePath = filePath,
                fileName = file.name,
                textValue = tfv,
                isDirty = false,
                undoRedo = UndoRedoManager(tfv),
            )
            _uiState.update { state ->
                val updated = state.tabs + newTab
                state.copy(
                    tabs = updated,
                    activeTabIndex = updated.lastIndex,
                )
            }
            scheduleSyntaxCheck(filePath, content)
            refreshFindMatches()
        }
    }

    fun createNewScratchTab() {
        scope.launch {
            val root = workspaceRepository.rootDir
            var idx = 1
            while (File(root, "script_$idx.py").exists()) {
                idx++
            }
            val result = workspaceRepository.createFile(
                root,
                "script_$idx.py",
                "# script_$idx.py\n\nprint('Hello from PyForge!')\n",
            )
            result.onSuccess { file ->
                openFile(file.absolutePath)
            }
        }
    }

    fun insertCodeFromNote(title: String, code: String) {
        val tab = _uiState.value.activeTab
        if (tab == null) {
            scope.launch {
                val safeTitle = title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifEmpty { "note_snippet" }
                val root = workspaceRepository.rootDir
                var candidate = "$safeTitle.py"
                var idx = 1
                while (File(root, candidate).exists()) {
                    candidate = "${safeTitle}_$idx.py"
                    idx++
                }
                workspaceRepository.createFile(root, candidate, code).onSuccess { openFile(it.absolutePath) }
            }
            return
        }
        val current = tab.textValue
        val cursor = current.selection.start.coerceIn(0, current.text.length)
        val prefix = if (current.text.isNotEmpty() && cursor > 0 && current.text[cursor - 1] != '\n') "\n" else ""
        val inserted = "$prefix$code\n"
        val updatedText = buildString {
            append(current.text, 0, cursor)
            append(inserted)
            append(current.text, cursor, current.text.length)
        }
        val newCursor = cursor + inserted.length
        onCodeChanged(TextFieldValue(updatedText, TextRange(newCursor)), bypassAutoIndent = true)
        _uiState.update { it.copy(statusBanner = "Inserted snippet from '$title'") }
    }

    fun selectTab(index: Int) {
        if (index in _uiState.value.tabs.indices) {
            saveActiveTabImmediate()
            _uiState.update { it.copy(activeTabIndex = index, completions = emptyList()) }
            refreshFindMatches()
        }
    }

    fun closeTab(index: Int) {
        val tabs = _uiState.value.tabs
        if (index !in tabs.indices) return
        val closing = tabs[index]
        if (closing.isDirty) {
            scope.launch {
                workspaceRepository.writeText(closing.filePath, closing.textValue.text)
            }
        }
        _uiState.update { state ->
            val nextTabs = state.tabs.toMutableList().apply { removeAt(index) }
            val nextIndex = when {
                nextTabs.isEmpty() -> 0
                state.activeTabIndex >= nextTabs.size -> nextTabs.lastIndex
                index < state.activeTabIndex -> state.activeTabIndex - 1
                else -> state.activeTabIndex
            }
            state.copy(tabs = nextTabs, activeTabIndex = nextIndex, completions = emptyList())
        }
    }

    fun onCodeChanged(incoming: TextFieldValue, bypassAutoIndent: Boolean = false) {
        val tab = _uiState.value.activeTab ?: return
        val tabSize = settings.value.tabSize
        val finalValue = if (!bypassAutoIndent) {
            PythonAutoIndent.applyOnNewline(tab.textValue, incoming, tabSize) ?: incoming
        } else {
            incoming
        }
        val textChanged = finalValue.text != tab.textValue.text
        if (textChanged) {
            tab.undoRedo.recordChange(finalValue)
        }

        _uiState.update { state ->
            val updatedTabs = state.tabs.toMutableList()
            val idx = state.activeTabIndex
            if (idx in updatedTabs.indices) {
                updatedTabs[idx] = updatedTabs[idx].copy(
                    textValue = finalValue,
                    isDirty = updatedTabs[idx].isDirty || textChanged,
                )
            }
            state.copy(tabs = updatedTabs)
        }

        if (textChanged) {
            if (settings.value.autoSave) {
                scheduleAutoSave(tab.filePath, finalValue.text)
            }
            scheduleSyntaxCheck(tab.filePath, finalValue.text)
            refreshFindMatches()
        }
        scheduleCompletions(finalValue.text, finalValue.selection.start)
    }

    fun insertQuickText(snippet: String) {
        val tab = _uiState.value.activeTab ?: return
        val current = tab.textValue
        val start = minOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        val end = maxOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        val textToInsert = if (snippet == "\t") " ".repeat(settings.value.tabSize) else snippet

        val updated = buildString {
            append(current.text, 0, start)
            append(textToInsert)
            append(current.text, end, current.text.length)
        }
        val newCursor = if (textToInsert.endsWith("()")) {
            start + textToInsert.length - 1
        } else {
            start + textToInsert.length
        }
        onCodeChanged(TextFieldValue(updated, TextRange(newCursor)), bypassAutoIndent = true)
    }

    fun applyCompletion(item: CompletionItem) {
        val tab = _uiState.value.activeTab ?: return
        val current = tab.textValue
        val cursor = current.selection.start.coerceIn(0, current.text.length)
        val before = current.text.substring(0, cursor)
        val lineStart = before.lastIndexOf('\n') + 1
        val linePrefix = before.substring(lineStart)

        val tokenStartInLine = run {
            val dotIdx = linePrefix.lastIndexOf('.')
            val wordMatch = Regex("([a-zA-Z_][a-zA-Z0-9_]*)$").find(linePrefix)
            when {
                dotIdx >= 0 && !linePrefix.substring(dotIdx + 1).contains(' ') -> dotIdx + 1
                wordMatch != null -> wordMatch.range.first
                else -> linePrefix.length
            }
        }
        val replaceStart = lineStart + tokenStartInLine
        val updatedText = buildString {
            append(current.text, 0, replaceStart)
            append(item.insert)
            append(current.text, cursor, current.text.length)
        }
        val newCursor = if (item.insert.endsWith("()")) {
            replaceStart + item.insert.length - 1
        } else {
            replaceStart + item.insert.length
        }
        _uiState.update { it.copy(completions = emptyList()) }
        onCodeChanged(TextFieldValue(updatedText, TextRange(newCursor)), bypassAutoIndent = true)
    }

    fun undo() {
        val tab = _uiState.value.activeTab ?: return
        val prev = tab.undoRedo.undo() ?: return
        updateTabValueDirect(prev)
    }

    fun redo() {
        val tab = _uiState.value.activeTab ?: return
        val next = tab.undoRedo.redo() ?: return
        updateTabValueDirect(next)
    }

    private fun updateTabValueDirect(value: TextFieldValue) {
        _uiState.update { state ->
            val updated = state.tabs.toMutableList()
            val idx = state.activeTabIndex
            if (idx in updated.indices) {
                val tab = updated[idx]
                updated[idx] = tab.copy(textValue = value, isDirty = true)
                if (settings.value.autoSave) {
                    scheduleAutoSave(tab.filePath, value.text)
                }
                scheduleSyntaxCheck(tab.filePath, value.text)
            }
            state.copy(tabs = updated)
        }
    }

    fun saveActiveTabImmediate() {
        val tab = _uiState.value.activeTab ?: return
        saveJob?.cancel()
        scope.launch {
            workspaceRepository.writeText(tab.filePath, tab.textValue.text)
            markTabClean(tab.filePath)
        }
    }

    private fun scheduleAutoSave(filePath: String, content: String) {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(450)
            workspaceRepository.writeText(filePath, content)
            markTabClean(filePath)
        }
    }

    private fun markTabClean(filePath: String) {
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.map { t ->
                    if (t.filePath == filePath) t.copy(isDirty = false) else t
                }
            )
        }
    }

    private fun scheduleSyntaxCheck(filePath: String, code: String) {
        lintJob?.cancel()
        lintJob = scope.launch {
            delay(300)
            val result = withContext(Dispatchers.IO) {
                pythonRuntime.checkSyntax(code, filePath)
            }
            _uiState.update { state ->
                state.copy(
                    tabs = state.tabs.map { t ->
                        if (t.filePath == filePath) t.copy(syntaxResult = result) else t
                    }
                )
            }
        }
    }

    private fun scheduleCompletions(code: String, cursorPos: Int) {
        completionJob?.cancel()
        completionJob = scope.launch {
            delay(140)
            val items = withContext(Dispatchers.IO) {
                pythonRuntime.getCompletions(code, cursorPos)
            }
            _uiState.update { it.copy(completions = items) }
        }
    }

    // Find & Replace
    fun toggleFindReplace(visible: Boolean? = null) {
        _uiState.update { state ->
            val next = visible ?: !state.findReplace.visible
            state.copy(findReplace = state.findReplace.copy(visible = next))
        }
        refreshFindMatches()
    }

    fun updateFindQuery(query: String) {
        _uiState.update { it.copy(findReplace = it.findReplace.copy(findQuery = query)) }
        refreshFindMatches()
    }

    fun updateReplaceQuery(query: String) {
        _uiState.update { it.copy(findReplace = it.findReplace.copy(replaceQuery = query)) }
    }

    fun toggleCaseSensitive() {
        _uiState.update {
            it.copy(findReplace = it.findReplace.copy(caseSensitive = !it.findReplace.caseSensitive))
        }
        refreshFindMatches()
    }

    private fun refreshFindMatches() {
        val state = _uiState.value
        val tab = state.activeTab
        val q = state.findReplace.findQuery
        if (tab == null || !state.findReplace.visible || q.isEmpty()) {
            _uiState.update {
                it.copy(findReplace = it.findReplace.copy(matchRanges = emptyList(), activeMatchIndex = -1))
            }
            return
        }
        val text = tab.textValue.text
        val ignoreCase = !state.findReplace.caseSensitive
        val matches = mutableListOf<IntRange>()
        var idx = text.indexOf(q, startIndex = 0, ignoreCase = ignoreCase)
        while (idx >= 0) {
            matches.add(idx until (idx + q.length))
            idx = text.indexOf(q, startIndex = idx + maxOf(1, q.length), ignoreCase = ignoreCase)
        }
        _uiState.update {
            val prevIdx = it.findReplace.activeMatchIndex
            val nextIdx = when {
                matches.isEmpty() -> -1
                prevIdx in matches.indices -> prevIdx
                else -> 0
            }
            it.copy(findReplace = it.findReplace.copy(matchRanges = matches, activeMatchIndex = nextIdx))
        }
    }

    fun navigateMatch(delta: Int) {
        val fr = _uiState.value.findReplace
        val tab = _uiState.value.activeTab ?: return
        if (fr.matchRanges.isEmpty()) return
        val nextIdx = Math.floorMod(fr.activeMatchIndex + delta, fr.matchRanges.size)
        val range = fr.matchRanges[nextIdx]
        _uiState.update { state ->
            val updatedTabs = state.tabs.toMutableList()
            val idx = state.activeTabIndex
            if (idx in updatedTabs.indices) {
                updatedTabs[idx] = updatedTabs[idx].copy(
                    textValue = updatedTabs[idx].textValue.copy(
                        selection = TextRange(range.first, range.last + 1)
                    )
                )
            }
            state.copy(
                tabs = updatedTabs,
                findReplace = state.findReplace.copy(activeMatchIndex = nextIdx),
            )
        }
    }

    fun replaceCurrentMatch() {
        val state = _uiState.value
        val fr = state.findReplace
        val tab = state.activeTab ?: return
        val idx = fr.activeMatchIndex
        if (idx !in fr.matchRanges.indices) return
        val range = fr.matchRanges[idx]
        val text = tab.textValue.text
        val replaced = buildString {
            append(text, 0, range.first)
            append(fr.replaceQuery)
            append(text, range.last + 1, text.length)
        }
        val newCursor = range.first + fr.replaceQuery.length
        onCodeChanged(TextFieldValue(replaced, TextRange(newCursor)), bypassAutoIndent = true)
    }

    fun replaceAllMatches() {
        val state = _uiState.value
        val fr = state.findReplace
        val tab = state.activeTab ?: return
        if (fr.findQuery.isEmpty() || fr.matchRanges.isEmpty()) return
        val updated = tab.textValue.text.replace(
            fr.findQuery,
            fr.replaceQuery,
            ignoreCase = !fr.caseSensitive,
        )
        val count = fr.matchRanges.size
        onCodeChanged(TextFieldValue(updated, TextRange(0)), bypassAutoIndent = true)
        _uiState.update { it.copy(statusBanner = "Replaced $count occurrence(s)") }
    }

    // Execution & Console
    fun setConsoleVisible(visible: Boolean) {
        _uiState.update { it.copy(isConsoleVisible = visible) }
    }

    fun clearConsole() {
        _uiState.update {
            it.copy(consoleEntries = emptyList(), lastExitCode = null, lastDurationMs = null)
        }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }

    fun runCurrentFile() {
        val tab = _uiState.value.activeTab ?: return
        if (_uiState.value.isRunning) return

        val code = tab.textValue.text
        val filePath = tab.filePath
        val workingDir = File(filePath).parentFile?.absolutePath ?: workspaceRepository.rootDir.absolutePath
        val sitePackagesDir = workspaceRepository.sitePackagesDir.absolutePath
        val plotDir = workspaceRepository.plotsCacheDir.absolutePath

        inputQueue = LinkedBlockingQueue()
        activePythonThreadId = 0L

        _uiState.update {
            it.copy(
                isConsoleVisible = true,
                isRunning = true,
                isWaitingForInput = false,
                inputPrompt = "",
                lastExitCode = null,
                lastDurationMs = null,
                consoleEntries = listOf(
                    ConsoleEntry(++consoleEntryId, ConsoleEntryType.STATUS, "▶ Running ${tab.fileName}...")
                ),
            )
        }

        val timeoutSec = settings.value.executionTimeoutSeconds
        if (timeoutSec > 0) {
            timeoutJob?.cancel()
            timeoutJob = scope.launch {
                delay(timeoutSec * 1000L)
                if (_uiState.value.isRunning) {
                    appendConsole(ConsoleEntryType.STDERR, "\n[Timeout] Execution exceeded ${timeoutSec}s limit.\n")
                    stopExecution()
                }
            }
        }

        val worker = Thread {
            val startTime = SystemClock.elapsedRealtime()
            try {
                // Save file synchronously before executing
                File(filePath).writeText(code)
                scope.launch { markTabClean(filePath) }

                val exitCode = pythonRuntime.executeScript(
                    filePath = filePath,
                    code = code,
                    workingDir = workingDir,
                    sitePackagesDir = sitePackagesDir,
                    plotOutputDir = plotDir,
                    inputProvider = object : InputProvider {
                        override fun requestInput(prompt: String): String? {
                            scope.launch {
                                _uiState.update {
                                    it.copy(isWaitingForInput = true, inputPrompt = prompt)
                                }
                            }
                            return when (val signal = inputQueue.take()) {
                                is InputSignal.Value -> {
                                    scope.launch {
                                        _uiState.update { it.copy(isWaitingForInput = false, inputPrompt = "") }
                                    }
                                    signal.text
                                }
                                InputSignal.Cancel -> {
                                    scope.launch {
                                        _uiState.update { it.copy(isWaitingForInput = false, inputPrompt = "") }
                                    }
                                    null
                                }
                            }
                        }
                    },
                    outputCallback = object : ExecutionOutputCallback {
                        override fun onStarted(threadId: Long) {
                            activePythonThreadId = threadId
                        }

                        override fun onStdout(text: String) {
                            appendConsole(ConsoleEntryType.STDOUT, text)
                        }

                        override fun onStderr(text: String) {
                            appendConsole(ConsoleEntryType.STDERR, text)
                        }

                        override fun onPlot(imagePath: String) {
                            appendConsole(ConsoleEntryType.PLOT, imagePath)
                        }
                    },
                )
                val elapsed = SystemClock.elapsedRealtime() - startTime
                scope.launch {
                    timeoutJob?.cancel()
                    _uiState.update {
                        it.copy(
                            isRunning = false,
                            isWaitingForInput = false,
                            inputPrompt = "",
                            lastExitCode = exitCode,
                            lastDurationMs = elapsed,
                            consoleEntries = it.consoleEntries + ConsoleEntry(
                                ++consoleEntryId,
                                ConsoleEntryType.STATUS,
                                "■ Finished (exit $exitCode) in ${elapsed}ms",
                            ),
                        )
                    }
                }
            } catch (t: Throwable) {
                val elapsed = SystemClock.elapsedRealtime() - startTime
                scope.launch {
                    timeoutJob?.cancel()
                    _uiState.update {
                        it.copy(
                            isRunning = false,
                            isWaitingForInput = false,
                            lastExitCode = 1,
                            lastDurationMs = elapsed,
                            consoleEntries = it.consoleEntries + ConsoleEntry(
                                ++consoleEntryId,
                                ConsoleEntryType.STDERR,
                                "Runtime error: ${t.message}\n",
                            ),
                        )
                    }
                }
            }
        }
        worker.name = "PyForge-Runner"
        executionThread = worker
        worker.start()
    }

    fun submitInput(inputText: String) {
        if (!_uiState.value.isRunning) return
        inputQueue.offer(InputSignal.Value(inputText))
    }

    fun stopExecution() {
        timeoutJob?.cancel()
        inputQueue.offer(InputSignal.Cancel)
        val tid = activePythonThreadId
        if (tid != 0L) {
            scope.launch(Dispatchers.IO) {
                pythonRuntime.interruptExecution(tid)
            }
        }
    }

    private fun appendConsole(type: ConsoleEntryType, text: String) {
        scope.launch {
            _uiState.update { state ->
                val list = state.consoleEntries.toMutableList()
                // Coalesce consecutive stdout/stderr chunks for smooth performance on loops
                val last = list.lastOrNull()
                if (last != null && last.type == type && (type == ConsoleEntryType.STDOUT || type == ConsoleEntryType.STDERR) && last.text.length < 4000) {
                    list[list.lastIndex] = last.copy(text = last.text + text)
                } else {
                    list.add(ConsoleEntry(++consoleEntryId, type, text))
                }
                // Keep console bounded to prevent OOM on infinite print loops
                val trimmed = if (list.size > 400) list.takeLast(350) else list
                state.copy(consoleEntries = trimmed)
            }
        }
    }

    fun reloadFileIfOpen(filePath: String) {
        val openIdx = _uiState.value.tabs.indexOfFirst { it.filePath == filePath }
        if (openIdx >= 0) {
            scope.launch {
                val content = workspaceRepository.readText(filePath)
                val tfv = TextFieldValue(content)
                _uiState.update { state ->
                    val updated = state.tabs.toMutableList()
                    if (openIdx in updated.indices) {
                        updated[openIdx] = updated[openIdx].copy(
                            textValue = tfv,
                            isDirty = false,
                            undoRedo = UndoRedoManager(tfv),
                        )
                    }
                    state.copy(tabs = updated)
                }
            }
        }
    }

    fun updateRenamedTab(oldPath: String, newFile: File) {
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.map { tab ->
                    if (tab.filePath == oldPath) {
                        tab.copy(filePath = newFile.absolutePath, fileName = newFile.name)
                    } else tab
                }
            )
        }
    }

    fun closeDeletedPathTabs(deletedPath: String) {
        _uiState.update { state ->
            val filtered = state.tabs.filterNot {
                it.filePath == deletedPath || it.filePath.startsWith("$deletedPath${File.separator}")
            }
            val nextIdx = if (filtered.isEmpty()) 0 else state.activeTabIndex.coerceIn(0, filtered.lastIndex)
            state.copy(tabs = filtered, activeTabIndex = nextIdx)
        }
    }
}
