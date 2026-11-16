package com.rasaser.free.navigasyon

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Pinned, publicly licensed Türkiye datasets. The endpoint is an implementation
 * detail: never display it in user-facing progress, logs or notifications.
 * Every completed archive is checked against a source SHA-256, not R2 ETag.
 */
internal data class TurkeyAsset(
    val name: String, val size: Long, val sha256: String
)

internal object TurkeyAssets {
    private const val BASE =
        "https://rasaser-turkiye-map-download.okiminsozu.workers.dev/v1/"
    fun url(name: String) = BASE + name

    val mapManifest = TurkeyAsset("turkey.manifest.json", 1048L,
        "b588343ca2ad3b51dbd76f36e57009e58d8712e9fcb454c5e6f7b4673ce8cd46")
    val map = TurkeyAsset("rasaser-turkiye-shortbread.zip", 605992151L,
        TurkeyMapInstaller.ZIP_SHA)
    val routeManifest = TurkeyAsset("valhalla-turkiye-manifest.json", 657L,
        "ce663429563e556ac78e729e8d919b72993f587a2b8426e5d556007022924f26")
    val routeConfig = TurkeyAsset("valhalla.json", 9598L,
        "bc0bb95856c09c51b859bb2f18423b43a0353504bb8e26529f3aad71bbf98fc3")
    val route = TurkeyAsset("valhalla_tiles.tar", 1148661760L,
        TurkeyRoutingInstaller.GRAPH_SHA)
    val searchManifest = TurkeyAsset("turkey-places-manifest.json", 382L,
        "1415b1c76a898e46da40691ededec05db3e1f75dfa870c8557fb662a1e49da01")
    val search = TurkeyAsset("turkey-places.zip", 66446365L,
        TurkeySearchInstaller.ZIP_SHA)
    val all = listOf(mapManifest,map,routeManifest,routeConfig,route,searchManifest,search)
    val totalBytes: Long = all.sumOf { it.size }
}

/**
 * Explicit byte-range validation avoids appending the wrong bytes after a
 * partial response, CDN cache change, or a bad origin implementation.
 */
internal object TurkeyRangeCheck {
    fun valid(contentRange: String?, expectedStart: Long, fileSize: Long): Boolean {
        if(contentRange == null || !contentRange.startsWith("bytes ")) return false
        val fields = contentRange.removePrefix("bytes ").split("/", limit=2)
        if(fields.size != 2) return false
        val bounds = fields[0].split("-",limit=2)
        if(bounds.size != 2) return false
        val begin = bounds[0].toLongOrNull() ?: return false
        val end = bounds[1].toLongOrNull() ?: return false
        val total = fields[1].toLongOrNull() ?: return false
        return begin == expectedStart && total == fileSize &&
            end >= begin && end < fileSize
    }
}

/**
 * One immutable .part per asset. A dropped connection, reboot, process death or
 * WorkManager retry never truncates a valid partial download. If the server
 * rejects Range and returns 200, we explicitly restart instead of appending.
 */
internal class ResumableTurkeyTransfer(private val folder: File) {
    init { if(!folder.exists() && !folder.mkdirs()) throw IOException("storage unavailable") }

    fun download(asset: TurkeyAsset, cancelled: () -> Boolean,
                 progress: (Long) -> Unit): File {
        val completed = File(folder,asset.name)
        if(completed.isFile && completed.length() == asset.size &&
            fileSha256(completed).equals(asset.sha256,true)) {
            progress(asset.size)
            return completed
        }
        // A bad *archive*, not an installed map, may be discarded safely.
        if(completed.exists() && !completed.delete()) throw IOException("old download cannot be replaced")
        val partial = File(folder,asset.name + ".part")
        val etagFile = File(folder,asset.name + ".etag")
        if(partial.length() > asset.size) {
            if(!partial.delete()) throw IOException("invalid partial download")
            etagFile.delete()
        }
        var position = partial.length()
        if(position == asset.size) {
            if(fileSha256(partial).equals(asset.sha256,true)) {
                if(!partial.renameTo(completed)) throw IOException("unable to finalize")
                etagFile.delete()
                progress(asset.size)
                return completed
            }
            partial.delete()
            etagFile.delete()
            position = 0
        }
        if(cancelled()) throw IOException("transfer interrupted")
        val connection = (URL(TurkeyAssets.url(asset.name)).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 25_000
            connection.readTimeout = 65_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept-Encoding","identity")
            connection.setRequestProperty("Cache-Control","no-transform")
            connection.setRequestProperty("User-Agent","RaSaSeR-Navigation/1.0")
            if(position > 0L) {
                connection.setRequestProperty("Range","bytes=$position-")
                val tag = etagFile.takeIf { it.isFile }?.readText()?.trim()
                if(!tag.isNullOrEmpty()) connection.setRequestProperty("If-Range",tag)
            }
            val code = connection.responseCode
            if(code == HttpURLConnection.HTTP_PARTIAL) {
                if(!TurkeyRangeCheck.valid(connection.getHeaderField("Content-Range"),
                        position,asset.size)) throw IOException("invalid resume range")
                val saved = etagFile.takeIf { it.exists() }?.readText()?.trim()
                val current = connection.getHeaderField("ETag")?.trim()
                if(!saved.isNullOrBlank() && !current.isNullOrBlank() && saved != current)
                    throw IOException("remote file version changed")
            } else if(code == HttpURLConnection.HTTP_OK) {
                if(position > 0) {
                    // An origin that cannot resume must not silently corrupt the file.
                    position = 0
                    etagFile.delete()
                }
                val length = connection.contentLengthLong
                if(length >= 0 && length != asset.size)
                    throw IOException("unexpected source length")
            } else {
                throw IOException("download paused: HTTP $code")
            }

            val tag = connection.getHeaderField("ETag")
            if(position == 0L && !tag.isNullOrBlank()) etagFile.writeText(tag)
            progress(position)
            connection.inputStream.buffered(256*1024).use { input ->
                RandomAccessFile(partial,"rw").use { output ->
                    output.setLength(position)
                    output.seek(position)
                    val buffer = ByteArray(256*1024)
                    var at = position
                    var lastUpdate = position
                    while(true) {
                        if(cancelled()) throw IOException("transfer interrupted")
                        val count = input.read(buffer)
                        if(count < 0) break
                        if(count == 0) continue
                        if(at + count > asset.size) throw IOException("source is larger than expected")
                        output.write(buffer,0,count)
                        at += count
                        if(at - lastUpdate >= 2L*1024*1024) {
                            progress(at)
                            lastUpdate = at
                        }
                    }
                    output.fd.sync()
                }
            }
        } finally {
            connection.disconnect()
        }
        if(partial.length() != asset.size)
            throw IOException("incomplete download; will resume")
        if(!fileSha256(partial).equals(asset.sha256,true)) {
            // Never treat a corrupt complete archive as resumable.
            partial.delete()
            etagFile.delete()
            throw IOException("download SHA-256 mismatch")
        }
        if(!partial.renameTo(completed)) throw IOException("unable to finalize archive")
        etagFile.delete()
        progress(asset.size)
        return completed
    }

    private fun fileSha256(file: File): String {
        val hash = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(256*1024).use { input ->
            val buffer=ByteArray(256*1024)
            while(true) {
                val n=input.read(buffer)
                if(n<0)break
                hash.update(buffer,0,n)
            }
        }
        return hash.digest().joinToString("") { "%02x".format(it) }
    }
}
