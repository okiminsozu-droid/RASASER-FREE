package com.rasaser.free.rasaserwidgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.rasaser.shared.promo.RaSaSeRPromo
import java.io.File

data class WidgetConfig(
    val kind: String = "app",
    val target: String = "",
    val caption: String = "",
    val opacity: Int = 100
)

/** Private, per-widget local persistence. No backup, account or network. */
object WidgetStore {
    private const val PREFS = "widget_configs"
    private fun p(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun key(id: Int, field: String) = "widget_${id}_${field}"
    private fun photo(ctx: Context, id: Int) = File(ctx.filesDir, "widget_${id}.png")
    private fun pending(ctx: Context, id: Int) = File(ctx.filesDir, "pending_${id}.png")

    fun exists(ctx: Context, id: Int): Boolean = p(ctx).getBoolean(key(id, "exists"), false)
    fun load(ctx: Context, id: Int): WidgetConfig {
        val pref = p(ctx)
        return WidgetConfig(
            kind = pref.getString(key(id, "kind"), "app") ?: "app",
            target = pref.getString(key(id, "target"), "") ?: "",
            caption = pref.getString(key(id, "caption"), "") ?: "",
            opacity = pref.getInt(key(id, "opacity"), 100).coerceIn(0, 100)
        )
    }
    /**
     * Free entitlement is for THREE DIFFERENT INSTALLED APPLICATIONS, not three
     * shortcuts. A second widget for the same app never spends another slot.
     * Migration reads the old successful widget records because earlier builds
     * stored only an aggregate count, which cannot tell repeated apps apart.
     */
    @Synchronized
    private fun usedApplicationPackages(ctx: Context): Set<String> {
        val prefs = p(ctx)
        val field = "free_distinct_app_packages"
        val current = prefs.getStringSet(field, emptySet())?.toSet().orEmpty()
        if (prefs.getBoolean("free_distinct_migrated", false)) return current
        val matcher = Regex("^widget_([0-9]+)_exists$")
        val activeTargets = prefs.all.keys.mapNotNull { fieldKey ->
            val id = matcher.matchEntire(fieldKey)?.groupValues?.get(1)?.toIntOrNull()
                ?: return@mapNotNull null
            if (!exists(ctx, id)) return@mapNotNull null
            val widget = load(ctx, id)
            if (widget.kind == "app") widget.target.trim().lowercase(java.util.Locale.ROOT)
                .takeIf { it.isNotBlank() }
            else null
        }.toSet()
        val migrated = current + activeTargets
        prefs.edit().putStringSet(field, migrated).putBoolean("free_distinct_migrated", true).commit()
        return migrated
    }

    fun creationCount(ctx: Context): Int = usedApplicationPackages(ctx).size
    fun canCreate(ctx: Context): Boolean =
        (OwnerPremium.isActive(ctx) || RaSaSeRPromo.isActive(ctx)) || creationCount(ctx) < 3

    fun canCreateFor(ctx: Context, targetPackage: String): Boolean {
        val target = targetPackage.trim().lowercase(java.util.Locale.ROOT)
        return target.isNotBlank() &&
            (OwnerPremium.isActive(ctx) || RaSaSeRPromo.isActive(ctx) || target in usedApplicationPackages(ctx) || creationCount(ctx) < 3)
    }

    @Synchronized
    fun save(ctx: Context, id: Int, data: WidgetConfig, countCreation: Boolean = true) {
        val firstSave = countCreation && !exists(ctx, id)
        val target = data.target.trim().lowercase(java.util.Locale.ROOT)
        if (firstSave && data.kind == "app" && !canCreateFor(ctx, target))
            throw IllegalStateException("Premium required")
        if (firstSave && data.kind != "app" && !canCreate(ctx))
            throw IllegalStateException("Premium required")

        val existingPackages = usedApplicationPackages(ctx)
        val editor = p(ctx).edit()
            .putBoolean(key(id, "exists"), true)
            .putString(key(id, "kind"), data.kind)
            .putString(key(id, "target"), data.target)
            .putString(key(id, "caption"), data.caption)
            .putInt(key(id, "opacity"), data.opacity.coerceIn(0, 100))
        if (firstSave && data.kind == "app" && target.isNotBlank()) {
            editor.putStringSet("free_distinct_app_packages", existingPackages + target)
        }
        if (!editor.commit()) throw IllegalStateException("Unable to save widget")
    }
    fun delete(ctx: Context, id: Int) {
        p(ctx).edit()
            .remove(key(id, "exists"))
            .remove(key(id, "kind"))
            .remove(key(id, "target"))
            .remove(key(id, "caption"))
            .remove(key(id, "opacity"))
            .apply()
        photo(ctx, id).delete()
        pending(ctx, id).delete()
    }
    fun restore(ctx: Context, oldId: Int, newId: Int) {
        if (!exists(ctx, oldId)) return
        save(ctx, newId, load(ctx, oldId), countCreation = false)
        val old = photo(ctx, oldId)
        if (old.exists()) {
            old.copyTo(photo(ctx, newId), overwrite = true)
        }
        delete(ctx, oldId)
    }
    fun removePhoto(ctx: Context, id: Int) {
        photo(ctx, id).delete()
        pending(ctx, id).delete()
    }
    fun commitPending(ctx: Context, id: Int) {
        val src = pending(ctx, id)
        if (src.exists()) {
            src.copyTo(photo(ctx, id), overwrite = true)
            src.delete()
        }
    }
    fun discardPending(ctx: Context, id: Int) { pending(ctx, id).delete() }
    /** Single-use photo chosen in the preview sheet before Android allocates a widget ID. */
    fun attachStagedPhoto(ctx: Context, sourceId: Int, newWidgetId: Int) {
        val src = pending(ctx, sourceId)
        if (!src.exists()) return
        try {
            src.copyTo(pending(ctx, newWidgetId), overwrite = true)
        } finally {
            src.delete()
        }
    }


    /** Decode selected URI twice to cap memory; write only into app-private storage. */
    fun stageSelectedPhoto(ctx: Context, id: Int, uri: Uri): Boolean {
        return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val probe = ctx.contentResolver.openInputStream(uri) ?: return false
        probe.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
        var sample = 1
        while (bounds.outWidth / sample > 768 || bounds.outHeight / sample > 768) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return false
        val scale = minOf(1f, 512f / maxOf(decoded.width, decoded.height))
        val bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded, (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1), true
            )
        } else decoded
        val ok = pending(ctx, id).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        if (bitmap !== decoded) bitmap.recycle()
        decoded.recycle()
        ok
        } catch (_: Exception) { false }
    }

    fun bitmap(ctx: Context, id: Int, maxSide: Int = 300, usePending: Boolean = false): Bitmap? {
        val file = if (usePending && pending(ctx, id).exists()) pending(ctx, id) else photo(ctx, id)
        if (!file.exists()) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sample = 1
            while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) {
                sample *= 2
            }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply {
                inSampleSize = sample
            }) ?: return null
            val ratio = minOf(1f, maxSide.toFloat() / maxOf(decoded.width, decoded.height))
            if (ratio >= 1f) decoded else Bitmap.createScaledBitmap(
                decoded, (decoded.width * ratio).toInt().coerceAtLeast(1),
                (decoded.height * ratio).toInt().coerceAtLeast(1), true
            ).also { decoded.recycle() }
        } catch (_: Exception) { null }
    }
}
