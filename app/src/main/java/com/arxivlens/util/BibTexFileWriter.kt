package com.arxivlens.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Writes a BibTeX string to a cache file and returns a shareable [Uri]
 * via [FileProvider].
 *
 * This is the only file in the BibTeX pipeline that touches Android APIs.
 * It receives the already-rendered string from [BibTexExporter] and handles
 * the I/O + URI generation so the ViewModel can hand the URI to the UI.
 */
object BibTexFileWriter {

    private const val EXPORTS_DIR = "exports"

    /**
     * Writes [bibTexContent] to `<cacheDir>/exports/<fileName>.bib` and
     * returns a [FileProvider] URI that can be shared via [Intent.ACTION_SEND].
     *
     * The exports directory is created if it does not exist.
     * Any previous file with the same name is overwritten.
     *
     * @param context     Application or Activity context.
     * @param bibTexContent The fully rendered `.bib` string from [BibTexExporter].
     * @param fileName    Base filename without extension, e.g. "MyCollection".
     * @return A content:// [Uri] safe to pass to other apps.
     */
    fun writeBibFile(
        context: Context,
        bibTexContent: String,
        fileName: String
    ): Uri {
        val exportsDir = File(context.cacheDir, EXPORTS_DIR).also { it.mkdirs() }
        val bibFile    = File(exportsDir, "${sanitizeFileName(fileName)}.bib")

        bibFile.writeText(bibTexContent, Charsets.UTF_8)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            bibFile
        )
    }

    /**
     * Builds a chooser [Intent] that lets the user share the `.bib` file
     * with any app (e.g. Files, email, Google Drive, etc.).
     *
     * @param uri       The content:// URI returned by [writeBibFile].
     * @param fileName  Base filename, used in the share sheet title.
     */
    fun buildShareIntent(uri: Uri, fileName: String): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type    = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${fileName}.bib")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "Share BibTeX file via…")
    }

    // -------------------------------------------------------------------------
    // Pure helper
    // -------------------------------------------------------------------------

    /**
     * Strips characters that are illegal in most file systems.
     * "My Collection!" → "My_Collection"
     */
    private fun sanitizeFileName(name: String): String =
        name.trim()
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(64)
}
