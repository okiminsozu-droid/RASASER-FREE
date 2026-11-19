package com.rasaser.free.rasaserwidgets

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
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
    private val codes = listOf("app", "url", "dial", "note", "wifi", "bluetooth", "settings", "torch")
    private val titles = listOf("Uygulama aç", "Web bağlantısı", "Telefon arama ekranı", "Yerel not",
        "Wi-Fi ayarları", "Bluetooth ayarları", "Telefon ayarları", "El feneri")
    private lateinit var kinds: Spinner
    private lateinit var target: android.widget.EditText
    private lateinit var caption: android.widget.EditText
    private lateinit var opacity: SeekBar
    private lateinit var preview: ImageView
    private lateinit var percent: TextView
    private var clearImage = false
    private var saved = false
    private val pictureRequest = 724

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID ||
            !AppWidgetManager.getInstance(this).getAppWidgetIds(
                ComponentName(this, ActionWidgetProvider::class.java)).contains(widgetId)) {
            Toast.makeText(this, "Widget kimliği geçerli değil.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        buildEditor()
    }
    private fun buildEditor() {
        val existing = WidgetStore.load(this, widgetId)
        val root = UiKit.page(this)
        UiKit.label(this, root, "RaSaSeR", 29f, UiKit.GOLD, true)
        UiKit.label(this, root, "WIDGET TASARIM ATÖLYESİ", 19f, UiKit.WHITE, true)
        UiKit.label(this, root, "Fotoğraf, dokunma eylemi ve görünürlük tamamen sana ait.", 14f, UiKit.MUTED)
        val options = UiKit.panel(this, root)
        UiKit.label(this, options, "DOKUNMA EYLEMİ", 13f, UiKit.GOLD, true)
        kinds = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, titles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        kinds.adapter = adapter
        kinds.setSelection(codes.indexOf(existing.kind).coerceAtLeast(0))
        options.addView(kinds, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, 48)))
        UiKit.label(this, options, "HEDEF / NOT / ADRES", 13f, UiKit.GOLD, true)
        target = UiKit.input(this, options, "Uygulama paketi, URL, numara veya not", existing.target)
        UiKit.button(this, options, "Yüklü uygulamalardan seç", false) { pickApp() }
        UiKit.label(this, options, "KISA ETİKET (İSTEĞE BAĞLI)", 13f, UiKit.GOLD, true)
        caption = UiKit.input(this, options, "Ana ekranda görünen yazı", existing.caption)

        val imageBox = UiKit.panel(this, root)
        UiKit.label(this, imageBox, "WIDGET FOTOĞRAFI", 13f, UiKit.GOLD, true)
        preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setBackgroundColor(0xFF25252C.toInt())
        }
        imageBox.addView(preview, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, 175)))
        refreshPreview()
        UiKit.button(this, imageBox, "Galeriden fotoğraf seç") {
            val pick = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(pick, pictureRequest)
        }
        UiKit.button(this, imageBox, "Fotoğrafı kaldır", false) {
            clearImage = true
            WidgetStore.discardPending(this, widgetId)
            refreshPreview()
        }

        val visibility = UiKit.panel(this, root)
        UiKit.label(this, visibility, "GÖRÜNÜRLÜK / ŞEFFAFLIK", 13f, UiKit.GOLD, true)
        percent = UiKit.label(this, visibility, "%${existing.opacity} görünür", 16f)
        opacity = SeekBar(this).apply {
            max = 100
            progress = existing.opacity
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    percent.text = "%$progress görünür"
                    preview.imageAlpha = (progress * 255 / 100)
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        }
        visibility.addView(opacity)
        UiKit.label(this, visibility, "%0: tamamen görünmez ama dokunulabilir. %100: tam görünür.", 13f, UiKit.MUTED)
        UiKit.button(this, root, "✓  Widget'ı kaydet") { saveAndFinish() }
        UiKit.button(this, root, "Vazgeç", false) { finish() }
    }
    private fun refreshPreview() {
        val bitmap = if (clearImage) null else WidgetStore.bitmap(this, widgetId, 400, usePending = true)
        if (bitmap == null) preview.setImageResource(R.drawable.ic_brand)
        else preview.setImageBitmap(bitmap)
        if (::opacity.isInitialized) preview.imageAlpha = (opacity.progress * 255 / 100)
    }
    private fun pickApp() {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val results = packageManager.queryIntentActivities(query, 0)
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        if (results.isEmpty()) {
            Toast.makeText(this, "Uygulama listesi alınamadı.", Toast.LENGTH_LONG).show()
            return
        }
        val labels = results.map { "${it.loadLabel(packageManager)}  •  ${it.activityInfo.packageName}" }
        AlertDialog.Builder(this).setTitle("Uygulama seç")
            .setItems(labels.toTypedArray()) { _, position ->
                target.setText(results[position].activityInfo.packageName)
                kinds.setSelection(0)
            }.setNegativeButton("İptal", null).show()
    }
    private fun saveAndFinish() {
        val kind = codes[kinds.selectedItemPosition]
        var value = target.text.toString().trim()
        if (kind in listOf("app", "url", "dial", "note") && value.isBlank()) {
            Toast.makeText(this, "Bu işlem için hedef veya not gir.", Toast.LENGTH_LONG).show()
            return
        }
        if (kind == "app" && packageManager.getLaunchIntentForPackage(value) == null) {
            Toast.makeText(this, "Yüklü uygulamalardan geçerli bir hedef seç.", Toast.LENGTH_LONG).show()
            return
        }
        if (kind == "url") {
            if (!value.startsWith("https://", true) && !value.startsWith("http://", true)) {
                value = "https://$value"
            }
            val parsed = android.net.Uri.parse(value)
            if (parsed.host.isNullOrBlank()) {
                Toast.makeText(this, "Geçerli bir web adresi gir.", Toast.LENGTH_LONG).show()
                return
            }
        }
        if (kind == "dial" && !value.matches(Regex("[+0-9() .-]{3,32}"))) {
            Toast.makeText(this, "Geçerli bir telefon numarası gir.", Toast.LENGTH_LONG).show()
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
        } catch (_: Exception) {
            Toast.makeText(this, "Kaydedilemedi, tekrar dene.", Toast.LENGTH_LONG).show()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pictureRequest && resultCode == RESULT_OK && data?.data != null) {
            if (WidgetStore.stageSelectedPhoto(this, widgetId, data.data!!)) {
                clearImage = false
                refreshPreview()
            } else Toast.makeText(this, "Fotoğraf okunamadı.", Toast.LENGTH_LONG).show()
        }
    }
    override fun onDestroy() {
        if (!saved && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
            WidgetStore.discardPending(this, widgetId)
        super.onDestroy()
    }
}
