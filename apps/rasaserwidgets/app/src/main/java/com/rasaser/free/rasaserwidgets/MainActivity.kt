package com.rasaser.free.rasaserwidgets

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Only one home action; all widget types are in its chooser. */
class MainActivity : Activity() {
    private val pink = 0xFFF5C8D5.toInt()
    private val powder = 0xFFFFE8EF.toInt()
    private val ink = 0xFF812643.toInt()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun tx(key: String) = VaryokText.t(this, key)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }
    override fun onResume() {
        super.onResume()
        render()
    }
    private fun surface() = GradientDrawable().apply {
        setColor(powder)
        cornerRadius = dp(20).toFloat()
        setStroke(dp(1), 0xFFE7A9BC.toInt())
    }
    private fun text(value: String, size: Float, weight: Boolean = false) =
        TextView(this).apply {
            this.text = value
            textSize = size
            setTextColor(ink)
            typeface = Typeface.create("sans-serif", if (weight) Typeface.BOLD else Typeface.NORMAL)
            gravity = Gravity.CENTER
        }

    private fun render() {
        window.statusBarColor = pink
        window.navigationBarColor = pink
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val scroll = ScrollView(this).apply {
            setBackgroundColor(pink)
            isFillViewport = true
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(19), dp(20), dp(25))
        }
        scroll.addView(root)
        setContentView(scroll)

        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }
        root.addView(tools, LinearLayout.LayoutParams(-1, dp(55)))
        val languageIndex = VaryokText.codes.indexOf(VaryokText.language(this)).coerceAtLeast(0)
        val language = text(VaryokText.names[languageIndex] + "  ▾", 15f, true).apply {
            background = surface()
            setPadding(dp(6), dp(8), dp(6), dp(8))
            contentDescription = tx("lang")
            setOnClickListener { showLanguagePicker() }
        }
        tools.addView(language, LinearLayout.LayoutParams(0, dp(51), 1f))
        val settings = text("⚙", 28f, true).apply {
            background = surface()
            contentDescription = tx("settings")
            setOnClickListener { showSettings() }
        }
        tools.addView(settings, LinearLayout.LayoutParams(dp(52), dp(51)).apply {
            leftMargin = dp(12)
        })

        root.addView(TextView(this), LinearLayout.LayoutParams(1, 0, 1f))
        root.addView(text("▦", 72f, true))
        root.addView(text("VARYOK", 44f, true))
        root.addView(text("RaSaSeR", 21f))
        root.addView(text("APPS & TECHNOLOGY", 12f).apply { letterSpacing = 0.13f })
        root.addView(TextView(this), LinearLayout.LayoutParams(1, dp(35)))

        val create = text("＋  " + tx("create"), 23f, true).apply {
            background = surface()
            elevation = dp(3).toFloat()
            contentDescription = tx("create")
            setOnClickListener { showWidgetTypes() }
        }
        root.addView(create, LinearLayout.LayoutParams(-1, dp(78)))

        root.addView(TextView(this), LinearLayout.LayoutParams(1, 0, 1f))
        root.addView(text(tx("premium_note"), 14f))
        root.addView(text(tx("quota").replace("%d",
            (3 - WidgetStore.creationCount(this)).coerceAtLeast(0).toString()), 15f, true).apply {
            background = surface()
            setPadding(dp(10), dp(14), dp(10), dp(14))
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        root.addView(TextView(this), LinearLayout.LayoutParams(1, dp(26)))
        root.addView(text(tx("site"), 16f, true).apply {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            contentDescription = tx("site")
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.okiminsozu.com")))
            }
        })
    }

    private fun showLanguagePicker() {
        AlertDialog.Builder(this).setTitle(tx("lang"))
            .setSingleChoiceItems(VaryokText.names.toTypedArray(),
                VaryokText.codes.indexOf(VaryokText.language(this))) { dialog, index ->
                VaryokText.setLanguage(this, VaryokText.codes[index])
                dialog.dismiss()
                render()
            }.setNegativeButton(tx("cancel"), null).show()
    }

    private fun showSettings() {
        AlertDialog.Builder(this).setTitle(tx("settings"))
            .setItems(arrayOf(tx("manage"), tx("lang"), tx("privacy"), tx("about"))) { _, index ->
                when (index) {
                    0 -> manageWidgets()
                    1 -> showLanguagePicker()
                    2 -> info(tx("privacy"), tx("privacy_desc"))
                    3 -> info(tx("about"), "VARYOK • RaSaSeR • APPS & TECHNOLOGY")
                }
            }.setNegativeButton(tx("cancel"), null).show()
    }
    private fun info(title: String, body: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(body)
            .setPositiveButton(tx("ok"), null).show()
    }
    private fun manageWidgets() {
        val ids = AppWidgetManager.getInstance(this)
            .getAppWidgetIds(ComponentName(this, ActionWidgetProvider::class.java)).sorted()
        if (ids.isEmpty()) { info(tx("manage"), tx("empty")); return }
        val labels = ids.map { id ->
            WidgetStore.load(this, id).caption.ifBlank { "Widget #$id" } + " • " + tx("edit")
        }
        AlertDialog.Builder(this).setTitle(tx("manage"))
            .setItems(labels.toTypedArray()) { _, index ->
                startActivity(Intent(this, WidgetEditorActivity::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, ids[index]))
            }.setNegativeButton(tx("cancel"), null).show()
    }
    private fun showWidgetTypes() {
        if (!WidgetStore.canCreate(this)) { info(tx("premium"), tx("premium_desc")); return }
        val choices = arrayOf("photo", "transparent", "app", "web", "templates", "dial",
            "note", "wifi", "bt", "system", "torch")
        AlertDialog.Builder(this).setTitle(tx("choose"))
            .setItems(choices.map { tx(it) }.toTypedArray()) { _, index ->
                when (choices[index]) {
                    "photo" -> pin("none")
                    "transparent" -> pin("app", 0)
                    "app" -> pin("app")
                    "web" -> pin("url")
                    "templates" -> showTemplates()
                    "dial" -> pin("dial")
                    "note" -> pin("note")
                    "wifi" -> pin("wifi")
                    "bt" -> pin("bluetooth")
                    "system" -> pin("settings")
                    "torch" -> pin("torch")
                }
            }.setNegativeButton(tx("cancel"), null).show()
    }
    private fun showTemplates() {
        AlertDialog.Builder(this).setTitle(tx("template_name"))
            .setItems(arrayOf(tx("simple"), tx("photo"), tx("transparent"))) { _, index ->
                when (index) {
                    0 -> pin("none")
                    1 -> pin("none")
                    2 -> pin("none", 0)
                }
            }.setNegativeButton(tx("cancel"), null).show()
    }
    private fun pin(kind: String, opacity: Int = 100) {
        if (!WidgetStore.canCreate(this)) { info(tx("premium"), tx("premium_desc")); return }
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, ActionWidgetProvider::class.java)
        VaryokText.rememberPreset(this, kind, opacity)
        if (manager.isRequestPinAppWidgetSupported) {
            if (!manager.requestPinAppWidget(provider, null, null))
                Toast.makeText(this, tx("pin_fail"), Toast.LENGTH_LONG).show()
        } else info(tx("create"), tx("pin_help"))
    }
}
