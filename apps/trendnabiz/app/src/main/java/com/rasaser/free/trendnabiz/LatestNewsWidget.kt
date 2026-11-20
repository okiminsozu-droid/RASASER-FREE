package com.rasaser.free.trendnabiz

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class LatestNewsWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) update(context, manager, id)
    }

    companion object {
        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val remote = RemoteViews(context.packageName, R.layout.trend_widget)
            val premium = UserPrefs.isPremium(context)
            val latest = NewsStore.cached(context).firstOrNull()
            remote.setTextViewText(
                R.id.widget_headline,
                if (!premium) context.getString(R.string.widget_locked)
                else latest?.title ?: context.getString(R.string.widget_placeholder)
            )
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (!premium) putExtra(NewsNotifications.EXTRA_ACTION, "premium")
            }
            remote.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(
                context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))
            manager.updateAppWidget(id, remote)
        }
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, LatestNewsWidget::class.java))
            for (id in ids) update(context, manager, id)
        }
    }
}
