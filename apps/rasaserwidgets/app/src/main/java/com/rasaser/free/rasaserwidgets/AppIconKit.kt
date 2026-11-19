package com.rasaser.free.rasaserwidgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.util.LruCache

/** Small, bounded bitmap cache; never stores installed app inventory on disk. */
object AppIconKit {
    private val cache = object : LruCache<String, Bitmap>(48) {}
    fun bitmap(ctx: Context, packageName: String, side: Int = 88): Bitmap? {
        if (packageName.isBlank()) return null
        val key = "$packageName@$side"
        synchronized(cache) { cache.get(key)?.let { return it } }
        return try {
            val drawable = ctx.packageManager.getApplicationIcon(packageName)
            val bmp = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, side, side)
            drawable.draw(canvas)
            synchronized(cache) { cache.put(key, bmp) }
            bmp
        } catch (_: Exception) { null }
    }
}
