package com.example.ui.viewmodel

import android.net.Uri
import com.example.data.db.DiagramData
import com.example.data.db.NoteFolderEntity
import com.example.data.db.NotesRepository
import com.example.data.db.StudyBlockEntity
import com.example.data.db.StudyBlockType
import com.example.data.db.StudyBookRepository
import com.example.data.db.StudyJsonCodec
import com.example.data.db.StudyTopicEntity
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class StudyBookViewModel(
    private val studyBookRepository: StudyBookRepository,
    notesRepository: NotesRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _activeTopicId = MutableStateFlow<Long?>(null)
    val activeTopicId: StateFlow<Long?> = _activeTopicId.asStateFlow()

    private val _isReadMode = MutableStateFlow(false)
    val isReadMode: StateFlow<Boolean> = _isReadMode.asStateFlow()

    private val _saveStatus = MutableStateFlow("Avtomatik saqlangan")
    val saveStatus: StateFlow<String> = _saveStatus.asStateFlow()

    private val _localTitleDraft = MutableStateFlow<String?>(null)
    val localTitleDraft: StateFlow<String?> = _localTitleDraft.asStateFlow()

    private var titleSaveJob: Job? = null
    private val blockSaveJobs = mutableMapOf<Long, Job>()

    val folders: StateFlow<List<NoteFolderEntity>> = notesRepository.allFolders.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    val topics: StateFlow<List<StudyTopicEntity>> = combine(_selectedCategory, _searchQuery) { cat, q ->
        cat to q
    }.flatMapLatest { (cat, q) ->
        studyBookRepository.searchTopics(cat, q.trim())
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    val activeTopic: StateFlow<StudyTopicEntity?> = _activeTopicId.flatMapLatest { id ->
        if (id == null) flowOf(null) else studyBookRepository.observeTopic(id)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    val activeBlocks: StateFlow<List<StudyBlockEntity>> = _activeTopicId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else studyBookRepository.observeBlocks(id)
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

    fun toggleReadMode() {
        _isReadMode.update { !it }
    }

    fun createNewTopic(title: String = "Yangi mavzu") {
        val cat = _selectedCategory.value.let { if (it == "All") "Python Basics" else it }
        scope.launch {
            val id = studyBookRepository.createTopic(title, cat)
            _localTitleDraft.value = title
            _isReadMode.value = false
            _activeTopicId.value = id
            _saveStatus.value = "Avtomatik saqlangan"
        }
    }

    fun openTopic(topic: StudyTopicEntity) {
        _localTitleDraft.value = topic.title
        _isReadMode.value = false
        _activeTopicId.value = topic.id
        _saveStatus.value = "Avtomatik saqlangan"
    }

    fun closeTopic() {
        titleSaveJob?.cancel()
        val topic = activeTopic.value
        val titleDraft = _localTitleDraft.value
        if (topic != null && titleDraft != null && titleDraft != topic.title) {
            scope.launch {
                studyBookRepository.updateTopic(topic.copy(title = titleDraft.ifBlank { "Yangi mavzu" }))
            }
        }
        _activeTopicId.value = null
        _localTitleDraft.value = null
    }

    fun updateTopicTitle(newTitle: String) {
        _localTitleDraft.value = newTitle
        val topic = activeTopic.value ?: return
        _saveStatus.value = "Saqlanmoqda..."
        titleSaveJob?.cancel()
        titleSaveJob = scope.launch {
            delay(350)
            studyBookRepository.updateTopic(topic.copy(title = newTitle.ifBlank { "Yangi mavzu" }))
            _saveStatus.value = "Avtomatik saqlangan"
        }
    }

    fun updateTopicCategory(category: String) {
        val topic = activeTopic.value ?: return
        scope.launch {
            studyBookRepository.updateTopic(topic.copy(category = category))
            _saveStatus.value = "Avtomatik saqlangan"
        }
    }

    fun togglePin(topic: StudyTopicEntity) {
        scope.launch {
            studyBookRepository.togglePin(topic.id, topic.isPinned)
        }
    }

    fun deleteTopic(topicId: Long) {
        if (_activeTopicId.value == topicId) {
            _activeTopicId.value = null
            _localTitleDraft.value = null
        }
        scope.launch {
            studyBookRepository.deleteTopic(topicId)
        }
    }

    fun addTextBlock() {
        val topicId = _activeTopicId.value ?: return
        scope.launch {
            studyBookRepository.addBlock(
                topicId,
                StudyBlockEntity(
                    topicId = topicId,
                    orderIndex = 0,
                    blockType = StudyBlockType.TEXT.raw,
                    primaryText = "",
                ),
            )
            _saveStatus.value = "Avtomatik saqlangan"
        }
    }

    fun addCodeNoteBlock() {
        val topicId = _activeTopicId.value ?: return
        scope.launch {
            studyBookRepository.addBlock(
                topicId,
                StudyBlockEntity(
                    topicId = topicId,
                    orderIndex = 0,
                    blockType = StudyBlockType.CODE_NOTE.raw,
                    primaryText = "# Python kodi\n",
                    secondaryText = "",
                ),
            )
            _saveStatus.value = "Avtomatik saqlangan"
        }
    }

    fun addImageFromUri(uri: Uri) {
        val topicId = _activeTopicId.value ?: return
        scope.launch {
            _saveStatus.value = "Rasm saqlanmoqda..."
            val path = studyBookRepository.copyPickedImageToInternal(uri)
            if (path != null) {
                studyBookRepository.addBlock(
                    topicId,
                    StudyBlockEntity(
                        topicId = topicId,
                        orderIndex = 0,
                        blockType = StudyBlockType.IMAGE.raw,
                        imagePath = path,
                        imageScale = 1f,
                        imageHeightDp = 220,
                    ),
                )
                _saveStatus.value = "Avtomatik saqlangan"
            } else {
                _saveStatus.value = "Rasmni ochib bo'lmadi"
            }
        }
    }

    fun addDiagramBlock(withPreset: Boolean = true) {
        val topicId = _activeTopicId.value ?: return
        val diagram = if (withPreset) StudyJsonCodec.linearPreset() else DiagramData()
        scope.launch {
            studyBookRepository.addBlock(
                topicId,
                StudyBlockEntity(
                    topicId = topicId,
                    orderIndex = 0,
                    blockType = StudyBlockType.DIAGRAM.raw,
                    primaryText = "Chiziqli algoritm sxemasi",
                    diagramJson = StudyJsonCodec.encodeDiagram(diagram),
                ),
            )
            _saveStatus.value = "Avtomatik saqlangan"
        }
    }

    fun updateBlockImmediate(block: StudyBlockEntity) {
        blockSaveJobs[block.id]?.cancel()
        scope.launch {
            studyBookRepository.updateBlock(block)
            _saveStatus.value = "Avtomatik saqlangan"
        }
    }

    fun updateBlockDebounced(block: StudyBlockEntity) {
        _saveStatus.value = "Saqlanmoqda..."
        blockSaveJobs[block.id]?.cancel()
        blockSaveJobs[block.id] = scope.launch {
            delay(320)
            studyBookRepository.updateBlock(block)
            _saveStatus.value = "Avtomatik saqlangan"
        }
    }

    fun moveBlock(blockId: Long, delta: Int) {
        val topicId = _activeTopicId.value ?: return
        scope.launch {
            studyBookRepository.moveBlock(topicId, blockId, delta)
        }
    }

    fun deleteBlock(block: StudyBlockEntity) {
        blockSaveJobs[block.id]?.cancel()
        scope.launch {
            studyBookRepository.deleteBlock(block)
        }
    }
}
