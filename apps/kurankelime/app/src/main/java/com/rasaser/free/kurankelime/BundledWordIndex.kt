package com.rasaser.free.kurankelime

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.text.Normalizer
import java.util.Locale

/**
 * The optional, entirely local OCR search/geometry pack is compiled into the APK
 * at assets/quran/word_ocr.sqlite. This must NEVER download user content.
 *
 * OCR words are explicitly unverified. The PDF is the visual source of truth;
 * a verse number is shown only if it has an independently checked crosswalk.
 * Real translations for en/de/es/zh must be added to verified_words before
 * enabling those languages' word glosses.
 */
class BundledWordIndex(private val context: Context) {
    companion object { private const val ASSET = "quran/word_ocr.sqlite"; private const val FIRST_ID = 1000000 }
    private val file = File(context.noBackupFilesDir, "quran_words_ocr_v2.sqlite")
    private var db: SQLiteDatabase? = null
    private val lock = Any()

    val isBundled: Boolean get() =
        try { context.assets.open(ASSET).close(); true } catch (_: Exception) { false }

    private fun ready(): SQLiteDatabase? = synchronized(lock) {
        if (db?.isOpen == true) return@synchronized db
        if (!isBundled) return@synchronized null
        if (!file.isFile || file.length() < 1000) {
            val pending = File(file.parentFile, file.name + ".pending")
            try {
                pending.delete()
                context.assets.open(ASSET).use { input ->
                    pending.outputStream().use { input.copyTo(it, 65536) }
                }
                if (file.exists()) file.delete()
                if (!pending.renameTo(file)) {
                    pending.copyTo(file, overwrite = true)
                    pending.delete()
                }
            } catch (_: Exception) { pending.delete(); return@synchronized null }
        }
        try {
            val opened = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
            val count = opened.rawQuery("SELECT COUNT(*) FROM pages", null).use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
            val sourceSha = opened.rawQuery("SELECT value FROM meta WHERE key='source_pdf_sha256'", null).use {
                if (it.moveToFirst()) it.getString(0) else ""
            }
            if (count != 304 || sourceSha != "387674c9a3bfb2c501c2dcca4b63946ea05f386dcd4d6fa4b63b4060b01397ea") {
                opened.close()
                return@synchronized null
            }
            db = opened
            opened
        } catch (_: Exception) { null }
    }

    private fun normalized(query: String): String {
        val v = query.lowercase(Locale.forLanguageTag("tr-TR")).replace('ı', 'i')
        return Normalizer.normalize(v, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
    }

    private fun fromCursor(c: android.database.Cursor): PageWord = PageWord(
        FIRST_ID + c.getInt(0), c.getInt(1), 0,
        c.getFloat(2), c.getFloat(3), c.getFloat(4), c.getFloat(5),
        c.getString(6), normalized(c.getString(6))
    )

    /** Search is local SQLite FTS, no network. Results link to original PDF page.
     *  Until independently verified, no surah/ayah number is fabricated.
     */
    fun search(query: String, limit: Int = 200): List<PageWord> {
        val database = ready() ?: return emptyList()
        val terms = normalized(query).split(' ').filter { it.isNotBlank() }.take(6)
        if (terms.isEmpty()) return emptyList()
        val match = terms.joinToString(" ") { it + "*" }
        val searchLang = if (query.any { it.code in 0x0600..0x06FF }) "ara" else "tur"
        val output = ArrayList<PageWord>()
        try {
            database.rawQuery(
                "SELECT t.id,t.page,t.x,t.y,t.w,t.h,t.token FROM ocr_fts f " +
                    "JOIN ocr_tokens t ON t.id=f.docid " +
                    "WHERE ocr_fts MATCH ? AND t.lang=? AND t.confidence>=40 " +
                    "ORDER BY t.page,t.y,t.x LIMIT ?",
                arrayOf(match, searchLang, limit.coerceIn(1, 500).toString())
            ).use { c -> while (c.moveToNext()) output += fromCursor(c) }
        } catch (_: android.database.sqlite.SQLiteException) {}
        return output
    }

    fun byId(id: Int): PageWord? {
        val database = ready() ?: return null
        if (id < FIRST_ID) return null
        return database.rawQuery(
            "SELECT id,page,x,y,w,h,token FROM ocr_tokens WHERE id=?",
            arrayOf((id - FIRST_ID).toString())
        ).use { c -> if (c.moveToFirst()) fromCursor(c) else null }
    }

    /** Return an OCR candidate within/near touch, including Arabic line above its gloss.
     *  Translation is OCR Turkish text, not a certified word-level alignment.
     */
    fun nearest(page: Int, x: Float, y: Float): PageWord? {
        val database = ready() ?: return null
        val candidates = ArrayList<PageWord>()
        database.rawQuery(
            "SELECT id,page,x,y,w,h,token FROM ocr_tokens " +
                "WHERE page=? AND lang='tur' AND confidence>=40 " +
                "AND x<=? AND (x+w)>=? AND y<=? AND (y+h)>=? LIMIT 30",
            arrayOf(page.toString(),(x + 13f).toString(),(x - 13f).toString(),
                (y + 27f).toString(),(y - 14f).toString())
        ).use { c -> while (c.moveToNext()) candidates += fromCursor(c) }
        return candidates.minByOrNull {
            val cx = it.x + it.width * .5f
            val cy = it.y - 7f
            (cx-x)*(cx-x) + (cy-y)*(cy-y)
        }
    }

    fun isVerified(id: Int): Boolean {
        val database = ready() ?: return false
        if (id < FIRST_ID) return false
        return database.rawQuery(
            "SELECT verified FROM ocr_tokens WHERE id=?",
            arrayOf((id - FIRST_ID).toString())
        ).use { it.moveToFirst() && it.getInt(0) == 1 }
    }
}
