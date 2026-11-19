package com.rasaser.free.rasaserwidgets

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

/**
 * Widget catalog: on-demand, in-memory listing of the current user's launchable apps.
 * No app inventory is written to disk and no package-wide or storage permission is needed.
 */
class WidgetCreateActivity : Activity() {
    private data class AppEntry(val title: String, val packageName: String)
    private data class Entry(val type: String, val title: String, val subtitle: String = "", val packageName: String = "")

    private val pink = 0xFFF5C8D5.toInt()
    private val powder = 0xFFFFE8EF.toInt()
    private val ink = 0xFF812643.toInt()
    private val secondary = 0xFF98516B.toInt()
    private val apps = mutableListOf<AppEntry>()
    private val visible = mutableListOf<Entry>()
    private lateinit var list: ListView
    private lateinit var search: EditText
    private lateinit var adapter: BaseAdapter
    private var requestSerial = 0
    private var listening = false
    private val kinds = listOf("photo", "transparent", "app", "web", "templates",
        "dial", "note", "wifi", "bt", "system", "torch")
    private fun tx(key: String) = VaryokText.t(this, key)
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    private val changedPackages = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { refreshApps() }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = pink
        window.navigationBarColor = pink
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(16), dp(17), dp(12))
            setBackgroundColor(pink)
        }
        setContentView(root)
        root.addView(TextView(this).apply {
            text = "VARYOK  •  " + tx("create")
            textSize = 24f
            setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(6), dp(7), dp(6), dp(12))
        })
        search = EditText(this).apply {
            isSingleLine = true
            hint = tx("apps_search")
            setTextColor(ink)
            setHintTextColor(secondary)
            background = GradientDrawable().apply {
                setColor(powder)
                cornerRadius = dp(17).toFloat()
            }
            setPadding(dp(16), dp(9), dp(16), dp(9))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    updateVisible()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(52)))
        root.addView(TextView(this).apply {
            text = tx("apps_hint")
            setTextColor(secondary)
            textSize = 12f
            setPadding(dp(6), dp(8), dp(6), dp(8))
        })
        list = ListView(this).apply {
            dividerHeight = dp(6)
            setBackgroundColor(pink)
            isFastScrollEnabled = true
        }
        adapter = object : BaseAdapter() {
            override fun getCount() = visible.size
            override fun getItem(position: Int): Any = visible[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun isEnabled(position: Int) = visible[position].type != "header"
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val item = visible[position]
                val v = (convertView as? TextView) ?: TextView(this@WidgetCreateActivity).apply {
                    setPadding(dp(14), dp(9), dp(14), dp(9))
                    minHeight = dp(52)
                }
                v.text = if (item.subtitle.isEmpty()) item.title
                    else item.title + "\n" + item.subtitle
                v.textSize = if (item.type == "header") 15f else 16f
                v.typeface = Typeface.create("sans-serif",
                    if (item.type == "header") Typeface.BOLD else Typeface.NORMAL)
                v.setTextColor(if (item.type == "header") secondary else ink)
                v.background = if (item.type == "header") null else GradientDrawable().apply {
                    setColor(powder)
                    cornerRadius = dp(14).toFloat()
                }
                return v
            }
        }
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ -> onSelected(visible[position]) }
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        updateVisible()
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
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(changedPackages, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(changedPackages, filter)
            }
            listening = true
        } catch (_: RuntimeException) { listening = false }
    }
    override fun onResume() {
        super.onResume()
        refreshApps()
    }
    override fun onStop() {
        requestSerial++
        if (listening) {
            try { unregisterReceiver(changedPackages) } catch (_: IllegalArgumentException) { }
            listening = false
        }
        super.onStop()
    }

    /** Async enumeration avoids locking the main thread on devices with many apps. */
    private fun refreshApps() {
        val serial = ++requestSerial
        val pm = packageManager
        Thread {
            val found = try {
                val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(query, 0)
                    .asSequence()
                    .filter { it.activityInfo?.packageName != null }
                    .distinctBy { it.activityInfo.packageName }
                    .map { AppEntry(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                    .toList()
            } catch (_: Exception) { emptyList() }
            runOnUiThread {
                if (!isFinishing && !isDestroyed && serial == requestSerial) {
                    apps.clear()
                    apps.addAll(found)
                    updateVisible()
                }
            }
        }.start()
    }
    private fun updateVisible() {
        if (!::adapter.isInitialized || !::search.isInitialized) return
        val query = search.text.toString().trim().lowercase(Locale.ROOT)
        visible.clear()
        if (query.isEmpty()) {
            visible.add(Entry("header", tx("choose")))
            kinds.forEach { key -> visible.add(Entry("kind", tx(key), "", key)) }
        }
        visible.add(Entry("header", tx("apps_all")))
        apps.asSequence()
            .filter { query.isEmpty() || it.title.lowercase(Locale.ROOT).contains(query) ||
                it.packageName.lowercase(Locale.ROOT).contains(query) }
            .forEach { visible.add(Entry("installed", it.title, it.packageName, it.packageName)) }
        if (apps.isEmpty()) visible.add(Entry("header", tx("apps_empty")))
        adapter.notifyDataSetChanged()
    }
    private fun onSelected(entry: Entry) {
        if (!WidgetStore.canCreate(this)) {
            AlertDialog.Builder(this).setTitle(tx("premium"))
                .setMessage(tx("premium_desc")).setPositiveButton(tx("ok"), null).show()
            return
        }
        if (entry.type == "installed") {
            pin("app", 100, entry.packageName, entry.title)
            return
        }
        when (entry.packageName) {
            "photo" -> pin("none")
            "transparent" -> pin("app", 0)
            "app" -> {
                search.requestFocus()
                val keyboard = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                keyboard.showSoftInput(search, InputMethodManager.SHOW_IMPLICIT)
                list.smoothScrollToPosition(kinds.size + 1)
            }
            "web" -> pin("url")
            "templates" -> showTemplates()
            "dial" -> pin("dial")
            "note" -> pin("note")
            "wifi" -> pin("wifi")
            "bt" -> pin("bluetooth")
            "system" -> pin("settings")
            "torch" -> pin("torch")
        }
    }
    private fun showTemplates() {
        AlertDialog.Builder(this).setTitle(tx("template_name"))
            .setItems(arrayOf(tx("simple"), tx("photo"), tx("transparent"))) { _, index ->
                if (index == 2) pin("none", 0) else pin("none")
            }.setNegativeButton(tx("cancel"), null).show()
    }
    private fun pin(kind: String, opacity: Int = 100, target: String = "", caption: String = "") {
        if (!WidgetStore.canCreate(this)) return
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, ActionWidgetProvider::class.java)
        VaryokText.rememberPreset(this, kind, opacity, target, caption)
        if (manager.isRequestPinAppWidgetSupported) {
            if (!manager.requestPinAppWidget(provider, null, null)) {
                VaryokText.clearPreset(this)
                Toast.makeText(this, tx("pin_fail"), Toast.LENGTH_LONG).show()
            }
        } else {
            AlertDialog.Builder(this).setTitle(tx("create")).setMessage(tx("pin_help"))
                .setPositiveButton(tx("ok"), null).show()
        }
    }
}
