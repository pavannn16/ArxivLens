package com.arxivlens.util

import com.arxivlens.data.model.SavedPaper

/**
 * Pure BibTeX export pipeline.
 *
 * Every function here is a pure function:
 *   - No I/O, no Android imports, no shared state
 *   - Same input always produces same output
 *   - Trivially unit-testable without any Android framework
 *
 * The entry point is [collectionToBibTex], which follows the functional
 * pipeline described in the project plan and adds cite-key deduplication:
 *
 *   papers.map { paperToBibTex(it, uniqueKey) }.joinToString("\n\n")
 */
object BibTexExporter {

    /**
     * Converts a list of [SavedPaper]s into a complete `.bib` file string.
     * Returns an empty string if the list is empty.
     *
     * Pipeline steps:
     *  1. splitBy { doi.isNotBlank() } — peer-reviewed entries (with DOI) go first.
     *  2. map { buildCiteKey }         — derive base cite keys.
     *  3. frequencyMap()               — count duplicates (generic function with for-loop).
     *  4. zip + map                    — pair each paper with its deduplicated key.
     *  5. joinToString("\n\n")         — fold into a single .bib string.
     */
    fun collectionToBibTex(papers: List<SavedPaper>): String {
        if (papers.isEmpty()) return ""

        // splitBy: generic FP utility — peer-reviewed (DOI present) entries first in the .bib
        val (peerReviewed, preprints) = papers.splitBy { it.doi.isNotBlank() }
        val orderedPapers = peerReviewed + preprints

        val baseKeys = orderedPapers.map { buildCiteKey(it) }
        // frequencyMap: generic FP utility using a for-loop — detects duplicate base keys
        val totals   = baseKeys.frequencyMap()
        val seen     = mutableMapOf<String, Int>()

        return orderedPapers
            .zip(baseKeys)
            .map { (paper, baseKey) ->
                val nextIndex = (seen[baseKey] ?: 0) + 1
                seen[baseKey] = nextIndex

                val uniqueKey = if ((totals[baseKey] ?: 0) > 1) {
                    "$baseKey${('a'.code + nextIndex - 1).toChar()}"
                } else {
                    baseKey
                }

                paperToBibTex(paper, uniqueKey)
            }
            .joinToString("\n\n")
    }

    /**
     * Converts one [SavedPaper] to a `@misc` BibTeX entry.
     *
     * Uses `@misc` + `eprint` + `archivePrefix = {arXiv}` — the universally
     * accepted standard for citing arXiv preprints in natbib / biblatex /
     * Overleaf. If a DOI is present, it is included as an extra field.
     *
     * Example output:
     * ```
     * @misc{vaswani2017,
     *   author        = {Ashish Vaswani and Noam Shazeer and ...},
     *   title         = {Attention Is All You Need},
     *   year          = {2017},
     *   eprint        = {1706.03762},
     *   archivePrefix = {arXiv},
     *   primaryClass  = {cs.CL},
     *   url           = {https://arxiv.org/abs/1706.03762}
     * }
     * ```
     */
    fun paperToBibTex(paper: SavedPaper): String {
        val citeKey     = buildCiteKey(paper)
        return paperToBibTex(paper, citeKey)
    }

    private fun paperToBibTex(paper: SavedPaper, citeKey: String): String {
        val authorField = formatAuthors(paper.authors)
        val year        = extractYear(paper.publishedDate)
        // arXiv ID may contain a version suffix like "2301.12345v2"; strip it for the eprint field
        val eprint      = paper.paperId.substringBefore("v").trim()
        val primaryClass = paper.categories
            .split(",")
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?: ""

        return buildString {
            appendLine("@misc{$citeKey,")
            appendField("author",        authorField)
            appendField("title",         sanitizeLatex(paper.title))
            appendField("year",          year)
            appendField("eprint",        eprint)
            appendField("archivePrefix", "arXiv")
            if (primaryClass.isNotBlank()) appendField("primaryClass", primaryClass)
            appendField("url",           paper.arxivUrl)
            if (paper.doi.isNotBlank())  appendField("doi", paper.doi)
            append("}")
        }
    }

    // -------------------------------------------------------------------------
    // Pure helper functions
    // -------------------------------------------------------------------------

    /**
     * Builds a BibTeX cite key in the format `<firstAuthorLastName><year>`.
     * Falls back to the arXiv ID if no author is present.
     *
     * Examples: "vaswani2017", "lecun1998", "arxiv2301_12345"
     */
    fun buildCiteKey(paper: SavedPaper): String {
        val year       = extractYear(paper.publishedDate)
        val firstAuthor = paper.authors.split(",").firstOrNull()?.trim() ?: ""
        val lastName    = firstAuthor.split(" ").lastOrNull()?.trim() ?: ""

        val base = if (lastName.isNotBlank()) lastName.lowercase() else "arxiv${paper.paperId.replace(".", "_")}"
        return sanitizeCiteKey("$base$year")
    }

    /**
     * Converts the comma-separated authors string from the DB into BibTeX
     * " and "-separated format required by BibTeX.
     *
     * "Ashish Vaswani, Noam Shazeer" → "Ashish Vaswani and Noam Shazeer"
     */
    fun formatAuthors(authorsCommaSeparated: String): String =
        authorsCommaSeparated
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" and ")

    /**
     * Extracts the 4-digit year from an ISO-8601 date string.
     * "2023-01-30T18:00:00Z" → "2023"
     * Returns "????" if parsing fails so the entry is still valid BibTeX.
     */
    fun extractYear(isoDate: String): String =
        isoDate.take(4).takeIf { it.all { c -> c.isDigit() } } ?: "????"

    /**
     * Removes characters illegal in BibTeX cite keys (spaces, braces, commas, etc.).
     */
    private fun sanitizeCiteKey(key: String): String =
        key.filter { it.isLetterOrDigit() || it == '_' || it == '-' }

    /**
     * Escapes a handful of special LaTeX characters that would break compilation
     * if left raw in a BibTeX title field. Wraps the result in braces to prevent
     * BibTeX from lowercasing the title.
     */
    fun sanitizeLatex(text: String): String {
        val escaped = text
            .replace("&",  "\\&")
            .replace("%",  "\\%")
            .replace("$",  "\\$")
            .replace("#",  "\\#")
            .replace("_",  "\\_")
            .replace("{",  "\\{")
            .replace("}",  "\\}")
            .replace("~",  "\\textasciitilde{}")
            .replace("^",  "\\textasciicircum{}")
            .replace("\\\\", "\\textbackslash{}")
        return "{$escaped}"
    }

    // -------------------------------------------------------------------------
    // StringBuilder extension for clean field formatting
    // -------------------------------------------------------------------------

    private fun StringBuilder.appendField(name: String, value: String) {
        val padded = name.padEnd(13)   // align '=' signs for readability
        appendLine("  $padded = {$value},")
    }
}
