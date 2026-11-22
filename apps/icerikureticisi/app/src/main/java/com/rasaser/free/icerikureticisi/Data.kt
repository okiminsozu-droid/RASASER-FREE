package com.rasaser.free.icerikureticisi

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.net.URI

data class Creator(val id: Long, val name: String)
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
    SQLiteOpenHelper(context.applicationContext, "creator_hub.db", null, 2) {
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
                created_at INTEGER NOT NULL
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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE accounts ADD COLUMN logo_file TEXT NOT NULL DEFAULT ''")
        }
    }

    fun creators(): List<Creator> = buildList {
        readableDatabase.rawQuery("SELECT id, name FROM creators ORDER BY id", null).use { c ->
            while (c.moveToNext()) add(Creator(c.getLong(0), c.getString(1)))
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

    fun addCreator(name: String): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= 80) {
            "İçerik adı 1–80 karakter olmalı."
        }
        return writableDatabase.insertOrThrow("creators", null, ContentValues().apply {
            put("name", trimmed)
            put("created_at", System.currentTimeMillis())
        })
    }

    fun deleteCreator(id: Long) {
        val logoFiles = accounts(id).map { it.logoFile }
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
        require(validPackage(packageName.trim())) { "Android paket adı geçersiz." }
        val verifiedUrl = secureUrl(url, SocialCatalog.find(platform)?.homeUrl ?: "")
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
}
