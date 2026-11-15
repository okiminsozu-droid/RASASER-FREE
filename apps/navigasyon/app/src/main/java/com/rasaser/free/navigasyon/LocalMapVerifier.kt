package com.rasaser.free.navigasyon

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Reads the locally installed Turkey package and checks SHA256 against the data-factory manifest.
 * Must run on a background thread. A dedicated download/import installer is a future module.
 */
object LocalMapVerifier {
    fun turkeyMapReady(context: Context): Boolean = try {
        val folder = File(context.filesDir, "maps")
        val map = File(folder, "turkey.mbtiles")
        val manifest = File(folder, "turkey.manifest.json")
        if (!map.isFile || !manifest.isFile || map.length() < 1024L) false
        else {
            val record = JSONObject(manifest.readText(Charsets.UTF_8))
            val expected = record.getString("map_sha256").lowercase()
            val byteCount = record.getJSONObject("map").getLong("size_bytes")
            if (!expected.matches(Regex("[0-9a-f]{64}")) || map.length() != byteCount) false
            else {
                val hash = MessageDigest.getInstance("SHA-256")
                map.inputStream().buffered().use { input ->
                    val buffer = ByteArray(262144)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        hash.update(buffer, 0, count)
                    }
                }
                val actual = hash.digest().joinToString("") { b -> "%02x".format(b) }
                expected == actual
            }
        }
    } catch (_: Exception) {
        false
    }
}
