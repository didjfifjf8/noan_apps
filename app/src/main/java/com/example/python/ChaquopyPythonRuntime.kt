package com.example.python

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONArray
import org.json.JSONObject

class ChaquopyPythonRuntime(
    private val context: Context,
) : PythonRuntime {

    private val startLock = Any()

    override fun ensureStarted() {
        if (!Python.isStarted()) {
            synchronized(startLock) {
                if (!Python.isStarted()) {
                    Python.start(AndroidPlatform(context.applicationContext))
                }
            }
        }
    }

    private fun py(): Python {
        ensureStarted()
        return Python.getInstance()
    }

    override fun executeScript(
        filePath: String,
        code: String,
        workingDir: String,
        sitePackagesDir: String,
        plotOutputDir: String,
        inputProvider: InputProvider,
        outputCallback: ExecutionOutputCallback,
    ): Int {
        val runner = py().getModule("pyforge_runner")
        val res = runner.callAttr(
            "execute_script",
            filePath,
            code,
            workingDir,
            sitePackagesDir,
            plotOutputDir,
            inputProvider,
            outputCallback,
        )
        return res.toInt()
    }

    override fun interruptExecution(threadId: Long): Boolean {
        if (!Python.isStarted() || threadId == 0L) return false
        return try {
            val runner = py().getModule("pyforge_runner")
            runner.callAttr("interrupt_thread", threadId).toBoolean()
        } catch (_: Throwable) {
            false
        }
    }

    override fun checkSyntax(code: String, filename: String): SyntaxCheckResult {
        return try {
            val lint = py().getModule("pyforge_lint")
            val jsonStr = lint.callAttr("check_syntax", code, filename).toString()
            val obj = JSONObject(jsonStr)
            SyntaxCheckResult(
                valid = obj.optBoolean("valid", true),
                line = obj.optInt("line", 1),
                offset = obj.optInt("offset", 1),
                message = obj.optString("message", ""),
                text = obj.optString("text", ""),
            )
        } catch (t: Throwable) {
            SyntaxCheckResult(valid = true)
        }
    }

    override fun getCompletions(code: String, cursorPos: Int): List<CompletionItem> {
        return try {
            val lint = py().getModule("pyforge_lint")
            val jsonStr = lint.callAttr("get_completions", code, cursorPos).toString()
            val arr = JSONArray(jsonStr)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        CompletionItem(
                            label = o.optString("label"),
                            insert = o.optString("insert"),
                            kind = o.optString("kind", "var"),
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    override fun getRuntimeInfo(sitePackagesDir: String): RuntimeInfo {
        return try {
            val pkg = py().getModule("pyforge_pkg")
            val obj = JSONObject(pkg.callAttr("get_runtime_info", sitePackagesDir).toString())
            RuntimeInfo(
                pythonVersion = obj.optString("python_version", "3.11"),
                implementation = obj.optString("implementation", "CPython"),
                architecture = obj.optString("architecture", ""),
                platform = obj.optString("platform", "android"),
                sitePackagesDir = obj.optString("site_packages_dir", sitePackagesDir),
                installedPackagesCount = obj.optInt("installed_packages_count", 0),
                stdlibModulesCount = obj.optInt("stdlib_modules_count", 0),
            )
        } catch (t: Throwable) {
            RuntimeInfo(sitePackagesDir = sitePackagesDir)
        }
    }

    override fun listLibraries(sitePackagesDir: String): LibrariesCatalog {
        val pkg = py().getModule("pyforge_pkg")
        val obj = JSONObject(pkg.callAttr("list_libraries", sitePackagesDir).toString())
        return LibrariesCatalog(
            installed = parseLibraryArray(obj.optJSONArray("installed")),
            stdlib = parseLibraryArray(obj.optJSONArray("stdlib")),
            available = parseLibraryArray(obj.optJSONArray("available")),
            incompatible = parseLibraryArray(obj.optJSONArray("incompatible")),
        )
    }

    override fun verifyImport(importName: String, sitePackagesDir: String): VerifyImportResult {
        val pkg = py().getModule("pyforge_pkg")
        val obj = JSONObject(pkg.callAttr("verify_import", importName, sitePackagesDir).toString())
        return VerifyImportResult(
            ok = obj.optBoolean("ok", false),
            module = obj.optString("module", importName),
            version = obj.optString("version", ""),
            location = obj.optString("location", ""),
            error = obj.optString("error", ""),
        )
    }

    override fun getModuleDocs(moduleName: String, sitePackagesDir: String): ModuleDocResult {
        val pkg = py().getModule("pyforge_pkg")
        val obj = JSONObject(pkg.callAttr("get_module_docs", moduleName, sitePackagesDir).toString())
        return ModuleDocResult(
            ok = obj.optBoolean("ok", false),
            name = obj.optString("name", moduleName),
            doc = obj.optString("doc", ""),
            error = obj.optString("error", ""),
        )
    }

    override fun searchPyPi(packageName: String, sitePackagesDir: String): PyPiSearchResult {
        val pkg = py().getModule("pyforge_pkg")
        val obj = JSONObject(pkg.callAttr("search_pypi", packageName, sitePackagesDir).toString())
        return PyPiSearchResult(
            ok = obj.optBoolean("ok", false),
            name = obj.optString("name", packageName),
            version = obj.optString("version", ""),
            summary = obj.optString("summary", ""),
            description = obj.optString("description", ""),
            homepage = obj.optString("homepage", ""),
            compatible = obj.optBoolean("compatible", false),
            incompatibilityReason = obj.optString("incompatibility_reason", ""),
            installed = obj.optBoolean("installed", false),
            installedVersion = if (obj.isNull("installed_version")) null else obj.optString("installed_version"),
            removable = obj.optBoolean("removable", false),
            wheelFilename = obj.optString("wheel_filename", ""),
            wheelSize = obj.optLong("wheel_size", 0L),
            error = obj.optString("error", ""),
        )
    }

    override fun installPackage(
        packageName: String,
        sitePackagesDir: String,
        upgrade: Boolean,
        progressCallback: InstallProgressCallback,
    ): PackageActionResult {
        val pkg = py().getModule("pyforge_pkg")
        val obj = JSONObject(
            pkg.callAttr("install_package", packageName, sitePackagesDir, progressCallback, upgrade).toString()
        )
        val logsArr = obj.optJSONArray("logs")
        val logs = buildList {
            if (logsArr != null) {
                for (i in 0 until logsArr.length()) {
                    add(logsArr.optString(i))
                }
            }
        }
        return PackageActionResult(
            ok = obj.optBoolean("ok", false),
            logs = logs,
            error = obj.optString("error", ""),
        )
    }

    override fun uninstallPackage(packageName: String, sitePackagesDir: String): PackageActionResult {
        val pkg = py().getModule("pyforge_pkg")
        val obj = JSONObject(pkg.callAttr("uninstall_package", packageName, sitePackagesDir).toString())
        return PackageActionResult(
            ok = obj.optBoolean("ok", false),
            message = obj.optString("message", ""),
            error = obj.optString("error", ""),
        )
    }

    private fun parseLibraryArray(arr: JSONArray?): List<LibraryItem> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val cat = when (o.optString("category")) {
                    "stdlib" -> LibraryCategory.STDLIB
                    "bundled_native" -> LibraryCategory.BUNDLED_NATIVE
                    "pip_runtime" -> LibraryCategory.PIP_RUNTIME
                    "pip_available" -> LibraryCategory.PIP_AVAILABLE
                    else -> LibraryCategory.INCOMPATIBLE
                }
                val reqArr = o.optJSONArray("requires")
                val reqs = buildList {
                    if (reqArr != null) {
                        for (j in 0 until reqArr.length()) {
                            add(reqArr.optString(j))
                        }
                    }
                }
                add(
                    LibraryItem(
                        name = o.optString("name"),
                        importName = o.optString("import_name"),
                        version = o.optString("version"),
                        summary = o.optString("summary"),
                        homepage = o.optString("homepage"),
                        category = cat,
                        installed = o.optBoolean("installed", false),
                        removable = o.optBoolean("removable", false),
                        compatible = o.optBoolean("compatible", true),
                        incompatibilityReason = o.optString("incompatibility_reason", ""),
                        requires = reqs,
                    )
                )
            }
        }
    }
}
