package com.arxivlens.util

/**
 * Pure functional programming utility extensions.
 *
 * Every function here is:
 *   - Generic: works over any element type T
 *   - Pure: no I/O, no side effects, same input → same output
 *   - Higher-order: accepts or returns lambdas
 *
 * Demonstrates:
 *   • Generic functions  — topNBy, splitBy, frequencyMap
 *   • Generic constraints — <K : Comparable<K>> on topNBy
 *   • for-loop control flow — explicit iteration inside frequencyMap
 *   • Lambda parameters   — selector and predicate arguments
 */

/**
 * Returns the top [n] elements of this list, ranked descending by a [Comparable] key.
 *
 * The upper-bound constraint [K : Comparable<K>] guarantees that only types
 * which support natural ordering (String, Int, Long, etc.) can be used as keys —
 * the same pattern used by Kotlin's own sortedBy / maxByOrNull.
 *
 * Usage in ArxivLens:
 *   papers.topNBy(5) { it.publishedDate }   // 5 most recently published
 *   papers.topNBy(3) { it.id }              // 3 lexicographically latest IDs
 *
 * @param n        Maximum number of elements to return.
 * @param selector Lambda that extracts the comparable sort key from each element.
 */
fun <T, K : Comparable<K>> List<T>.topNBy(n: Int, selector: (T) -> K): List<T> =
    sortedByDescending(selector).take(n)

/**
 * Partitions this list into a [Pair] of (elements matching [predicate], elements not matching).
 *
 * A named alias for the stdlib [partition] that reads clearly in pipeline chains.
 * Both halves preserve the original relative order of elements.
 *
 * Usage in ArxivLens:
 *   val (peerReviewed, preprints) = papers.splitBy { it.doi.isNotBlank() }
 *
 * @param predicate Lambda that returns true for elements to put in the first list.
 */
fun <T> List<T>.splitBy(predicate: (T) -> Boolean): Pair<List<T>, List<T>> =
    partition(predicate)

/**
 * Builds a frequency map counting how many times each element appears.
 *
 * Uses a [for] loop with an Elvis-operator accumulation to demonstrate explicit
 * control-flow iteration as a pure alternative to [groupingBy].eachCount().
 *
 * Example:
 *   listOf("a", "b", "a", "c", "b", "b").frequencyMap()
 *   // → {a=2, b=3, c=1}
 *
 * Used in [BibTexExporter.collectionToBibTex] to detect duplicate cite-key bases
 * before appending disambiguation suffixes (vaswani2017a, vaswani2017b, …).
 */
fun <T> List<T>.frequencyMap(): Map<T, Int> {
    val counts = mutableMapOf<T, Int>()
    for (item in this) {
        counts[item] = (counts[item] ?: 0) + 1
    }
    return counts
}
