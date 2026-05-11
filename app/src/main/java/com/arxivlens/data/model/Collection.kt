package com.arxivlens.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a named collection (folder) of saved papers.
 * Analogous to a Zotero collection or a Mendeley folder.
 */
@Entity(tableName = "collections")
data class Collection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
