package com.rasaser.free.kurankelime

import android.annotation.TargetApi
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Point
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.models.selection.SelectionBoundary
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.File
import java.util.Locale

/**
 * Entirely on-device PDF text finder. On Android 15+ it builds a durable SQLite FTS4 index
 * from the user's locally imported PDF. No remote service, permission or third-party SDK.
 * A scanned/image-only PDF has no searchable text: this class does not fabricate OCR results.
 *
 * Only the indexing code is public. Imported PDF contents stay in the app's private storage.
 */
class PdfTextEngine(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "quran_pdf_text_v1.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE metadata (k TEXT PRIMARY KEY, v TEXT NOT NULL)")
        db.execSQL("CREATE TABLE segment (id INTEGER PRIMARY KEY, page INTEGER NOT NULL, x REAL NOT NULL, y REAL NOT NULL, w REAL NOT NULL, h REAL NOT NULL, content TEXT NOT NULL)")
        db.execSQL("CREATE INDEX idx_segment_page ON segment(page)")
        db.execSQL("CREATE VIRTUAL TABLE segment_fts USING fts4(content, tokenize=unicode61)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Explicit PDF-index migration is required")
    }

    private fun meta(db: SQLiteDatabase, key: String): String? =
        db.rawQuery("SELECT v FROM metadata WHERE k=?", arrayOf(key)).use {
            if (it.moveToFirst()) it.getString(0) else null
        }

    private fun saveMeta(db: SQLiteDatabase, key: String, value: String) {
        db.insertWithOnConflict("metadata", null, ContentValues().apply {
            put("k", key); put("v", value)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun identity(file: File): String = file.length().toString() + ":" + file.lastModified()

    fun isReady(file: File): Boolean {
        val db = readableDatabase
        return meta(db, "book") == identity(file) && meta(db, "complete") == "1"
    }

    fun hasText(file: File): Boolean {
        val db = readableDatabase
        return isReady(file) && (meta(db, "rows")?.toLongOrNull() ?: 0L) > 0L
    }

    /**
     * Supports resuming from the previous fully committed page after process death.
     * Call on Dispatchers.IO, never on the main thread. Progress callback is 1..pageCount.
     */
    @TargetApi(35)
    fun buildIfNeeded(file: File, onProgress: (Int, Int) -> Unit = { _, _ -> }): Boolean {
        if (Build.VERSION.SDK_INT < 35) return false
        if (isReady(file)) return hasText(file)
        val db = writableDatabase
        val id = identity(file)
        if (meta(db, "book") != id) {
            db.beginTransaction()
            try {
                db.delete("segment_fts", null, null)
                db.delete("segment", null, null)
                db.delete("metadata", null, null)
                saveMeta(db, "book", id)
                saveMeta(db, "next_page", "1")
                saveMeta(db, "complete", "0")
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { pdf ->
                val first = (meta(db, "next_page")?.toIntOrNull() ?: 1).coerceIn(1, pdf.pageCount + 1)
                for (pageNo in first..pdf.pageCount) {
                    // Read text while the PDF page is open; never persist it to this public repo.
                    val segments = ArrayList<ExtractedSegment>()
                    pdf.openPage(pageNo - 1).use { page ->
                        val scaleX = 595f / page.width
                        val scaleY = 842f / page.height
                        for (block in page.textContents) {
                            val content = block.text.trim().take(2000)
                            if (content.isBlank()) continue
                            val bounds = block.bounds
                            val lines = content.lines().filter { it.isNotBlank() }
                            for ((i, bound) in bounds.withIndex()) {
                                val line = if (lines.size == bounds.size) lines[i].trim() else content
                                if (line.isBlank() || bound.width() <= 0f || bound.height() <= 0f) continue
                                segments += ExtractedSegment(
                                    bound.left * scaleX, bound.top * scaleY,
                                    bound.width() * scaleX, bound.height() * scaleY, line
                                )
                            }
                        }
                    }
                    db.beginTransaction()
                    try {
                        db.execSQL("DELETE FROM segment_fts WHERE docid IN (SELECT id FROM segment WHERE page=?)", arrayOf(pageNo))
                        db.delete("segment", "page=?", arrayOf(pageNo.toString()))
                        for (segment in segments) {
                            val row = db.insertOrThrow("segment", null, ContentValues().apply {
                                put("page", pageNo); put("x", segment.x); put("y", segment.y)
                                put("w", segment.w); put("h", segment.h); put("content", segment.text)
                            })
                            db.execSQL("INSERT INTO segment_fts(docid,content) VALUES(?,?)",
                                arrayOf(row, segment.text))
                        }
                        saveMeta(db, "next_page", (pageNo + 1).toString())
                        db.setTransactionSuccessful()
                    } finally { db.endTransaction() }
                    onProgress(pageNo, pdf.pageCount)
                }
            }
        }
        db.beginTransaction()
        try {
            val count = db.rawQuery("SELECT COUNT(*) FROM segment", null).use {
                if (it.moveToFirst()) it.getLong(0) else 0L
            }
            saveMeta(db, "rows", count.toString())
            saveMeta(db, "complete", "1")
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        return hasText(file)
    }

    fun search(query: String, limit: Int = 200): List<PageWord> {
        val terms = query.trim().lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .split(Regex("\\s+")).filter { it.isNotBlank() }.take(6)
        if (terms.isEmpty()) return emptyList()
        val fts = terms.joinToString(" ") { it + "*" }
        val output = ArrayList<PageWord>()
        try {
            readableDatabase.rawQuery(
                "SELECT s.id,s.page,s.x,s.y,s.w,s.h,s.content FROM segment_fts f " +
                    "JOIN segment s ON s.id=f.docid WHERE segment_fts MATCH ? " +
                    "ORDER BY s.page,s.id LIMIT ?",
                arrayOf(fts, limit.coerceIn(1, 500).toString())
            ).use { c ->
                while (c.moveToNext()) output += PageWord(
                    -c.getInt(0), c.getInt(1), 0, c.getFloat(2), c.getFloat(3),
                    c.getFloat(4), c.getFloat(5), c.getString(6), c.getString(6)
                )
            }
        } catch (_: android.database.sqlite.SQLiteException) { /* Invalid query cannot crash UI. */ }
        return output
    }

    fun byId(id: Int): PageWord? {
        if (id >= 0) return null
        return readableDatabase.rawQuery(
            "SELECT id,page,x,y,w,h,content FROM segment WHERE id=?",
            arrayOf((-id.toLong()).toString())
        ).use { c ->
            if (!c.moveToFirst()) null else PageWord(
                -c.getInt(0), c.getInt(1), 0, c.getFloat(2), c.getFloat(3),
                c.getFloat(4), c.getFloat(5), c.getString(6), c.getString(6)
            )
        }
    }

    /** The Android 15 PDF engine resolves a touched point to a real text selection. */
    @TargetApi(35)
    fun selectAt(file: File, pageNo: Int, x: Float, y: Float): PageWord? {
        if (Build.VERSION.SDK_INT < 35) return null
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    renderer.openPage(pageNo - 1).use { page ->
                        val point = Point(
                            (x * page.width / 595f).toInt().coerceIn(0, page.width - 1),
                            (y * page.height / 842f).toInt().coerceIn(0, page.height - 1)
                        )
                        val boundary = SelectionBoundary(point)
                        val selected = page.selectContent(boundary, boundary)
                            ?.selectedTextContents?.firstOrNull { it.text.isNotBlank() }
                            ?: return@use null
                        val rect = selected.bounds.firstOrNull() ?: return@use null
                        PageWord(
                            0, pageNo, 0, rect.left * 595f / page.width,
                            rect.top * 842f / page.height, rect.width() * 595f / page.width,
                            rect.height() * 842f / page.height,
                            selected.text.trim(), selected.text.trim()
                        )
                    }
                }
            }
        } catch (_: Exception) { null }
    }

    private data class ExtractedSegment(
        val x: Float, val y: Float, val w: Float, val h: Float, val text: String
    )
}
