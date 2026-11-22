package com.example.data.workspace

import android.content.Context
import com.example.data.db.ProjectMetaDao
import com.example.data.db.ProjectMetaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class WorkspaceEntry(
    val name: String,
    val absolutePath: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
)

class WorkspaceRepository(
    private val context: Context,
    private val projectMetaDao: ProjectMetaDao,
) {
    val rootDir: File
        get() = File(context.filesDir, "pyforge_projects").apply { mkdirs() }

    val sitePackagesDir: File
        get() = File(context.filesDir, "site-packages").apply { mkdirs() }

    val plotsCacheDir: File
        get() = File(context.cacheDir, "plots").apply { mkdirs() }

    suspend fun ensureStarterWorkspace(): File = withContext(Dispatchers.IO) {
        val root = rootDir
        val starter = File(root, "hello_pyforge")
        if (!starter.exists() && root.listFiles().isNullOrEmpty()) {
            starter.mkdirs()
            val mainFile = File(starter, "main.py")
            mainFile.writeText(
                """# Welcome to PyForge — Real CPython 3.11 on Android
import math
import sys
from utils import greet

def main():
    print("Python version:", sys.version.split()[0])
    print("sqrt(144) =", math.sqrt(144))
    greet("Developer")

    squares = [x * x for x in range(1, 6)]
    print("Squares:", squares)

if __name__ == "__main__":
    main()
"""
            )
            val utilsFile = File(starter, "utils.py")
            utilsFile.writeText(
                """def greet(name: str) -> None:
    print(f"Hello, {name}! Ready to code offline.")
"""
            )
            projectMetaDao.upsert(
                ProjectMetaEntity(
                    relativePath = "hello_pyforge",
                    displayName = "hello_pyforge",
                    lastOpenedFile = mainFile.absolutePath,
                )
            )
        }
        val defaultFile = File(starter, "main.py")
        if (defaultFile.exists()) {
            defaultFile
        } else {
            val firstPy = root.walkTopDown().firstOrNull { it.isFile && it.extension == "py" }
            firstPy ?: File(root, "scratch.py").apply {
                if (!exists()) writeText("print('Hello from PyForge!')\n")
            }
        }
    }

    suspend fun listDirectory(dir: File): List<WorkspaceEntry> = withContext(Dispatchers.IO) {
        val target = if (dir.exists() && dir.isDirectory) dir else rootDir
        val rootPath = rootDir.absolutePath
        (target.listFiles() ?: emptyArray())
            .sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
            .map { file ->
                val rel = file.absolutePath.removePrefix(rootPath).trimStart(File.separatorChar)
                WorkspaceEntry(
                    name = file.name,
                    absolutePath = file.absolutePath,
                    relativePath = rel,
                    isDirectory = file.isDirectory,
                    sizeBytes = if (file.isFile) file.length() else 0L,
                    lastModified = file.lastModified(),
                )
            }
    }

    suspend fun createFile(parentDir: File, fileName: String, initialContent: String = ""): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val safeName = sanitizeName(fileName)
                require(safeName.isNotEmpty()) { "File name cannot be empty" }
                val finalName = if ('.' in safeName) safeName else "$safeName.py"
                val file = File(parentDir, finalName)
                require(!file.exists()) { "A file named '$finalName' already exists" }
                file.parentFile?.mkdirs()
                file.writeText(initialContent)
                file
            }
        }

    suspend fun createFolder(parentDir: File, folderName: String): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val safeName = sanitizeName(folderName)
                require(safeName.isNotEmpty()) { "Folder name cannot be empty" }
                val dir = File(parentDir, safeName)
                require(!dir.exists()) { "A folder named '$safeName' already exists" }
                check(dir.mkdirs()) { "Failed to create folder '$safeName'" }
                if (parentDir.absolutePath == rootDir.absolutePath) {
                    projectMetaDao.upsert(
                        ProjectMetaEntity(
                            relativePath = safeName,
                            displayName = safeName,
                        )
                    )
                }
                dir
            }
        }

    suspend fun renameEntry(entry: WorkspaceEntry, newName: String): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val safeName = sanitizeName(newName)
                require(safeName.isNotEmpty()) { "Name cannot be empty" }
                val oldFile = File(entry.absolutePath)
                require(oldFile.exists()) { "Original item does not exist" }
                val finalName = if (!oldFile.isDirectory && '.' !in safeName && oldFile.extension == "py") {
                    "$safeName.py"
                } else {
                    safeName
                }
                val newFile = File(oldFile.parentFile, finalName)
                require(!newFile.exists()) { "An item named '$finalName' already exists" }
                check(oldFile.renameTo(newFile)) { "Failed to rename '${oldFile.name}'" }
                newFile
            }
        }

    suspend fun deleteEntry(entry: WorkspaceEntry): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(entry.absolutePath)
            if (target.exists()) {
                check(target.deleteRecursively()) { "Failed to delete '${entry.name}'" }
            }
            projectMetaDao.deleteByPath(entry.relativePath)
        }
    }

    suspend fun readText(filePath: String): String = withContext(Dispatchers.IO) {
        val f = File(filePath)
        if (f.exists() && f.isFile) f.readText() else ""
    }

    suspend fun writeText(filePath: String, content: String) = withContext(Dispatchers.IO) {
        val f = File(filePath)
        f.parentFile?.mkdirs()
        f.writeText(content)
    }

    suspend fun recordFileOpened(filePath: String) = withContext(Dispatchers.IO) {
        val f = File(filePath)
        val rootPath = rootDir.absolutePath
        val rel = f.absolutePath.removePrefix(rootPath).trimStart(File.separatorChar)
        val topFolder = rel.substringBefore(File.separatorChar, "")
        if (topFolder.isNotEmpty()) {
            projectMetaDao.upsert(
                ProjectMetaEntity(
                    relativePath = topFolder,
                    displayName = topFolder,
                    lastOpenedFile = filePath,
                    lastOpenedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    private fun sanitizeName(raw: String): String {
        return raw.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_")
    }
}
