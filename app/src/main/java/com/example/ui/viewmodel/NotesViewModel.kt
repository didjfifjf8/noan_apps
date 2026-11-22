package com.example.ui.viewmodel

import com.example.data.db.NoteEntity
import com.example.data.db.NoteFolderEntity
import com.example.data.db.NotesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteEditorDraft(
    val id: Long = 0L,
    val title: String = "",
    val description: String = "",
    val codeSnippet: String = "",
    val category: String = "Personal Notes",
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isSaving: Boolean = false,
    val lastSavedLabel: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(
    private val notesRepository: NotesRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _editingNote = MutableStateFlow<NoteEditorDraft?>(null)
    val editingNote: StateFlow<NoteEditorDraft?> = _editingNote.asStateFlow()

    private var autoSaveJob: Job? = null

    val folders: StateFlow<List<NoteFolderEntity>> = notesRepository.allFolders.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    val notes: StateFlow<List<NoteEntity>> = combine(_selectedCategory, _searchQuery) { cat, q ->
        cat to q
    }.flatMapLatest { (cat, q) ->
        notesRepository.searchNotes(cat, q.trim())
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun createNewNote() {
        val defaultCategory = _selectedCategory.value.let { if (it == "All") "Personal Notes" else it }
        scope.launch {
            val now = System.currentTimeMillis()
            val newNote = NoteEntity(
                title = "Untitled Note",
                description = "",
                codeSnippet = "# Python snippet\n",
                category = defaultCategory,
                isPinned = false,
                createdAt = now,
                updatedAt = now,
            )
            val id = notesRepository.insertNote(newNote)
            _editingNote.value = NoteEditorDraft(
                id = id,
                title = newNote.title,
                description = newNote.description,
                codeSnippet = newNote.codeSnippet,
                category = newNote.category,
                isPinned = false,
                createdAt = now,
                lastSavedLabel = "Saved",
            )
        }
    }

    fun openNoteForEdit(note: NoteEntity) {
        _editingNote.value = NoteEditorDraft(
            id = note.id,
            title = note.title,
            description = note.description,
            codeSnippet = note.codeSnippet,
            category = note.category,
            isPinned = note.isPinned,
            createdAt = note.createdAt,
            lastSavedLabel = "Saved",
        )
    }

    fun updateDraft(
        title: String? = null,
        description: String? = null,
        codeSnippet: String? = null,
        category: String? = null,
        isPinned: Boolean? = null,
    ) {
        val cur = _editingNote.value ?: return
        val updated = cur.copy(
            title = title ?: cur.title,
            description = description ?: cur.description,
            codeSnippet = codeSnippet ?: cur.codeSnippet,
            category = category ?: cur.category,
            isPinned = isPinned ?: cur.isPinned,
            isSaving = true,
            lastSavedLabel = "Saving...",
        )
        _editingNote.value = updated
        scheduleAutoSave(updated)
    }

    private fun scheduleAutoSave(draft: NoteEditorDraft) {
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch {
            delay(350)
            persistDraft(draft)
        }
    }

    private suspend fun persistDraft(draft: NoteEditorDraft) {
        val entity = NoteEntity(
            id = draft.id,
            title = draft.title.ifBlank { "Untitled Note" },
            description = draft.description,
            codeSnippet = draft.codeSnippet,
            category = draft.category,
            isPinned = draft.isPinned,
            createdAt = draft.createdAt,
            updatedAt = System.currentTimeMillis(),
        )
        if (draft.id == 0L) {
            val newId = notesRepository.insertNote(entity)
            _editingNote.update { cur ->
                cur?.copy(id = newId, isSaving = false, lastSavedLabel = "Auto-saved")
            }
        } else {
            notesRepository.updateNote(entity)
            _editingNote.update { cur ->
                cur?.copy(isSaving = false, lastSavedLabel = "Auto-saved")
            }
        }
    }

    fun closeNoteEditor() {
        val draft = _editingNote.value
        autoSaveJob?.cancel()
        if (draft != null) {
            scope.launch {
                persistDraft(draft)
                _editingNote.value = null
            }
        } else {
            _editingNote.value = null
        }
    }

    fun togglePin(note: NoteEntity) {
        scope.launch {
            notesRepository.togglePin(note.id, note.isPinned)
        }
    }

    fun deleteNote(noteId: Long) {
        autoSaveJob?.cancel()
        if (_editingNote.value?.id == noteId) {
            _editingNote.value = null
        }
        scope.launch {
            notesRepository.deleteNote(noteId)
        }
    }

    fun createFolder(name: String) {
        scope.launch {
            notesRepository.createFolder(name)
        }
    }

    fun deleteFolder(name: String) {
        scope.launch {
            if (_selectedCategory.value == name) {
                _selectedCategory.value = "All"
            }
            notesRepository.deleteFolder(name)
        }
    }
}
