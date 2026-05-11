package com.arxivlens.data.model

/**
 * Immutable domain model for an arXiv paper.
 *
 * Returned by the XML parser and passed unchanged through the UI layer.
 * Kept as a pure data class with no Room annotations — the FP "value object"
 * pattern: equality is structural, instances are never mutated.
 *
 * Data class features demonstrated:
 *   • Structural equality  — two Papers with identical fields are == (auto-generated equals)
 *   • copy()               — used in ArxivXmlParser to derive pdfUrl without mutation
 *   • toString()           — overridden below to produce a concise human-readable summary
 */
data class Paper(
    /** arXiv ID, e.g. "2301.12345v2" */
    val id: String,
    val title: String,
    val authors: List<String>,
    val abstract: String,
    /** ISO-8601 date string from the Atom feed, e.g. "2023-01-30T18:00:00Z" */
    val publishedDate: String,
    val updatedDate: String,
    val categories: List<String>,
    val pdfUrl: String,
    val arxivUrl: String,
    /** Empty string when the author did not register a DOI with arXiv. */
    val doi: String = ""
) {
    /**
     * Human-readable one-line summary — overrides the data class default toString()
     * (which would print every field). Used for logging and debug output.
     *
     * Example: Paper[2301.12345v2] "Attention Is All You Need" by Vaswani (2017)
     */
    override fun toString(): String {
        val year       = publishedDate.take(4)
        val firstAuthor = authors.firstOrNull()?.substringAfterLast(" ") ?: "unknown"
        return "Paper[$id] \"$title\" by $firstAuthor ($year)"
    }
}
