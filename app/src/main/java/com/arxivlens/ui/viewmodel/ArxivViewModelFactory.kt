package com.arxivlens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arxivlens.data.db.ArxivRepository
import com.arxivlens.data.network.ArxivApiService

/**
 * Factory for [SearchViewModel] and [CollectionsViewModel].
 * Obtained from [ArxivLensApp] so the app-scoped singletons are injected.
 */
class ArxivViewModelFactory(
    private val repository: ArxivRepository,
    private val apiService: ArxivApiService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(SearchViewModel::class.java) ->
            SearchViewModel(apiService, repository) as T
        modelClass.isAssignableFrom(CollectionsViewModel::class.java) ->
            CollectionsViewModel(repository) as T
        else ->
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

/**
 * Factory for [CollectionDetailViewModel], which requires a [collectionId]
 * that is only known at navigation time.
 */
class CollectionDetailViewModelFactory(
    private val repository: ArxivRepository,
    private val collectionId: Long
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CollectionDetailViewModel::class.java)) {
            return CollectionDetailViewModel(repository, collectionId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

/**
 * Lightweight factory for [CollectionsViewModel] — does not need [ArxivApiService].
 */
class CollectionsViewModelFactory(
    private val repository: ArxivRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CollectionsViewModel::class.java)) {
            return CollectionsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
