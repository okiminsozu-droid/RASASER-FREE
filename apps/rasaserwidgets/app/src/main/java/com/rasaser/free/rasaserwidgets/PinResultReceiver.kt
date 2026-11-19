package com.rasaser.free.rasaserwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * requestPinAppWidget does NOT invoke the provider's configure Activity.
 * The launcher's success callback supplies the allocated widget ID, so persist
 * the user's app/icon/photo selection here and render the widget immediately.
 */
class PinResultReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_PIN_CREATED = "com.rasaser.free.rasaserwidgets.PIN_CREATED"
        private const val EXTRA_TOKEN = "varyok.pin.token"
        private const val EXTRA_TARGET = "varyok.pin.target"
        private const val EXTRA_CAPTION = "varyok.pin.caption"
        private const val EXTRA_OPACITY = "varyok.pin.opacity"
        private const val EXTRA_PHOTO = "varyok.pin.photo"

        fun callback(context: Context, token: Int, target: String, caption: String,
                     opacity: Int, hasPhoto: Boolean): PendingIntent {
            val intent = Intent(context, PinResultReceiver::class.java).apply {
                action = ACTION_PIN_CREATED
                data = Uri.parse("varyok://pin-created/$token")
                putExtra(EXTRA_TOKEN, token)
                putExtra(EXTRA_TARGET, target)
                putExtra(EXTRA_CAPTION, caption)
                putExtra(EXTRA_OPACITY, opacity.coerceIn(0, 100))
                putExtra(EXTRA_PHOTO, hasPhoto)
            }
            // Mutable is essential: Android's launcher fills EXTRA_APPWIDGET_ID.
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            return PendingIntent.getBroadcast(context, token, intent, flags)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PIN_CREATED) return
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val token = intent.getIntExtra(EXTRA_TOKEN, Int.MIN_VALUE)
        if (token == Int.MIN_VALUE) return
        try {
            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
            val packageName = intent.getStringExtra(EXTRA_TARGET)?.trim().orEmpty()
            if (packageName.isBlank()) return
            if (!WidgetStore.canCreateFor(context, packageName) &&
                !WidgetStore.exists(context, widgetId)) return

            // Restore optional custom photo before creating the widget's RemoteViews.
            if (intent.getBooleanExtra(EXTRA_PHOTO, false)) {
                WidgetStore.attachStagedPhoto(context, token, widgetId)
                WidgetStore.commitPending(context, widgetId)
            }

            WidgetStore.save(
                context, widgetId,
                WidgetConfig(
                    kind = "app",
                    target = packageName,
                    caption = intent.getStringExtra(EXTRA_CAPTION).orEmpty().take(64),
                    opacity = intent.getIntExtra(EXTRA_OPACITY, 100).coerceIn(0, 100)
                )
            )
            ActionWidgetProvider.render(context, widgetId)
        } catch (_: Exception) {
            // Never report a successful creation when persistence has failed.
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID &&
                !WidgetStore.exists(context, widgetId)) {
                WidgetStore.removePhoto(context, widgetId)
            }
        } finally {
            WidgetStore.discardPending(context, token)
        }
    }
}
