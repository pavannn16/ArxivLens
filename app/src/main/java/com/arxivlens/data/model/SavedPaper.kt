package com.arxivlens.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room entity that stores a paper inside a collection.
 *
 * Paper fields are stored denormalized (no separate papers table) so that
 * offline reading works without any join. The composite primary key
 * (collectionId + paperId) naturally prevents saving the same paper twice
 * in the same collection.
 *
 * ForeignKey with CASCADE ensures all saved papers are deleted when their
 * parent collection is deleted — no orphan rows.
 */
@Entity(
    tableName = "saved_papers",
    primaryKeys = ["collectionId", "paperId"],
    foreignKeys = [
        ForeignKey(
            entity = Collection::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("collectionId")]
)
data class SavedPaper(
    val collectionId: Long,
    val paperId: String,
    val title: String,
    /** Authors stored as a comma-separated string to avoid a TypeConverter. */
    val authors: String,
    val abstract: String,
    val publishedDate: String,
    /** Categories stored as a comma-separated string. */
    val categories: String,
    val pdfUrl: String,
    val arxivUrl: String,
    val doi: String,
    val savedAt: Long = System.currentTimeMillis()
)

// ---------------------------------------------------------------------------
// Pure conversion functions (FP — no side effects, no mutation)
// ---------------------------------------------------------------------------

/** Maps a domain [Paper] into a [SavedPaper] row for the given collection. */
fun Paper.toSavedPaper(collectionId: Long): SavedPaper = SavedPaper(
    collectionId = collectionId,
    paperId      = id,
    title        = title,
    authors      = authors.joinToString(", "),
    abstract     = abstract,
    publishedDate = publishedDate,
    categories   = categories.joinToString(", "),
    pdfUrl       = pdfUrl,
    arxivUrl     = arxivUrl,
    doi          = doi
)

/** Reconstructs a domain [Paper] from a stored [SavedPaper] row. */
fun SavedPaper.toPaper(): Paper = Paper(
    id            = paperId,
    title         = title,
    authors       = authors.split(", ").filter { it.isNotBlank() },
    abstract      = abstract,
    publishedDate = publishedDate,
    updatedDate   = publishedDate,   // Atom feed updated is not stored separately
    categories    = categories.split(", ").filter { it.isNotBlank() },
    pdfUrl        = pdfUrl,
    arxivUrl      = arxivUrl,
    doi           = doi
)
