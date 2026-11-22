package com.example.ui.viewmodel

import com.example.data.settings.IdeSettings
import com.example.data.settings.SettingsRepository
import com.example.data.workspace.WorkspaceRepository
import com.example.python.PythonRuntime
import com.example.python.RuntimeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val pythonRuntime: PythonRuntime,
    private val workspaceRepository: WorkspaceRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val settings: StateFlow<IdeSettings> = settingsRepository.settings.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = IdeSettings(),
    )

    private val _runtimeInfo = MutableStateFlow(RuntimeInfo())
    val runtimeInfo: StateFlow<RuntimeInfo> = _runtimeInfo.asStateFlow()

    private val _sitePackagesSizeKb = MutableStateFlow(0L)
    val sitePackagesSizeKb: StateFlow<Long> = _sitePackagesSizeKb.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun loadDiagnostics() {
        scope.launch {
            val siteDir = workspaceRepository.sitePackagesDir
            val (info, kb) = withContext(Dispatchers.IO) {
                val rInfo = pythonRuntime.getRuntimeInfo(siteDir.absolutePath)
                val bytes = siteDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                rInfo to (bytes / 1024L)
            }
            _runtimeInfo.value = info
            _sitePackagesSizeKb.value = kb
        }
    }

    fun setFontSize(sp: Int) {
        scope.launch { settingsRepository.setFontSize(sp) }
    }

    fun setTabSize(spaces: Int) {
        scope.launch { settingsRepository.setTabSize(spaces) }
    }

    fun setAutoSave(enabled: Boolean) {
        scope.launch { settingsRepository.setAutoSave(enabled) }
    }

    fun setShowLineNumbers(enabled: Boolean) {
        scope.launch { settingsRepository.setShowLineNumbers(enabled) }
    }

    fun setShowSymbolBar(enabled: Boolean) {
        scope.launch { settingsRepository.setShowSymbolBar(enabled) }
    }

    fun setExecutionTimeout(seconds: Int) {
        scope.launch { settingsRepository.setExecutionTimeout(seconds) }
    }

    fun clearPlotCache() {
        scope.launch(Dispatchers.IO) {
            workspaceRepository.plotsCacheDir.listFiles()?.forEach { it.delete() }
            _statusMessage.value = "Cleared cached plot images"
        }
    }

    fun dismissStatus() {
        _statusMessage.value = null
    }
}
