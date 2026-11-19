package com.rasaser.free.rasaserwidgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle

/** Explicit, private widget click target: resolves the saved launchable app. */
class AppShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID && WidgetStore.exists(this, id)) {
            val widget = WidgetStore.load(this, id)
            if (widget.kind == "app" && widget.target.isNotBlank()) {
                try {
                    val launchIntent = packageManager.getLaunchIntentForPackage(widget.target)
                        ?: run {
                            val query = Intent(Intent.ACTION_MAIN)
                                .addCategory(Intent.CATEGORY_LAUNCHER)
                                .setPackage(widget.target)
                            @Suppress("DEPRECATION")
                            val match = packageManager.queryIntentActivities(query, 0).firstOrNull()
                            match?.activityInfo?.let { info ->
                                Intent(Intent.ACTION_MAIN)
                                    .addCategory(Intent.CATEGORY_LAUNCHER)
                                    .setClassName(info.packageName, info.name)
                            }
                        }
                    launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)?.let { startActivity(it) }
                } catch (_: Exception) {
                    // A removed/disabled app must not crash the launcher or VARYOK.
                }
            }
        }
        finish()
    }
}
