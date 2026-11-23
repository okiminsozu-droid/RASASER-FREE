package com.rasaser.free.icerikureticisi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Prepares user-chosen images ONLY in local memory, compressed to a conservative
 * <=950KB JPEG for cross-PDS image embed compatibility. No RaSaSeR upload.
 */
object BlueskyPhoto {
    private const val MAX_BLOB = 950_000
    private const val MAX_DIMENSION = 1600
    private val SUPPORTED = setOf("image/jpeg", "image/png", "image/webp")

    fun jpeg(context: Context, uri: Uri): ByteArray {
        require(uri.scheme == "content") { "Yalnızca telefonunuzdan seçtiğiniz resmi kullanın." }
        require(context.contentResolver.getType(uri) in SUPPORTED) {
            "Bluesky için JPG, PNG veya WEBP fotoğraf seçin."
        }
        val resolver = context.contentResolver
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        require(options.outWidth in 1..30000 && options.outHeight in 1..30000) {
            "Bluesky fotoğrafının boyutları okunamadı."
        }
        var sample = 1
        while (max(options.outWidth, options.outHeight) / sample > MAX_DIMENSION * 2) {
            sample *= 2
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null,
                BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                })
        } ?: throw IllegalArgumentException("Bluesky görseli okunamadı.")
        try {
            val factor = minOf(1.0,
                MAX_DIMENSION.toDouble() / max(decoded.width, decoded.height).toDouble())
            var bitmap = if (factor < 1.0) Bitmap.createScaledBitmap(
                decoded, max(1, (decoded.width * factor).toInt()),
                max(1, (decoded.height * factor).toInt()), true
            ) else decoded

            try {
                repeat(5) {
                    val canvasBitmap = Bitmap.createBitmap(
                        bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888
                    )
                    try {
                        Canvas(canvasBitmap).apply {
                            drawColor(Color.WHITE)
                            drawBitmap(bitmap, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
                        }
                        for (quality in listOf(86, 73, 60, 45)) {
                            val bytes = ByteArrayOutputStream().use { stream ->
                                check(canvasBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream))
                                stream.toByteArray()
                            }
                            if (bytes.size <= MAX_BLOB) return bytes
                        }
                    } finally {
                        canvasBitmap.recycle()
                    }
                    val reduced = Bitmap.createScaledBitmap(
                        bitmap, max(1, bitmap.width * 3 / 4),
                        max(1, bitmap.height * 3 / 4), true
                    )
                    if (bitmap !== decoded) bitmap.recycle()
                    bitmap = reduced
                }
                throw IllegalArgumentException("Görsel sıkıştırılamadı. Daha küçük fotoğraf seçin.")
            } finally {
                if (bitmap !== decoded) bitmap.recycle()
            }
        } finally {
            decoded.recycle()
        }
    }
}
