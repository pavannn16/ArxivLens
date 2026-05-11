package com.arxivlens.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arxivlens.data.model.SavedPaper
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [SavedPaper] rows.
 */
@Dao
interface SavedPaperDao {

    /**
     * Emits all papers in a given collection, ordered by save-time descending.
     * Re-emits automatically whenever the collection's contents change.
     */
    @Query("SELECT * FROM saved_papers WHERE collectionId = :collectionId ORDER BY savedAt DESC")
    fun getPapersForCollection(collectionId: Long): Flow<List<SavedPaper>>

    /** Checks whether a specific paper is already saved in a specific collection. */
    @Query(
        "SELECT COUNT(*) FROM saved_papers WHERE collectionId = :collectionId AND paperId = :paperId"
    )
    suspend fun isSaved(collectionId: Long, paperId: String): Int

    /**
     * Inserts a paper into a collection.
     * IGNORE strategy silently skips duplicates (same collectionId + paperId).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSavedPaper(savedPaper: SavedPaper): Long

    /** Removes a specific paper from a specific collection. */
    @Query(
        "DELETE FROM saved_papers WHERE collectionId = :collectionId AND paperId = :paperId"
    )
    suspend fun deleteSavedPaper(collectionId: Long, paperId: String): Int

    /** Returns the count of papers in a collection — used for the collections list subtitle. */
    @Query("SELECT COUNT(*) FROM saved_papers WHERE collectionId = :collectionId")
    fun getPaperCountForCollection(collectionId: Long): Flow<Int>
}
