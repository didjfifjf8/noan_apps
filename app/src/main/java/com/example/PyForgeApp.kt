package com.example

import android.app.Application
import com.example.data.db.NotesRepository
import com.example.data.db.PyForgeDatabase
import com.example.data.db.StudyBookRepository
import com.example.data.settings.SettingsRepository
import com.example.data.workspace.WorkspaceRepository
import com.example.python.ChaquopyPythonRuntime
import com.example.python.PythonRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PyForgeApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: PyForgeDatabase
        private set
    lateinit var notesRepository: NotesRepository
        private set
    lateinit var workspaceRepository: WorkspaceRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var pythonRuntime: PythonRuntime
        private set
    lateinit var studyBookRepository: StudyBookRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = PyForgeDatabase.getInstance(this)
        notesRepository = NotesRepository(database.noteDao())
        studyBookRepository = StudyBookRepository(this, database.studyDao())
        workspaceRepository = WorkspaceRepository(this, database.projectMetaDao())
        settingsRepository = SettingsRepository(this)
        pythonRuntime = ChaquopyPythonRuntime(this)

        // Warm up CPython and seed starter data asynchronously for instant cold launch
        appScope.launch {
            notesRepository.seedDefaultsIfEmpty()
            studyBookRepository.seedDefaultTopicIfEmpty()
            workspaceRepository.ensureStarterWorkspace()
            pythonRuntime.ensureStarted()
        }
    }
}
