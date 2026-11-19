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
import android.widget.ImageView
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
        // onResume builds the home once; avoid redundant UI inflation on launch.
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
        tools.addView(language, LinearLayout.LayoutParams(dp(168), dp(51)))

        root.addView(TextView(this), LinearLayout.LayoutParams(1, 0, 1f))
        // The exact user-approved VARYOK / RaSaSeR logo, with its black-gold identity.
        val brand = ImageView(this).apply {
            setImageResource(R.drawable.varyok_brand_full)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            contentDescription = "VARYOK • RaSaSeR • APPS & TECHNOLOGY"
        }
        root.addView(brand, LinearLayout.LayoutParams(-1, dp(296)))
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
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        footer.addView(text(tx("site"), 16f, true).apply {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            contentDescription = tx("site")
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.okiminsozu.com")))
            }
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        footer.addView(text("ⓘ", 26f, true).apply {
            gravity = Gravity.CENTER
            contentDescription = tx("privacy")
            setOnClickListener { info(tx("privacy"), tx("privacy_desc")) }
        }, LinearLayout.LayoutParams(dp(47), dp(48)))
        root.addView(footer)
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

    private fun info(title: String, body: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(body)
            .setPositiveButton(tx("ok"), null).show()
    }
    private fun showWidgetTypes() {
        // Even after three distinct apps, existing app shortcuts remain free.
        startActivity(Intent(this, WidgetCreateActivity::class.java))
    }

}
