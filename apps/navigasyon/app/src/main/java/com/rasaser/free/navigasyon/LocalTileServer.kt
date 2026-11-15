package com.rasaser.free.navigasyon

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Serves local MBTiles as vector tiles to MapLibre.
 * Loopback-only, no remote interface, no network services or API keys.
 * MBTiles TMS row -> XYZ row is converted here.
 */
class LocalTileServer(private val context: Context, mapFile: File) : AutoCloseable {
    private val db = SQLiteDatabase.openDatabase(mapFile.absolutePath, null,
        SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS)
    private val socket = ServerSocket(0, 12, InetAddress.getByName("127.0.0.1"))
    private val active = AtomicBoolean(true)
    private val workers = Executors.newFixedThreadPool(3) { task ->
        Thread(task, "rasaser-tiles").apply { isDaemon = true }
    }
    val port: Int = socket.localPort
    private val listener = Thread({
        while (active.get()) {
            try {
                val client = socket.accept()
                workers.execute { handle(client) }
            } catch (_: Exception) {
                if (!active.get()) break
            }
        }
    }, "rasaser-local-map").apply { isDaemon = true; start() }

    private fun handle(client: Socket) {
        client.use { conn ->
            try {
                conn.soTimeout = 6000
                val input = BufferedInputStream(conn.getInputStream())
                val lineBuffer = StringBuilder()
                var b: Int
                while (lineBuffer.length < 512) {
                    b = input.read()
                    if (b == -1 || b == 10) break
                    if (b != 13) lineBuffer.append(b.toChar())
                }
                val request = lineBuffer.toString().split(' ')
                if (request.size < 2 || request[0] != "GET") {
                    send(conn, 405, "text/plain", "Method not allowed".toByteArray())
                    return
                }
                val font = Regex("^/fonts/Noto(?:%20| )Sans(?:%20| )Regular/(\\\\d{1,5})-(\\\\d{1,5})\\\\.pbf$")
                    .matchEntire(request[1])
                if (font!=null) {
                    val first=font.groupValues[1].toInt()
                    val last=font.groupValues[2].toInt()
                    if(first in 0..65280 && first%256==0 && last==first+255) {
                        val path="fonts/Noto Sans Regular/$first-$last.pbf"
                        val payload=runCatching {
                            context.assets.open(path).use { it.readBytes() }
                        }.getOrNull()
                        if(payload!=null && payload.isNotEmpty()) {
                            send(conn,200,"application/x-protobuf",payload)
                            return
                        }
                    }
                    send(conn,404,"text/plain",byteArrayOf())
                    return
                }
                val matched = Regex("^/tiles/(\\d{1,2})/(\\d{1,8})/(\\d{1,8})\\.pbf$").matchEntire(request[1])
                if (matched == null) {
                    send(conn, 404, "text/plain", "Not found".toByteArray())
                    return
                }
                val (zs,xs,ys) = matched.destructured
                val z=zs.toInt(); val x=xs.toInt(); val y=ys.toInt()
                if (z !in 0..14 || x !in 0 until (1 shl z) || y !in 0 until (1 shl z)) {
                    send(conn, 404, "text/plain", byteArrayOf())
                    return
                }
                val tms = (1 shl z)-1-y
                val bytes = synchronized(db) {
                    db.rawQuery("SELECT tile_data FROM tiles WHERE zoom_level=? AND tile_column=? AND tile_row=? LIMIT 1",
                        arrayOf(z.toString(),x.toString(),tms.toString())).use { cursor ->
                        if (cursor.moveToFirst()) cursor.getBlob(0) else null
                    }
                }
                if (bytes == null) send(conn,404,"text/plain",byteArrayOf())
                else send(conn,200,"application/x-protobuf",bytes,
                    compressed=bytes.size>=2 && bytes[0]==0x1f.toByte() && bytes[1]==0x8b.toByte())
            } catch (_: Exception) {
                // Broken connection or inaccessible database; do not leak filesystem details.
            }
        }
    }

    private fun send(socket: Socket, code:Int, type:String, body:ByteArray, compressed:Boolean=false) {
        val label = when(code){200->"OK";404->"Not Found";405->"Method Not Allowed";else->"Error"}
        val headers=buildString {
            append("HTTP/1.1 $code $label\r\n")
            append("Content-Type: $type\r\n")
            append("Content-Length: ${body.size}\r\n")
            append("Cache-Control: public, max-age=86400\r\n")
            if (compressed) append("Content-Encoding: gzip\r\n")
            append("Connection: close\r\n\r\n")
        }
        BufferedOutputStream(socket.getOutputStream()).use { output ->
            output.write(headers.toByteArray(StandardCharsets.US_ASCII))
            output.write(body)
            output.flush()
        }
    }

    override fun close() {
        if (!active.getAndSet(false)) return
        runCatching { socket.close() }
        workers.shutdownNow()
        synchronized(db) { if (db.isOpen) db.close() }
    }
}
