package com.rasaser.free.rasaserwidgets

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Shared offline-only obsidian / soft-gold Android UI. */
object UiKit {
    const val GOLD = 0xFFD4B996.toInt()
    const val WHITE = 0xFFF6F2EB.toInt()
    const val MUTED = 0xFFAAA6A3.toInt()
    const val BACKGROUND = 0xFF090A0E.toInt()
    const val SURFACE = 0xFF18191F.toInt()
    fun dp(a: Activity, n: Int) = (n * a.resources.displayMetrics.density).toInt()
    fun background(color: Int, radius: Float, stroke: Int? = null): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            if (stroke != null) setStroke(2, stroke)
        }
    }
    fun page(a: Activity): LinearLayout {
        val scroll = ScrollView(a).apply {
            setBackgroundColor(BACKGROUND)
            fitsSystemWindows = true
            isFillViewport = true
        }
        val box = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(a, 22), dp(a, 32), dp(a, 22), dp(a, 28))
        }
        scroll.addView(box)
        a.setContentView(scroll)
        return box
    }
    fun text(a: Activity, value: String, size: Float = 16f, color: Int = WHITE, bold: Boolean = false): TextView =
        TextView(a).apply {
            text = value
            textSize = size
            setTextColor(color)
            typeface = if (bold) Typeface.create("sans-serif-medium", Typeface.BOLD) else
                Typeface.create("sans-serif", Typeface.NORMAL)
            setPadding(0, dp(a, 7), 0, dp(a, 7))
        }
    fun label(a: Activity, parent: LinearLayout, value: String, size: Float = 16f, color: Int = WHITE, bold: Boolean = false): TextView {
        val t = text(a, value, size, color, bold)
        parent.addView(t)
        return t
    }
    fun panel(a: Activity, parent: LinearLayout): LinearLayout {
        val panel = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            background = background(SURFACE, dp(a, 20).toFloat(), 0xFF353037.toInt())
            setPadding(dp(a, 17), dp(a, 17), dp(a, 17), dp(a, 17))
        }
        parent.addView(panel, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(a, 18) })
        return panel
    }
    fun button(a: Activity, parent: LinearLayout, value: String, primary: Boolean = true, click: () -> Unit): Button {
        val button = Button(a).apply {
            text = value
            isAllCaps = false
            textSize = 15f
            setTextColor(if (primary) BACKGROUND else GOLD)
            background = background(if (primary) GOLD else 0xFF25252C.toInt(), dp(a, 15).toFloat(),
                if (primary) null else 0xFF685544.toInt())
            setOnClickListener { click() }
        }
        parent.addView(button, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(a, 50)
        ).apply { topMargin = dp(a, 12) })
        return button
    }
    fun input(a: Activity, parent: LinearLayout, hint: String, content: String = ""): EditText {
        val field = EditText(a).apply {
            setSingleLine(true)
            setTextColor(WHITE)
            setHintTextColor(MUTED)
            setText(content)
            this.hint = hint
            textSize = 15f
            background = background(0xFF25252C.toInt(), dp(a, 13).toFloat())
            setPadding(dp(a, 13), dp(a, 11), dp(a, 13), dp(a, 11))
        }
        parent.addView(field, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(a, 53)
        ).apply { topMargin = dp(a, 8) })
        return field
    }
}
