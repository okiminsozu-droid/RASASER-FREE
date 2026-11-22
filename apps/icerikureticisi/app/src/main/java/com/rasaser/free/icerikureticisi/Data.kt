package com.rasaser.free.icerikureticisi

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.net.URI
import org.json.JSONObject

data class Creator(
    val id: Long,
    val name: String,
    val logoFile: String = "",
    val translatedNamesJson: String = "{}"
) {
    fun displayName(language: String): String {
        if (language == "tr") return name
        return try {
            JSONObject(translatedNamesJson).optString(language).ifBlank { name }
        } catch (_: Exception) { name }
    }
}
data class SocialAccount(
    val id: Long,
    val creatorId: Long,
    val platform: String,
    val label: String,
    val profileUrl: String,
    val packageName: String,
    val logoFile: String
)
data class Platform(val name: String, val packageName: String, val homeUrl: String)

data class ScheduledShare(
    val id: Long,
    val creatorId: Long,
    val mediaUri: String,
    val mediaMime: String,
    val caption: String,
    val targetIds: List<Long>,
    val scheduledAt: Long,
    val ready: Boolean
)


object SocialCatalog {
    val presets = listOf(
        Platform("Instagram", "com.instagram.android", "https://www.instagram.com/"),
        Platform("Facebook", "com.facebook.katana", "https://www.facebook.com/"),
        Platform("YouTube", "com.google.android.youtube", "https://www.youtube.com/"),
        Platform("TikTok", "com.zhiliaoapp.musically", "https://www.tiktok.com/")
    )

    fun find(name: String): Platform? = presets.firstOrNull { it.name == name }

    /**
     * No password or authentication token ever enters this database.
     * Only secure HTTPS profile links are accepted.
     */
    fun secureUrl(raw: String, fallback: String = ""): String {
        val input = (raw.trim().ifBlank { fallback }).trim()
        if (input.isEmpty()) return ""
        val candidate = if (input.contains("://")) input else "https://$input"
        val uri = try { URI(candidate) } catch (_: Exception) { null }
        require(uri != null && uri.scheme.equals("https", true) &&
            !uri.host.isNullOrBlank() && uri.userInfo == null) {
            "Geçerli bir HTTPS profil bağlantısı girin."
        }
        return candidate
    }

    fun validPackage(packageName: String): Boolean =
        packageName.isBlank() ||
        Regex("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+").matches(packageName)
}

/** Device-only SQLite. No internet, login, password or cloud sync. */
class CreatorDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "creator_hub.db", null, 4) {
    private val appContext = context.applicationContext

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE creators (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                logo_file TEXT NOT NULL DEFAULT '',
                translated_names TEXT NOT NULL DEFAULT '{}'
            )"""
        )
        db.execSQL(
            """CREATE TABLE accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                creator_id INTEGER NOT NULL REFERENCES creators(id) ON DELETE CASCADE,
                platform TEXT NOT NULL,
                display_name TEXT NOT NULL,
                profile_url TEXT NOT NULL,
                package_name TEXT NOT NULL,
                logo_file TEXT NOT NULL DEFAULT ''
            )"""
        )
        db.execSQL("CREATE INDEX idx_accounts_creator ON accounts(creator_id)")
        createScheduledTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE accounts ADD COLUMN logo_file TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 3) createScheduledTable(db)
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE creators ADD COLUMN logo_file TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE creators ADD COLUMN translated_names TEXT NOT NULL DEFAULT '{}'")
        }
    }

    private fun createScheduledTable(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS scheduled_shares (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                creator_id INTEGER NOT NULL REFERENCES creators(id) ON DELETE CASCADE,
                media_uri TEXT NOT NULL,
                media_mime TEXT NOT NULL,
                caption TEXT NOT NULL,
                target_ids TEXT NOT NULL,
                scheduled_at INTEGER NOT NULL,
                ready INTEGER NOT NULL DEFAULT 0
            )"""
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_share_creator ON scheduled_shares(creator_id)")
    }

    fun creators(): List<Creator> = buildList {
        readableDatabase.rawQuery(
            "SELECT id, name, logo_file, translated_names FROM creators ORDER BY id", null
        ).use { c ->
            while (c.moveToNext()) add(
                Creator(c.getLong(0), c.getString(1), c.getString(2), c.getString(3))
            )
        }
    }

    fun accounts(creatorId: Long): List<SocialAccount> = buildList {
        readableDatabase.rawQuery(
            "SELECT id, creator_id, platform, display_name, profile_url, package_name, logo_file " +
                "FROM accounts WHERE creator_id=? ORDER BY id DESC", arrayOf(creatorId.toString())
        ).use { c ->
            while (c.moveToNext()) add(
                SocialAccount(c.getLong(0), c.getLong(1), c.getString(2),
                    c.getString(3), c.getString(4), c.getString(5), c.getString(6))
            )
        }
    }

    fun trackedPackages(): Set<String> = buildSet {
        readableDatabase.rawQuery(
            "SELECT DISTINCT package_name FROM accounts WHERE package_name <> ''", null
        ).use { c ->
            while (c.moveToNext()) add(c.getString(0))
        }
    }

    fun addCreator(name: String, logoFile: String = ""): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= 80) {
            "İçerik adı 1–80 karakter olmalı."
        }
        return writableDatabase.insertOrThrow("creators", null, ContentValues().apply {
            put("name", trimmed)
            put("created_at", System.currentTimeMillis())
            put("logo_file", logoFile)
            put("translated_names", "{}")
        })
    }

    fun setCreatorLogo(creatorId: Long, logoFile: String) {
        val oldFile = readableDatabase.rawQuery(
            "SELECT logo_file FROM creators WHERE id=?", arrayOf(creatorId.toString())
        ).use { c -> if (c.moveToFirst()) c.getString(0) else "" }
        val rows = writableDatabase.update("creators", ContentValues().apply {
            put("logo_file", logoFile)
        }, "id=?", arrayOf(creatorId.toString()))
        if (rows != 1) throw IllegalStateException("Profil bulunamadı.")
        if (oldFile != logoFile) LocalLogos.delete(appContext, oldFile)
    }

    fun setCreatorTranslations(creatorId: Long, translations: Map<String, String>) {
        val json = JSONObject()
        for ((lang, value) in translations) {
            if (lang in AppLanguage.codes && lang != "tr" && value.isNotBlank()) {
                json.put(lang, value.trim().take(80))
            }
        }
        writableDatabase.update("creators", ContentValues().apply {
            put("translated_names", json.toString())
        }, "id=?", arrayOf(creatorId.toString()))
    }

    fun deleteCreator(id: Long) {
        val logoFiles = accounts(id).map { it.logoFile } +
            creators().firstOrNull { it.id == id }?.logoFile.orEmpty()
        writableDatabase.delete("creators", "id=?", arrayOf(id.toString()))
        logoFiles.forEach { LocalLogos.delete(appContext, it) }
    }

    fun addAccount(
        creatorId: Long,
        platform: String,
        label: String,
        url: String,
        packageName: String,
        logoFile: String = ""
    ): Long {
        require(platform.trim().isNotBlank() && platform.length <= 50)
        require(label.trim().isNotBlank() && label.length <= 80)
        require(SocialCatalog.validPackage(packageName.trim())) { "Android paket adı geçersiz." }
        val verifiedUrl = SocialCatalog.secureUrl(url, SocialCatalog.find(platform)?.homeUrl ?: "")
        require(verifiedUrl.isNotBlank() || packageName.isNotBlank()) {
            "Hesap bağlantısı veya uygulama paket adı ekleyin."
        }
        return writableDatabase.insertOrThrow("accounts", null, ContentValues().apply {
            put("creator_id", creatorId)
            put("platform", platform.trim())
            put("display_name", label.trim())
            put("profile_url", verifiedUrl)
            put("package_name", packageName.trim())
            put("logo_file", logoFile)
        })
    }

    fun deleteAccount(id: Long) {
        val file = readableDatabase.rawQuery(
            "SELECT logo_file FROM accounts WHERE id=?", arrayOf(id.toString())
        ).use { c -> if (c.moveToFirst()) c.getString(0) else "" }
        writableDatabase.delete("accounts", "id=?", arrayOf(id.toString()))
        LocalLogos.delete(appContext, file)
    }

    fun addScheduledShare(
        creatorId: Long, mediaUri: String, mime: String,
        caption: String, targets: List<Long>, whenMillis: Long
    ): Long {
        require(mediaUri.startsWith("content://")) { "Medya dosyası seçin." }
        require(mime.startsWith("image/") || mime.startsWith("video/")) { "Fotoğraf veya video seçin." }
        require(targets.isNotEmpty()) { "En az bir hesap seçin." }
        require(whenMillis > System.currentTimeMillis()) { "Gelecekte bir zaman seçin." }
        return writableDatabase.insertOrThrow("scheduled_shares", null, ContentValues().apply {
            put("creator_id", creatorId)
            put("media_uri", mediaUri)
            put("media_mime", mime)
            put("caption", caption.take(5000))
            put("target_ids", targets.distinct().joinToString(","))
            put("scheduled_at", whenMillis)
            put("ready", 0)
        })
    }

    private fun fromScheduleCursor(c: android.database.Cursor): ScheduledShare =
        ScheduledShare(
            id = c.getLong(0),
            creatorId = c.getLong(1),
            mediaUri = c.getString(2),
            mediaMime = c.getString(3),
            caption = c.getString(4),
            targetIds = c.getString(5).split(",").mapNotNull { it.toLongOrNull() },
            scheduledAt = c.getLong(6),
            ready = c.getInt(7) != 0
        )

    fun scheduledShare(id: Long): ScheduledShare? =
        readableDatabase.rawQuery(
            "SELECT id, creator_id, media_uri, media_mime, caption, target_ids, scheduled_at, ready " +
                "FROM scheduled_shares WHERE id=?", arrayOf(id.toString())
        ).use { c -> if (c.moveToFirst()) fromScheduleCursor(c) else null }

    fun scheduledFor(creatorId: Long): List<ScheduledShare> = buildList {
        readableDatabase.rawQuery(
            "SELECT id, creator_id, media_uri, media_mime, caption, target_ids, scheduled_at, ready " +
                "FROM scheduled_shares WHERE creator_id=? ORDER BY scheduled_at DESC",
            arrayOf(creatorId.toString())
        ).use { c -> while (c.moveToNext()) add(fromScheduleCursor(c)) }
    }

    fun markScheduleReady(id: Long) {
        writableDatabase.update("scheduled_shares", ContentValues().apply { put("ready", 1) },
            "id=?", arrayOf(id.toString()))
    }

    fun deleteScheduledShare(id: Long) {
        writableDatabase.delete("scheduled_shares", "id=?", arrayOf(id.toString()))
    }
}

