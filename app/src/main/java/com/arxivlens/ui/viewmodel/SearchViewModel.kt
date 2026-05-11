package com.arxivlens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arxivlens.data.db.ArxivRepository
import com.arxivlens.data.model.Collection
import com.arxivlens.data.model.Paper
import com.arxivlens.data.model.SearchUiState
import com.arxivlens.data.model.SortOption
import com.arxivlens.data.model.toSavedPaper
import com.arxivlens.data.network.ArxivApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchViewModel(
    private val apiService: ArxivApiService,
    private val repository: ArxivRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState

    // Sort option — combined with uiState to produce displayState
    private val _sortOption = MutableStateFlow(SortOption.RELEVANCE)
    val sortOption: StateFlow<SortOption> = _sortOption

    fun setSortOption(option: SortOption) { _sortOption.value = option }

    /** The list the UI actually renders — sorted according to [sortOption]. */
    val displayState: StateFlow<SearchUiState> = combine(_uiState, _sortOption) { state, sort ->
        if (state is SearchUiState.Success) {
            val sorted = sort.comparator()
                ?.let { state.papers.sortedWith(it) }
                ?: state.papers
            SearchUiState.Success(sorted)
        } else state
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState.Idle
    )

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    /** All collections — used to populate the "Save to collection" bottom sheet. */
    val collections: StateFlow<List<Collection>> =
        repository.getAllCollections()
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // ------------------------------------------------------------------
    // "Save to collection" dialog state
    // ------------------------------------------------------------------

    private val _pendingPaper = MutableStateFlow<Paper?>(null)
    val pendingPaper: StateFlow<Paper?> = _pendingPaper

    // ------------------------------------------------------------------
    // Selected paper (for detail screen)
    // ------------------------------------------------------------------

    private val _selectedPaper = MutableStateFlow<Paper?>(null)
    val selectedPaper: StateFlow<Paper?> = _selectedPaper

    fun selectPaper(paper: Paper) {
        // Data class structural equality (==): no-op if the same paper is already selected.
        if (_selectedPaper.value == paper) return
        _selectedPaper.value = paper
    }

    fun clearSelectedPaper() {
        _selectedPaper.value = null
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    // ------------------------------------------------------------------
    // Search
    // ------------------------------------------------------------------

    fun search() {
        val q = _query.value.trim()
        if (q.isBlank()) return

        _uiState.value = SearchUiState.Loading

        viewModelScope.launch {
            // OkHttp execute() blocks — must run on IO
            val result = withContext(Dispatchers.IO) { apiService.search(q) }

            _uiState.value = result.fold(
                onSuccess = { papers ->
                    if (papers.isEmpty()) SearchUiState.Error("No results found for \"$q\"")
                    else SearchUiState.Success(papers)
                },
                onFailure = { e ->
                    SearchUiState.Error(e.message ?: "Network error")
                }
            )
        }
    }

    // ------------------------------------------------------------------
    // Save paper
    // ------------------------------------------------------------------

    /** Opens the "save to collection" bottom sheet for the given paper. */
    fun onSavePaperClick(paper: Paper) {
        _pendingPaper.value = paper
    }

    /** Dismisses the "save to collection" bottom sheet. */
    fun dismissSaveDialog() {
        _pendingPaper.value = null
    }

    /** Saves [paper] into [collectionId], dismisses the dialog, and emits a feedback message. */
    fun savePaper(paper: Paper, collectionId: Long) {
        viewModelScope.launch {
            val rowId = repository.savePaper(paper.toSavedPaper(collectionId))
            _saveMessage.value = if (rowId > 0L) "Saved to collection ✓" else "Already in this collection"
            dismissSaveDialog()
        }
    }

    /** Creates a collection and immediately saves [paper] into it. */
    fun savePaperToNewCollection(paper: Paper, collectionName: String) {
        val trimmedName = collectionName.trim()
        if (trimmedName.isBlank()) return

        viewModelScope.launch {
            val collectionId = repository.createCollection(trimmedName)
            val rowId = repository.savePaper(paper.toSavedPaper(collectionId))
            _saveMessage.value = if (rowId > 0L) {
                "Created \"$trimmedName\" and saved paper"
            } else {
                "Created \"$trimmedName\""
            }
            dismissSaveDialog()
        }
    }

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage

    fun onSaveMessageShown() { _saveMessage.value = null }
}
