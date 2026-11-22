package com.rasaser.free.icerikureticisi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Local artwork only. The app does not require storage or INTERNET permissions. */
object LocalLogos {
    private fun dir(context: Context): File =
        File(context.filesDir, "social_icons").also { it.mkdirs() }

    fun importImage(context: Context, uri: Uri): String {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw IllegalArgumentException("Logo okunamadı.")
        require(bounds.outWidth in 1..16000 && bounds.outHeight in 1..16000) {
            "Logo boyutları desteklenmiyor."
        }
        var sample = 1
        while (bounds.outWidth / sample > 512 || bounds.outHeight / sample > 512) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IllegalArgumentException("Görsel biçimi desteklenmiyor.")
        val maxEdge = maxOf(bitmap.width, bitmap.height)
        val factor = minOf(1f, 256f / maxEdge)
        val small = Bitmap.createScaledBitmap(
            bitmap, (bitmap.width * factor).toInt().coerceAtLeast(1),
            (bitmap.height * factor).toInt().coerceAtLeast(1), true
        )
        val file = File(dir(context), "${UUID.randomUUID()}.png")
        try {
            FileOutputStream(file).use { stream ->
                check(small.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    "Logo kaydedilemedi."
                }
            }
        } finally {
            if (small !== bitmap) bitmap.recycle()
            small.recycle()
        }
        return file.absolutePath
    }

    fun load(context: Context, path: String): Bitmap? {
        val file = checkedFile(context, path) ?: return null
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    fun delete(context: Context, path: String) {
        checkedFile(context, path)?.delete()
    }

    private fun checkedFile(context: Context, path: String): File? {
        if (path.isBlank()) return null
        val folder = dir(context).canonicalFile
        val file = File(path).canonicalFile
        return file.takeIf {
            it.parentFile == folder && it.extension.equals("png", true)
        }
    }
}
