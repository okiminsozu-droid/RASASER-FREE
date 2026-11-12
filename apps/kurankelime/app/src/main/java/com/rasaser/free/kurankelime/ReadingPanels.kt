package com.rasaser.free.kurankelime

import android.graphics.Bitmap
import android.graphics.RectF
import kotlin.math.ceil
import kotlin.math.floor

/**
 * One reading screen = one original Quran text panel, NEVER four panels.
 *
 * The unchanged source package has 304 PDF raster pages:
 *   PDF 1–2: one full-size panel each.
 *   PDF 3–303: four panels each, read top-left, top-right, bottom-left, bottom-right.
 *   PDF 304: two upper panels (Ikhlas/Falaq, Falaq/Nas).
 *
 * Total 2 + 301*4 + 2 = 1208 screens.
 * Source word coordinates remain in 595 x 842 parent-PDF units.
 */
internal object ReadingPanels {
    const val COUNT = 1208
    private const val FULL_WIDTH = 595f
    private const val FULL_HEIGHT = 842f

    data class Slot(
        val ordinal: Int,
        val pdfPage: Int,
        val quadrant: Int,
        val crop: RectF
    )

    fun firstScreen(pdfPage: Int): Int = when (pdfPage.coerceIn(1, 304)) {
        1 -> 1
        2 -> 2
        304 -> 1207
        else -> 3 + (pdfPage - 3) * 4
    }

    fun slot(ordinal: Int): Slot {
        val n = ordinal.coerceIn(1, COUNT)
        if (n <= 2) return Slot(n, n, -1, RectF(0f, 0f, 1f, 1f))
        val parent = if (n >= 1207) 304 else 3 + (n - 3) / 4
        val quadrant = if (n >= 1207) n - 1207 else (n - 3) % 4
        // Cropping includes the narrow page borders and all letter descenders.
        // The source panels use the same position on every composite raster.
        val left = if (quadrant % 2 == 0) 0.037f else 0.505f
        val right = if (quadrant % 2 == 0) 0.489f else 0.959f
        val top = if (quadrant < 2) 0.082f else 0.515f
        val bottom = if (quadrant < 2) 0.515f else 0.945f
        return Slot(n, parent, quadrant, RectF(left, top, right, bottom))
    }

    /** Convert a precisely located OCR hit to its corresponding single-panel screen. */
    fun screenAt(pdfPage: Int, x: Float, y: Float): Int {
        val parent = pdfPage.coerceIn(1, 304)
        if (parent < 3) return parent
        val column = if (x / FULL_WIDTH >= 0.5f) 1 else 0
        if (parent == 304) return firstScreen(parent) + column
        val row = if (y / FULL_HEIGHT >= 0.515f) 1 else 0
        return firstScreen(parent) + row * 2 + column
    }

    fun toSheetX(slot: Slot, localFraction: Float): Float =
        (slot.crop.left + localFraction * slot.crop.width()) * FULL_WIDTH

    fun toSheetY(slot: Slot, localFraction: Float): Float =
        (slot.crop.top + localFraction * slot.crop.height()) * FULL_HEIGHT

    fun panelX(slot: Slot, sheetX: Float): Float =
        ((sheetX / FULL_WIDTH - slot.crop.left) / slot.crop.width())

    fun panelY(slot: Slot, sheetY: Float): Float =
        ((sheetY / FULL_HEIGHT - slot.crop.top) / slot.crop.height())

    /**
     * Decode existing 304 WebP assets once and crop the one visible panel in RAM.
     * No new bundled images; no duplicated APK assets; caller owns the returned bitmap.
     */
    fun crop(original: Bitmap, slot: Slot): Bitmap {
        if (slot.quadrant < 0) return original
        val region = slot.crop
        val l = floor(region.left * original.width).toInt().coerceIn(0, original.width - 1)
        val t = floor(region.top * original.height).toInt().coerceIn(0, original.height - 1)
        val r = ceil(region.right * original.width).toInt().coerceIn(l + 1, original.width)
        val b = ceil(region.bottom * original.height).toInt().coerceIn(t + 1, original.height)
        // Create an independent cropped bitmap rather than holding the whole 4-panel raster.
        val output = Bitmap.createBitmap(r - l, b - t, Bitmap.Config.RGB_565)
        val canvas = android.graphics.Canvas(output)
        canvas.drawBitmap(
            original,
            android.graphics.Rect(l, t, r, b),
            android.graphics.Rect(0, 0, output.width, output.height),
            null
        )
        return output
    }
}
