package com.rasaser.free.navigasyon

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Optional 1.15GB road graph. The underlying release is open-source OSM data.
 * Downloads are user-initiated and Wi-Fi-only, and every byte is checked before
 * installation. Navigating without a destination still works without this pack.
 */
class TurkeyRoutingInstaller(private val context:Context) {
    companion object {
        private const val RELEASE_BASE =
            "https://github.com/okiminsozu-droid/RASASER-FREE/releases/download/rasaser-turkiye-routing-v0.1/"
        private const val DATA_NAME = "rasaser-turkiye-routing-v01.tar"
        private const val R2_URL = "https://pub-e5b8f3a2a9e84a9dbfc1c01ca1442264.r2.dev/v1/valhalla_tiles.tar"
        private const val R2_META = "https://pub-e5b8f3a2a9e84a9dbfc1c01ca1442264.r2.dev/v1/"
        const val GRAPH_BYTES = 1148661760L
        const val GRAPH_SHA = "bdfd9a67d8fdd0d5856013e889c6e48cd8a14560934cc5d2b6f3c16a8f6a554f"
    }
    private val manager:DownloadManager
        get()=context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private fun downloadFile():File = File(
        requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)),DATA_NAME)

    fun beginWifiDownload(fromR2:Boolean=true):Long {
        if(StatFs(context.filesDir.absolutePath).availableBytes<3_000_000_000L)
            throw IllegalStateException("At least 3GB of free space is needed")
        val target=downloadFile()
        if(target.exists()) target.delete()
        val request=DownloadManager.Request(Uri.parse(if(fromR2) R2_URL else RELEASE_BASE+"valhalla_tiles.tar"))
            .setTitle("RaSaSeR Türkiye çevrimdışı rota verisi")
            .setDescription("Rota grafiği · yalnız Wi-Fi")
            .setAllowedOverMetered(false)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context,Environment.DIRECTORY_DOWNLOADS,DATA_NAME)
        return manager.enqueue(request)
    }

    fun transferStatus(id:Long):Int {
        manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            return if(cursor!=null && cursor.moveToFirst())
                cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            else DownloadManager.STATUS_FAILED
        }
    }

    /** Must run off main thread after DownloadManager reports SUCCESSFUL. */
    fun installDownloaded(source:File=downloadFile(),
                          metadataOverride:JSONObject?=null,
                          configOverride:JSONObject?=null):Boolean {
        val metadata=metadataOverride ?: fetchJson(R2_META+"valhalla-turkiye-manifest.json") ?:return false
        val config=configOverride ?: fetchJson(R2_META+"valhalla.json") ?:return false
        val expectedSha=metadata.optString("graph_sha256","").lowercase()
        val expectedBytes=metadata.optLong("file_size",0)
        if(expectedSha!=GRAPH_SHA)return false
        if(expectedBytes != GRAPH_BYTES)return false
        if(!source.isFile || source.length()!=expectedBytes)return false
        val digest=MessageDigest.getInstance("SHA-256")
        source.inputStream().buffered().use {stream ->
            val block=ByteArray(256*1024)
            while(true) {
                val n=stream.read(block);if(n<0)break
                digest.update(block,0,n)
            }
        }
        if(digest.digest().joinToString("") { "%02x".format(it) }!=expectedSha)return false

        val mjolnir=config.optJSONObject("mjolnir") ?:return false
        // Only the tiles from this validated release may be used; no tile URL.
        mjolnir.remove("tile_url")
        val folder=File(context.filesDir,"routing").apply { mkdirs() }
        val pending=File(folder,"valhalla.next.tar")
        val pendingManifest=File(folder,"valhalla.next.manifest.json")
        val pendingConfig=File(folder,"valhalla.next.json")
        try {
            // Keep previous working graph; never auto-delete it.
            if(File(folder,"valhalla_tiles.tar").exists()) return false
            source.inputStream().buffered().use {src ->
                pending.outputStream().buffered().use {src.copyTo(it,512*1024)}
            }
            pendingManifest.writeText(metadata.toString(2),Charsets.UTF_8)
            pendingConfig.writeText(config.toString(2),Charsets.UTF_8)
            if(!pending.renameTo(File(folder,"valhalla_tiles.tar")))return false
            if(!pendingManifest.renameTo(File(folder,"valhalla.manifest.json")))return false
            if(!pendingConfig.renameTo(File(folder,"valhalla.json")))return false
            return true
        } catch (_:Exception) {return false}
        finally { pending.delete();pendingManifest.delete();pendingConfig.delete() }
    }

    private fun fetchJson(url:String):JSONObject? {
        return try {
            val conn=(URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout=12000
                readTimeout=20000
                instanceFollowRedirects=true
                setRequestProperty("Accept","application/json")
            }
            try {
                if(conn.responseCode!=200 || conn.url.protocol!="https") null
                else {
                    val bytes=java.io.ByteArrayOutputStream()
                    conn.inputStream.use { input ->
                        val buffer=ByteArray(8192)
                        while(bytes.size()<=100_000) {
                            val n=input.read(buffer)
                            if(n<0)break
                            bytes.write(buffer,0,n)
                        }
                    }
                    if(bytes.size()>100_000) null
                    else JSONObject(bytes.toString("UTF-8"))
                }
            } finally {
                conn.disconnect()
            }
        } catch (_:Exception) {null}
    }
}
