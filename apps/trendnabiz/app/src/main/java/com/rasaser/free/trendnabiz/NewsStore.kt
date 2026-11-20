package com.rasaser.free.trendnabiz

import android.content.Context
import android.content.SharedPreferences

/** Ephemeral event model: the application never writes news to SharedPreferences, SQLite or files. */
data class NewsItem(
    val id: String,
    val title: String,
    val description: String,
    val source: String,
    val sourceUrl: String,
    val publishedAt: Long,
    val magnitude: Double = 0.0,
    val category: String = "world",
    val severity: String = "normal",
    val region: String = ""
) {
    val critical: Boolean get() = severity == "critical"
}

object UserPrefs {
    private const val NAME = "trendnabiz_prefs"
    fun store(context: Context): SharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    fun language(context: Context): UiLang = UiLang.parse(store(context).getString("language","tr"))
    fun setLanguage(context: Context, value: UiLang) = store(context).edit().putString("language",value.code).apply()
    fun criticalOnly(context: Context): Boolean = store(context).getBoolean("critical_only",false)
    fun setCriticalOnly(context: Context, value: Boolean) = store(context).edit().putBoolean("critical_only",value).apply()
    fun isPremium(context: Context): Boolean =
        BuildConfig.DEBUG && store(context).getBoolean("debug_premium_preview",false)
    fun setDebugPremium(context: Context, enabled: Boolean) {
        if (BuildConfig.DEBUG) store(context).edit().putBoolean("debug_premium_preview", enabled).apply()
    }
    /** Remove legacy cached event data from earlier development previews. */
    fun clearLegacyNewsCache(context: Context) {
        store(context).edit()
            .remove("news_cache").remove("baseline_ready").remove("baseline_source")
            .remove("news_server").apply()
    }
}
