package com.rasaser.free.kurankelime

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject

data class WordToken(
    val id: Long,
    val sura: Int,
    val ayah: Int,
    val position: Int,
    val arabic: String,
    val kind: String,
    val tr: String,
    val en: String,
    val de: String,
    val es: String,
    val zh: String
) {
    fun meaning(language: String): String = when (language) {
        "en" -> en
        "de" -> de
        "es" -> es
        "zh" -> zh
        else -> tr
    }
}

data class Verse(val sura: Int, val ayah: Int, val arabic: String)
data class SearchHit(val wordId: Long, val ayah: Int, val arabic: String, val meaning: String)

class QuranStore(private val appContext: Context) :
    SQLiteOpenHelper(appContext, "kurankelime_v1.db", null, 1) {

    private val prefs = appContext.getSharedPreferences("reader_preferences", Context.MODE_PRIVATE)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE verse (sura INTEGER NOT NULL, ayah INTEGER NOT NULL, arabic TEXT NOT NULL, PRIMARY KEY(sura,ayah))")
        db.execSQL("CREATE TABLE word (id INTEGER PRIMARY KEY AUTOINCREMENT, sura INTEGER NOT NULL, ayah INTEGER NOT NULL, position INTEGER NOT NULL, arabic TEXT NOT NULL, kind TEXT NOT NULL, tr TEXT NOT NULL, en TEXT NOT NULL, de TEXT NOT NULL, es TEXT NOT NULL, zh TEXT NOT NULL, UNIQUE(sura,ayah,position))")
        db.execSQL("CREATE VIRTUAL TABLE word_fts USING fts4(ar,tr,en,de,es,zh,tokenize=unicode61)")
        db.execSQL("CREATE TABLE verse_favorite (sura INTEGER NOT NULL, ayah INTEGER NOT NULL, PRIMARY KEY(sura,ayah))")
        db.execSQL("CREATE TABLE word_favorite (word_id INTEGER PRIMARY KEY)")
        importPreviewSeed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Do not delete reading progress or favorites on upgrades.
        throw IllegalStateException("Database schema migration must be explicitly implemented.")
    }

    private fun importPreviewSeed(db: SQLiteDatabase) {
        val json = appContext.assets.open("seed/fatiha_preview.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val verses = JSONObject(json).getJSONArray("verses")
        db.beginTransaction()
        try {
            for (i in 0 until verses.length()) {
                val verse = verses.getJSONObject(i)
                val ayah = verse.getInt("ayah")
                db.insertOrThrow("verse", null, ContentValues().apply {
                    put("sura", 1)
                    put("ayah", ayah)
                    put("arabic", verse.getString("arabic"))
                })
                val words = verse.getJSONArray("words")
                for (j in 0 until words.length()) {
                    val w = words.getJSONObject(j)
                    val wordId = db.insertOrThrow("word", null, ContentValues().apply {
                        put("sura", 1)
                        put("ayah", ayah)
                        put("position", w.getInt("position"))
                        put("arabic", w.getString("arabic"))
                        put("kind", w.getString("kind"))
                        for (language in listOf("tr", "en", "de", "es", "zh")) {
                            put(language, w.getString(language))
                        }
                    })
                    db.insertOrThrow("word_fts", null, ContentValues().apply {
                        put("rowid", wordId)
                        put("ar", w.getString("arabic"))
                        for (language in listOf("tr", "en", "de", "es", "zh")) {
                            put(language, w.getString(language))
                        }
                    })
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun verses(): List<Verse> {
        val output = mutableListOf<Verse>()
        readableDatabase.rawQuery(
            "SELECT sura,ayah,arabic FROM verse ORDER BY sura,ayah", null
        ).use { c ->
            while (c.moveToNext()) output += Verse(c.getInt(0), c.getInt(1), c.getString(2))
        }
        return output
    }

    fun words(sura: Int, ayah: Int): List<WordToken> {
        val output = mutableListOf<WordToken>()
        readableDatabase.rawQuery(
            "SELECT id,sura,ayah,position,arabic,kind,tr,en,de,es,zh FROM word WHERE sura=? AND ayah=? ORDER BY position",
            arrayOf(sura.toString(), ayah.toString())
        ).use { c ->
            while (c.moveToNext()) {
                output += WordToken(
                    c.getLong(0), c.getInt(1), c.getInt(2), c.getInt(3),
                    c.getString(4), c.getString(5), c.getString(6), c.getString(7),
                    c.getString(8), c.getString(9), c.getString(10)
                )
            }
        }
        return output
    }

    fun search(query: String, language: String): List<SearchHit> {
        val safeTerms = query.trim()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }.take(6)
        if (safeTerms.isEmpty()) return emptyList()
        val match = safeTerms.joinToString(" ") { it + "*" }
        val column = if (language in listOf("tr", "en", "de", "es", "zh")) language else "tr"
        val output = mutableListOf<SearchHit>()
        val sql = "SELECT w.id,w.ayah,w.arabic,w." + column +
            " FROM word_fts f JOIN word w ON w.id=f.docid WHERE word_fts MATCH ? ORDER BY w.sura,w.ayah,w.position LIMIT 120"
        try {
            readableDatabase.rawQuery(sql, arrayOf(match)).use { c ->
                while (c.moveToNext()) {
                    output += SearchHit(c.getLong(0), c.getInt(1), c.getString(2), c.getString(3))
                }
            }
        } catch (_: android.database.sqlite.SQLiteException) {
            // FTS tokenizer limits must not crash the reading screen.
        }
        return output
    }

    fun language(): String = prefs.getString("language", "tr") ?: "tr"
    fun saveLanguage(code: String) {
        require(code in listOf("tr", "en", "de", "es", "zh"))
        prefs.edit().putString("language", code).apply()
    }

    fun isDark(): Boolean = prefs.getBoolean("dark", true)
    fun saveDark(value: Boolean) { prefs.edit().putBoolean("dark", value).apply() }
    fun lastAyah(): Int = prefs.getInt("last_ayah", 1).coerceIn(1, 7)
    fun lastPosition(): Int = prefs.getInt("last_position", 1)
    fun saveProgress(ayah: Int, position: Int = 1) {
        prefs.edit().putInt("last_ayah", ayah).putInt("last_position", position).apply()
    }

    fun isFavoriteVerse(ayah: Int): Boolean {
        readableDatabase.rawQuery(
            "SELECT 1 FROM verse_favorite WHERE sura=1 AND ayah=?", arrayOf(ayah.toString())
        ).use { return it.moveToFirst() }
    }
    fun toggleVerseFavorite(ayah: Int) {
        val db = writableDatabase
        if (isFavoriteVerse(ayah)) {
            db.delete("verse_favorite", "sura=1 AND ayah=?", arrayOf(ayah.toString()))
        } else {
            db.insertWithOnConflict("verse_favorite", null, ContentValues().apply {
                put("sura", 1); put("ayah", ayah)
            }, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }
    fun favoriteVerses(): List<Int> {
        val result = mutableListOf<Int>()
        readableDatabase.rawQuery(
            "SELECT ayah FROM verse_favorite WHERE sura=1 ORDER BY ayah", null
        ).use { c -> while (c.moveToNext()) result += c.getInt(0) }
        return result
    }

    fun isFavoriteWord(wordId: Long): Boolean {
        readableDatabase.rawQuery(
            "SELECT 1 FROM word_favorite WHERE word_id=?", arrayOf(wordId.toString())
        ).use { return it.moveToFirst() }
    }
    fun toggleWordFavorite(wordId: Long) {
        if (isFavoriteWord(wordId)) {
            writableDatabase.delete("word_favorite", "word_id=?", arrayOf(wordId.toString()))
        } else {
            writableDatabase.insertWithOnConflict("word_favorite", null, ContentValues().apply {
                put("word_id", wordId)
            }, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }
    fun favoriteWords(): List<WordToken> {
        val output = mutableListOf<WordToken>()
        readableDatabase.rawQuery(
            "SELECT w.sura,w.ayah FROM word_favorite wf JOIN word w ON wf.word_id=w.id ORDER BY w.sura,w.ayah,w.position", null
        ).use { c ->
            val seen = mutableSetOf<Pair<Int, Int>>()
            while (c.moveToNext()) seen += c.getInt(0) to c.getInt(1)
            for ((sura, ayah) in seen) {
                val ids = favoriteWordIdsForVerse(sura, ayah)
                output += words(sura, ayah).filter { it.id in ids }
            }
        }
        return output
    }
    private fun favoriteWordIdsForVerse(sura: Int, ayah: Int): Set<Long> {
        val ids = mutableSetOf<Long>()
        readableDatabase.rawQuery(
            "SELECT wf.word_id FROM word_favorite wf JOIN word w ON wf.word_id=w.id WHERE w.sura=? AND w.ayah=?",
            arrayOf(sura.toString(), ayah.toString())
        ).use { c -> while (c.moveToNext()) ids += c.getLong(0) }
        return ids
    }
}
