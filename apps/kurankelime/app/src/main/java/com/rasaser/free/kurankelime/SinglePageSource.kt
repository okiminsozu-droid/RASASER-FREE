package com.rasaser.free.kurankelime

import android.content.Context
import android.graphics.RectF
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

/**
 * The source PDF has 304 composite sheets but 605 *complete* reading pages.
 * Each complete page is assembled losslessly from the original embedded images.
 * No generative text, OCR retyping or enlarged four-panel raster is used.
 *
 * Word coordinates are still stored in original 595x842 PDF-sheet space,
 * so this class is the only adapter between the original search and the new pages.
 */
internal class SinglePageSource(context: Context) {
    data class Span(
        val sourcePanel: Int,
        val sheetBox: RectF,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    ) {
        fun distanceToSheet(px: Float, py: Float): Float {
            val dx = when {
                px < sheetBox.left -> sheetBox.left - px
                px > sheetBox.right -> px - sheetBox.right
                else -> 0f
            }
            val dy = when {
                py < sheetBox.top -> sheetBox.top - py
                py > sheetBox.bottom -> py - sheetBox.bottom
                else -> 0f
            }
            return dx * dx + dy * dy
        }
        fun includesPixel(px: Float, py: Float): Boolean =
            px >= x && px <= x + width && py >= y && py <= y + height
    }

    data class Entry(
        val number: Int,
        val sourcePdfPage: Int,
        val width: Float,
        val height: Float,
        val spans: List<Span>
    ) {
        val assetName: String get() =
            "quran/single_pages/page_" + number.toString().padStart(4, '0') + ".webp"
    }

    val pages: List<Entry>
    val count: Int get() = pages.size
    private val bySource: Map<Int,List<Entry>>

    init {
        val root = context.assets.open("quran/single_pages/pages.json").bufferedReader(Charsets.UTF_8)
            .use { JSONObject(it.readText()) }
        require(root.getInt("page_count") == 605) { "605 kaynak sayfa bekleniyor" }
        val data = root.getJSONArray("pages")
        val parsed = ArrayList<Entry>(605)
        for (i in 0 until data.length()) {
            val page = data.getJSONObject(i)
            val size = page.getJSONArray("dimensions")
            val segments = page.getJSONArray("segments")
            val spans = ArrayList<Span>(segments.length())
            for (j in 0 until segments.length()) {
                val seg = segments.getJSONObject(j)
                val coords = seg.getJSONArray("source_bbox")
                val pixels = seg.getJSONArray("source_pixels")
                val origin = seg.getJSONArray("output_origin")
                spans.add(
                    Span(
                        seg.getInt("panel_index"),
                        RectF(coords.getDouble(0).toFloat(),coords.getDouble(1).toFloat(),
                            coords.getDouble(2).toFloat(),coords.getDouble(3).toFloat()),
                        origin.getDouble(0).toFloat(),origin.getDouble(1).toFloat(),
                        pixels.getDouble(0).toFloat(),pixels.getDouble(1).toFloat()
                    )
                )
            }
            parsed.add(
                Entry(page.getInt("n"),page.getInt("source_pdf_page"),
                    size.getDouble(0).toFloat(),size.getDouble(1).toFloat(),spans)
            )
        }
        require(parsed.size == 605 && parsed.indices.all { parsed[it].number == it + 1 })
        pages = parsed
        bySource = pages.groupBy { it.sourcePdfPage }
    }

    fun page(number: Int): Entry = pages[number.coerceIn(1,count) - 1]

    fun firstForPdf(pdfPage: Int): Int =
        bySource[pdfPage]?.firstOrNull()?.number ?: 1

    /**
     * Navigate from an OCR search result to the *whole* page containing it.
     * On four-panel PDF sheets the upper two blocks are one page, the lower two
     * blocks the next page. Source x/y must not be discarded.
     */
    fun atPdf(pdfPage: Int, x: Float, y: Float): Int {
        val candidates = bySource[pdfPage] ?: return 1
        return candidates.minByOrNull { page ->
            page.spans.minOf { it.distanceToSheet(x,y) }
        }?.number ?: candidates.first().number
    }

    /** Convert tap on lossless 605-page image back to original offline SQLite coordinates. */
    fun tapPosition(entry: Entry, fractionalX: Float, fractionalY: Float): Pair<Float,Float>? {
        val px = fractionalX * entry.width
        val py = fractionalY * entry.height
        val span = entry.spans.firstOrNull { it.includesPixel(px,py) } ?: return null
        val bx = span.sheetBox
        return (
            bx.left + ((px-span.x)/span.width)*bx.width()
        ) to (
            bx.top + ((py-span.y)/span.height)*bx.height()
        )
    }

    /** Convert a PDF-sheet word box to a normalized rectangle on its true single page. */
    fun highlight(entry: Entry, word: PageWord): RectF? {
        if (word.page != entry.sourcePdfPage) return null
        val cx = word.x + word.width*0.5f
        val cy = word.y + word.height*0.5f
        val span = entry.spans.minByOrNull { it.distanceToSheet(cx,cy) } ?: return null
        val b = span.sheetBox
        if (b.width() <= 0f || b.height() <= 0f) return null
        val left = (span.x + (word.x-b.left)/b.width()*span.width)/entry.width
        val top = (span.y + (word.y-b.top)/b.height()*span.height)/entry.height
        val right = (span.x+(word.x+word.width-b.left)/b.width()*span.width)/entry.width
        val bottom = (span.y+(word.y+word.height-b.top)/b.height()*span.height)/entry.height
        return RectF(
            min(left,right).coerceIn(0f,1f), min(top,bottom).coerceIn(0f,1f),
            max(left,right).coerceIn(0f,1f), max(top,bottom).coerceIn(0f,1f)
        )
    }
}
