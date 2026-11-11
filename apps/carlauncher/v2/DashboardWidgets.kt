package com.rasaser.launcher

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private const val WIDGET_HOST_ID = 10872
private const val PREF_WIDGETS = "dashboard_widget_ids"

private fun readWidgetIds(context: Context): List<Int> =
    context.getSharedPreferences("rasaser_dashboard", Context.MODE_PRIVATE)
        .getString(PREF_WIDGETS, "").orEmpty().split(",").mapNotNull { it.toIntOrNull() }

private fun saveWidgetIds(context: Context, ids: List<Int>) {
    context.getSharedPreferences("rasaser_dashboard", Context.MODE_PRIVATE)
        .edit().putString(PREF_WIDGETS, ids.joinToString(",")).apply()
}

/**
 * Hosts genuine third-party Android AppWidgets.
 * User consent is required to bind widgets; unsupported car head units may reject this.
 * The widget picker and bound widgets live in a modal above the preserved dashboard.
 */
@Composable
fun DashboardWidgets(activity: ComponentActivity, language: String, accent: Color, onClose: () -> Unit) {
    val manager = remember(activity) { AppWidgetManager.getInstance(activity) }
    val host = remember(activity) { AppWidgetHost(activity, WIDGET_HOST_ID) }
    var widgetIds by remember { mutableStateOf(readWidgetIds(activity)) }
    var choose by remember { mutableStateOf(false) }
    var waitingId by remember { mutableIntStateOf(0) }

    val binding = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, waitingId) ?: waitingId
        if (result.resultCode == Activity.RESULT_OK && manager.getAppWidgetInfo(id) != null) {
            widgetIds = (widgetIds + id).distinct()
            saveWidgetIds(activity, widgetIds)
        } else {
            host.deleteAppWidgetId(id)
        }
        waitingId = 0
    }
    DisposableEffect(host) {
        runCatching { host.startListening() }
        onDispose { runCatching { host.stopListening() } }
    }

    fun add(info: AppWidgetProviderInfo) {
        val id = host.allocateAppWidgetId()
        if (manager.bindAppWidgetIdIfAllowed(id, info.provider)) {
            widgetIds = (widgetIds + id).distinct()
            saveWidgetIds(activity, widgetIds)
            choose = false
        } else {
            waitingId = id
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            }
            try {
                binding.launch(intent)
                choose = false
            } catch (_: Exception) {
                host.deleteAppWidgetId(id)
                activity.toast(uiText(language, "noWidget"))
            }
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(0.7f).fillMaxHeight(0.8f),
            color = Panel, shape = RoundedCornerShape(18.dp), border = BorderStroke(2.dp, accent)
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(uiText(language, "widget"), color = accent, fontSize = 18.sp)
                    Spacer(Modifier.weight(1f))
                    Text("＋", color = accent, fontSize = 24.sp,
                        modifier = Modifier.clickable { choose = !choose }.padding(horizontal = 12.dp))
                    Text("✕", color = Color.White, fontSize = 21.sp,
                        modifier = Modifier.clickable(onClick = onClose).padding(4.dp))
                }
                if (choose) {
                    val providers = remember { manager.installedProviders.sortedBy { it.loadLabel(activity.packageManager) } }
                    LazyColumn {
                        items(providers) { info ->
                            Text(info.loadLabel(activity.packageManager), color = Color.White,
                                modifier = Modifier.fillMaxWidth().clickable { add(info) }.padding(10.dp))
                        }
                    }
                } else {
                    if (widgetIds.isEmpty()) {
                        Text(uiText(language, "widgetChoose"), color = Color.LightGray, modifier = Modifier.padding(12.dp))
                    }
                    LazyColumn {
                        items(widgetIds) { id ->
                            val info = manager.getAppWidgetInfo(id)
                            if (info != null) {
                                Column {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(info.loadLabel(activity.packageManager),
                                            color = accent, modifier = Modifier.weight(1f))
                                        Text("✕", color = Color.Red,
                                            modifier = Modifier.clickable {
                                                host.deleteAppWidgetId(id)
                                                widgetIds = widgetIds - id
                                                saveWidgetIds(activity, widgetIds)
                                            }.padding(8.dp))
                                    }
                                    AndroidView(
                                        factory = { host.createView(it, id, info) },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 240.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
