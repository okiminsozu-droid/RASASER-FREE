package com.rasaser.free.rasaserwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews

/** Rebuilds complete RemoteViews, including image, on every change to avoid stale bitmaps. */
class ActionWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { render(context, it) }
    }
    override fun onAppWidgetOptionsChanged(
        context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle
    ) {
        render(context, appWidgetId)
    }
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetStore.delete(context, it) }
    }
    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        oldWidgetIds.zip(newWidgetIds).forEach { (old, new) ->
            WidgetStore.restore(context, old, new)
            render(context, new)
        }
    }

    companion object {
        fun render(context: Context, id: Int) {
            if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
            val data = WidgetStore.load(context, id)
            val manager = AppWidgetManager.getInstance(context)
            val rv = RemoteViews(context.packageName, R.layout.widget_tile)
            val options = manager.getAppWidgetOptions(id)
            val wDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 80)
            val hDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80)
            val side = (minOf(wDp, hDp) * context.resources.displayMetrics.density)
                .toInt().coerceIn(48, 300)
            val picture = WidgetStore.bitmap(context, id, side)
            if (picture != null) rv.setImageViewBitmap(R.id.widget_image, picture)
            else rv.setImageViewResource(R.id.widget_image, R.drawable.ic_brand)
            val alpha = (data.opacity.coerceIn(0, 100) * 255 / 100)
            rv.setInt(R.id.widget_image, "setImageAlpha", alpha)
            rv.setTextViewText(R.id.widget_label, data.caption)
            rv.setViewVisibility(R.id.widget_label, if (data.caption.isBlank()) View.GONE else View.VISIBLE)
            rv.setTextColor(R.id.widget_label, Color.argb(alpha, 244, 235, 213))
            val clickIntent = when (data.kind) {
                "app" -> context.packageManager.getLaunchIntentForPackage(data.target)
                    ?: Intent(context, MainActivity::class.java)
                "url" -> Intent(Intent.ACTION_VIEW, Uri.parse(data.target))
                "dial" -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(data.target)))
                "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
                "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                "settings" -> Intent(Settings.ACTION_SETTINGS)
                "note", "torch" -> Intent(context, ActionActivity::class.java)
                    .putExtra(ActionActivity.EXTRA_WIDGET_ID, id)
                    .putExtra(ActionActivity.EXTRA_ACTION, data.kind)
                else -> Intent(context, MainActivity::class.java)
            }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pending = PendingIntent.getActivity(context, id, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            rv.setOnClickPendingIntent(R.id.widget_root, pending)
            manager.updateAppWidget(id, rv)
        }
    }
}
