package com.rasaser.free.kurankelime

import android.content.Context

class PageStore(context: Context) {
    private val prefs = context.getSharedPreferences("quran_pdf_pages", Context.MODE_PRIVATE)
    fun lastPage(): Int = prefs.getInt("last", 1).coerceIn(1, 304)
    fun setLastPage(page: Int) {
        if (page in 1..304) prefs.edit().putInt("last", page).apply()
    }
    fun favorites(): List<Int> = (prefs.getStringSet("favorites", emptySet()) ?: emptySet())
        .mapNotNull { it.toIntOrNull() }.filter { it in 1..304 }.distinct().sorted()
    fun isFavorite(page: Int) = page in favorites()
    fun toggleFavorite(page: Int) {
        require(page in 1..304)
        val pages = favorites().map(Int::toString).toMutableSet()
        if (!pages.add(page.toString())) pages.remove(page.toString())
        prefs.edit().putStringSet("favorites", pages).apply()
    }
}
