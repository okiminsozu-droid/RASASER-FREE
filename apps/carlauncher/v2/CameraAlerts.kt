package com.rasaser.launcher

import android.content.Context
import android.location.Location
import android.speech.tts.TextToSpeech
import android.os.SystemClock
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

data class CameraPoint(val id: Long, val latitude: Double, val longitude: Double)

/**
 * OpenStreetMap speed_camera nodes — not Google Maps speed-camera data.
 * Public Overpass is best-effort only, cache/throttle calls in the Activity.
 * Not all cameras are mapped; users must always obey road signs and speed limits.
 */
fun queryOSMSpeedCameras(lat: Double, lon: Double): List<CameraPoint> {
    if (!lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0) return emptyList()
    val q = "[out:json][timeout:12];node(around:2000,$lat,$lon)[\"highway\"=\"speed_camera\"];out;"
    val url = "https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(q, "UTF-8")
    val json = JSONObject(httpGet(url))
    val items = json.optJSONArray("elements") ?: return emptyList()
    return (0 until items.length()).mapNotNull { i ->
        val point = items.getJSONObject(i)
        if (point.has("lat") && point.has("lon"))
            CameraPoint(point.optLong("id"), point.getDouble("lat"), point.getDouble("lon"))
        else null
    }
}

fun distanceToCameraMetres(lat: Double, lon: Double, point: CameraPoint): Float {
    val result = FloatArray(1)
    Location.distanceBetween(lat, lon, point.latitude, point.longitude, result)
    return result[0]
}

class CameraVoice(private val context: Context) {
    private val tts = TextToSpeech(context) {}
    private val lastAnnouncements = mutableMapOf<Long, Long>()

    fun announce(point: CameraPoint, lang: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - (lastAnnouncements[point.id] ?: -3_600_000L) < 3_600_000L) return
        lastAnnouncements[point.id] = now
        val locale = when (lang) {
            "tr" -> Locale("tr", "TR")
            "de" -> Locale.GERMAN
            "es" -> Locale("es", "ES")
            "ja" -> Locale.JAPANESE
            else -> Locale.ENGLISH
        }
        tts.language = locale
        tts.speak(uiText(lang, "radarDistance"), TextToSpeech.QUEUE_FLUSH, null, "camera-${point.id}")
    }

    fun stop() {
        tts.stop()
        tts.shutdown()
    }
}
