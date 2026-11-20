package com.rasaser.free.trendnabiz

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.URI
import java.util.LinkedHashMap
import java.util.concurrent.TimeUnit

/**
 * App-visible live websocket connection. Does not retain news on disk.
 * Android can suspend or terminate this connection in the background.
 * Lock-screen SLA requires a compliant separately-tested push receiver.
 */
class LiveNewsClient(
    private val onEvent: (NewsItem) -> Unit,
    private val onConnected: (Boolean) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())
    private val http = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .connectTimeout(12, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val target = "wss://trendnabiz-engine.okiminsozu.workers.dev/v1/live"
    private var socket: WebSocket? = null
    private var started = false
    private var attempt = 0
    private val seen = LinkedHashMap<String, Long>()
    private val reconnect = Runnable { if(started) connect() }

    fun start() {
        if (started) return
        started = true
        connect()
    }

    private fun connect() {
        if(!started) return
        try {
            socket = http.newWebSocket(Request.Builder().url(target).build(), object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    main.post {
                        if (started) {
                            attempt = 0
                            onConnected(true)
                        } else webSocket.close(1000, null)
                    }
                }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    if(text.length > 4096) return
                    val obj = try { JSONObject(text) } catch (_: Exception) { return }
                    if(obj.optString("type") != "event") return
                    val item = parse(obj) ?: return
                    main.post {
                        if(!started) return@post
                        val now = System.currentTimeMillis()
                        seen.entries.removeAll { it.value <= now }
                        if(seen.containsKey(item.id)) return@post
                        seen[item.id] = now + 180_000L // In-memory ID only, no retained article content
                        onEvent(item)
                    }
                }
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(1000,null)
                }
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    main.post { if(started) { onConnected(false); retry() } }
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    main.post { if(started) { onConnected(false); retry() } }
                }
            })
        } catch (_: Exception) { retry() }
    }

    private fun retry() {
        main.removeCallbacks(reconnect)
        val delay = minOf(60_000L, 2000L * (1L shl minOf(attempt++, 5)))
        main.postDelayed(reconnect, delay)
    }

    private fun parse(obj: JSONObject): NewsItem? {
        val id=obj.optString("id").take(180)
        val title=obj.optString("title").take(200)
        val description=obj.optString("summary").take(340)
        val source=obj.optString("source").take(80)
        val sourceUrl=obj.optString("sourceUrl")
        val issuedAt=obj.optLong("issuedAt")
        val now=System.currentTimeMillis()
        if(id.isBlank() || title.isBlank() || source.isBlank()) return null
        if(issuedAt < now-180_000L || issuedAt > now+30_000L) return null
        val url=try { URI(sourceUrl) } catch(_:Exception){return null}
        if(url.scheme!="https" || url.host.isNullOrBlank() || url.userInfo!=null) return null
        val magnitude=Regex("M([0-9]+(?:\\.[0-9]+)?)").find(title)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        return NewsItem(
            id=id,title=title,description=description,source=source,
            sourceUrl=sourceUrl,publishedAt=issuedAt,magnitude=magnitude,
            category=obj.optString("category","world"),severity=obj.optString("priority","normal")
        )
    }

    fun stop() {
        started = false
        main.removeCallbacks(reconnect)
        socket?.close(1000, "app_not_visible")
        socket = null
        seen.clear()
        onConnected(false)
    }
}
