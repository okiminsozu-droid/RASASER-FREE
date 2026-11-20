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
 * WebSocket reader, only factual transient notices. It does not save any article to disk.
 * Android can suspend network activity even when the user enabled live foreground monitoring.
 */
class LiveNewsClient(
    private val onEvent: (NewsItem) -> Unit,
    private val onConnected: (Boolean) -> Unit,
    private val onState: (String) -> Unit = {}
) {
    private val main = Handler(Looper.getMainLooper())
    private val http = OkHttpClient.Builder()
        .pingInterval(25, TimeUnit.SECONDS)
        .connectTimeout(12, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val target = "wss://trendnabiz-engine.okiminsozu.workers.dev/v1/live"
    private var socket: WebSocket? = null
    private var started = false
    private var attempt = 0
    private var generation = 0
    private val seen = LinkedHashMap<String, Long>()
    private val reconnect = Runnable { if (started) connect() }

    fun start() {
        if(started) return
        started=true
        onState("connecting")
        connect()
    }
    private fun connect() {
        if(!started) return
        val g=++generation
        onState("connecting")
        try {
            socket = http.newWebSocket(Request.Builder().url(target).build(), object:WebSocketListener() {
                override fun onOpen(ws:WebSocket, response:Response) {
                    main.post {
                        if(!started || g!=generation) { ws.close(1000,null); return@post }
                        attempt=0
                        onState("connected")
                        onConnected(true)
                        ws.send("ping")
                    }
                }
                override fun onMessage(ws:WebSocket, text:String) {
                    if(text.length>4096) return
                    val obj=try { JSONObject(text) } catch (_:Exception) { return }
                    if(obj.optString("type")=="pong" || obj.optString("type")=="hello"){
                        main.post { if(g==generation&&started)onState("connected") }
                        return
                    }
                    if(obj.optString("type")!="event") return
                    val item=parse(obj) ?: return
                    main.post {
                        if(!started||g!=generation)return@post
                        val now=System.currentTimeMillis()
                        seen.entries.removeAll{it.value<=now}
                        if(seen.containsKey(item.id)) return@post
                        seen[item.id]=now+180000L
                        onEvent(item)
                    }
                }
                override fun onClosing(ws:WebSocket, code:Int, reason:String) { ws.close(1000,null) }
                override fun onClosed(ws:WebSocket, code:Int, reason:String) {
                    main.post { if(started&&g==generation){ onConnected(false);onState("closed:$code");retry() } }
                }
                override fun onFailure(ws:WebSocket, error:Throwable, response:Response?) {
                    val state="error:"+(response?.code?.toString()?:error.javaClass.simpleName)
                    main.post { if(started&&g==generation){onConnected(false);onState(state);retry()} }
                }
            })
        } catch(t:Exception) {
            onState("error:"+t.javaClass.simpleName)
            retry()
        }
    }
    private fun retry() {
        main.removeCallbacks(reconnect)
        val delay=minOf(60000L,2000L*(1L shl minOf(attempt++,5)))
        main.postDelayed(reconnect,delay)
    }
    private fun parse(obj:JSONObject):NewsItem? {
        val id=obj.optString("id").take(180)
        val title=obj.optString("title").take(200)
        val description=obj.optString("summary").take(340)
        val source=obj.optString("source").take(80)
        val sourceUrl=obj.optString("sourceUrl")
        val issuedAt=obj.optLong("issuedAt")
        val now=System.currentTimeMillis()
        if(id.isBlank()||title.isBlank()||source.isBlank())return null
        if(issuedAt<=now-180000L||issuedAt>now+30000L)return null
        val url=try{URI(sourceUrl)}catch(_:Exception){return null}
        if(url.scheme!="https"||url.host.isNullOrBlank()||url.userInfo!=null)return null
        val magnitude=Regex("M([0-9]+(?:\\.[0-9]+)?)").find(title)?.groupValues?.get(1)?.toDoubleOrNull()?:0.0
        return NewsItem(id,title,description,source,sourceUrl,issuedAt,magnitude,
            obj.optString("category","world"),obj.optString("priority","normal"))
    }
    fun requestCloudTest(): Boolean = if(started && socket!=null) socket?.send("self_test")==true else false

    fun stop() {
        started=false
        generation++
        main.removeCallbacks(reconnect)
        socket?.close(1000,"disconnect")
        socket=null
        seen.clear()
        onConnected(false)
        onState("stopped")
    }
}
