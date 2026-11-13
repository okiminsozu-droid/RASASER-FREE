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
    companion object {
        private const val ASSET = "quran/word_ocr.sqlite"
        private const val FIRST_ID = 1000000

        /** Exclude malformed OCR rows containing embedded TSV/raw coordinate data. */
        fun isCleanSearchWord(text: String): Boolean =
            text.isNotBlank() && text.length <= 64 &&
            text.none { it == '\n' || it == '\r' || it == '\t' ||
                (it.code < 32 || it.code == 127) }
    }
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
                    "AND length(t.token) BETWEEN 1 AND 64 " +
                    "AND instr(t.token,char(9))=0 AND instr(t.token,char(10))=0 " +
                    "AND instr(t.token,char(13))=0 " +
                    "ORDER BY t.page,t.y,t.x LIMIT ?",
                arrayOf(match, searchLang, limit.coerceIn(1, 500).toString())
            ).use { c -> while (c.moveToNext()) {
                val word = fromCursor(c)
                if (isCleanSearchWord(word.gloss)) output += word
            } }
        } catch (_: android.database.sqlite.SQLiteException) {}
        return output
    }

    /** A selected printed word. OCR is a reading aid, not an authenticated Quran text. */
    data class Selection(
        val word: PageWord,
        val text: String,
        val sourceLanguage: String,
        val confidence: Float
    )

    /**
     * Select the OCR candidate whose visible letter box actually contains the touch.
     * Search both Arabic source words and Turkish glosses, not only Turkish.
     * Coordinates are in the preserved 595x842 source PDF sheet.
     */
    fun selectNearest(page: Int, x: Float, y: Float): Selection? {
        val database=ready() ?: return null
        data class Candidate(val row: PageWord,val lang:String,val conf:Float,val score:Float)
        val candidates=ArrayList<Candidate>()
        database.rawQuery(
            "SELECT id,page,x,y,w,h,token,lang,confidence FROM ocr_tokens " +
            "WHERE page=? AND lang IN ('ara','tur') " +
            "AND ((lang='ara' AND confidence>=28) OR (lang='tur' AND confidence>=45)) " +
            "AND x<=? AND (x+w)>=CAST(? AS REAL) AND y<=? AND (y+h)>=CAST(? AS REAL) LIMIT 100",
            arrayOf(page.toString(),(x+7f).toString(),(x-7f).toString(),
                (y+7f).toString(),(y-7f).toString())
        ).use { c ->
            while(c.moveToNext()) {
                val w=fromCursor(c)
                val l=c.getString(7)
                val conf=c.getFloat(8)
                val dx=when { x<w.x -> w.x-x; x>w.x+w.width -> x-(w.x+w.width); else -> 0f }
                val dy=when { y<w.y -> w.y-y; y>w.y+w.height -> y-(w.y+w.height); else -> 0f }
                val score=dx*dx+dy*dy+
                    // Resolve overlapping OCR rectangles by normalized center distance.
                    0.025f*((x-w.x-w.width/2f)*(x-w.x-w.width/2f) +
                        (y-w.y-w.height/2f)*(y-w.y-w.height/2f))
                candidates.add(Candidate(w,l,conf,score))
            }
        }
        val best=candidates.minByOrNull { it.score } ?: return null
        // Reject a faraway OCR word instead of visibly selecting the wrong word.
        if (best.score>70f) return null
        return Selection(best.row,best.row.gloss,if(best.lang=="ara") "ar" else "tr",best.conf)
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
                "AND x<=? AND (x+w)>=CAST(? AS REAL) AND y<=? AND (y+h)>=CAST(? AS REAL) LIMIT 30",
            arrayOf(page.toString(),(x + 13f).toString(),(x - 13f).toString(),
                (y + 27f).toString(),(y - 14f).toString())
        ).use { c -> while (c.moveToNext()) candidates += fromCursor(c) }
        return candidates.minByOrNull {
            val cx = it.x + it.width * .5f
            val cy = it.y - 7f
            (cx-x)*(cx-x) + (cy-y)*(cy-y)
        }
    }

    /**
     * Verifiable reference information for each OCR result.
     * Per-token verse numbers are shown ONLY when manually verified.
     * Otherwise display the estimated / visually checked verse RANGE of its page,
     * explicitly distinguishing that range from an exact word-to-verse link.
     */
    fun referenceLabel(id: Int, page: Int): String? {
        val database = ready() ?: return null
        if (id >= FIRST_ID) {
            try {
                database.rawQuery(
                    "SELECT sura,ayah,verified FROM ocr_tokens WHERE id=?",
                    arrayOf((id - FIRST_ID).toString())
                ).use { c ->
                    if (c.moveToFirst() && c.getInt(2)==1 && !c.isNull(0) && !c.isNull(1)) {
                        val sura=c.getInt(0);val ayah=c.getInt(1)
                        return chapterName(database,sura) + " Suresi · " + ayah + ". ayet"
                    }
                }
            } catch (_: android.database.sqlite.SQLiteException) { }
        }
        return try {
            database.rawQuery(
                "SELECT start_sura,start_ayah,end_sura,end_ayah,status " +
                    "FROM page_verse_bounds WHERE page=?",
                arrayOf(page.toString())
            ).use { c ->
                if (!c.moveToFirst()) return@use null
                val firstS=c.getInt(0);val firstA=c.getInt(1)
                val lastS=c.getInt(2);val lastA=c.getInt(3)
                val confirmed=c.getString(4)=="VISUALLY_CONFIRMED_PAGE"
                val chapter=chapterName(database,firstS)
                val chapterText=if (firstS==lastS) chapter + " Suresi"
                    else chapter + " – " + chapterName(database,lastS) + " sureleri"
                val verseText=if (firstS==lastS) "$firstA–$lastA. ayetler"
                    else "$firstS:$firstA – $lastS:$lastA"
                val quality=if (confirmed) " · sayfa aralığı"
                    else " · yaklaşık sayfa aralığı"
                chapterText + " · " + verseText + quality
            }
        } catch (_: android.database.sqlite.SQLiteException) { null }
    }

    private fun chapterName(database: SQLiteDatabase, sura: Int): String {
        return try {
            database.rawQuery(
                "SELECT name_latin FROM reference_verses WHERE sura=? LIMIT 1",
                arrayOf(sura.toString())
            ).use { c ->
                if (c.moveToFirst()) {
                    when(sura) {
                        1 -> "Fâtiha"
                        2 -> "Bakara"
                        3 -> "Âl-i İmrân"
                        4 -> "Nisâ"
                        5 -> "Mâide"
                        18 -> "Kehf"
                        36 -> "Yâsîn"
                        55 -> "Rahmân"
                        67 -> "Mülk"
                        93 -> "Duhâ"
                        94 -> "İnşirâh"
                        95 -> "Tîn"
                        96 -> "Alak"
                        97 -> "Kadir"
                        98 -> "Beyyine"
                        99 -> "Zilzâl"
                        100 -> "Âdiyât"
                        101 -> "Kâria"
                        102 -> "Tekâsür"
                        103 -> "Asr"
                        104 -> "Hümeze"
                        105 -> "Fîl"
                        106 -> "Kureyş"
                        107 -> "Mâûn"
                        108 -> "Kevser"
                        109 -> "Kâfirûn"
                        110 -> "Nasr"
                        111 -> "Tebbet"
                        112 -> "İhlâs"
                        113 -> "Felak"
                        114 -> "Nâs"
                        else -> c.getString(0).replace("Al-","El-")
                    }
                } else sura.toString()
            }
        } catch (_: Exception) { sura.toString() }
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
