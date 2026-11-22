package com.example.ui.viewmodel

import com.example.data.workspace.WorkspaceEntry
import com.example.data.workspace.WorkspaceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ProjectsUiState(
    val currentPath: String = "",
    val breadcrumbs: List<Pair<String, String>> = emptyList(),
    val entries: List<WorkspaceEntry> = emptyList(),
    val isAtRoot: Boolean = true,
    val statusMessage: String? = null,
)

class ProjectsViewModel(
    private val workspaceRepository: WorkspaceRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            workspaceRepository.ensureStarterWorkspace()
            navigateTo(workspaceRepository.rootDir.absolutePath)
        }
    }

    fun refresh() {
        val path = _uiState.value.currentPath.ifEmpty { workspaceRepository.rootDir.absolutePath }
        navigateTo(path)
    }

    fun navigateTo(dirPath: String) {
        scope.launch {
            val root = workspaceRepository.rootDir
            val target = File(dirPath).let { if (it.exists() && it.isDirectory) it else root }
            val entries = workspaceRepository.listDirectory(target)
            val rootAbs = root.absolutePath
            val targetAbs = target.absolutePath
            val rel = targetAbs.removePrefix(rootAbs).trimStart(File.separatorChar)

            val crumbs = mutableListOf("Projects" to rootAbs)
            if (rel.isNotEmpty()) {
                var acc = rootAbs
                rel.split(File.separatorChar).forEach { seg ->
                    acc = "$acc${File.separator}$seg"
                    crumbs.add(seg to acc)
                }
            }

            _uiState.update {
                it.copy(
                    currentPath = targetAbs,
                    breadcrumbs = crumbs,
                    entries = entries,
                    isAtRoot = targetAbs == rootAbs,
                )
            }
        }
    }

    fun navigateUp(): Boolean {
        val current = File(_uiState.value.currentPath)
        val root = workspaceRepository.rootDir
        if (current.absolutePath == root.absolutePath) return false
        val parent = current.parentFile ?: root
        navigateTo(parent.absolutePath)
        return true
    }

    fun createFile(name: String, initialContent: String = "", onCreated: ((File) -> Unit)? = null) {
        scope.launch {
            val dir = File(_uiState.value.currentPath.ifEmpty { workspaceRepository.rootDir.absolutePath })
            val template = initialContent.ifEmpty {
                "# $name\n\ndef main():\n    print(\"Running $name\")\n\nif __name__ == \"__main__\":\n    main()\n"
            }
            workspaceRepository.createFile(dir, name, template)
                .onSuccess { file ->
                    refresh()
                    _uiState.update { it.copy(statusMessage = "Created ${file.name}") }
                    onCreated?.invoke(file)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(statusMessage = err.message ?: "Failed to create file") }
                }
        }
    }

    fun createFolder(name: String) {
        scope.launch {
            val dir = File(_uiState.value.currentPath.ifEmpty { workspaceRepository.rootDir.absolutePath })
            workspaceRepository.createFolder(dir, name)
                .onSuccess { folder ->
                    refresh()
                    _uiState.update { it.copy(statusMessage = "Created folder '${folder.name}'") }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(statusMessage = err.message ?: "Failed to create folder") }
                }
        }
    }

    fun renameEntry(entry: WorkspaceEntry, newName: String, onRenamed: (File) -> Unit) {
        scope.launch {
            workspaceRepository.renameEntry(entry, newName)
                .onSuccess { file ->
                    refresh()
                    onRenamed(file)
                    _uiState.update { it.copy(statusMessage = "Renamed to ${file.name}") }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(statusMessage = err.message ?: "Failed to rename") }
                }
        }
    }

    fun deleteEntry(entry: WorkspaceEntry, onDeleted: (String) -> Unit) {
        scope.launch {
            workspaceRepository.deleteEntry(entry)
                .onSuccess {
                    refresh()
                    onDeleted(entry.absolutePath)
                    _uiState.update { it.copy(statusMessage = "Deleted ${entry.name}") }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(statusMessage = err.message ?: "Failed to delete") }
                }
        }
    }

    fun importExternalFile(fileName: String, content: String, onImported: (File) -> Unit) {
        scope.launch {
            val dir = File(_uiState.value.currentPath.ifEmpty { workspaceRepository.rootDir.absolutePath })
            var targetName = fileName.ifBlank { "imported.py" }
            var idx = 1
            while (File(dir, targetName).exists()) {
                val base = targetName.substringBeforeLast('.')
                val ext = targetName.substringAfterLast('.', "py")
                targetName = "${base}_$idx.$ext"
                idx++
            }
            workspaceRepository.createFile(dir, targetName, content)
                .onSuccess { file ->
                    refresh()
                    _uiState.update { it.copy(statusMessage = "Imported ${file.name}") }
                    onImported(file)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(statusMessage = err.message ?: "Import failed") }
                }
        }
    }

    fun dismissStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
