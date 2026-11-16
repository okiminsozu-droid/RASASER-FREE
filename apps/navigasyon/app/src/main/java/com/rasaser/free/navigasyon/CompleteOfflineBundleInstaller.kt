package com.rasaser.free.navigasyon

import android.content.Context
import android.os.StatFs
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Self-contained device-test pack: all data comes from SHA256-pinned APK assets.
 * No server, account, paid API or extra user download is needed after installation.
 * Each large file is streamed and verified before any live file is replaced.
 */
class CompleteOfflineBundleInstaller(private val context: Context) {
    data class Result(
        val mapReady: Boolean,
        val searchReady: Boolean,
        val routingReady: Boolean,
        val error: String? = null
    ) {
        val complete: Boolean get() = mapReady && searchReady && routingReady
    }

    private val mapFile get() = File(context.filesDir, "maps/turkey.mbtiles")
    private val mapManifest get() = File(context.filesDir, "maps/turkey.manifest.json")
    private val routingDir get() = File(context.filesDir, "routing")
    private val graph get() = File(routingDir, "valhalla_tiles.tar")
    private val routeManifest get() = File(routingDir, "valhalla.manifest.json")
    private val routeConfig get() = File(routingDir, "valhalla.json")

    private val requiredAssets = listOf(
        "maps/turkey.mbtiles", "search/turkey-places.sqlite",
        "routing/valhalla_tiles.tar", "routing/valhalla.json",
        "routing/valhalla-turkiye-manifest.json"
    )

    fun available(): Boolean = requiredAssets.all { name ->
        runCatching { context.assets.open(name).use { true } }.getOrDefault(false)
    }

    private fun routeReady(): Boolean {
        return try {
            if (!graph.isFile || graph.length() != TurkeyRoutingInstaller.GRAPH_BYTES ||
                !routeManifest.isFile || !routeConfig.isFile) false
            else {
                val record = JSONObject(routeManifest.readText(Charsets.UTF_8))
                record.optLong("file_size", -1) == TurkeyRoutingInstaller.GRAPH_BYTES &&
                    record.optString("graph_sha256", "").lowercase() == TurkeyRoutingInstaller.GRAPH_SHA &&
                    JSONObject(routeConfig.readText(Charsets.UTF_8)).has("mjolnir")
            }
        } catch (_: Exception) { false }
    }

    fun install(reportProgress: (String) -> Unit): Result {
        val search = TurkeySearchInstaller(context)
        if (!available()) return Result(false, false, false, "Paketin bir veya daha fazla dosyası eksik.")
        var goodMap = false
        var goodSearch = false
        var goodRoute = false
        try {
            goodMap = LocalMapVerifier.turkeyMapReady(context)
            goodSearch = search.installed()
            goodRoute = routeReady()
            val required = (if (goodMap) 0L else TurkeyMapInstaller.MAP_BYTES) +
                (if (goodSearch) 0L else TurkeySearchInstaller.DB_BYTES) +
                (if (goodRoute) 0L else TurkeyRoutingInstaller.GRAPH_BYTES) +
                250_000_000L
            val free = StatFs(context.filesDir.absolutePath).availableBytes
            if (free < required) return Result(goodMap, goodSearch, goodRoute,
                "Yetersiz alan. Verilerin kurulumu için yaklaşık " +
                    ((required - free + 999_999_999L) / 1_000_000_000L) + " GB daha boş alan gerekiyor.")
            if (!goodMap) {
                reportProgress("Türkiye haritası hazırlanıyor")
                goodMap = installMap(reportProgress)
                if (!goodMap) return Result(false, goodSearch, goodRoute, "Türkiye haritası doğrulanamadı.")
            }
            if (!goodSearch) {
                reportProgress("Türkiye yer arama verisi hazırlanıyor")
                goodSearch = search.installBundled() && search.installed()
                if (!goodSearch) return Result(goodMap, false, goodRoute,
                    "Yer arama indeksi doğrulanamadı: " + search.lastFailure().name)
            }
            if (!goodRoute) {
                reportProgress("Türkiye rota verisi hazırlanıyor")
                goodRoute = installGraph(reportProgress)
                if (!goodRoute) return Result(goodMap, goodSearch, false,
                    "Türkiye rota grafiği doğrulanamadı.")
            }
            reportProgress("Türkiye harita, arama ve rota verileri hazır")
            return Result(goodMap, goodSearch, goodRoute)
        } catch (e: Exception) {
            return Result(goodMap, goodSearch, goodRoute,
                "Kurulum tamamlanamadı: " + (e.message?.take(140) ?: e.javaClass.simpleName))
        }
    }

    private fun verifiedCopy(
        asset: String, output: File, bytes: Long, expectedSha: String,
        label: String, report: (String) -> Unit
    ): Boolean {
        output.parentFile?.mkdirs()
        output.delete()
        val digest = MessageDigest.getInstance("SHA-256")
        var copied = 0L
        var lastBucket = -1
        context.assets.open(asset).buffered(512 * 1024).use { input ->
            output.outputStream().buffered(512 * 1024).use { out ->
                val buffer = ByteArray(512 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    copied += count
                    if (copied > bytes) return false
                    digest.update(buffer, 0, count)
                    out.write(buffer, 0, count)
                    val percent = (100L * copied / bytes).toInt()
                    val bucket = percent / 5
                    if (bucket != lastBucket) {
                        lastBucket = bucket
                        report(label + " %" + percent)
                    }
                }
            }
        }
        return copied == bytes &&
            digest.digest().joinToString("") { "%02x".format(it) } == expectedSha
    }

    private fun installMap(report: (String) -> Unit): Boolean {
        val folder = mapFile.parentFile ?: return false
        folder.mkdirs()
        val staged = File(folder, "turkey.bundled.next.mbtiles")
        val stagedMeta = File(folder, "turkey.bundled.next.manifest.json")
        val backup = File(folder, "turkey.previous.mbtiles")
        val backupMeta = File(folder, "turkey.previous.manifest.json")
        try {
            if (!verifiedCopy("maps/turkey.mbtiles", staged,
                    TurkeyMapInstaller.MAP_BYTES, TurkeyMapInstaller.MAP_SHA,
                    "Harita", report)) return false
            val record = JSONObject()
                .put("project", "RaSaSeR Navigasyon Türkiye")
                .put("map_sha256", TurkeyMapInstaller.MAP_SHA)
                .put("source_archive_sha256", TurkeyMapInstaller.ZIP_SHA)
                .put("data_license", "ODbL 1.0 © OpenStreetMap contributors")
                .put("map", JSONObject()
                    .put("size_bytes", TurkeyMapInstaller.MAP_BYTES).put("format", "pbf"))
            stagedMeta.writeText(record.toString(2), Charsets.UTF_8)
            if (mapFile.exists()) {
                if (backup.exists() || !mapFile.renameTo(backup)) return false
                if (mapManifest.exists() && !mapManifest.renameTo(backupMeta)) {
                    backup.renameTo(mapFile)
                    return false
                }
            }
            if (!staged.renameTo(mapFile) || !stagedMeta.renameTo(mapManifest)) {
                mapFile.delete()
                backup.renameTo(mapFile)
                backupMeta.renameTo(mapManifest)
                return false
            }
            // All installed bytes already passed a complete SHA256 validation.
            return true
        } finally {
            staged.delete()
            stagedMeta.delete()
        }
    }

    private fun installGraph(report: (String) -> Unit): Boolean {
        routingDir.mkdirs()
        val manifestText = context.assets.open("routing/valhalla-turkiye-manifest.json")
            .bufferedReader().use { it.readText() }
        val configText = context.assets.open("routing/valhalla.json")
            .bufferedReader().use { it.readText() }
        val manifest = JSONObject(manifestText)
        val config = JSONObject(configText)
        if (manifest.optString("graph_sha256", "").lowercase() != TurkeyRoutingInstaller.GRAPH_SHA ||
            manifest.optLong("file_size", -1) != TurkeyRoutingInstaller.GRAPH_BYTES ||
            config.optJSONObject("mjolnir") == null) return false
        config.getJSONObject("mjolnir").remove("tile_url")

        val staged = File(routingDir, "valhalla.bundled.next.tar")
        val metaStage = File(routingDir, "valhalla.bundled.next.manifest.json")
        val configStage = File(routingDir, "valhalla.bundled.next.json")
        try {
            // Repair metadata without duplicating a previously verified, full graph.
            if (graph.exists()) {
                if (graph.length() != TurkeyRoutingInstaller.GRAPH_BYTES ||
                    !sha256(graph).equals(TurkeyRoutingInstaller.GRAPH_SHA, ignoreCase = true))
                    return false
            } else {
                if (!verifiedCopy("routing/valhalla_tiles.tar", staged,
                        TurkeyRoutingInstaller.GRAPH_BYTES, TurkeyRoutingInstaller.GRAPH_SHA,
                        "Rota", report)) return false
            }
            metaStage.writeText(manifest.toString(2), Charsets.UTF_8)
            configStage.writeText(config.toString(2), Charsets.UTF_8)
            if (routeManifest.exists() && !routeManifest.delete()) return false
            if (routeConfig.exists() && !routeConfig.delete()) return false
            if (!metaStage.renameTo(routeManifest) || !configStage.renameTo(routeConfig)) return false
            if (!graph.exists() && !staged.renameTo(graph)) return false
            return routeReady()
        } finally {
            staged.delete()
            metaStage.delete()
            configStage.delete()
        }
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val b = ByteArray(512 * 1024)
            while (true) {
                val n = input.read(b)
                if (n < 0) break
                md.update(b, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
