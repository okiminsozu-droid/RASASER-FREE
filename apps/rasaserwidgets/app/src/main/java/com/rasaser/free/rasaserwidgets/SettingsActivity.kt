package com.rasaser.free.rasaserwidgets

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Settings now contains *only* the user-created widgets with their icons and delete actions.
 * Native launcher placement belongs to the Android home screen; removal requires the launcher.
 */
class SettingsActivity : Activity() {
    private val pink = 0xFFF5C8D5.toInt()
    private val powder = 0xFFFFE8EF.toInt()
    private val ink = 0xFF812643.toInt()
    private fun dp(n: Int) = (resources.displayMetrics.density * n).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = pink
        window.navigationBarColor = pink
    }
    override fun onResume() {
        super.onResume()
        drawWidgets()
    }
    private fun drawWidgets() {
        val scroll = ScrollView(this).apply { setBackgroundColor(pink) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(22), dp(17), dp(12))
        }
        scroll.addView(column)
        setContentView(scroll)
        val ids = AppWidgetManager.getInstance(this)
            .getAppWidgetIds(ComponentName(this, ActionWidgetProvider::class.java))
            .filter { WidgetStore.exists(this, it) }
            .sorted()
        ids.forEach { id ->
            val data = WidgetStore.load(this, id)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(13), dp(9), dp(11), dp(9))
                background = GradientDrawable().apply {
                    setColor(powder)
                    cornerRadius = dp(18).toFloat()
                }
            }
            val icon = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
            val photo = WidgetStore.bitmap(this, id, 96)
            when {
                photo != null -> icon.setImageBitmap(photo)
                data.kind == "app" -> {
                    val image = AppIconKit.bitmap(this@SettingsActivity, data.target, 96)
                    if (image != null) icon.setImageBitmap(image)
                    else icon.setImageResource(R.drawable.ic_brand)
                }
                else -> icon.setImageResource(R.drawable.ic_brand)
            }
            row.addView(icon, LinearLayout.LayoutParams(dp(52), dp(52)))
            val name = TextView(this).apply {
                text = data.caption.ifBlank { if (data.kind == "app") data.target else "Widget #$id" }
                textSize = 16f
                setTextColor(ink)
                setSingleLine(true)
                ellipsize = android.text.TextUtils.TruncateAt.END
                setOnClickListener {
                    startActivity(Intent(this@SettingsActivity, WidgetEditorActivity::class.java)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                }
            }
            row.addView(name, LinearLayout.LayoutParams(0, dp(55), 1f).apply {
                leftMargin = dp(13)
            })
            val delete = ImageView(this).apply {
                setImageResource(R.drawable.ic_delete)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                contentDescription = CatalogText.t(this@SettingsActivity, "deleteConfirm")
                setOnClickListener { confirmDelete(id) }
            }
            row.addView(delete, LinearLayout.LayoutParams(dp(47), dp(47)))
            row.setOnClickListener {
                startActivity(Intent(this, WidgetEditorActivity::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
            }
            column.addView(row, LinearLayout.LayoutParams(-1, dp(76)).apply {
                bottomMargin = dp(10)
            })
        }
    }
    private fun confirmDelete(id: Int) {
        AlertDialog.Builder(this)
            .setTitle(CatalogText.t(this, "deleteTitle"))
            .setMessage(CatalogText.t(this, "deleteMessage"))
            .setNegativeButton(VaryokText.t(this, "cancel"), null)
            .setPositiveButton(CatalogText.t(this, "deleteConfirm")) { _, _ ->
                WidgetStore.delete(this, id)
                // Clears the widget's image and tap target. The launcher owns placement.
                ActionWidgetProvider.render(this, id)
                drawWidgets()
            }.show()
    }
}
