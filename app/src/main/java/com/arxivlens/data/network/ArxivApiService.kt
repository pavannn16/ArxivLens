package com.arxivlens.data.network

import com.arxivlens.data.model.Paper
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Thin wrapper around OkHttp that fetches arXiv Atom XML and delegates
 * parsing to [ArxivXmlParser].
 *
 * All methods are suspending so callers must run them on Dispatchers.IO.
 * The class itself is stateless — safe to share across coroutines.
 */
class ArxivApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Searches arXiv for papers matching [query].
     *
     * @param query   Free-text search string (e.g. "transformer attention").
     * @param start   0-based offset for pagination.
     * @param maxResults Maximum papers to return (arXiv caps at 2000 per call).
     * @return [Result.success] containing a (possibly empty) list of [Paper]s,
     *         or [Result.failure] with the underlying exception.
     *
     * Using Kotlin's [Result] type keeps error handling functional — callers
     * fold/map over the result without catching exceptions themselves.
     */
    fun search(
        query: String,
        start: Int = 0,
        maxResults: Int = 20
    ): Result<List<Paper>> {
        // Encode the query for a safe URL. This handles spaces and symbols such as +, #, and quotes.
        val encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())
        val url = buildString {
            append("https://export.arxiv.org/api/query")
            append("?search_query=all:$encodedQuery")
            append("&start=$start")
            append("&max_results=$maxResults")
            append("&sortBy=relevance")
            append("&sortOrder=descending")
        }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "ArxivLens-Android/1.0")
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(
                    IOException("arXiv API returned HTTP ${response.code}")
                )
            }
            val body = response.body?.string()
                ?: return Result.failure(IOException("Empty response body"))
            Result.success(ArxivXmlParser.parse(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
