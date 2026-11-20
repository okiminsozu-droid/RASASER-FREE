package com.rasaser.free.trendnabiz

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.URLEncoder
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private val sky = Color(0xFFDDF3FF)
private val pale = Color(0xFFF8FDFF)
private val azure = Color(0xFF0C86D3)
private val orange = Color(0xFFFF9529)
private val ink = Color(0xFF131C27)
private val red = Color(0xFFE52E40)

class MainActivity : ComponentActivity() {
    private var incoming by mutableStateOf<Pair<String, String>?>(null)
    private var speechEngine: TextToSpeech? = null
    private val uiHandler = Handler(Looper.getMainLooper())
    private var liveClient: LiveNewsClient? = null
    private var liveStories by mutableStateOf<List<NewsItem>>(emptyList())
    private var liveConnected by mutableStateOf(false)
    private var liveState by mutableStateOf("connecting")
    private var notificationStory by mutableStateOf<NewsItem?>(null)

    private fun newLiveStory(story: NewsItem) {
        val now=System.currentTimeMillis()
        if(story.publishedAt <= now-180000L) return
        liveStories=(listOf(story)+liveStories.filterNot { it.id == story.id })
            .filter { it.publishedAt > now-180000L }.take(40)
        NewsNotifications.publish(this,story)
        uiHandler.postDelayed({
            liveStories=liveStories.filter { it.publishedAt > System.currentTimeMillis()-180000L }
        },(story.publishedAt+180000L-now).coerceAtLeast(0L)+750L)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NewsNotifications.createChannels(this)
        UserPrefs.clearLegacyNewsCache(this)
        liveClient = LiveNewsClient(::newLiveStory, { liveConnected = it }, { liveState = it })
        incoming = readAction(intent)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = azure, secondary = orange, background = sky, onBackground = ink
            )) {
                TrendNabizApp(incoming, notificationStory, liveStories, liveConnected, liveState, { liveClient?.stop(); liveClient?.start() }, { liveClient?.requestCloudTest() == true }, { incoming=null; notificationStory=null }, ::executePremiumAction)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incoming = readAction(intent)
    }

    private fun readAction(intent: Intent?): Pair<String, String>? {
        val kind = intent?.getStringExtra(NewsNotifications.EXTRA_ACTION) ?: return null
        notificationStory = NewsNotifications.readStory(intent)
        return kind to (intent.getStringExtra(NewsNotifications.EXTRA_ID) ?: "")
    }

    private fun executePremiumAction(story: NewsItem, kind: String, lang: UiLang) {
        if (!UserPrefs.isPremium(this)) return
        when (kind) {
            "translate" -> {
                // Explicit user action: external Google Translate, no hidden API or background transfer.
                val combined = story.title + "\n" + story.description
                val encoded = URLEncoder.encode(combined, "UTF-8")
                val uri = Uri.parse("https://translate.google.com/?sl=auto&tl=${lang.code}&text=$encoded&op=translate")
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
            "listen" -> {
                val phrase = story.title + ". " + story.description
                speechEngine?.stop()
                speechEngine?.shutdown()
                speechEngine = TextToSpeech(this) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        speechEngine?.language = Locale.forLanguageTag(lang.tag)
                        speechEngine?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "trendnabiz_story")
                    }
                }
            }
            "share" -> {
                val text = "${story.title}\n${story.description}\n${story.source}: ${story.sourceUrl}"
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                startActivity(Intent.createChooser(share, null))
            }
        }
    }

    override fun onStart() {
        super.onStart()
        liveClient?.start()
    }
    override fun onStop() {
        liveClient?.stop()
        liveStories=emptyList()
        uiHandler.removeCallbacksAndMessages(null)
        super.onStop()
    }
    override fun onDestroy() {
        speechEngine?.stop()
        speechEngine?.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun TrendNabizApp(
    externalAction: Pair<String, String>?,
    notificationStory: NewsItem?,
    liveItems: List<NewsItem>,
    isConnected: Boolean,
    connectionState: String,
    reconnect: () -> Unit,
    cloudTest: () -> Boolean,
    actionConsumed: () -> Unit,
    performAction: (NewsItem, String, UiLang) -> Unit
) {
    val context = LocalContext.current
    var languageCode by rememberSaveable { mutableStateOf(UserPrefs.language(context).code) }
    val lang = UiLang.parse(languageCode)
    var currentPage by rememberSaveable { mutableStateOf("home") }
    val stories = liveItems
    var criticalOnly by remember { mutableStateOf(UserPrefs.criticalOnly(context)) }
    var debugPremium by remember { mutableStateOf(UserPrefs.isPremium(context)) }
    var backgroundWatching by remember { mutableStateOf(UserPrefs.monitoring(context)) }
    var notificationAllowed by remember { mutableStateOf(NewsNotifications.allowed(context)) }

    val requestNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notificationAllowed = NewsNotifications.allowed(context) }

    LaunchedEffect(externalAction) {
        val action = externalAction ?: return@LaunchedEffect
        val (kind, id) = action
        if (kind == "premium") {
            currentPage = "premium"
        } else if (kind == "open") {
            currentPage = "home"
        } else if (!UserPrefs.isPremium(context)) {
            currentPage = "premium"
        } else {
            val item = stories.firstOrNull { it.id == id } ?: notificationStory
            if (item != null) performAction(item, kind, lang)
        }
        actionConsumed()
    }

    val actionForStory: (NewsItem, String) -> Unit = { story, kind ->
        if (UserPrefs.isPremium(context)) performAction(story, kind, lang)
        else currentPage = "premium"
    }

    Surface(color = sky, modifier = Modifier.fillMaxSize()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().background(pale).padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage != "home") {
                    IconButton(onClick = { currentPage = "home" }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = UiText.get(lang, "back"), tint = ink)
                    }
                }
                Text("TREND", fontWeight = FontWeight.Black, fontSize = 21.sp, color = azure)
                Text("NABIZ", fontWeight = FontWeight.Black, fontSize = 21.sp, color = red)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { currentPage = "premium" }) {
                    Icon(Icons.Filled.Star, contentDescription = UiText.get(lang, "premium"), tint = orange)
                }
                IconButton(onClick = { currentPage = "settings" }) {
                    Icon(Icons.Filled.Language, contentDescription = UiText.get(lang, "language"), tint = azure)
                }
                IconButton(onClick = { currentPage = "settings" }) {
                    Icon(Icons.Filled.Settings, contentDescription = UiText.get(lang, "settings"), tint = ink)
                }
            }

            when (currentPage) {
                "premium" -> {
                    PremiumPage(lang, modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp))
                }
                "licenses" -> {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
                        Text(UiText.get(lang,"about"),fontSize=22.sp,fontWeight=FontWeight.Black,color=ink)
                        val licenses=remember { context.assets.open("legal/licenses.txt").bufferedReader().use { it.readText() } }
                        Text(licenses,color=ink,fontSize=13.sp,lineHeight=21.sp)
                    }
                }
                "settings" -> {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(UiText.get(lang, "settings"), fontWeight = FontWeight.Black, fontSize = 24.sp, color = ink)
                        Text(UiText.get(lang, "language"), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = ink)
                        UiLang.entries.forEach { choice ->
                            val selected = choice == lang
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    languageCode = choice.code
                                    UserPrefs.setLanguage(context, choice)
                                },
                                colors = CardDefaults.cardColors(containerColor = if (selected) orange else pale)
                            ) {
                                Text(choice.display, modifier = Modifier.padding(16.dp), color = ink, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                        HorizontalDivider(color = azure.copy(alpha = .22f))
                        Text(UiText.get(lang, "alerts"), color = ink, fontWeight = FontWeight.Bold)
                        if (!notificationAllowed) {
                            OrangeButton(UiText.get(lang, "notificationsOff")) {
                                if (Build.VERSION.SDK_INT >= 33) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                                else notificationAllowed = NewsNotifications.allowed(context)
                            }
                        } else {
                            Text("✓ " + UiText.get(lang, "alerts"), color = ink)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(UiText.get(lang, "notifyCritical"), modifier = Modifier.weight(1f), color = ink)
                            Switch(checked = criticalOnly, onCheckedChange = {
                                criticalOnly = it
                                UserPrefs.setCriticalOnly(context, it)
                            })
                        }
                        Text(if (criticalOnly) UiText.get(lang, "notifyCritical") else UiText.get(lang, "notifyNormal"), color = ink)

                        if (BuildConfig.DEBUG) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(UiText.get(lang,"background"), modifier=Modifier.weight(1f), color=ink, fontWeight=FontWeight.SemiBold)
                                Switch(checked=backgroundWatching,onCheckedChange={enabled ->
                                    if(enabled && !NewsNotifications.allowed(context)){
                                        if(Build.VERSION.SDK_INT>=33) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        try {
                                            val intent=Intent(context,LiveMonitoringService::class.java).setAction(
                                                if(enabled) LiveMonitoringService.ACTION_START else LiveMonitoringService.ACTION_STOP
                                            )
                                            if(enabled) androidx.core.content.ContextCompat.startForegroundService(context,intent)
                                            else context.startService(intent)
                                            backgroundWatching=enabled
                                        } catch(_:Exception) { backgroundWatching=false }
                                    }
                                })
                            }
                            Text(UiText.get(lang,"backgroundHint"),color=ink,fontSize=12.sp)
                        }

                        OrangeButton(UiText.get(lang, "test")) {
                            if (!NewsNotifications.allowed(context) && Build.VERSION.SDK_INT >= 33) {
                                requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else NewsNotifications.test(context)
                        }
                        if(isConnected) {
                            OrangeButton(UiText.get(lang,"cloudTest")) {
                                if(!cloudTest()) NewsNotifications.test(context)
                            }
                            Text(UiText.get(lang,"cloudTestHint"),color=ink,fontSize=12.sp)
                        }

                        if (BuildConfig.DEBUG) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(UiText.get(lang, "debugPremium"), modifier = Modifier.weight(1f), color = ink)
                                Switch(checked = debugPremium, onCheckedChange = {
                                    UserPrefs.setDebugPremium(context, it)
                                    debugPremium = it
                                    LatestNewsWidget.refresh(context)
                                })
                            }
                            Text(UiText.get(lang, "debugNotice"), color = ink, fontSize = 11.sp)
                        }
                        OrangeButton(UiText.get(lang,"about")) { currentPage="licenses" }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            BrandHeader(lang)
                            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = azure)
                                    Spacer(Modifier.width(7.dp))
                                    Text(UiText.get(lang, "feed"), color = ink, fontWeight = FontWeight.Black, fontSize = 21.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(UiText.get(lang, "live"), color = ink)
                                Text(UiText.get(lang, if(isConnected) "liveConnected" else "liveDisconnected"), color = ink, fontSize = 12.sp)
                                if(!isConnected && connectionState.startsWith("error:")) Text(UiText.get(lang,"connectionError")+" "+connectionState.removePrefix("error:"),color=red,fontSize=12.sp)
                                Spacer(Modifier.height(8.dp))
                                if (!notificationAllowed) {
                                    OrangeButton(UiText.get(lang, "notificationsOff")) {
                                        if (Build.VERSION.SDK_INT >= 33) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    Spacer(Modifier.height(7.dp))
                                }
                                OrangeButton(UiText.get(lang, "reconnect")) { reconnect() }
                                if (stories.isEmpty()) Text(UiText.get(lang, "liveEmpty"), color = ink)
                            }
                        }
                        items(stories, key = { it.id }) { story ->
                            NewsCard(story, lang, actionForStory)
                        }
                        item { Spacer(Modifier.height(28.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandHeader(lang: UiLang) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.brand_logo),
            contentDescription = "TRENDNABIZ • RaSaSeR logo",
            modifier = Modifier.size(126.dp),
            contentScale = ContentScale.Fit
        )
        Text("Sana onların haberleri geldi mi?", color = ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("Did you receive their news?", color = ink, fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(3.dp))
        Text("RaSaSeR", fontWeight = FontWeight.Black, color = azure, fontSize = 17.sp)
        Text("APPS & TECHNOLOGY", fontWeight = FontWeight.Bold, color = ink, fontSize = 10.sp)
        Text(
            "www.okiminsozu.com",
            modifier = Modifier.clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.okiminsozu.com")))
            },
            fontWeight = FontWeight.Bold, color = azure, fontSize = 12.sp
        )
    }
}

@Composable
private fun NewsCard(
    story: NewsItem,
    lang: UiLang,
    onAction: (NewsItem, String) -> Unit
) {
    val context = LocalContext.current
    val displayDate = remember(story.publishedAt, lang) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.forLanguageTag(lang.tag))
            .format(Date(story.publishedAt))
    }
    val info = NewsLocalization.description(story, lang)
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = pale),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(R.drawable.brand_mark), contentDescription = "TRENDNABIZ", modifier = Modifier.size(42.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(story.title, fontWeight = FontWeight.Black, color = ink, fontSize = 18.sp)
                    Text(displayDate, color = ink, fontSize = 12.sp)
                }
                if (story.critical) Icon(Icons.Filled.Campaign, contentDescription = null, tint = red)
            }
            Text(info, color = ink, fontSize = 15.sp)
            Text(
                "${UiText.get(lang, "source")}: ${story.source} ↗",
                color = azure, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.clickable {
                    if (story.sourceUrl.startsWith("https://")) {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(story.sourceUrl))) }
                        catch (_: ActivityNotFoundException) { }
                    }
                }
            )
            HorizontalDivider(color = azure.copy(alpha = .20f))
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                NewsActionButton(Icons.Filled.Translate, UiText.get(lang, "translate")) { onAction(story, "translate") }
                NewsActionButton(Icons.Filled.VolumeUp, UiText.get(lang, "listen")) { onAction(story, "listen") }
                NewsActionButton(Icons.Filled.Share, UiText.get(lang, "share")) { onAction(story, "share") }
            }
        }
    }
}

@Composable
private fun NewsActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.background(orange, RoundedCornerShape(12.dp))) {
            Icon(icon, contentDescription = label, tint = ink)
        }
        Text(label, color = ink, fontSize = 11.sp)
    }
}

@Composable
private fun OrangeButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = orange, contentColor = ink)
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PremiumPage(lang: UiLang, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(66.dp), tint = orange)
        Text(UiText.get(lang, "premium"), fontWeight = FontWeight.Black, fontSize = 25.sp, color = ink)
        Text(UiText.get(lang, "premiumDetail"), color = ink, textAlign = TextAlign.Center)
        listOf("🌐 " + UiText.get(lang, "translate"), "🔊 " + UiText.get(lang, "listen"),
            "↗ " + UiText.get(lang, "share"), "▦ Widget").forEach { title ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = pale)) {
                Text(title, modifier = Modifier.padding(16.dp), color = ink, fontWeight = FontWeight.Bold)
            }
        }
        Text(UiText.get(lang, "purchase"), color = ink, fontWeight = FontWeight.Bold)
        Text(UiText.get(lang, "billingPending"), color = ink, fontSize = 12.sp)
    }
}
