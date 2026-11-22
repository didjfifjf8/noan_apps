package com.example.python

interface ExecutionOutputCallback {
    fun onStarted(threadId: Long)
    fun onStdout(text: String)
    fun onStderr(text: String)
    fun onPlot(imagePath: String)
}

interface InputProvider {
    fun requestInput(prompt: String): String?
}

data class SyntaxCheckResult(
    val valid: Boolean,
    val line: Int = 1,
    val offset: Int = 1,
    val message: String = "",
    val text: String = "",
)

data class CompletionItem(
    val label: String,
    val insert: String,
    val kind: String,
)

enum class LibraryCategory {
    STDLIB,
    BUNDLED_NATIVE,
    PIP_RUNTIME,
    PIP_AVAILABLE,
    INCOMPATIBLE,
}

data class LibraryItem(
    val name: String,
    val importName: String,
    val version: String,
    val summary: String,
    val homepage: String,
    val category: LibraryCategory,
    val installed: Boolean,
    val removable: Boolean,
    val compatible: Boolean,
    val incompatibilityReason: String = "",
    val requires: List<String> = emptyList(),
)

data class LibrariesCatalog(
    val installed: List<LibraryItem> = emptyList(),
    val stdlib: List<LibraryItem> = emptyList(),
    val available: List<LibraryItem> = emptyList(),
    val incompatible: List<LibraryItem> = emptyList(),
)

data class RuntimeInfo(
    val pythonVersion: String = "3.11",
    val implementation: String = "CPython",
    val architecture: String = "",
    val platform: String = "android",
    val sitePackagesDir: String = "",
    val installedPackagesCount: Int = 0,
    val stdlibModulesCount: Int = 0,
)

data class PyPiSearchResult(
    val ok: Boolean,
    val name: String = "",
    val version: String = "",
    val summary: String = "",
    val description: String = "",
    val homepage: String = "",
    val compatible: Boolean = false,
    val incompatibilityReason: String = "",
    val installed: Boolean = false,
    val installedVersion: String? = null,
    val removable: Boolean = false,
    val wheelFilename: String = "",
    val wheelSize: Long = 0,
    val error: String = "",
)

data class PackageActionResult(
    val ok: Boolean,
    val message: String = "",
    val logs: List<String> = emptyList(),
    val error: String = "",
)

data class VerifyImportResult(
    val ok: Boolean,
    val module: String = "",
    val version: String = "",
    val location: String = "",
    val error: String = "",
)

data class ModuleDocResult(
    val ok: Boolean,
    val name: String = "",
    val doc: String = "",
    val error: String = "",
)

interface InstallProgressCallback {
    fun onProgress(message: String, percent: Int)
}

interface PythonRuntime {
    fun ensureStarted()

    fun executeScript(
        filePath: String,
        code: String,
        workingDir: String,
        sitePackagesDir: String,
        plotOutputDir: String,
        inputProvider: InputProvider,
        outputCallback: ExecutionOutputCallback,
    ): Int

    fun interruptExecution(threadId: Long): Boolean

    fun checkSyntax(code: String, filename: String): SyntaxCheckResult

    fun getCompletions(code: String, cursorPos: Int): List<CompletionItem>

    fun getRuntimeInfo(sitePackagesDir: String): RuntimeInfo

    fun listLibraries(sitePackagesDir: String): LibrariesCatalog

    fun verifyImport(importName: String, sitePackagesDir: String): VerifyImportResult

    fun getModuleDocs(moduleName: String, sitePackagesDir: String): ModuleDocResult

    fun searchPyPi(packageName: String, sitePackagesDir: String): PyPiSearchResult

    fun installPackage(
        packageName: String,
        sitePackagesDir: String,
        upgrade: Boolean,
        progressCallback: InstallProgressCallback,
    ): PackageActionResult

    fun uninstallPackage(packageName: String, sitePackagesDir: String): PackageActionResult
}
