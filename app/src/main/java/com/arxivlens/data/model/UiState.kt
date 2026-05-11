package com.arxivlens.data.model

/**
 * Sealed class modelling every possible state of the search screen.
 *
 * Using a sealed class instead of multiple boolean flags is the
 * functional-style "make illegal states unrepresentable" pattern:
 * the UI can never be simultaneously Loading and showing results.
 */
sealed class SearchUiState {
    /** Initial state — no query has been submitted yet. */
    data object Idle : SearchUiState()

    /** A network request is in flight. */
    data object Loading : SearchUiState()

    /** The request completed successfully; [papers] may be empty. */
    data class Success(val papers: List<Paper>) : SearchUiState()

    /** The request failed; [message] is safe to show in the UI. */
    data class Error(val message: String) : SearchUiState()
}

/**
 * Sealed class modelling every possible state of the collections screen.
 */
sealed class CollectionsUiState {
    data object Loading : CollectionsUiState()
    data class Success(val collections: List<Collection>) : CollectionsUiState()
    data class Error(val message: String) : CollectionsUiState()
}

/**
 * Sealed class modelling every possible state of a single collection detail screen.
 */
sealed class CollectionDetailUiState {
    data object Loading : CollectionDetailUiState()
    data class Success(
        val collection: Collection,
        val papers: List<SavedPaper>
    ) : CollectionDetailUiState()
    data class Error(val message: String) : CollectionDetailUiState()
}
