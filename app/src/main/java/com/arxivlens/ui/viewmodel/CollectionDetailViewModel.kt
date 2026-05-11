package com.arxivlens.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arxivlens.data.db.ArxivRepository
import com.arxivlens.data.model.Collection
import com.arxivlens.data.model.CollectionDetailUiState
import com.arxivlens.data.model.SavedPaper
import com.arxivlens.util.BibTexExporter
import com.arxivlens.util.BibTexFileWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CollectionDetailViewModel(
    private val repository: ArxivRepository,
    private val collectionId: Long
) : ViewModel() {

    /**
     * Combines the collection metadata and its paper list into a single UI state.
     * Uses the functional [combine] operator — both sources are observed reactively.
     */
    val uiState: StateFlow<CollectionDetailUiState> = combine(
        flow { emit(repository.getCollectionById(collectionId)) },
        repository.getPapersForCollection(collectionId)
    ) { collection: Collection?, papers: List<SavedPaper> ->
        if (collection == null) CollectionDetailUiState.Error("Collection not found")
        else CollectionDetailUiState.Success(collection, papers)
    }
        .catch { e -> emit(CollectionDetailUiState.Error(e.message ?: "Error loading collection")) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = CollectionDetailUiState.Loading
        )

    // ------------------------------------------------------------------
    // One-shot share event — null means "no pending share"
    // ------------------------------------------------------------------

    private val _shareEvent = MutableStateFlow<Intent?>(null)
    val shareEvent: StateFlow<Intent?> = _shareEvent

    fun onShareEventConsumed() { _shareEvent.value = null }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    fun removePaper(paperId: String) {
        viewModelScope.launch {
            repository.removePaper(collectionId, paperId)
        }
    }

    /**
     * Renders the BibTeX string for all papers in the collection and
     * posts a share-sheet [Intent] to [_shareEvent].
     *
     * The pure pipeline: BibTexExporter generates the string → BibTexFileWriter
     * writes the file and builds the Intent → the UI calls startActivity.
     */
    fun exportBibTex(context: Context) {
        val currentState = uiState.value
        if (currentState !is CollectionDetailUiState.Success) return

        val papers         = currentState.papers
        val collectionName = currentState.collection.name

        if (papers.isEmpty()) return

        viewModelScope.launch {
            val bibContent = BibTexExporter.collectionToBibTex(papers)
            // writeBibFile does blocking disk I/O — dispatch to Dispatchers.IO
            val uri = withContext(Dispatchers.IO) {
                BibTexFileWriter.writeBibFile(context, bibContent, collectionName)
            }
            val intent = BibTexFileWriter.buildShareIntent(uri, collectionName)
            _shareEvent.value = intent
        }
    }
}
