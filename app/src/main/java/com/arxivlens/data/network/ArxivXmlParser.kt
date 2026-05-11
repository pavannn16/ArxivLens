package com.arxivlens.data.network

import android.util.Xml
import com.arxivlens.data.model.Paper
import org.xmlpull.v1.XmlPullParser

/**
 * Pure function XML parser for the arXiv Atom 1.0 feed.
 *
 * "Pure" here means: given the same XML string it always returns the same
 * list of [Paper]s. No side effects, no shared mutable state. This makes
 * it trivially testable — just pass a string, assert the list.
 *
 * The arXiv Atom feed structure (relevant elements):
 *
 * <feed>
 *   <entry>
 *     <id>http://arxiv.org/abs/XXXX.XXXXXvN</id>
 *     <title>...</title>
 *     <summary>...</summary>
 *     <published>2023-01-30T18:00:00Z</published>
 *     <updated>2023-01-31T10:00:00Z</updated>
 *     <author><name>Foo Bar</name></author>
 *     <author><name>Baz Qux</name></author>
 *     <category term="cs.LG" />
 *     <link rel="alternate" href="http://arxiv.org/abs/XXXX" />
 *     <link rel="related"   href="http://arxiv.org/pdf/XXXX" title="pdf" />
 *     <arxiv:doi>10.xxxxx</arxiv:doi>     <!-- optional -->
 *   </entry>
 *   ...
 * </feed>
 */
object ArxivXmlParser {

    /**
     * Parses an arXiv Atom XML string and returns a list of [Paper]s.
     * Returns an empty list if the feed contains no entries.
     * Throws [org.xmlpull.v1.XmlPullParserException] on malformed XML
     * (caller should wrap in try/catch or use [ArxivApiService] which
     * returns [Result]).
     */
    fun parse(xml: String): List<Paper> {
        val parser: XmlPullParser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(xml.reader())
        }

        val papers = mutableListOf<Paper>()
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "entry") {
                papers.add(readEntry(parser))
            }
            eventType = parser.next()
        }

        return papers
    }

    // -------------------------------------------------------------------------
    // Private helpers — each reads one <entry> or one sub-element
    // -------------------------------------------------------------------------

    private fun readEntry(parser: XmlPullParser): Paper {
        parser.require(XmlPullParser.START_TAG, null, "entry")
        parser.next() // advance into entry's children

        var rawId       = ""
        var title       = ""
        var abstract    = ""
        var published   = ""
        var updated     = ""
        var doi         = ""
        val authors     = mutableListOf<String>()
        val categories  = mutableListOf<String>()
        var pdfUrl      = ""
        var arxivUrl    = ""

        while (!(parser.eventType == XmlPullParser.END_TAG && parser.name == "entry")) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "id"        -> rawId     = readText(parser)
                    "title"     -> title     = readText(parser).normalizeWhitespace()
                    "summary"   -> abstract  = readText(parser).normalizeWhitespace()
                    "published" -> published = readText(parser)
                    "updated"   -> updated   = readText(parser)
                    "author"    -> authors.add(readAuthor(parser))
                    "doi"       -> doi       = readText(parser)  // arxiv: ns element
                    "category"  -> {
                        val term = parser.getAttributeValue(null, "term")
                        if (!term.isNullOrBlank()) categories.add(term)
                        skipTag(parser)
                    }
                    "link"      -> {
                        val rel   = parser.getAttributeValue(null, "rel")
                        val href  = parser.getAttributeValue(null, "href") ?: ""
                        val title = parser.getAttributeValue(null, "title")
                        when {
                            rel == "alternate"                -> arxivUrl = href.toHttps()
                            rel == "related" && title == "pdf" -> pdfUrl = href.toHttps()
                        }
                        skipTag(parser)
                    }
                    else        -> skipTag(parser)
                }
            } else {
                parser.next()
            }
        }

        // arXiv id field is a full URL like "http://arxiv.org/abs/2301.12345v2"
        val paperId = rawId.substringAfterLast("/")

        val rawPaper = Paper(
            id            = paperId,
            title         = title,
            authors       = authors,
            abstract      = abstract,
            publishedDate = published,
            updatedDate   = updated,
            categories    = categories,
            pdfUrl        = pdfUrl,
            arxivUrl      = arxivUrl,
            doi           = doi
        )

        // Data class copy(): return an immutable copy with pdfUrl derived from arxivUrl
        // when the Atom feed does not include an explicit PDF link element.
        // Only pdfUrl changes — all other fields are identical to rawPaper.
        return if (rawPaper.pdfUrl.isBlank() && rawPaper.arxivUrl.isNotBlank()) {
            rawPaper.copy(pdfUrl = rawPaper.arxivUrl.replace("/abs/", "/pdf/"))
        } else {
            rawPaper
        }
    }

    /** Reads <author><name>Foo Bar</name></author> and returns the name. */
    private fun readAuthor(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, null, "author")
        var name = ""
        while (!(parser.eventType == XmlPullParser.END_TAG && parser.name == "author")) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "name") {
                name = readText(parser)
            } else {
                parser.next()
            }
        }
        return name
    }

    /**
     * Reads the text content of the current element and advances past its
     * END_TAG. Works for simple text-only elements.
     */
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text ?: ""
            parser.nextTag()
        }
        return result
    }

    /**
     * Skips the current element and all its children. Called for any tag
     * we don't need so the parser doesn't get confused by nested elements.
     */
    private fun skipTag(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) return
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG   -> depth--
            }
        }
    }

    // -------------------------------------------------------------------------
    // Pure string helpers
    // -------------------------------------------------------------------------

    /** Collapses runs of whitespace (including newlines) into a single space. */
    private fun String.normalizeWhitespace(): String =
        trim().replace(Regex("\\s+"), " ")

    /** Upgrades http:// to https:// for Android cleartext policy compliance. */
    private fun String.toHttps(): String =
        if (startsWith("http://")) replaceFirst("http://", "https://") else this
}
