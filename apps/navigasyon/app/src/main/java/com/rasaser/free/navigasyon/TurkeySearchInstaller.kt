package com.rasaser.free.navigasyon

import android.app.DownloadManager
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.util.Log
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * Verifies and installs the public ODbL Türkiye search pack entirely on device.
 * Keeps any previously usable index on failure. No GPS/name data leaves device.
 */
class TurkeySearchInstaller(private val context: Context) {
    companion object {
        const val RELEASE_URL =
            "https://github.com/okiminsozu-droid/RASASER-FREE/releases/download/rasaser-turkiye-search-v0.1/turkey-places.zip"
        const val R2_URL = "https://rasaser-turkiye-map-download.okiminsozu.workers.dev/v1/turkey-places.zip"
        const val ZIP_BYTES=66_446_365L
        const val DB_BYTES=150_110_208L
        const val ZIP_SHA="048f474b1ff427ac2310b5201a283d027d87ec769ca7e808254ffbbdf6b603f0"
        const val DB_SHA="8ce8f9592e223a4d310c4e4b12151334e50dc13fa5054f44ec3ff525adce038f"
        private const val TAG="RaSaSeRSearchInstall"
    }

    enum class Stage { READY, NEED_DOWNLOAD, ZIP_MISSING, ZIP_SIZE, ZIP_CHECKSUM,
        ZIP_ENTRY, UNPACK_SIZE, UNPACK_CHECKSUM, SQLITE_OPEN, SQLITE_SCHEMA,
        SQLITE_INTEGRITY, FILE_INSTALL, NO_STORAGE, UNKNOWN }
    data class Result(val installed: Boolean, val stage:Stage)
    private var lastStage=Stage.NEED_DOWNLOAD
    fun lastFailure():Stage=lastStage
    private val manager:DownloadManager
        get()=context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private fun file():File=File(requireNotNull(
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)),"turkey-places-v01.zip")
    private val directory:File get()=File(context.filesDir,"search")
    private val index:File get()=File(directory,"turkey-places.sqlite")

    fun installed():Boolean =
        index.isFile && index.length()==DB_BYTES && runCatching {
            SQLiteDatabase.openDatabase(index.path,null,SQLiteDatabase.OPEN_READONLY).use {db->
                db.rawQuery("SELECT value FROM info WHERE key='schema'",null).use {c->
                    c.moveToFirst() && c.getString(0)=="1"
                }
            }
        }.getOrDefault(false)

    /**
     * Reuses a previously fully downloaded, pinned archive after an interrupted
     * installation. Caller must invoke install() off the main thread.
     */
    fun cachedZipExists():Boolean=file().isFile && file().length()==ZIP_BYTES

    fun beginWifiDownload(fromR2:Boolean=true):Long {
        if(StatFs(context.filesDir.absolutePath).availableBytes<460_000_000L)
            throw IllegalStateException("storage")
        val dest=file()
        // A fully downloaded ZIP may be used again by install(), never delete
        // it until its checksum proves invalid. Retry is explicit.
        if(dest.exists())dest.delete()
        val request=DownloadManager.Request(Uri.parse(if(fromR2) R2_URL else RELEASE_URL))
            .setTitle("RaSaSeR Türkiye yer arama")
            .setDescription("Çevrimdışı ad arama • 66 MB")
            .setAllowedOverMetered(false)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context,Environment.DIRECTORY_DOWNLOADS,dest.name)
        return manager.enqueue(request)
    }

    data class Transfer(val status:Int,val reason:Int)
    fun transfer(id:Long):Transfer =
        manager.query(DownloadManager.Query().setFilterById(id)).use {c->
            if(c==null || !c.moveToFirst())Transfer(DownloadManager.STATUS_FAILED,-1)
            else Transfer(
                c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            )
        }
    fun downloadState(id:Long):Int=transfer(id).status

    /** Must run on a worker thread after successful transfer or for retry. */
    fun install(downloadId:Long?=null):Boolean=installWithResult(downloadId).installed

    fun installWithResult(downloadId:Long?=null, sourceArchive:File?=null):Result {
        if(installed())return report(true,Stage.READY)
        if(StatFs(context.filesDir.absolutePath).availableBytes<350_000_000L)
            return report(false,Stage.NO_STORAGE)
        val cached=file()
        var temporary:File?=null
        val archive=if(sourceArchive!=null) sourceArchive
        else if(cached.isFile && cached.length()==ZIP_BYTES)cached
        else if(downloadId!=null && downloadId>=0L) {
            // Some vendors keep DownloadManager results behind content://.
            val temp=File(context.cacheDir,"places-v01-downloaded.zip")
            temp.delete()
            try {
                manager.openDownloadedFile(downloadId).use {descriptor->
                    android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor.dup()).use {input->
                        temp.outputStream().buffered().use {out->
                            copyBounded(input,out,ZIP_BYTES)
                        }
                    }
                }
                temporary=temp
                temp
            } catch (e:Exception) {
                Log.w(TAG,"DownloadManager content cannot be opened",e)
                temp.delete()
                return report(false,Stage.ZIP_MISSING)
            }
        } else return report(false,Stage.ZIP_MISSING)

        try {
            if(archive.length()!=ZIP_BYTES)return report(false,Stage.ZIP_SIZE)
            if(sha256(archive)!=ZIP_SHA)return report(false,Stage.ZIP_CHECKSUM)
            directory.mkdirs()
            val stage=File(directory,"turkey-places.next.sqlite")
            stage.delete()
            try {
                var count=0L
                var entries=0
                val checksum=MessageDigest.getInstance("SHA-256")
                ZipInputStream(archive.inputStream().buffered()).use {zip->
                    while(true) {
                        val entry=zip.nextEntry?:break
                        if(entry.isDirectory || entry.name!="turkey-places.sqlite" || entries!=0)
                            return report(false,Stage.ZIP_ENTRY)
                        entries++
                        stage.outputStream().buffered().use {out->
                            val block=ByteArray(128*1024)
                            while(true) {
                                val n=zip.read(block)
                                if(n<0)break
                                count+=n
                                if(count>DB_BYTES)return report(false,Stage.UNPACK_SIZE)
                                checksum.update(block,0,n)
                                out.write(block,0,n)
                            }
                        }
                        zip.closeEntry()
                    }
                }
                if(entries!=1 || count!=DB_BYTES)
                    return report(false,Stage.UNPACK_SIZE)
                if(checksum.digest().joinToString(""){"%02x".format(it)}!=DB_SHA)
                    return report(false,Stage.UNPACK_CHECKSUM)
                val good=try {
                    SQLiteDatabase.openDatabase(stage.path,null,SQLiteDatabase.OPEN_READONLY).use {db->
                        val schema=db.rawQuery("SELECT value FROM info WHERE key='schema'",null).use {c->
                            c.moveToFirst() && c.getString(0)=="1"
                        }
                        if(!schema)return report(false,Stage.SQLITE_SCHEMA)
                        val hasPlace=db.rawQuery(
                            "SELECT count(*) FROM places LIMIT 1",null
                        ).use {c->c.moveToFirst() && c.getLong(0)>1_000_000L}
                        if(!hasPlace)return report(false,Stage.SQLITE_SCHEMA)
                        val probe=db.rawQuery(
                            "SELECT count(*) FROM places_fts WHERE normalized MATCH 'ankara*'",null
                        ).use {c->c.moveToFirst() && c.getLong(0)>0}
                        if(!probe)return report(false,Stage.SQLITE_SCHEMA)
                        db.rawQuery("PRAGMA quick_check",null).use {c->
                            c.moveToFirst() && c.getString(0)=="ok"
                        }
                    }
                } catch(e:Exception) {
                    Log.w(TAG,"SQLite index incompatible with device",e)
                    return report(false,Stage.SQLITE_OPEN)
                }
                if(!good)return report(false,Stage.SQLITE_INTEGRITY)
                if(index.exists()) {
                    if(installed())return report(true,Stage.READY)
                    // Preserve a broken index for diagnosis, but never treat it
                    // as installed or overwrite a known-good database.
                    val backup=File(directory,"turkey-places.invalid.sqlite")
                    if(backup.exists() || !index.renameTo(backup))
                        return report(false,Stage.FILE_INSTALL)
                    if(!stage.renameTo(index)) {
                        backup.renameTo(index)
                        return report(false,Stage.FILE_INSTALL)
                    }
                } else if(!stage.renameTo(index)) {
                    return report(false,Stage.FILE_INSTALL)
                }
                return report(true,Stage.READY)
            } finally {stage.delete()}
        } catch(e:Exception){
            Log.w(TAG,"Search pack verification failed at $lastStage",e)
            return report(false,Stage.UNKNOWN)
        } finally { temporary?.delete() }
    }


    /**
     * The independently signed field-test variant bundles a verified local
     * 1M-place SQLite database, eliminating initial DownloadManager failures.
     * Normal release variants can keep using the separate free ZIP download.
     */
    fun bundledAvailable():Boolean=runCatching {
        context.assets.open("search/turkey-places.sqlite").use { true }
    }.getOrDefault(false)

    /** Called off the main thread; verifies the complete embedded ODbL index. */
    fun installBundled():Boolean {
        if(installed())return true
        if(!bundledAvailable())return false
        if(StatFs(context.filesDir.absolutePath).availableBytes<260_000_000L)
            return report(false,Stage.NO_STORAGE).installed
        directory.mkdirs()
        val stage=File(directory,"turkey-places.embedded.next.sqlite")
        stage.delete()
        try {
            val sha=MessageDigest.getInstance("SHA-256")
            var copied=0L
            context.assets.open("search/turkey-places.sqlite").use {input->
                stage.outputStream().buffered().use {out->
                    val buffer=ByteArray(256*1024)
                    while(true){
                        val n=input.read(buffer)
                        if(n<0)break
                        copied+=n
                        if(copied>DB_BYTES)return report(false,Stage.UNPACK_SIZE).installed
                        sha.update(buffer,0,n)
                        out.write(buffer,0,n)
                    }
                }
            }
            if(copied!=DB_BYTES)return report(false,Stage.UNPACK_SIZE).installed
            if(sha.digest().joinToString(""){"%02x".format(it)}!=DB_SHA)
                return report(false,Stage.UNPACK_CHECKSUM).installed
            val compatible=try {
                SQLiteDatabase.openDatabase(stage.path,null,SQLiteDatabase.OPEN_READONLY).use {db->
                    val schema=db.rawQuery("SELECT value FROM info WHERE key='schema'",null).use {cur->
                        cur.moveToFirst() && cur.getString(0)=="1"
                    }
                    val total=db.rawQuery("SELECT count(*) FROM places",null).use {cur->
                        cur.moveToFirst() && cur.getLong(0)>=1_000_000L
                    }
                    val fts=db.rawQuery(
                        "SELECT count(*) FROM places_fts WHERE normalized MATCH 'ankara*'",null
                    ).use {cur->cur.moveToFirst() && cur.getLong(0)>0}
                    schema && total && fts
                }
            }catch(e:Exception){
                Log.w(TAG,"Bundled SQLite incompatible with this device",e)
                return report(false,Stage.SQLITE_OPEN).installed
            }
            if(!compatible)return report(false,Stage.SQLITE_SCHEMA).installed
            if(installed())return true
            if(index.exists()){
                val backup=File(directory,"turkey-places.invalid.sqlite")
                if(backup.exists() || !index.renameTo(backup))
                    return report(false,Stage.FILE_INSTALL).installed
            }
            if(!stage.renameTo(index))return report(false,Stage.FILE_INSTALL).installed
            report(true,Stage.READY)
            return true
        }catch(e:Exception){
            Log.w(TAG,"Bundled index verification failed",e)
            return report(false,Stage.UNKNOWN).installed
        }finally{
            stage.delete()
        }
    }

    private fun report(ok:Boolean,stage:Stage):Result {
        lastStage=stage
        if(!ok)Log.w(TAG,"Installation stage: $stage")
        return Result(ok,stage)
    }
    private fun copyBounded(input:InputStream,out:java.io.OutputStream,limit:Long){
        val buffer=ByteArray(128*1024);var total=0L
        while(true) {
            val n=input.read(buffer)
            if(n<0)break
            total+=n
            if(total>limit)throw IllegalStateException("Oversize archive")
            out.write(buffer,0,n)
        }
    }
    private fun sha256(path:File):String{
        val md=MessageDigest.getInstance("SHA-256")
        path.inputStream().buffered().use {stream->
            val buf=ByteArray(256*1024)
            while(true){val n=stream.read(buf);if(n<0)break;md.update(buf,0,n)}
        }
        return md.digest().joinToString(""){"%02x".format(it)}
    }
}
