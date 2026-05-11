package com.arxivlens.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arxivlens.data.model.Collection
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [Collection] rows.
 *
 * All read operations return [Flow] so the UI automatically recomposes
 * whenever the database changes — no manual polling or LiveData needed.
 */
@Dao
interface CollectionDao {

    /** Emits the full list of collections ordered newest-first. Re-emits on every change. */
    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    fun getAllCollections(): Flow<List<Collection>>

    /** One-shot fetch — useful when we need a collection's name in a coroutine. */
    @Query("SELECT * FROM collections WHERE id = :id LIMIT 1")
    suspend fun getCollectionById(id: Long): Collection?

    /** Inserts a new collection; returns the auto-generated row id. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCollection(collection: Collection): Long

    /** Deletes a collection by id; cascade will remove all its saved papers automatically. */
    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: Long): Int
}
