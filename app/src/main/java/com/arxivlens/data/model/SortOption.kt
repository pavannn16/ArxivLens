package com.arxivlens.data.model

/**
 * Sort options available for search results.
 *
 * Each option defines a [Comparator] used to re-order the paper list client-side.
 * RELEVANCE keeps the original arXiv API ranking (no comparator applied).
 * MOST_REVISED uses the version number in the arXiv ID as a proxy for community interest.
 * PEER_REVIEWED surfaces DOI-registered (journal-published) papers first.
 */
enum class SortOption(val label: String) {
    RELEVANCE("Relevance (default)"),
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    MOST_REVISED("Most revised"),
    PEER_REVIEWED("Peer reviewed first"),
    TITLE_AZ("Title A \u2192 Z");

    /** Returns null for RELEVANCE (keep original order), otherwise a comparator. */
    fun comparator(): Comparator<Paper>? = when (this) {
        RELEVANCE     -> null
        NEWEST        -> compareByDescending { it.publishedDate }
        OLDEST        -> compareBy { it.publishedDate }
        MOST_REVISED  -> compareByDescending { it.versionNumber() }
        PEER_REVIEWED -> compareByDescending { if (it.doi.isNotBlank()) 1 else 0 }
        TITLE_AZ      -> compareBy { it.title.lowercase() }
    }
}

/** Extracts the revision number from an arXiv ID, e.g. "2301.12345v3" → 3. */
private fun Paper.versionNumber(): Int =
    Regex("v(\\d+)$").find(id)?.groupValues?.get(1)?.toIntOrNull() ?: 1
