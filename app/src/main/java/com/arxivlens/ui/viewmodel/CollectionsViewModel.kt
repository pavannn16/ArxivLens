package com.arxivlens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arxivlens.data.db.ArxivRepository
import com.arxivlens.data.model.Collection
import com.arxivlens.data.model.CollectionsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CollectionsViewModel(private val repository: ArxivRepository) : ViewModel() {

    /**
     * The full list of collections as a UI state.
     * Backed by a Room Flow — automatically updates when the DB changes.
     */
    val collectionsState: StateFlow<CollectionsUiState> =
        repository.getAllCollections()
            .map<List<Collection>, CollectionsUiState> { CollectionsUiState.Success(it) }
            .catch { e -> emit(CollectionsUiState.Error(e.message ?: "Failed to load collections")) }
            .stateIn(
                scope          = viewModelScope,
                started        = SharingStarted.WhileSubscribed(5_000),
                initialValue   = CollectionsUiState.Loading
            )

    // ------------------------------------------------------------------
    // Dialog state — whether "New Collection" dialog is visible
    // ------------------------------------------------------------------

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog

    fun openCreateDialog()  { _showCreateDialog.value = true  }
    fun closeCreateDialog() { _showCreateDialog.value = false }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    fun createCollection(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createCollection(name)
            closeCreateDialog()
        }
    }

    fun deleteCollection(collection: Collection) {
        viewModelScope.launch {
            repository.deleteCollection(collection)
        }
    }

    // ------------------------------------------------------------------
    // Delete confirmation
    // ------------------------------------------------------------------

    private val _pendingDelete = MutableStateFlow<Collection?>(null)
    val pendingDelete: StateFlow<Collection?> = _pendingDelete

    fun requestDelete(collection: Collection) { _pendingDelete.value = collection }
    fun cancelDelete()                        { _pendingDelete.value = null }
    fun confirmDelete() {
        val c = _pendingDelete.value ?: return
        _pendingDelete.value = null
        viewModelScope.launch { repository.deleteCollection(c) }
    }
}
