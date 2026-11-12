package com.rasaser.free.kurankelime

import android.content.Context

class PageStore(context: Context) {
    private val prefs = context.getSharedPreferences("quran_pdf_pages", Context.MODE_PRIVATE)
    fun lastPage(): Int = prefs.getInt("last", 1).coerceIn(1, 304)
    // New individual-panel progress without replacing existing PDF page progress.
    fun lastScreen(): Int {
        val value = prefs.getInt("last_panel_screen", 0)
        return if (value in 1..ReadingPanels.COUNT) value
        else ReadingPanels.firstScreen(lastPage())
    }
    fun setLastScreen(ordinal: Int) {
        if (ordinal in 1..ReadingPanels.COUNT)
            prefs.edit().putInt("last_panel_screen", ordinal).apply()
    }
    // Save new 605-page reading position separately. Old 1,208-panel and PDF
    // progress/favorites remain untouched and can still be restored.
    fun lastSinglePage(): Int = prefs.getInt("last_single_page", 0)
    fun setLastSinglePage(ordinal: Int) {
        if (ordinal in 1..605) prefs.edit().putInt("last_single_page",ordinal).apply()
    }
    fun oldScreenAsSinglePage(): Int {
        val old = lastScreen()
        return when {
            old <= 2 -> old
            old >= 1207 -> 605
            else -> 3 + (old - 3) / 2
        }
    }
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
