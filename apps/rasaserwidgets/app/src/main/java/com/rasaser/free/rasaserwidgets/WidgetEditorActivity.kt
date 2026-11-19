package com.rasaser.free.rasaserwidgets

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class WidgetEditorActivity : Activity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val codes = listOf("none", "app", "url", "dial", "note", "wifi", "bluetooth", "settings", "torch")
    private val titles get() = listOf("none", "open_app", "web_link", "dial_screen", "local_note",
        "wifi_settings", "bt_settings", "phone_settings", "flashlight").map { wt(it) }
    private lateinit var kinds: Spinner
    private lateinit var target: android.widget.EditText
    private lateinit var caption: android.widget.EditText
    private lateinit var opacity: SeekBar
    private lateinit var preview: ImageView
    private lateinit var percent: TextView
    private var clearImage = false
    private var saved = false
    @Volatile private var photoLoading = false
    private fun wt(key: String) = WidgetText.t(this, key)
    private val pictureRequest = 724

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID ||
            !AppWidgetManager.getInstance(this).getAppWidgetIds(
                ComponentName(this, ActionWidgetProvider::class.java)).contains(widgetId)) {
            Toast.makeText(this, wt("invalid_id"), Toast.LENGTH_LONG).show()
            finish()
            return
        }
        // Allow editing existing shortcuts and choosing one of the already free
        // applications even after the three distinct app slots are occupied.
        // The final permission check happens inside WidgetStore.save().
        buildEditor()
    }
    private fun buildEditor() {
        val existing = WidgetStore.load(this, widgetId)
        val preset = if (WidgetStore.exists(this, widgetId)) null else VaryokText.takePreset(this)
        // Apply a custom photo selected on the preview sheet before the launcher assigned the ID.
        if (preset?.kind == "app" && preset.target.isNotBlank()) {
            WidgetStore.attachStagedPhoto(this, -48047, widgetId)
        }
        val root = UiKit.page(this)
        UiKit.label(this, root, "VARYOK", 29f, UiKit.GOLD, true)
        UiKit.label(this, root, wt("editor_name"), 19f, UiKit.WHITE, true)
        UiKit.label(this, root, wt("editor_desc"), 14f, UiKit.MUTED)
        val options = UiKit.panel(this, root)
        UiKit.label(this, options, wt("tap_action"), 13f, UiKit.GOLD, true)
        kinds = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, titles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        kinds.adapter = adapter
        kinds.setSelection(codes.indexOf(preset?.kind ?: existing.kind).coerceAtLeast(0))
        options.addView(kinds, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, 48)))
        UiKit.label(this, options, wt("target_label"), 13f, UiKit.GOLD, true)
        target = UiKit.input(this, options, wt("target_hint"), preset?.target?.ifBlank { existing.target } ?: existing.target)
        UiKit.button(this, options, wt("pick_apps"), false) { pickApp() }
        UiKit.label(this, options, wt("caption_label"), 13f, UiKit.GOLD, true)
        caption = UiKit.input(this, options, wt("caption_hint"), preset?.caption?.ifBlank { existing.caption } ?: existing.caption)

        val imageBox = UiKit.panel(this, root)
        UiKit.label(this, imageBox, wt("photo_label"), 13f, UiKit.GOLD, true)
        preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setBackgroundColor(0xFF25252C.toInt())
        }
        imageBox.addView(preview, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, 175)))
        refreshPreview()
        UiKit.button(this, imageBox, wt("choose_photo")) {
            // The system picker lists newly added gallery photos each time it opens.
            // No storage-wide or media-library read permission is requested.
            val pick = if (Build.VERSION.SDK_INT >= 33) {
                Intent(MediaStore.ACTION_PICK_IMAGES).apply { type = "image/*" }
            } else Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(pick, pictureRequest)
        }
        UiKit.button(this, imageBox, wt("remove_photo"), false) {
            clearImage = true
            WidgetStore.discardPending(this, widgetId)
            refreshPreview()
        }

        val visibility = UiKit.panel(this, root)
        UiKit.label(this, visibility, wt("visibility_label"), 13f, UiKit.GOLD, true)
        percent = UiKit.label(this, visibility, WidgetText.percent(this, preset?.opacity ?: existing.opacity), 16f)
        opacity = SeekBar(this).apply {
            max = 100
            progress = preset?.opacity ?: existing.opacity
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    percent.text = WidgetText.percent(this@WidgetEditorActivity, progress)
                    preview.imageAlpha = (progress * 255 / 100)
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        }
        visibility.addView(opacity)
        UiKit.label(this, visibility, wt("visibility_help"), 13f, UiKit.MUTED)
        UiKit.button(this, root, wt("save")) { saveAndFinish() }
        UiKit.button(this, root, wt("cancel"), false) { finish() }
    }
    private fun refreshPreview() {
        val bitmap = if (clearImage) null else WidgetStore.bitmap(this, widgetId, 400, usePending = true)
        if (bitmap != null) {
            preview.setImageBitmap(bitmap)
        } else {
            val kind = if (::kinds.isInitialized) codes[kinds.selectedItemPosition] else "none"
            val app = if (::target.isInitialized) target.text.toString().trim() else ""
            val icon = if (kind == "app") AppIconKit.bitmap(this, app, 160) else null
            if (icon != null) preview.setImageBitmap(icon)
            else preview.setImageResource(R.drawable.ic_brand)
        }
        if (::opacity.isInitialized) preview.imageAlpha = (opacity.progress * 255 / 100)
    }
    private fun pickApp() {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val results = packageManager.queryIntentActivities(query, 0)
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        if (results.isEmpty()) {
            Toast.makeText(this, wt("no_apps"), Toast.LENGTH_LONG).show()
            return
        }
        val labels = results.map { "${it.loadLabel(packageManager)}  •  ${it.activityInfo.packageName}" }
        AlertDialog.Builder(this).setTitle(wt("choose_app"))
            .setItems(labels.toTypedArray()) { _, position ->
                target.setText(results[position].activityInfo.packageName)
                kinds.setSelection(codes.indexOf("app"))
            }.setNegativeButton(wt("cancel"), null).show()
    }
    private fun saveAndFinish() {
        if (photoLoading) {
            Toast.makeText(this, wt("loading_photo"), Toast.LENGTH_SHORT).show()
            return
        }
        val kind = codes[kinds.selectedItemPosition]
        var value = target.text.toString().trim()
        if (kind in listOf("app", "url", "dial", "note") && value.isBlank()) {
            Toast.makeText(this, wt("need_target"), Toast.LENGTH_LONG).show()
            return
        }
        if (kind == "app" && packageManager.getLaunchIntentForPackage(value) == null) {
            Toast.makeText(this, wt("invalid_app"), Toast.LENGTH_LONG).show()
            return
        }
        if (kind == "url") {
            if (!value.startsWith("https://", true) && !value.startsWith("http://", true)) {
                value = "https://$value"
            }
            val parsed = android.net.Uri.parse(value)
            if (parsed.host.isNullOrBlank()) {
                Toast.makeText(this, wt("invalid_url"), Toast.LENGTH_LONG).show()
                return
            }
        }
        if (kind == "dial" && !value.matches(Regex("[+0-9() .-]{3,32}"))) {
            Toast.makeText(this, wt("invalid_phone"), Toast.LENGTH_LONG).show()
            return
        }
        try {
            if (clearImage) WidgetStore.removePhoto(this, widgetId)
            else WidgetStore.commitPending(this, widgetId)
            WidgetStore.save(this, widgetId, WidgetConfig(
                kind = kind, target = value, caption = caption.text.toString().take(64),
                opacity = opacity.progress))
            ActionWidgetProvider.render(this, widgetId)
            saved = true
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            finish()
        } catch (_: IllegalStateException) {
            AlertDialog.Builder(this).setTitle(VaryokText.t(this, "premium"))
                .setMessage(VaryokText.t(this, "premium_desc"))
                .setPositiveButton(VaryokText.t(this, "ok"), null).show()
        } catch (_: Exception) {
            Toast.makeText(this, wt("failed_save"), Toast.LENGTH_LONG).show()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pictureRequest && resultCode == RESULT_OK && data?.data != null) {
            if (photoLoading) return
            photoLoading = true
            val selected = data.data!!
            val id = widgetId
            Thread {
                val result = WidgetStore.stageSelectedPhoto(this@WidgetEditorActivity, id, selected)
                runOnUiThread {
                    photoLoading = false
                    if (isFinishing || isDestroyed) {
                        WidgetStore.discardPending(this@WidgetEditorActivity, id)
                    } else if (result) {
                        clearImage = false
                        refreshPreview()
                    } else {
                        Toast.makeText(this@WidgetEditorActivity, wt("failed_photo"), Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }
    }
    override fun onDestroy() {
        if (!saved && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
            WidgetStore.discardPending(this, widgetId)
        super.onDestroy()
    }
}
