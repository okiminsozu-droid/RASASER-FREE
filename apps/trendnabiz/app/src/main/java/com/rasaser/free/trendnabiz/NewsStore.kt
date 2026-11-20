package com.rasaser.free.trendnabiz

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale

data class NewsItem(
    val id: String,
    val title: String,
    val description: String,
    val source: String,
    val sourceUrl: String,
    val publishedAt: Long,
    val magnitude: Double = 0.0,
    val category: String = "earthquake",
    val severity: String = "normal",
    val region: String = ""
) {
    val critical: Boolean get() = severity == "critical" || (category == "earthquake" && magnitude >= 6.0)
}

object UserPrefs {
    private const val NAME = "trendnabiz_prefs"
    fun store(context: Context): SharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    fun language(context: Context): UiLang = UiLang.parse(store(context).getString("language", "tr"))
    fun setLanguage(context: Context, value: UiLang) = store(context).edit().putString("language", value.code).apply()
    fun criticalOnly(context: Context): Boolean = store(context).getBoolean("critical_only", false)
    fun setCriticalOnly(context: Context, value: Boolean) = store(context).edit().putBoolean("critical_only", value).apply()

    fun newsServer(context: Context): String = store(context).getString("news_server", "") ?: ""
    fun setNewsServer(context: Context, raw: String): Boolean {
        val value = raw.trim().trimEnd('/')
        if (value.isNotEmpty()) {
            val uri = try { URI(value) } catch (_: Exception) { return false }
            if (uri.scheme != "https" || uri.host.isNullOrBlank()
                || uri.userInfo != null || uri.query != null || uri.fragment != null
                || (uri.path != null && uri.path.isNotEmpty() && uri.path != "/")) return false
        }
        store(context).edit().putString("news_server",value).apply()
        return true
    }

    // Debug-only Premium preview; release builds cannot bypass Google Play purchase verification.
    fun isPremium(context: Context): Boolean =
        BuildConfig.DEBUG && store(context).getBoolean("debug_premium_preview", false)
    fun setDebugPremium(context: Context, enabled: Boolean) {
        if (BuildConfig.DEBUG) store(context).edit().putBoolean("debug_premium_preview", enabled).apply()
    }
}

object NewsStore {
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
                    magnitude = obj.optDouble("mag", 0.0),
                    category = obj.optString("category", "earthquake"),
                    severity = obj.optString("severity", "normal"),
                    region = obj.optString("region", "")
                )
            }.filter { it.id.isNotBlank() }
        } catch (_: Exception) { emptyList() }
    }

    private fun write(context: Context, stories: List<NewsItem>) {
        val arr = JSONArray()
        stories.take(MAX).forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("detail", item.description)
                put("source", item.source)
                put("url", item.sourceUrl)
                put("time", item.publishedAt)
                put("mag", item.magnitude)
                put("category", item.category)
                put("severity", item.severity)
                put("region", item.region)
            })
        }
        UserPrefs.store(context).edit().putString("news_cache",arr.toString()).apply()
    }

    private fun download(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10_000
            connection.readTimeout = 12_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json,application/geo+json")
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("HTTP ${connection.responseCode}")
            }
            val text = connection.inputStream.bufferedReader().use { reader ->
                val result = StringBuilder()
                val buffer = CharArray(4096)
                while (true) {
                    val n = reader.read(buffer)
                    if (n < 0) break
                    result.append(buffer,0,n)
                    if (result.length > 1_000_000) throw IllegalStateException("Source response too large")
                }
                result.toString()
            }
            return JSONObject(text)
        } finally { connection.disconnect() }
    }

    private fun usgsFallback(data: JSONObject): List<NewsItem> {
        val features = data.getJSONArray("features")
        val out = mutableListOf<NewsItem>()
        for (i in 0 until minOf(features.length(),MAX)) {
            val feature = features.optJSONObject(i) ?: continue
            val props = feature.optJSONObject("properties") ?: continue
            val id = feature.optString("id").takeIf { it.isNotBlank() } ?: continue
            val mag = props.optDouble("mag", 0.0)
            val place = props.optString("place").ifBlank { "Location not provided" }
            val url = props.optString("url")
            if (!url.startsWith("https://earthquake.usgs.gov/")) continue
            val title = "M${String.format(Locale.US, "%.1f", mag)} · $place"
            out += NewsItem(
                id=id,title=title,
                description="USGS earthquake report: $place; magnitude ${String.format(Locale.US, "%.1f", mag)}.",
                source="USGS",sourceUrl=url,publishedAt=props.optLong("time",0L),
                magnitude=mag,category="earthquake",region=place,
                severity=if(mag>=6.0) "critical" else "normal"
            )
        }
        return out
    }

    private fun backendEvents(data: JSONObject): List<NewsItem> {
        val arr=data.getJSONArray("events")
        val result=mutableListOf<NewsItem>()
        for(i in 0 until minOf(arr.length(),MAX)) {
            val obj=arr.optJSONObject(i) ?: continue
            val id=obj.optString("id")
            val url=obj.optString("url")
            val title=obj.optString("title")
            val source=obj.optString("source")
            val at=obj.optLong("published_at")
            if (id.isBlank() || title.isBlank() || source.isBlank() || at<=0 || !url.startsWith("https://")) continue
            result += NewsItem(
                id=id.take(180),title=title.take(280),
                description=obj.optString("summary").take(750),
                source=source.take(90),
                sourceUrl=url,publishedAt=at*1000,
                category=obj.optString("category", "world"),
                severity=obj.optString("severity", "normal"),
                region=obj.optString("region", "")
            )
        }
        return result
    }

    /** First sync for each source creates a baseline, never flooding old stories as notifications. */
    suspend fun sync(context: Context): List<NewsItem> = withContext(Dispatchers.IO) {
        val base = UserPrefs.newsServer(context)
        val key = if(base.isNotBlank()) "server:$base" else "direct:usgs"
        val data = if(base.isNotBlank()) download("$base/v1/events?limit=$MAX") else download(USGS_URL)
        val current = if(base.isNotBlank()) backendEvents(data) else usgsFallback(data)
        val oldKey=UserPrefs.store(context).getString("baseline_source",null)
        val initialized=oldKey==key
        val oldIds=if(initialized) cached(context).map{ it.id }.toSet() else emptySet()
        write(context,current)
        UserPrefs.store(context).edit().putString("baseline_source",key).apply()
        if(initialized) current.filterNot{ it.id in oldIds } else emptyList()
    }
}
