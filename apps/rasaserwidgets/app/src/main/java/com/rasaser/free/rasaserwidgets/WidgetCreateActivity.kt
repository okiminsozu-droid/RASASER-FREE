package com.rasaser.free.rasaserwidgets

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Widget Oluştur: only phone apps, with their real icons. Lightweight bottom sheet
 * for selected app's icon style, preview and transparency, per supplied screenshots.
 */
class WidgetCreateActivity : Activity() {
    private data class AppEntry(val title: String, val packageName: String)
    private val pink = 0xFFF5C8D5.toInt()
    private val powder = 0xFFFFE8EF.toInt()
    private val ink = 0xFF812643.toInt()
    private val soft = 0xFF90546B.toInt()
    private val apps = mutableListOf<AppEntry>()
    private val filtered = mutableListOf<AppEntry>()
    private val iconWorkers = Executors.newFixedThreadPool(2)
    private lateinit var adapter: BaseAdapter
    private lateinit var list: ListView
    private lateinit var search: EditText
    private var serial = 0
    private var registered = false
    private var current: AppEntry? = null
    private var bottomSheet: Dialog? = null
    private var iconPreview: ImageView? = null
    private var defaultIcon: TextView? = null
    private var customIcon: TextView? = null
    private var opacity = 100
    private var isCustom = false
    private var selectingImage = false
    private var pinRequested = false
    private val photoRequest = 971
    private val stagingId = -48047
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun tt(key: String) = CatalogText.t(this, key)
    private val appChanges = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refreshApps()
    }
    private fun shape(color: Int = powder, radius: Int = 16) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
    private fun label(value: String, size: Float, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(ink)
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        gravity = Gravity.CENTER_VERTICAL
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = pink
        window.navigationBarColor = pink
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(19), dp(16), dp(19), dp(8))
            setBackgroundColor(pink)
        }
        setContentView(root)
        root.addView(label(tt("title"), 24f, true), LinearLayout.LayoutParams(-1, dp(66)))
        search = EditText(this).apply {
            setSingleLine(true)
            hint = tt("search")
            textSize = 17f
            setTextColor(ink)
            setHintTextColor(soft)
            background = shape()
            setPadding(dp(18), dp(12), dp(18), dp(12))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = filterApps()
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(57)))
        list = ListView(this).apply {
            divider = null
            dividerHeight = dp(5)
            isFastScrollEnabled = true
            setBackgroundColor(pink)
            setOnItemClickListener { _, _, position, _ ->
                if (position in filtered.indices) showCustomize(filtered[position])
            }
        }
        adapter = object : BaseAdapter() {
            override fun getCount() = filtered.size
            override fun getItem(position: Int): Any = filtered[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, old: View?, parent: ViewGroup): View {
                val row = (old as? LinearLayout) ?: LinearLayout(this@WidgetCreateActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(8), dp(6), dp(9), dp(6))
                    background = shape()
                    val icon = ImageView(this@WidgetCreateActivity).apply {
                        id = android.R.id.icon
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                    addView(icon, LinearLayout.LayoutParams(dp(49), dp(49)))
                    val name = label("", 17f, true).apply {
                        id = android.R.id.text1
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                    addView(name, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(15) })
                    addView(label("›", 30f, true), LinearLayout.LayoutParams(dp(24), -2))
                }
                val entry = filtered[position]
                val image = row.findViewById<ImageView>(android.R.id.icon)
                image.tag = entry.packageName
                image.setImageResource(R.drawable.ic_brand)
                row.findViewById<TextView>(android.R.id.text1).text = entry.title
                // Keep installed-app enumeration and icon decoding off the UI thread.
                iconWorkers.execute {
                    val icon = AppIconKit.bitmap(this@WidgetCreateActivity, entry.packageName, 96)
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed && image.tag == entry.packageName && icon != null)
                            image.setImageBitmap(icon)
                    }
                }
                return row
            }
        }
        list.adapter = adapter
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        filterApps()
    }
    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        try {
            if (Build.VERSION.SDK_INT >= 33)
                registerReceiver(appChanges, filter, Context.RECEIVER_NOT_EXPORTED)
            else {
                @Suppress("DEPRECATION")
                registerReceiver(appChanges, filter)
            }
            registered = true
        } catch (_: RuntimeException) { registered = false }
    }
    override fun onResume() {
        super.onResume()
        refreshApps()
    }
    override fun onStop() {
        serial++
        if (registered) {
            try { unregisterReceiver(appChanges) } catch (_: IllegalArgumentException) {}
            registered = false
        }
        super.onStop()
    }
    override fun onDestroy() {
        bottomSheet?.dismiss()
        iconWorkers.shutdownNow()
        super.onDestroy()
    }
    private fun refreshApps() {
        val sequence = ++serial
        val pm = packageManager
        Thread {
            val result = try {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
                    .asSequence()
                    .filter { !it.activityInfo.packageName.isNullOrBlank() }
                    .distinctBy { it.activityInfo.packageName }
                    .map { AppEntry(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                    .toList()
            } catch (_: Exception) { emptyList() }
            runOnUiThread {
                if (!isFinishing && !isDestroyed && sequence == serial) {
                    apps.clear()
                    apps.addAll(result)
                    filterApps()
                }
            }
        }.start()
    }
    private fun filterApps() {
        if (!::search.isInitialized || !::adapter.isInitialized) return
        val query = search.text.toString().trim().lowercase(Locale.ROOT)
        filtered.clear()
        apps.filterTo(filtered) { query.isEmpty() ||
            it.title.lowercase(Locale.ROOT).contains(query) ||
            it.packageName.lowercase(Locale.ROOT).contains(query) }
        adapter.notifyDataSetChanged()
    }
    private fun showCustomize(app: AppEntry) {
        if (!WidgetStore.canCreate(this)) {
            AlertDialog.Builder(this).setTitle(VaryokText.t(this, "premium"))
                .setMessage(VaryokText.t(this, "premium_desc"))
                .setPositiveButton(VaryokText.t(this, "ok"), null).show()
            return
        }
        current = app
        opacity = 100
        isCustom = false
        selectingImage = false
        pinRequested = false
        WidgetStore.discardPending(this, stagingId)
        val dialog = Dialog(this)
        bottomSheet = dialog
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(17), dp(22), dp(24))
            background = GradientDrawable().apply {
                setColor(0xFFFFF9FB.toInt())
                cornerRadii = floatArrayOf(dp(28).toFloat(), dp(28).toFloat(),
                    dp(28).toFloat(), dp(28).toFloat(), 0f, 0f, 0f, 0f)
            }
        }
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        heading.addView(label(tt("customize") + " “" + app.title + "”", 20f, true).apply {
            maxLines = 2
        }, LinearLayout.LayoutParams(0, -2, 1f))
        heading.addView(label("×", 28f, true).apply {
            gravity = Gravity.CENTER
            contentDescription = tt("close")
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(dp(40), dp(44)))
        page.addView(heading)
        page.addView(label(tt("preview"), 17f, true), LinearLayout.LayoutParams(-1, dp(48)))
        val previewPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = shape(0xFF33303B.toInt(), 19)
        }
        iconPreview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            val ico = AppIconKit.bitmap(this@WidgetCreateActivity, app.packageName, 124)
            if (ico != null) setImageBitmap(ico) else setImageResource(R.drawable.ic_brand)
        }
        previewPanel.addView(iconPreview, LinearLayout.LayoutParams(dp(86), dp(86)))
        val caption = TextView(this).apply {
            text = app.title
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            maxLines = 1
        }
        previewPanel.addView(caption)
        page.addView(previewPanel, LinearLayout.LayoutParams(-1, dp(143)))
        page.addView(label(tt("iconStyle"), 16f, true), LinearLayout.LayoutParams(-1, dp(48)))
        val options = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        defaultIcon = label(tt("default"), 16f, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                isCustom = false
                WidgetStore.discardPending(this@WidgetCreateActivity, stagingId)
                renderStyle(app)
            }
        }
        customIcon = label(tt("custom"), 16f, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { chooseCustomPhoto() }
        }
        options.addView(defaultIcon, LinearLayout.LayoutParams(0, dp(49), 1f))
        options.addView(customIcon, LinearLayout.LayoutParams(0, dp(49), 1f).apply { leftMargin = dp(8) })
        page.addView(options)
        renderStyle(app)
        page.addView(label(tt("transparency"), 16f, true),
            LinearLayout.LayoutParams(-1, dp(45)))
        val slider = SeekBar(this).apply {
            max = 100
            progress = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    opacity = progress
                    iconPreview?.imageAlpha = progress * 255 / 100
                    caption.alpha = progress / 100f
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        page.addView(slider)
        val create = label(tt("create"), 19f, true).apply {
            gravity = Gravity.CENTER
            background = shape(0xFFF0BAD0.toInt(), 18)
            setOnClickListener {
                if (selectingImage) {
                    Toast.makeText(this@WidgetCreateActivity, tt("photoLoading"), Toast.LENGTH_SHORT).show()
                } else {
                    pinApp(app)
                    if (pinRequested) dialog.dismiss()
                }
            }
        }
        page.addView(create, LinearLayout.LayoutParams(-1, dp(62)).apply { topMargin = dp(20) })
        dialog.setContentView(page)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.42f)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
        }
        dialog.setOnDismissListener {
            if (!pinRequested) WidgetStore.discardPending(this, stagingId)
            if (bottomSheet === dialog) bottomSheet = null
        }
        dialog.show()
        dialog.window?.setLayout(-1, -2)
    }
    private fun renderStyle(app: AppEntry) {
        val selected = shape(0xFFEDAFC6.toInt(), 14)
        val unselected = shape(0xFFF4E1E9.toInt(), 14)
        defaultIcon?.background = if (!isCustom) selected else unselected
        customIcon?.background = if (isCustom) selected else unselected
        val ico = if (isCustom) WidgetStore.bitmap(this, stagingId, 200, true) else null
        if (ico != null) iconPreview?.setImageBitmap(ico)
        else {
            val icon = AppIconKit.bitmap(this, app.packageName, 124)
            if (icon != null) iconPreview?.setImageBitmap(icon)
        }
        iconPreview?.imageAlpha = opacity * 255 / 100
    }
    private fun chooseCustomPhoto() {
        val select = if (Build.VERSION.SDK_INT >= 33)
            Intent(MediaStore.ACTION_PICK_IMAGES).apply { type = "image/*" }
        else Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(select, photoRequest)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != photoRequest || resultCode != RESULT_OK || data?.data == null) return
        selectingImage = true
        val uri = data.data!!
        val app = current ?: return
        Thread {
            val okay = WidgetStore.stageSelectedPhoto(this@WidgetCreateActivity, stagingId, uri)
            runOnUiThread {
                selectingImage = false
                if (!isFinishing && !isDestroyed && bottomSheet?.isShowing == true && current == app && okay) {
                    isCustom = true
                    renderStyle(app)
                } else if (!okay) {
                    Toast.makeText(this, tt("photoError"), Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    private fun pinApp(app: AppEntry) {
        if (!WidgetStore.canCreate(this)) return
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, ActionWidgetProvider::class.java)
        // Pinning skips the configure activity. Attach all values to a unique,
        // mutable success callback; the launcher fills in the real appWidgetId.
        val token = (android.os.SystemClock.elapsedRealtimeNanos() and 0x7fffffffL).toInt()
        val hasPhoto = isCustom
        if (hasPhoto) {
            WidgetStore.attachStagedPhoto(this, stagingId, token)
        }
        val onSuccess = PinResultReceiver.callback(
            this, token, app.packageName, app.title, opacity, hasPhoto
        )
        pinRequested = try {
            if (manager.isRequestPinAppWidgetSupported) {
                manager.requestPinAppWidget(provider, null, onSuccess)
            } else {
                AlertDialog.Builder(this).setMessage(VaryokText.t(this, "pin_help"))
                    .setPositiveButton(VaryokText.t(this, "ok"), null).show()
                false
            }
        } catch (_: Exception) { false }
        if (!pinRequested) {
            WidgetStore.discardPending(this, token)
            onSuccess.cancel()
            Toast.makeText(this, VaryokText.t(this, "pin_fail"), Toast.LENGTH_SHORT).show()
        }
    }
}
