package com.rasaser.free.navigasyon

import android.app.DownloadManager
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/** Optional free national search database, pinned to the actual published ODbL pack. */
class TurkeySearchInstaller(private val context:Context) {
    companion object {
        const val RELEASE_URL="https://github.com/okiminsozu-droid/RASASER-FREE/releases/download/rasaser-turkiye-search-v0.1/turkey-places.zip"
        const val ZIP_BYTES=66_446_365L
        const val DB_BYTES=150_110_208L
        const val ZIP_SHA="048f474b1ff427ac2310b5201a283d027d87ec769ca7e808254ffbbdf6b603f0"
        const val DB_SHA="8ce8f9592e223a4d310c4e4b12151334e50dc13fa5054f44ec3ff525adce038f"
    }
    private val manager:DownloadManager
        get()=context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private fun zipFile():File=File(requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)),"turkey-places-v01.zip")

    fun beginWifiDownload():Long {
        if(StatFs(context.filesDir.absolutePath).availableBytes<500_000_000L)
            throw IllegalStateException("Not enough space for 150MB search database")
        val file=zipFile()
        if(file.exists()) file.delete()
        val request=DownloadManager.Request(Uri.parse(RELEASE_URL))
            .setTitle("RaSaSeR Türkiye çevrimdışı yer arama")
            .setDescription("Ücretsiz işletme, yol ve yer isimleri · 66 MB")
            .setAllowedOverMetered(false).setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context,Environment.DIRECTORY_DOWNLOADS,file.name)
        return manager.enqueue(request)
    }
    fun downloadState(id:Long):Int =
        manager.query(DownloadManager.Query().setFilterById(id)).use { c ->
            if(c==null||!c.moveToFirst())DownloadManager.STATUS_FAILED
            else c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        }

    fun install():Boolean {
        val file=zipFile()
        if(!file.isFile || file.length()!=ZIP_BYTES || sha256(file)!=ZIP_SHA)return false
        val root=File(context.filesDir,"search").apply{mkdirs()}
        val next=File(root,"turkey-places.next.sqlite")
        try {
            next.delete()
            var entries=0
            var length=0L
            val hash=MessageDigest.getInstance("SHA-256")
            ZipInputStream(file.inputStream().buffered()).use { zip ->
                while(true) {
                    val entry=zip.nextEntry?:break
                    if(entry.isDirectory || entry.name!="turkey-places.sqlite")return false
                    entries++
                    if(entries!=1)return false
                    next.outputStream().buffered().use { out ->
                        val buf=ByteArray(128*1024)
                        while(true) {
                            val count=zip.read(buf)
                            if(count<0)break
                            length+=count
                            if(length>DB_BYTES)return false
                            hash.update(buf,0,count)
                            out.write(buf,0,count)
                        }
                    }
                    zip.closeEntry()
                }
            }
            if(entries!=1 || length!=DB_BYTES ||
                hash.digest().joinToString(""){"%02x".format(it)}!=DB_SHA)return false
            SQLiteDatabase.openDatabase(next.path,null,SQLiteDatabase.OPEN_READONLY).use {db->
                db.rawQuery("SELECT value FROM info WHERE key='schema'",null).use {c->
                    if(!c.moveToFirst() || c.getString(0)!="1")return false
                }
                db.rawQuery("PRAGMA quick_check",null).use {c->
                    if(!c.moveToFirst() || c.getString(0)!="ok")return false
                }
            }
            val installed=File(root,"turkey-places.sqlite")
            if(installed.exists()) {
                // Preserve the previous index on all failed updates.
                val backup=File(root,"turkey-places.previous.sqlite")
                if(backup.exists() || !installed.renameTo(backup))return false
            }
            return next.renameTo(installed)
        } catch (_:Exception) {return false}
        finally {next.delete()}
    }
    private fun sha256(file:File):String {
        val hash=MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { stream ->
            val buf=ByteArray(128*1024)
            while(true){
                val count=stream.read(buf)
                if(count<0)break
                hash.update(buf,0,count)
            }
        }
        return hash.digest().joinToString(""){"%02x".format(it)}
    }
}
