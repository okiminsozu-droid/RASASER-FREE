package com.rasaser.free.kurankelime

import android.content.Context
import java.text.Normalizer
import java.util.Locale
import java.util.zip.GZIPInputStream

/** PDF pages remain pixel-identical; positions are derived from the original word gloss text layer.
 *  This optional private asset is bundled only in a locally assembled test APK, not in the public repository.
 */
data class PageWord(
    val id: Int,
    val page: Int,
    val source: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val gloss: String,
    val searchable: String
)

class PageWordIndex(private val context: Context) {
    private val searchableLocale = Locale.forLanguageTag("tr-TR")
    private val lines: List<PageWord> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { readPrivateIndex() }
    private val grouped: Map<Int, List<PageWord>> by lazy { lines.groupBy { it.page } }
    private val byId: Map<Int, PageWord> by lazy { lines.associateBy { it.id } }

    val hasFullIndex: Boolean
        get() = try {
            context.assets.open("quran/word_positions.tsv.gz").close()
            true
        } catch (_: Exception) { false }

    private fun key(value: String): String {
        val normalized = Normalizer.normalize(value.lowercase(searchableLocale), Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{M}+"), "")
            .replace('ı', 'i').replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
    }

    private fun readPrivateIndex(): List<PageWord> {
        val rows = ArrayList<PageWord>(90000)
        try {
            GZIPInputStream(context.assets.open("quran/word_positions.tsv.gz"))
                .bufferedReader(Charsets.UTF_8).use { reader ->
                    reader.readLine() // TSV header
                    var serial = 1
                    while (true) {
                        val line = reader.readLine() ?: break
                        val p = line.split('\t', limit = 7)
                        if (p.size != 7) continue
                        val page = p[0].toIntOrNull() ?: continue
                        val source = p[1].toIntOrNull() ?: continue
                        val x = p[2].toFloatOrNull() ?: continue
                        val y = p[3].toFloatOrNull() ?: continue
                        val width = p[4].toFloatOrNull() ?: continue
                        val height = p[5].toFloatOrNull() ?: continue
                        if (page !in 1..304 || width <= 0 || height <= 0) continue
                        val gloss = p[6].trim('"').trim()
                        if (gloss.isBlank()) continue
                        rows.add(PageWord(serial++, page, source, x, y, width, height, gloss, key(gloss)))
                    }
                }
        } catch (_: Exception) {
            return emptyList() // Public source preview has no index; no network dependency.
        }
        return rows
    }

    /** Search Turkish word meanings from all pages. No OCR, web connection, or unrelated meali. */
    fun search(query: String, limit: Int = 250): List<PageWord> {
        val q = key(query)
        if (q.isBlank()) return emptyList()
        val terms = q.split(' ').filter { it.isNotBlank() }
        if (terms.isEmpty()) return emptyList()
        return lines.asSequence()
            .filter { word -> terms.all { t -> word.searchable.contains(t) } }
            .take(limit)
            .toList()
    }

    fun byId(id: Int): PageWord? = byId[id]

    /** Tap selection includes the Arabic word immediately above its Turkish gloss. */
    fun nearestWord(page: Int, x: Float, y: Float): PageWord? {
        val candidates = grouped[page] ?: return null
        return candidates.asSequence().mapNotNull { word ->
            val left = word.x - 7f
            val right = word.x + word.width + 7f
            val top = word.y - 22f
            val bottom = word.y + word.height + 7f
            if (x < left || x > right || y < top || y > bottom) null
            else {
                val centerX = word.x + word.width * .5f
                val centerY = word.y + word.height * .5f
                word to ((x-centerX)*(x-centerX) + (y-centerY)*(y-centerY))
            }
        }.minByOrNull { it.second }?.first
    }
}
