package com.rasaser.launcher

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.util.Locale

data class RadioStation(val title: String, val url: String, val stationId: String)
data class TvChannel(val title: String, val url: String)
data class CountryEntry(val iso: String, val name: String, val flag: String)

fun worldCountries(lang: String): List<CountryEntry> {
    val locale = Locale.forLanguageTag(lang)
    return Locale.getISOCountries().mapNotNull { iso ->
        if (iso.length != 2) return@mapNotNull null
        val region = Locale("", iso)
        val name = region.getDisplayCountry(locale).ifBlank { iso }
        val flag = iso.uppercase(Locale.ROOT).map { letter ->
            String(Character.toChars(0x1F1E6 + (letter.code - 'A'.code)))
        }.joinToString("")
        CountryEntry(iso, name, flag)
    }.sortedBy { it.name.lowercase(locale) }
}

fun fetchWorldRadio(code: String): List<RadioStation> {
    val country = code.uppercase(Locale.ROOT).takeIf { it.length == 2 && it.all { c -> c in 'A'..'Z' } } ?: return emptyList()
    val servers = listOf("https://de1.api.radio-browser.info", "https://nl1.api.radio-browser.info")
    for (server in servers) {
        try {
            val response = httpGet(
                "$server/json/stations/bycountrycodeexact/$country?hidebroken=true&order=clickcount&reverse=true&limit=150"
            )
            val array = JSONArray(response)
            val result = mutableListOf<RadioStation>()
            for (i in 0 until array.length()) {
                val station = array.getJSONObject(i)
                val url = station.optString("url_resolved").ifBlank { station.optString("url") }
                val name = station.optString("name").trim()
                if (url.startsWith("https://", true) && name.isNotBlank()) {
                    result += RadioStation(name, url, station.optString("stationuuid"))
                }
            }
            return result.distinctBy { it.url }
        } catch (_: Exception) { /* try a second public server */ }
    }
    return emptyList()
}

fun loadInternetTV(countryCode: String): List<TvChannel> {
    val country = countryCode.lowercase(Locale.ROOT).takeIf { it.length == 2 && it.all { c -> c in 'a'..'z' } } ?: return emptyList()
    // Public link directory: broadcasters control streams and their rights. No channel is packaged or rebroadcast.
    val playlist = httpGet("https://iptv-org.github.io/iptv/countries/$country.m3u")
    val found = mutableListOf<TvChannel>()
    var title = ""
    for (line in playlist.lineSequence()) {
        val value = line.trim()
        if (value.startsWith("#EXTINF:", true)) title = value.substringAfterLast(',').trim().take(80)
        else if (value.startsWith("https://", true)) {
            if (title.isNotEmpty() && (value.contains(".m3u8", true) || value.contains(".mp4", true))) {
                found += TvChannel(title, value)
            }
            title = ""
        }
        if (found.size >= 100) break
    }
    return found.distinctBy { it.url }
}

@Composable
fun CountryMenu(lang: String, selected: String, accent: Color, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val countries = remember(lang) { worldCountries(lang) }
    val current = countries.find { it.iso == selected }
    Box {
        Text(
            text = "${current?.flag ?: "🌍"} ${current?.name ?: selected} ▾",
            color = accent, fontSize = 12.sp,
            modifier = Modifier.clickable { expanded = true }.padding(vertical = 3.dp)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Column(Modifier.heightIn(max = 320.dp)) {
                countries.forEach { entry ->
                    DropdownMenuItem(
                        text = { Text("${entry.flag} ${entry.name}", fontSize = 13.sp) },
                        onClick = { expanded = false; onSelect(entry.iso) }
                    )
                }
            }
        }
    }
}

@Composable
fun TelevisionWindow(
    country: String, lang: String, speedKmh: Int, accent: Color,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var selectedCountry by remember { mutableStateOf(country) }
    var channels by remember { mutableStateOf<List<TvChannel>>(emptyList()) }
    var chosen by remember { mutableStateOf<TvChannel?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val player = remember(context) { ExoPlayer.Builder(context).build() }

    DisposableEffect(player) {
        onDispose { player.stop(); player.release() }
    }

    LaunchedEffect(selectedCountry) {
        chosen = null
        channels = emptyList()
        player.stop()
        loading = true
        error = ""
        channels = try { withContext(Dispatchers.IO) { loadInternetTV(selectedCountry) } }
            catch (_: Exception) { error = uiText(lang, "tvUnavailable"); emptyList() }
        loading = false
    }
    LaunchedEffect(chosen, speedKmh) {
        if (speedKmh >= 5 || chosen == null) {
            player.pause()
        } else {
            try {
                player.setMediaItem(MediaItem.fromUri(chosen!!.url))
                player.prepare()
                player.playWhenReady = true
            } catch (_: Exception) { error = uiText(lang, "streamError") }
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.82f).fillMaxHeight(0.78f),
            color = Color(0xFF15110B),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(2.dp, accent)
        ) {
            Column(Modifier.fillMaxSize().padding(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(uiText(lang, "tv"), color = accent)
                    Spacer(Modifier.width(12.dp))
                    CountryMenu(lang, selectedCountry, accent) { selectedCountry = it }
                    Spacer(Modifier.weight(1f))
                    Text("✕", color = Color.White, fontSize = 22.sp,
                        modifier = Modifier.clickable(onClick = onClose).padding(5.dp))
                }
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(2f).fillMaxHeight().background(Color.Black), contentAlignment = Alignment.Center) {
                        if (speedKmh >= 5) {
                            Text(uiText(lang, "tvParked"), color = Color.White, modifier = Modifier.padding(16.dp))
                        } else if (chosen == null) {
                            Text(uiText(lang, "selectChannel"), color = Color.LightGray)
                        } else {
                            AndroidView(factory = { PlayerView(it).apply {
                                useController = true
                                this.player = player
                            } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        if (loading) CircularProgressIndicator(color = accent)
                        if (error.isNotEmpty()) Text(error, color = Color.Yellow, fontSize = 12.sp)
                        LazyColumn {
                            items(channels) { ch ->
                                Text(ch.title, color = if (chosen == ch) accent else Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.fillMaxWidth().clickable { chosen = ch }.padding(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
