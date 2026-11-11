#!/usr/bin/env python3
"""Apply additive v2 dashboard features to the extracted, unchanged original ZIP sources.
The source-packed archive and original baseline branch remain immutable.
"""
from pathlib import Path
import shutil
import sys

HERE = Path(__file__).resolve().parent
PROJECT = HERE.parent / "original"
JAVA = PROJECT / "app/src/main/java/com/rasaser/launcher"
MAIN = JAVA / "MainActivity.kt"
GRADLE = PROJECT / "app/build.gradle.kts"
MANIFEST = PROJECT / "app/src/main/AndroidManifest.xml"

def replace(source, before, after, description):
    n = source.count(before)
    if n != 1:
        raise SystemExit(f"Patch anchor {description} expected 1, found {n}")
    return source.replace(before, after, 1)

s = MAIN.read_text(encoding="utf-8")
s = replace(s, 'import android.content.Intent\n',
'''import android.content.Intent
import android.net.Uri
import android.location.Geocoder
import android.os.SystemClock
''', "imports Android")
s = replace(s, 'import androidx.compose.foundation.clickable\n',
'''import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
''', "imports gestures")
s = replace(s, 'import androidx.compose.material3.Surface\n',
'''import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
''', "imports dialogs")
s = replace(s, 'import androidx.compose.ui.platform.LocalContext\n',
'''import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
''', "imports pointer")
s = replace(s, 'import kotlinx.coroutines.withContext\n',
'''import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
''', "imports coroutine")

s = replace(s, '''    var lang by remember { mutableStateOf("tr") }
    fun t(k: String): String = STR[lang]?.get(k) ?: STR["en"]!![k] ?: k
''', '''    val prefs = remember { act.getSharedPreferences("rasaser_dashboard", Context.MODE_PRIVATE) }
    var lang by remember { mutableStateOf(prefs.getString("language", "tr") ?: "tr") }
    fun t(k: String): String = uiText(lang, k)
    LaunchedEffect(lang) { prefs.edit().putString("language", lang).apply() }
''', "language selection and persistence")
s = replace(s, '''    var prepared by remember { mutableStateOf(false) }

    val mapView''', '''    var prepared by remember { mutableStateOf(false) }
    var drivingMode by remember { mutableStateOf(true) }
    var placeName by remember { mutableStateOf("") }
    var lastFixAt by remember { mutableLongStateOf(0L) }
    var radioCountry by remember { mutableStateOf(prefs.getString("radio_country", "TR") ?: "TR") }
    var stations by remember { mutableStateOf<List<RadioStation>>(emptyList()) }
    var stationIndex by remember { mutableIntStateOf(0) }
    var radioLoading by remember { mutableStateOf(false) }
    var tvOpen by remember { mutableStateOf(false) }
    var widgetsOpen by remember { mutableStateOf(false) }
    var addingShortcut by remember { mutableStateOf(false) }
    var shortcutOptions by remember { mutableStateOf<String?>(null) }
    val baseShortcuts = listOf(
        "com.google.android.apps.maps", "com.google.android.youtube",
        "com.spotify.music", "com.android.chrome"
    )
    var shortcutPkgs by remember {
        mutableStateOf(prefs.getString("shortcuts", null)?.split("|")?.filter { it.isNotEmpty() } ?: baseShortcuts)
    }
    fun persistShortcuts(list: List<String>) {
        shortcutPkgs = list.distinct()
        prefs.edit().putString("shortcuts", shortcutPkgs.joinToString("|")).apply()
    }
    var cameras by remember { mutableStateOf<List<CameraPoint>>(emptyList()) }
    var nearbyCamera by remember { mutableStateOf<CameraPoint?>(null) }
    val radarSpeaker = remember { CameraVoice(act) }
    DisposableEffect(radarSpeaker) { onDispose { radarSpeaker.stop() } }

    val mapView''', "new saved dashboard state")

s = replace(s, '''                if (p0.hasSpeed()) speed = (p0.speed * 3.6f).roundToInt()
''', '''                if (p0.hasSpeed()) speed = (p0.speed * 3.6f).roundToInt()
                lastFixAt = SystemClock.elapsedRealtime()
                if (drivingMode) {
                    mapView.controller.setCenter(GeoPoint(p0.latitude, p0.longitude))
                    if (p0.hasBearing() && p0.hasSpeed() && p0.speed > 0.5f)
                        mapView.mapOrientation = -p0.bearing
                }
''', "follow car and GPS heading")

s = replace(s, '''    // İlk konum gelince haritayı ortala
    LaunchedEffect(loc != null) { loc?.let { mapView.controller.animateTo(it) } }

    // Saat''', '''    // Araç hareketini canlı takip et; büyük yön oku harita üzerinde ayrıca gösterilir.
    LaunchedEffect(loc, drivingMode) {
        if (drivingMode) loc?.let { mapView.controller.setCenter(it) }
        else mapView.mapOrientation = 0f
    }

    val latestPoint by rememberUpdatedState(loc)
    val latestLanguage by rememberUpdatedState(lang)
    // Android Geocoder'ın sağladığı mahalle, sokak ve şehir adlarını göster.
    LaunchedEffect(Unit) {
        while (true) {
            latestPoint?.let { point ->
                val label = withContext(Dispatchers.IO) {
                    try {
                        @Suppress("DEPRECATION")
                        val a = Geocoder(act, Locale.forLanguageTag(latestLanguage))
                            .getFromLocation(point.latitude, point.longitude, 1)?.firstOrNull()
                        listOfNotNull(a?.thoroughfare, a?.subLocality, a?.locality, a?.adminArea)
                            .distinct().joinToString(" · ")
                    } catch (_: Exception) { "" }
                }
                if (label.isNotBlank()) placeName = label
            }
            delay(20_000L)
        }
    }
    // Topluluk kaynaklı OSM kameraları: dünya genelinde eksiksiz olduğu iddia edilmez.
    LaunchedEffect(hasLoc) {
        while (hasLoc) {
            val point = latestPoint
            if (point != null) cameras = withContext(Dispatchers.IO) {
                try { queryOSMSpeedCameras(point.latitude, point.longitude) }
                catch (_: Exception) { emptyList() }
            }
            delay(if (point == null) 5_000L else 180_000L)
        }
    }
    LaunchedEffect(loc, cameras) {
        val point = loc
        val nearest = if (point == null) null else cameras.minByOrNull {
            distanceToCameraMetres(point.latitude, point.longitude, it)
        }
        val dist = if (point != null && nearest != null)
            distanceToCameraMetres(point.latitude, point.longitude, nearest) else Float.MAX_VALUE
        nearbyCamera = if (dist <= 500f) nearest else null
        if (speed > 2 && nearest != null && dist <= 500f) radarSpeaker.announce(nearest, lang)
    }

    // Saat''', "drive geocoding and camera feed")

s = replace(s, '''            now = Date()
            netOn''', '''            now = Date()
            if (lastFixAt > 0 && SystemClock.elapsedRealtime() - lastFixAt > 12_000L) speed = 0
            netOn''', "GPS stale speed")

s = replace(s, '''    fun togglePlay() {
        if (STREAM_URL.isBlank()) { act.toast("Radyo adresi tanımlı değil"); return }
        try {
            if (playing) { player.pause(); playing = false }
            else if (!prepared) {
                player.setDataSource(STREAM_URL)
                player.setOnPreparedListener { it.start(); playing = true }
                player.setOnErrorListener { _, _, _ -> playing = false; prepared = false; player.reset(); true }
                player.prepareAsync()
                prepared = true
            } else { player.start(); playing = true }
        } catch (e: Exception) { act.toast(t("error")) }
    }

    val clock''', '''    fun playStation(index: Int) {
        val station = stations.getOrNull(index) ?: return
        try {
            player.reset()
            prepared = false
            playing = false
            stationIndex = index
            player.setDataSource(station.url)
            player.setOnPreparedListener { it.start(); prepared = true; playing = true }
            player.setOnErrorListener { _, _, _ ->
                playing = false; prepared = false
                runCatching { player.reset() }
                true
            }
            player.prepareAsync()
        } catch (_: Exception) { playing = false; prepared = false; act.toast(t("streamError")) }
    }
    fun togglePlay() {
        if (playing) { runCatching { player.pause() }; playing = false }
        else if (stations.isEmpty()) act.toast(t("noStations"))
        else if (prepared) { runCatching { player.start(); playing = true } }
        else playStation(stationIndex)
    }
    fun nextStation(step: Int) {
        if (stations.isNotEmpty()) playStation((stationIndex + step + stations.size) % stations.size)
    }
    LaunchedEffect(radioCountry) {
        prefs.edit().putString("radio_country", radioCountry).apply()
        radioLoading = true
        runCatching { player.reset() }
        playing = false
        prepared = false
        stations = withContext(Dispatchers.IO) { fetchWorldRadio(radioCountry) }
        stationIndex = 0
        radioLoading = false
    }

    val clock''', "world radio playback state")

s = replace(s, '''                    // Harita
                    Card2(Modifier.weight(2f).fillMaxHeight(), accent) {''', '''                    // Harita, mevcut yerleşim korunur; sürüş katmanı üstüne eklenir.
                    Card2(Modifier.weight(2f).fillMaxHeight(), accent) {''', "map comment")

s = replace(s, '''                        Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) { MapBtn("🧭") { mapView.mapOrientation = 0f } }
''', '''                        Row(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                            MapBtn("🚗") {
                                drivingMode = true
                                mapView.controller.setZoom(17.0)
                                loc?.let { mapView.controller.setCenter(it) }
                            }
                            Spacer(Modifier.width(6.dp))
                            MapBtn("G") {
                                try {
                                    val p = loc ?: DEFAULT_POINT
                                    act.tryStart(Intent(Intent.ACTION_VIEW, Uri.parse(
                                        "geo:${p.latitude},${p.longitude}?q=${p.latitude},${p.longitude}"
                                    )).setPackage("com.google.android.apps.maps"))
                                } catch (_: Exception) { act.toast(t("error")) }
                            }
                            Spacer(Modifier.width(6.dp))
                            MapBtn("🧭") { drivingMode = !drivingMode; if (!drivingMode) mapView.mapOrientation = 0f }
                        }
                        if (drivingMode && loc != null) {
                            Box(Modifier.align(Alignment.Center).size(68.dp)
                                .background(Color(0xBB0D141A), RoundedCornerShape(35.dp))
                                .border(2.dp, accent, RoundedCornerShape(35.dp)),
                                contentAlignment = Alignment.Center) {
                                CarUiIcon("drive", accent, Modifier.size(54.dp))
                            }
                        }
                        if (placeName.isNotBlank()) {
                            Box(Modifier.align(Alignment.BottomCenter).padding(7.dp)
                                .background(Color(0xE0111111), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)) {
                                T("📍 $placeName", 12, Color.White, maxLines = 1)
                            }
                        }
''', "driving arrow and neighborhood overlay")

s = replace(s, '''                                T("✔ " + t("inactive"), 15)
                                T(t("source"), 11, Color.LightGray)
''', '''                                T(if (nearbyCamera == null) "○ " + t("inactive")
                                  else "⚠ " + t("radarDistance"), 15,
                                  if (nearbyCamera == null) Color.White else accent)
                                T(t("source"), 11, Color.LightGray)
''', "radar status truthful")

start = s.index('                    // Müzik\n                    Card2(')
end = s.index('                    // Hava durumu\n', start)
s = s[:start] + '''                    // Online radyo: ülke araması ve ileri/geri, aynı kartta görünür.
                    Card2(Modifier.weight(1.6f).fillMaxHeight(), accent) {
                        Column(Modifier.fillMaxSize().padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(50.dp).clip(RoundedCornerShape(10.dp))
                                    .background(Brush.verticalGradient(listOf(
                                        Color(0xFF39280C), Color(0xFF15110B))))
                                    .border(1.dp, accent, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center) {
                                    CarUiIcon("radio", accent, Modifier.size(38.dp))
                                }
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    CountryMenu(lang, radioCountry, accent) { radioCountry = it }
                                    T(t("radio"), 16, bold = true, maxLines = 1)
                                    T(if (radioLoading) t("loading")
                                      else stations.getOrNull(stationIndex)?.title ?: t("noStations"),
                                      12, Color.LightGray, maxLines = 1)
                                }
                            }
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(40.dp).clip(CircleShape)
                                    .border(1.dp, accent, CircleShape)
                                    .clickable { nextStation(-1) },
                                    contentAlignment = Alignment.Center) { T("⏮", 23, accent) }
                                Box(Modifier.size(46.dp).clip(CircleShape)
                                    .border(2.dp, accent, CircleShape)
                                    .clickable { togglePlay() },
                                    contentAlignment = Alignment.Center) {
                                    T(if (playing) "⏸" else "▶", 24, accent)
                                }
                                Box(Modifier.size(40.dp).clip(CircleShape)
                                    .border(1.dp, accent, CircleShape)
                                    .clickable { nextStation(1) },
                                    contentAlignment = Alignment.Center) { T("⏭", 23, accent) }
                                T(if (playing) "●" else "○", 14, accent)
                            }
                        }
                    }
''' + s[end:]

start = s.index('            // Kısayollar\n')
end = s.index('        // Alt araç çubuğu\n', start)
s = s[:start] + '''            // Aynı sağ kartı koru; artık dikey kaydırılabilir ve + ile eklenebilir.
            Card2(Modifier.width(170.dp).fillMaxHeight(), accent) {
                Column(Modifier.fillMaxSize().padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { T("★ " + t("shortcuts"), 15, accent, true) }
                        Box(Modifier.clickable { addingShortcut = true; showApps = true }) {
                            T("＋", 24, accent, true)
                        }
                    }
                    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        shortcutPkgs.forEach { pkg ->
                            val name = when (pkg) {
                                "com.google.android.apps.maps" -> t("maps")
                                "com.google.android.youtube" -> "YouTube"
                                "com.spotify.music" -> "Spotify"
                                "com.android.chrome" -> "Chrome"
                                else -> try {
                                    val info = act.packageManager.getApplicationInfo(pkg, 0)
                                    act.packageManager.getApplicationLabel(info).toString()
                                } catch (_: Exception) { pkg.substringAfterLast('.') }
                            }
                            val icon = when (pkg) {
                                "com.google.android.apps.maps" -> "📍"
                                "com.google.android.youtube" -> "▶"
                                "com.spotify.music" -> "🎧"
                                "com.android.chrome" -> "🌐"
                                else -> "▦"
                            }
                            val color = when (pkg) {
                                "com.google.android.apps.maps" -> Color(0xFF34A853)
                                "com.google.android.youtube" -> Color(0xFFE62117)
                                "com.spotify.music" -> Color(0xFF1DB954)
                                "com.android.chrome" -> Color(0xFF4285F4)
                                else -> Color(0xFF333333)
                            }
                            Shortcut(icon, name, color, { act.launchPkg(pkg) }, { shortcutOptions = pkg })
                        }
                        Shortcut("＋", t("addapp"), Color(0xFF333333),
                            { addingShortcut = true; showApps = true })
                    }
                }
            }
        }

''' + s[end:]

start=s.index('        // Alt araç çubuğu\n')
end=s.index('\n    if (showApps) AppsDialog(', start)
s=s[:start]+'''        // Önceki alt çubuk düzeni ve görünümü korunuyor; istenen tuş değişiklikleri.
        val tools: List<Triple<String, String, () -> Unit>> = listOf(
            Triple("🛒", t("playstore")) { act.launchPkg("com.android.vending") },
            Triple("APP_ICON", t("apps")) { addingShortcut = false; showApps = true },
            Triple("📺", t("tv")) { tvOpen = !tvOpen },
            Triple("📁", t("files")) { act.tryStart(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) },
            Triple("📷", t("camera")) { act.tryStart(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) },
            Triple("🎙", t("rec")) { act.tryStart(Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)) },
            Triple("ᛒ", t("bt")) { act.tryStart(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
            Triple("＋", t("addapp")) { addingShortcut = true; showApps = true },
            Triple("🎨", t("themes")) { accentIdx = (accentIdx + 1) % accents.size },
            Triple("WIDGET_ICON", t("widget")) { widgetsOpen = true },
            Triple("POWER_ICON", t("close")) { act.moveTaskToBack(true) }
        )
        Row(Modifier.fillMaxWidth().height(62.dp)) {
            tools.forEach { (emoji, label, action) ->
                val touch = if (emoji == "📺") Modifier.pointerInput(tvOpen) {
                    detectTapGestures(onPress = {
                        val released = withTimeoutOrNull(3000L) { tryAwaitRelease() }
                        if (released == null) tvOpen = false else if (released) tvOpen = !tvOpen
                    })
                } else Modifier.clickable { action() }
                Card2(Modifier.weight(1f).fillMaxHeight().then(touch), accent) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        if (emoji == "APP_ICON") CarUiIcon("apps", accent, Modifier.size(28.dp))
                        else if (emoji == "WIDGET_ICON") CarUiIcon("widget", accent, Modifier.size(28.dp))
                        else if (emoji == "POWER_ICON") CarUiIcon("power", accent, Modifier.size(28.dp))
                        else T(emoji, 20)
                        T(label, 10, maxLines = 1)
                    }
                }
            }
        }
    }
''' + s[end:]
s=replace(s, '''    if (showApps) AppsDialog(act, accent) { showApps = false }
}''', '''    if (showApps) AppsDialog(
        act, accent, { showApps = false }, lang,
        if (addingShortcut) { item ->
            persistShortcuts(shortcutPkgs + item.pkg)
            addingShortcut = false
            showApps = false
        } else null
    )
    shortcutOptions?.let { pkg ->
        AlertDialog(
            onDismissRequest = { shortcutOptions = null },
            title = { Text(t("apps")) },
            text = { Text(pkg.substringAfterLast(".")) },
            confirmButton = {
                TextButton(onClick = {
                    persistShortcuts(shortcutPkgs - pkg)
                    shortcutOptions = null
                }) { Text(t("remove")) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        act.tryStart(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))
                        shortcutOptions = null
                    }) { Text(t("uninstall")) }
                    TextButton(onClick = { shortcutOptions = null }) { Text(t("cancel")) }
                }
            }
        )
    }
    if (tvOpen) TelevisionWindow(
        radioCountry, lang, speed,
        loc != null && lastFixAt > 0L &&
            SystemClock.elapsedRealtime() - lastFixAt < 15_000L && speed < 5,
        accent
    ) { tvOpen = false }
    if (widgetsOpen) DashboardWidgets(act, lang, accent) { widgetsOpen = false }
}''', "dialogs overlay")

s=replace(s, '''fun Shortcut(icon: String, label: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E1A10)).clickable { onClick() }.padding(8.dp),''', '''fun Shortcut(
    icon: String, label: String, tint: Color, onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E1A10))
            .pointerInput(onClick, onLongClick) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongClick() })
            }.padding(8.dp),''', "long press shortcuts")

s=replace(s, '''fun AppsDialog(ctx: Context, accent: Color, onDismiss: () -> Unit) {''', '''fun AppsDialog(
    ctx: Context, accent: Color, onDismiss: () -> Unit,
    lang: String = "tr", onSelect: ((AppItem) -> Unit)? = null
) {
    var longPressed by remember { mutableStateOf<AppItem?>(null) }
    longPressed?.let { app ->
        AlertDialog(
            onDismissRequest = { longPressed = null },
            title = { Text(app.label) },
            text = { Text(app.pkg) },
            confirmButton = {
                TextButton(onClick = {
                    if (onSelect != null) onSelect(app)
                    longPressed = null
                }) { Text(uiText(lang, "addShortcut")) }
            },
            dismissButton = {
                TextButton(onClick = {
                    ctx.tryStart(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.pkg}")))
                    longPressed = null
                }) { Text(uiText(lang, "uninstall")) }
            }
        )
    }''', "apps dialog signature")
s=replace(s, '''                        Modifier.clickable { ctx.launchPkg(a.pkg); onDismiss() }.padding(8.dp),''', '''                        Modifier.pointerInput(a.pkg, onSelect) {
                            detectTapGestures(
                                onTap = { if (onSelect != null) onSelect(a) else { ctx.launchPkg(a.pkg); onDismiss() } },
                                onLongPress = { longPressed = a }
                            )
                        }.padding(8.dp),''', "apps pick and long press")
# Keep original shortcut tiles, but show the actual icon of user-added applications.
s = replace(s, '''                            Shortcut(icon, name, color, { act.launchPkg(pkg) }, { shortcutOptions = pkg })''', '''                            val appIcon = remember(pkg) {
                                if (pkg in baseShortcuts) null
                                else runCatching {
                                    act.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap()
                                }.getOrNull()
                            }
                            Shortcut(icon, name, color, { act.launchPkg(pkg) },
                                { shortcutOptions = pkg }, appIcon)''', "real app icons")
s = replace(s, '''    onLongClick: () -> Unit = {}
) {
    Row(''', '''    onLongClick: () -> Unit = {},
    appIcon: androidx.compose.ui.graphics.ImageBitmap? = null
) {
    Row(''', "optional shortcut artwork")
s = replace(s, '''        Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(tint), contentAlignment = Alignment.Center) { T(icon, 18) }''',
'''        Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(tint),
            contentAlignment = Alignment.Center) {
            if (appIcon != null) Image(appIcon, null, Modifier.size(34.dp))
            else T(icon, 18)
        }''', "show shortcut artwork")

# Keep the live TV overlay within the original dashboard (not a blocking system Dialog).
# The bottom TV button stays visible and can be held for three seconds to close it.
s = replace(s, '''    // ---------- UI ----------
    Column(Modifier.fillMaxSize().background(Bg).padding(6.dp)) {''',
'''    // ---------- UI ----------
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(Bg).padding(6.dp)) {''', "non-modal parent layout")
s = replace(s, '''    }

    if (showApps) AppsDialog(''',
'''    }
    if (tvOpen) TelevisionWindow(
        radioCountry, lang, speed,
        loc != null && lastFixAt > 0L &&
            SystemClock.elapsedRealtime() - lastFixAt < 15_000L && speed < 5,
        accent, Modifier.align(Alignment.Center)
    ) { tvOpen = false }
    }

    if (showApps) AppsDialog(''', "live TV over existing content")
s = replace(s, '''    if (tvOpen) TelevisionWindow(
        radioCountry, lang, speed,
        loc != null && lastFixAt > 0L &&
            SystemClock.elapsedRealtime() - lastFixAt < 15_000L && speed < 5,
        accent
    ) { tvOpen = false }
    if (widgetsOpen) DashboardWidgets''',
'''    if (widgetsOpen) DashboardWidgets''', "remove prior modal TV placement")
# Weather labels and missing-GPS text must follow the selected language too.
s = replace(s, '''T(w?.let { wDesc(it.code, lang == "tr") } ?: "", 14)''',
'''T(w?.let { weatherText(it.code, lang) } ?: "", 14)''', "all-language weather")
s = replace(s, '''T("📍 " + if (loc == null) "Uşak / Banaz" else t("me"), 14)''',
'''T("📍 " + if (loc == null) t("gpsMissing") else placeName.ifBlank { t("me") }, 14,
                                        maxLines = 1)''', "real locality instead of fixed test coordinates")

# System-approved default-home role is the reliable Android 15 automatic boot flow.
# Never force-launch an Activity from BOOT_COMPLETED in the background.
s = replace(s, '''import android.annotation.SuppressLint''',
'''import android.annotation.SuppressLint
import android.app.role.RoleManager''', "HOME role imports")
s = replace(s, '''    val accents = listOf(Gold, Color(0xFF4DA3FF), Color(0xFF3DDC84))''',
'''    val homeRoleRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    fun requestHomeRoleIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val roles = act.getSystemService(RoleManager::class.java)
            if (roles?.isRoleAvailable(RoleManager.ROLE_HOME) == true &&
                !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                homeRoleRequest.launch(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
            }
        } else {
            act.tryStart(Intent(Settings.ACTION_HOME_SETTINGS))
        }
    }

    val accents = listOf(Gold, Color(0xFF4DA3FF), Color(0xFF3DDC84))''', "default home selection consent")
s = replace(s, '''NavItem("🏠", t("home"), navSel == "home", accent) { navSel = "home" }''',
'''NavItem("🏠", t("home"), navSel == "home", accent) {
                        navSel = "home"
                        requestHomeRoleIfNeeded()
                    }''', "HOME item selects default launcher role")

# Render the long-press action dialog above the app chooser, not behind it.
apps_start = s.index("fun AppsDialog(")
alert_start = s.index("    longPressed?.let { app ->", apps_start)
alert_end = s.index("    val apps = remember {", alert_start)
alert_code = s[alert_start:alert_end]
s = s[:alert_start] + s[alert_end:]
last_close = s.rfind("\n}")
if last_close < apps_start:
    raise SystemExit("Cannot locate AppsDialog closing brace")
s = s[:last_close] + "\n" + alert_code + s[last_close:]

# Nav entry must keep driving inside the existing embedded MapView.
s = replace(s,
'''NavItem("🧭", t("route"), navSel == "route", accent) { navSel = "route"; loc?.let { mapView.controller.animateTo(it) } }''',
'''NavItem("🧭", t("route"), navSel == "route", accent) {
                        navSel = "route"
                        drivingMode = true
                        mapView.controller.setZoom(17.0)
                        loc?.let { mapView.controller.setCenter(it) }
                    }''', "in-panel driving map entry")

MAIN.write_text(s,encoding="utf-8")

for kotlin in ["Translations.kt", "WorldMedia.kt", "DashboardWidgets.kt", "CameraAlerts.kt", "CarUiIcons.kt"]:
    shutil.copy2(HERE / kotlin, JAVA / kotlin)

g=GRADLE.read_text(encoding="utf-8")
g=replace(g, 'versionCode = 1\n        versionName = "1.0"',
'''versionCode = 3
        versionName = "1.2.1-dashboard-fix"''', "version")
g=replace(g, '    implementation("org.osmdroid:osmdroid-android:6.1.18")',
'''    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")''', "media3")
# Distinct debug package keeps the user's original working test APK installed.
g = replace(g, '''    buildTypes {
        release {''',
'''    buildTypes {
        debug {
            applicationIdSuffix = ".v21test"
            versionNameSuffix = "-test"
        }
        release {''', "isolated v2 debug applicationId")
# No debug signing of publish/release artifacts. The private pipeline will supply upload signing.
g = replace(g, '''            signingConfig = signingConfigs.getByName("debug")''',
'''            // PRIVATE signing only; never use debug key for a production release.''', "release signing isolation")
GRADLE.write_text(g,encoding="utf-8")

m=MANIFEST.read_text(encoding="utf-8")
m=replace(m, '    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />',
'''    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.BIND_APPWIDGET" />''', "widget permission")

# Selected premium black-gold CAR LAUNCHER logo; never modify the original ZIP asset.
from hashlib import sha256
logo_source = HERE / "assets" / "car_launcher_icon.jpg"
if not logo_source.is_file():
    raise SystemExit("Approved launcher icon asset missing")
logo_bytes = logo_source.read_bytes()
if sha256(logo_bytes).hexdigest() != "cf161e4b44f40f099cc12be78d27de5b13f603735ddd2f40a706e9108e5d1707":
    raise SystemExit("Launcher icon SHA-256 mismatch")
logo_dir = PROJECT / "app/src/main/res/drawable-nodpi"
logo_dir.mkdir(parents=True, exist_ok=True)
(logo_dir / "car_launcher_icon.jpg").write_bytes(logo_bytes)
m = replace(m, 'android:icon="@drawable/ic_launcher"',
    'android:icon="@drawable/car_launcher_icon"', "approved launcher icon")

MANIFEST.write_text(m,encoding="utf-8")
print(f"Dashboard v2 patch applied to {MAIN}; original packed ZIP remains unchanged")
