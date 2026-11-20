package com.rasaser.free.trendnabiz

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class NewsItem(
    val id: String,
    val title: String,
    val description: String,
    val source: String,
    val sourceUrl: String,
    val publishedAt: Long,
    val magnitude: Double = 0.0
) {
    val critical: Boolean get() = magnitude >= 6.0
}

object UserPrefs {
    private const val NAME = "trendnabiz_prefs"
    fun store(context: Context): SharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    fun language(context: Context): UiLang = UiLang.parse(store(context).getString("language", "tr"))
    fun setLanguage(context: Context, value: UiLang) = store(context).edit().putString("language", value.code).apply()
    fun criticalOnly(context: Context): Boolean = store(context).getBoolean("critical_only", false)
    fun setCriticalOnly(context: Context, value: Boolean) = store(context).edit().putBoolean("critical_only", value).apply()
    // Reserved for a future verified Play Billing / backend entitlement; never write this from an in-app toggle.
    fun isPremium(context: Context): Boolean = false
}

object NewsStore {
    // Independently documented, public USGS GeoJSON feed. No proprietary key or paid API.
    private const val USGS_URL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_day.geojson"
    private const val MAX = 60

    fun cached(context: Context): List<NewsItem> {
        val text = UserPrefs.store(context).getString("news_cache", "[]") ?: "[]"
        return try {
            val array = JSONArray(text)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                NewsItem(
                    id = obj.optString("id"),
                    title = obj.optString("title"),
                    description = obj.optString("detail"),
                    source = obj.optString("source"),
                    sourceUrl = obj.optString("url"),
                    publishedAt = obj.optLong("time"),
                    magnitude = obj.optDouble("mag", 0.0)
                )
            }.filter { it.id.isNotBlank() }
        } catch (_: Exception) { emptyList() }
    }

    private fun write(context: Context, stories: List<NewsItem>) {
        val array = JSONArray()
        for (item in stories.take(MAX)) {
            array.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("detail", item.description)
                put("source", item.source)
                put("url", item.sourceUrl)
                put("time", item.publishedAt)
                put("mag", item.magnitude)
            })
        }
        UserPrefs.store(context).edit().putString("news_cache", array.toString()).apply()
    }

    /** Returns new items only after first successful sync; first sync creates baseline without alert flood. */
    suspend fun sync(context: Context): List<NewsItem> = withContext(Dispatchers.IO) {
        val connection = URL(USGS_URL).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 12_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/geo+json,application/json")
            if (connection.responseCode !in 200..299) throw IllegalStateException("HTTP ${connection.responseCode}")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val features = JSONObject(body).getJSONArray("features")
            val result = mutableListOf<NewsItem>()
            for (i in 0 until minOf(features.length(), MAX)) {
                val feature = features.optJSONObject(i) ?: continue
                val props = feature.optJSONObject("properties") ?: continue
                val id = feature.optString("id").takeIf { it.isNotBlank() } ?: continue
                val mag = props.optDouble("mag", 0.0)
                val place = props.optString("place").ifBlank { "Location not provided" }
                val url = props.optString("url")
                if (!url.startsWith("https://earthquake.usgs.gov/")) continue
                val title = "M${String.format(Locale.US, "%.1f", mag)} · $place"
                result += NewsItem(
                    id = id,
                    title = title,
                    description = "USGS earthquake report: $place; magnitude ${String.format(Locale.US, "%.1f", mag)}.",
                    source = "USGS",
                    sourceUrl = url,
                    publishedAt = props.optLong("time", 0L),
                    magnitude = mag
                )
            }
            val old = cached(context).map { it.id }.toSet()
            val prefs = UserPrefs.store(context)
            val initialized = prefs.getBoolean("baseline_ready", false)
            write(context, result)
            prefs.edit().putBoolean("baseline_ready", true).apply()
            if (initialized) result.filterNot { it.id in old } else emptyList()
        } finally {
            connection.disconnect()
        }
    }
}
