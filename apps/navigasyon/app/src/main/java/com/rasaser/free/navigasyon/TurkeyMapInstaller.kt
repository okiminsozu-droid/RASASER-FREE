package com.rasaser.free.navigasyon

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * A user-initiated, pinned-country download. DownloadManager handles transfer and
 * resumption. Install streams into private files with checksum verification.
 */
class TurkeyMapInstaller(private val ctx: Context) {
    companion object {
        const val RELEASE_URL = "https://github.com/okiminsozu-droid/RASASER-FREE/releases/download/rasaser-turkiye-maps-v0.1/rasaser-turkiye-shortbread.zip"
        const val ZIP_SHA = "c7d8b277a916ffd4ec7839c7c0420104c19b88fcfcea456be7078346003bacfc"
        const val MAP_SHA = "54a9ab9739133e037add2cb1b7e41912374a070f0b386744f7da24c81e7a4e30"
        const val ZIP_BYTES = 605992151L
        const val MAP_BYTES = 669908992L
        private const val DOWNLOAD_NAME = "rasaser-turkiye-v01.zip"
    }

    private val manager: DownloadManager
        get() = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    data class Transfer(val status:Int, val downloaded:Long, val total:Long, val failureReason:Int)
    private fun location(): File =
        File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),DOWNLOAD_NAME)

    fun download(wifiOnly: Boolean = true): Long {
        if (StatFs(ctx.filesDir.absolutePath).availableBytes < 1_700_000_000L)
            throw IllegalStateException("Insufficient free disk capacity")
        val target=location()
        if(target.exists()) target.delete()
        val request=DownloadManager.Request(Uri.parse(RELEASE_URL))
            .setTitle("RaSaSeR Navigasyon • Türkiye")
            .setDescription("Ücretsiz çevrimdışı Türkiye haritası")
            .setAllowedOverMetered(!wifiOnly)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(ctx,Environment.DIRECTORY_DOWNLOADS,DOWNLOAD_NAME)
        return manager.enqueue(request)
    }

    fun transfer(id:Long):Transfer {
        manager.query(DownloadManager.Query().setFilterById(id)).use { c ->
            if(c==null || !c.moveToFirst()) return Transfer(DownloadManager.STATUS_FAILED,0,0,-1)
            fun long(name:String)=c.getLong(c.getColumnIndexOrThrow(name))
            fun int(name:String)=c.getInt(c.getColumnIndexOrThrow(name))
            return Transfer(
                int(DownloadManager.COLUMN_STATUS),
                long(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
                long(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
                int(DownloadManager.COLUMN_REASON)
            )
        }
    }

    fun installDownloaded(): Boolean = installZip(location())

    /** File-picker imports use the same digest verification as downloads. */
    fun importFrom(stream:InputStream): Boolean {
        val staging=File(ctx.cacheDir,"rasaser-import-${System.nanoTime()}.zip")
        try {
            staging.outputStream().buffered().use { out -> stream.use { it.copyTo(out,256*1024) } }
            return installZip(staging)
        } finally { staging.delete() }
    }

    fun installZip(archive:File): Boolean {
        if(!archive.isFile || archive.length()!=ZIP_BYTES) return false
        if(sha256(archive)!=ZIP_SHA) return false
        val maps=File(ctx.filesDir,"maps").apply { mkdirs() }
        val stage=File(maps,"turkey.next.mbtiles")
        val manifestStage=File(maps,"turkey.next.manifest.json")
        try {
            stage.delete();manifestStage.delete()
            var found=0
            val digest=MessageDigest.getInstance("SHA-256")
            var count=0L
            ZipInputStream(archive.inputStream().buffered()).use { zip ->
                while(true) {
                    val entry=zip.nextEntry ?: break
                    if(!entry.isDirectory && entry.name.endsWith(".mbtiles",ignoreCase=true)) {
                        found++
                        if(found>1) return false
                        stage.outputStream().buffered().use { out ->
                            val buffer=ByteArray(256*1024)
                            while (true) {
                                val n=zip.read(buffer)
                                if(n<0)break
                                count+=n
                                if(count>MAP_BYTES) return false
                                digest.update(buffer,0,n)
                                out.write(buffer,0,n)
                            }
                        }
                    }
                    zip.closeEntry() // validates CRC of each ZIP entry
                }
            }
            if(found!=1 || count!=MAP_BYTES) return false
            val actual=digest.digest().joinToString(""){"%02x".format(it)}
            if(actual!=MAP_SHA) return false
            val record=JSONObject().apply {
                put("project","RaSaSeR Navigasyon Türkiye")
                put("map_sha256",MAP_SHA)
                put("source_archive_sha256",ZIP_SHA)
                put("data_license","ODbL 1.0 © OpenStreetMap contributors")
                put("map",JSONObject().put("size_bytes",MAP_BYTES).put("format","pbf"))
            }
            manifestStage.writeText(record.toString(2),Charsets.UTF_8)
            // Stage first; do not delete a functioning map on checksum failure.
            val previous=File(maps,"turkey.mbtiles")
            val previousManifest=File(maps,"turkey.manifest.json")
            val backup=File(maps,"turkey.previous.mbtiles")
            val backupManifest=File(maps,"turkey.previous.manifest.json")
            if(previous.exists()) {
                if(backup.exists()) return false // never overwrite a previously protected map
                if(!previous.renameTo(backup)) return false
                if(previousManifest.exists() && !previousManifest.renameTo(backupManifest)) {
                    backup.renameTo(previous)
                    return false
                }
            }
            if(!stage.renameTo(previous) || !manifestStage.renameTo(previousManifest)) {
                if(previous.exists()) previous.delete()
                if(backup.exists()) backup.renameTo(previous)
                if(backupManifest.exists()) backupManifest.renameTo(previousManifest)
                return false
            }
            return true
        } catch (_: Exception) {
            return false
        } finally {
            stage.delete()
            manifestStage.delete()
        }
    }

    private fun sha256(file:File):String {
        val md=MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { s ->
            val buf=ByteArray(256*1024)
            while(true) {
                val n=s.read(buf)
                if(n<0) break
                md.update(buf,0,n)
            }
        }
        return md.digest().joinToString(""){"%02x".format(it)}
    }
}
