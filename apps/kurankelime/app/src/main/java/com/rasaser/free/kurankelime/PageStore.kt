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
    /**
     * Search-result bookmarks are separate from the original page-favorites set.
     * Persist OCR token IDs (and Fatiha preview IDs) without altering page bookmarks.
     */
    fun wordFavorites(): List<Int> =
        (prefs.getStringSet("word_favorites", emptySet()) ?: emptySet())
            .mapNotNull { it.toIntOrNull() }.filter { it != 0 }.distinct().sorted()

    fun isWordFavorite(wordId: Int): Boolean =
        wordId != 0 && prefs.getStringSet("word_favorites", emptySet())
            ?.contains(wordId.toString()) == true

    fun toggleWordFavorite(wordId: Int) {
        if (wordId == 0) return
        val items = (prefs.getStringSet("word_favorites", emptySet()) ?: emptySet()).toMutableSet()
        if (!items.add(wordId.toString())) items.remove(wordId.toString())
        prefs.edit().putStringSet("word_favorites", items).apply()
    }

}
