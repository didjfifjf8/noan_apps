package com.example.ui.viewmodel

import com.example.data.workspace.WorkspaceRepository
import com.example.python.InstallProgressCallback
import com.example.python.LibrariesCatalog
import com.example.python.LibraryItem
import com.example.python.ModuleDocResult
import com.example.python.PyPiSearchResult
import com.example.python.PythonRuntime
import com.example.python.RuntimeInfo
import com.example.python.VerifyImportResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LibraryFilter(val label: String) {
    INSTALLED("Installed"),
    STDLIB("Standard Lib"),
    PYPI_PURE("Pure PyPI"),
    INCOMPATIBLE("Incompatible"),
}

data class LibrariesUiState(
    val isLoading: Boolean = true,
    val runtimeInfo: RuntimeInfo = RuntimeInfo(),
    val catalog: LibrariesCatalog = LibrariesCatalog(),
    val searchQuery: String = "",
    val selectedFilter: LibraryFilter = LibraryFilter.INSTALLED,
    val pypiSearchResult: PyPiSearchResult? = null,
    val isSearchingPyPi: Boolean = false,
    val isInstalling: Boolean = false,
    val installingPackageName: String = "",
    val installProgressMessage: String = "",
    val installProgressPercent: Int = 0,
    val installLogs: List<String> = emptyList(),
    val activeDoc: ModuleDocResult? = null,
    val isLoadingDoc: Boolean = false,
    val lastVerifyResult: VerifyImportResult? = null,
    val statusMessage: String? = null,
)

class LibrariesViewModel(
    private val pythonRuntime: PythonRuntime,
    private val workspaceRepository: WorkspaceRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(LibrariesUiState())
    val uiState: StateFlow<LibrariesUiState> = _uiState.asStateFlow()

    init {
        refreshCatalog()
    }

    fun refreshCatalog() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val siteDir = workspaceRepository.sitePackagesDir.absolutePath
            val (info, catalog) = withContext(Dispatchers.IO) {
                pythonRuntime.ensureStarted()
                pythonRuntime.getRuntimeInfo(siteDir) to pythonRuntime.listLibraries(siteDir)
            }
            _uiState.update {
                it.copy(isLoading = false, runtimeInfo = info, catalog = catalog)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query, pypiSearchResult = null) }
    }

    fun selectFilter(filter: LibraryFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun searchOnPyPi(packageName: String) {
        val trimmed = packageName.trim()
        if (trimmed.isEmpty()) return
        scope.launch {
            _uiState.update { it.copy(isSearchingPyPi = true, pypiSearchResult = null) }
            val siteDir = workspaceRepository.sitePackagesDir.absolutePath
            val res = withContext(Dispatchers.IO) {
                pythonRuntime.searchPyPi(trimmed, siteDir)
            }
            _uiState.update { it.copy(isSearchingPyPi = false, pypiSearchResult = res) }
        }
    }

    fun installPackage(packageName: String, upgrade: Boolean = false) {
        val trimmed = packageName.trim()
        if (trimmed.isEmpty() || _uiState.value.isInstalling) return

        scope.launch {
            _uiState.update {
                it.copy(
                    isInstalling = true,
                    installingPackageName = trimmed,
                    installProgressMessage = "Starting pip for $trimmed...",
                    installProgressPercent = 5,
                    installLogs = listOf("pip install ${if (upgrade) "--upgrade " else ""}$trimmed"),
                )
            }
            val siteDir = workspaceRepository.sitePackagesDir.absolutePath
            val result = withContext(Dispatchers.IO) {
                pythonRuntime.installPackage(
                    packageName = trimmed,
                    sitePackagesDir = siteDir,
                    upgrade = upgrade,
                    progressCallback = object : InstallProgressCallback {
                        override fun onProgress(message: String, percent: Int) {
                            scope.launch {
                                _uiState.update { state ->
                                    state.copy(
                                        installProgressMessage = message,
                                        installProgressPercent = percent,
                                        installLogs = state.installLogs + message,
                                    )
                                }
                            }
                        }
                    },
                )
            }
            _uiState.update {
                it.copy(
                    isInstalling = false,
                    statusMessage = if (result.ok) {
                        "${if (upgrade) "Updated" else "Installed"} '$trimmed' successfully"
                    } else {
                        result.error.ifEmpty { "Failed to install '$trimmed'" }
                    },
                    pypiSearchResult = null,
                )
            }
            refreshCatalog()
        }
    }

    fun uninstallPackage(packageName: String) {
        scope.launch {
            val siteDir = workspaceRepository.sitePackagesDir.absolutePath
            val res = withContext(Dispatchers.IO) {
                pythonRuntime.uninstallPackage(packageName, siteDir)
            }
            _uiState.update {
                it.copy(
                    statusMessage = if (res.ok) res.message else res.error,
                    pypiSearchResult = null,
                )
            }
            refreshCatalog()
        }
    }

    fun verifyImport(item: LibraryItem) {
        scope.launch {
            val siteDir = workspaceRepository.sitePackagesDir.absolutePath
            val res = withContext(Dispatchers.IO) {
                pythonRuntime.verifyImport(item.importName, siteDir)
            }
            _uiState.update {
                it.copy(
                    lastVerifyResult = res,
                    statusMessage = if (res.ok) {
                        "Verified: import ${res.module} (v${res.version})"
                    } else {
                        "Import failed: ${res.error}"
                    },
                )
            }
        }
    }

    fun openDocumentation(item: LibraryItem) {
        scope.launch {
            _uiState.update { it.copy(isLoadingDoc = true) }
            val siteDir = workspaceRepository.sitePackagesDir.absolutePath
            val doc = withContext(Dispatchers.IO) {
                pythonRuntime.getModuleDocs(item.importName, siteDir)
            }
            _uiState.update { it.copy(isLoadingDoc = false, activeDoc = doc) }
        }
    }

    fun closeDocumentation() {
        _uiState.update { it.copy(activeDoc = null) }
    }

    fun clearInstallLogs() {
        if (!_uiState.value.isInstalling) {
            _uiState.update { it.copy(installLogs = emptyList(), installProgressPercent = 0, installProgressMessage = "") }
        }
    }

    fun dismissStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
