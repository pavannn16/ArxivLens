package com.arxivlens.data.db

import com.arxivlens.data.model.Collection
import com.arxivlens.data.model.SavedPaper
import kotlinx.coroutines.flow.Flow

/**
 * Repository: single source of truth for all local database operations.
 *
 * Sits between the DAO layer and the ViewModels. ViewModels never talk to DAOs
 * directly — they only call repository methods. This keeps the ViewModels clean
 * and makes the data layer easily testable by substituting a fake repository.
 */
class ArxivRepository(private val db: ArxivDatabase) {

    // ------------------------------------------------------------------
    // Collections
    // ------------------------------------------------------------------

    fun getAllCollections(): Flow<List<Collection>> =
        db.collectionDao().getAllCollections()

    suspend fun getCollectionById(id: Long): Collection? =
        db.collectionDao().getCollectionById(id)

    suspend fun createCollection(name: String): Long =
        db.collectionDao().insertCollection(Collection(name = name.trim()))

    suspend fun deleteCollection(collection: Collection) =
        db.collectionDao().deleteCollection(collection.id)

    // ------------------------------------------------------------------
    // Saved papers
    // ------------------------------------------------------------------

    fun getPapersForCollection(collectionId: Long): Flow<List<SavedPaper>> =
        db.savedPaperDao().getPapersForCollection(collectionId)

    suspend fun isSaved(collectionId: Long, paperId: String): Boolean =
        db.savedPaperDao().isSaved(collectionId, paperId) > 0

    suspend fun savePaper(savedPaper: SavedPaper): Long =
        db.savedPaperDao().insertSavedPaper(savedPaper)

    suspend fun removePaper(collectionId: Long, paperId: String) =
        db.savedPaperDao().deleteSavedPaper(collectionId, paperId)

    fun getPaperCountForCollection(collectionId: Long): Flow<Int> =
        db.savedPaperDao().getPaperCountForCollection(collectionId)
}
