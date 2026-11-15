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
        const val GRAPH_BYTES = 1148661760L
        const val GRAPH_SHA = "764ecd092a5ef594b4364396f4fef3b560842a03b004338b29910aa3e20f9a9c"
    }
    private val manager:DownloadManager
        get()=context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private fun downloadFile():File = File(
        requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)),DATA_NAME)

    fun beginWifiDownload():Long {
        if(StatFs(context.filesDir.absolutePath).availableBytes<3_000_000_000L)
            throw IllegalStateException("At least 3GB of free space is needed")
        val target=downloadFile()
        if(target.exists()) target.delete()
        val request=DownloadManager.Request(Uri.parse(RELEASE_BASE+"valhalla_tiles.tar"))
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
    fun installDownloaded():Boolean {
        val source=downloadFile()
        val metadata=fetchJson(RELEASE_BASE+"valhalla-turkiye-manifest.json") ?:return false
        val config=fetchJson(RELEASE_BASE+"valhalla.json") ?:return false
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
