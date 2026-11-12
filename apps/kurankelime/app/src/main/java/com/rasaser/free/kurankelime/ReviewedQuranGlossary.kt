package com.rasaser.free.kurankelime

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.text.Normalizer

/**
 * Optional, local, manually reviewed Quran WORD glossary.
 *
 * No OCR candidate, nearby Turkish printed text, auto-translation or preview
 * seed is ever promoted to a "verified" Quran meaning.
 *
 * Private asset (when independently reviewed/licensed):
 * assets/quran/verified_word_glosses.sqlite
 * Table reviewed_word_glosses:
 * page INTEGER, ocr_id INTEGER, source_language TEXT, target_language TEXT,
 * source_text TEXT, meaning TEXT, source_reference TEXT, reviewer TEXT,
 * review_status TEXT, source_pdf_sha256 TEXT.
 * The page/ocr_id match the stable 605-page selectable OCR package.
 */
internal class ReviewedQuranGlossary(private val context: Context) {
    data class Entry(val meaning: String, val reference: String)
    private val asset = "quran/verified_word_glosses.sqlite"
    private val sourceSha = "387674c9a3bfb2c501c2dcca4b63946ea05f386dcd4d6fa4b63b4060b01397ea"
    private val localFile = File(context.noBackupFilesDir, "quran_reviewed_words_v1.sqlite")
    private val guard = Any()
    private var connection: SQLiteDatabase? = null

    private fun db(): SQLiteDatabase? = synchronized(guard) {
        connection?.takeIf { it.isOpen }?.let { return@synchronized it }
        // Public builds have NO reviewed glossary. This is intentional.
        try { context.assets.open(asset).close() } catch (_: Exception) { return@synchronized null }
        if (!localFile.isFile || localFile.length() < 1024L) {
            val pending = File(localFile.parentFile, localFile.name + ".pending")
            try {
                pending.delete()
                context.assets.open(asset).use { source ->
                    pending.outputStream().use { output -> source.copyTo(output, 65536) }
                }
                if (!pending.renameTo(localFile)) {
                    pending.copyTo(localFile, overwrite = true)
                    pending.delete()
                }
            } catch (_: Exception) {
                pending.delete()
                return@synchronized null
            }
        }
        try {
            val opened = SQLiteDatabase.openDatabase(
                localFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY
            )
            connection = opened
            opened
        } catch (_: Exception) { null }
    }

    fun find(
        page: Int, ocrId: Int, source: String, target: String, selectedText: String
    ): Entry? {
        if (page !in 1..605 || ocrId <= 0 || selectedText.isBlank()) return null
        if (source !in setOf("ar", "tr") || target !in setOf("tr", "en", "de", "es", "zh", "ar"))
            return null
        val database = db() ?: return null
        return try {
            database.rawQuery(
                "SELECT source_text,meaning,source_reference,reviewer,source_pdf_sha256 " +
                    "FROM reviewed_word_glosses WHERE page=? AND ocr_id=? " +
                    "AND source_language=? AND target_language=? " +
                    "AND review_status='approved' LIMIT 1",
                arrayOf(page.toString(), ocrId.toString(), source, target)
            ).use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val original = cursor.getString(0)?.trim().orEmpty()
                val meaning = cursor.getString(1)?.trim().orEmpty()
                val reference = cursor.getString(2)?.trim().orEmpty()
                val reviewer = cursor.getString(3)?.trim().orEmpty()
                val hash = cursor.getString(4).orEmpty()
                // Require exact source-token match. No fuzzy matching of scripture.
                val canonical = { value: String ->
                    Normalizer.normalize(value.trim(), Normalizer.Form.NFC)
                }
                if (canonical(original) != canonical(selectedText) ||
                    meaning.isBlank() || reference.isBlank() || reviewer.isBlank() ||
                    hash != sourceSha
                ) null else Entry(meaning, reference)
            }
        } catch (_: Exception) { null }
    }

    fun close() = synchronized(guard) {
        connection?.close()
        connection = null
    }
}
