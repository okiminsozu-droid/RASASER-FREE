package com.rasaser.free.kurankelime

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Obsidian = Color(0xFF092B45)
private val Ink = Color(0xFF061426)
private val Panel = Color(0xFF1D3557)
private val Gold = Color(0xFFE8B85B)
private val Paper = Color(0xFFF8F5ED)
private val PaperCard = Color(0xFFEEE8DD)
private val DarkText = Color(0xFF242F39)
private val PastelBlue = Color(0xFF264B6B)
private val PastelGreen = Color(0xFF215A50)
private val PastelBeige = Color(0xFF505D6D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WordByWordApplication() }
    }
    override fun onDestroy() {
        OfflineRecitation.stop()
        super.onDestroy()
    }
}

@Composable
private fun WordByWordApplication() {
    val context = LocalContext.current
    val store = remember { QuranStore(context.applicationContext) }
    val verses = remember { store.verses() }
    var screen by rememberSaveable { mutableStateOf("home") }
    var ayah by rememberSaveable { mutableIntStateOf(store.lastAyah()) }
    var language by rememberSaveable { mutableStateOf(store.language()) }
    var dark by rememberSaveable { mutableStateOf(store.isDark()) }
    var favoritesRevision by remember { mutableIntStateOf(0) }
    var wordMenu by remember { mutableStateOf<WordToken?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<SearchHit>()) }
    var chooseLanguage by remember { mutableStateOf(false) }

    val scheme = if (dark) darkColorScheme(
        primary = Gold, onPrimary = Ink, background = Obsidian,
        onBackground = Color.White, surface = Ink, onSurface = Color.White
    ) else lightColorScheme(
        primary = Color(0xFF8E6429), onPrimary = Color.White, background = Paper,
        onBackground = DarkText, surface = PaperCard, onSurface = DarkText
    )

    LaunchedEffect(screen, query, language) {
        if (screen == "search") {
            searchResults = withContext(Dispatchers.IO) { store.search(query, language) }
        }
    }

    fun openAyah(number: Int) {
        ayah = number.coerceIn(1, verses.size)
        store.saveProgress(ayah)
        screen = "reader"
    }
    fun setLanguage(code: String) {
        language = code
        store.saveLanguage(code)
        chooseLanguage = false
    }
    val readerWords = remember(ayah) { store.words(1, ayah) }
    val readerVerse = verses.firstOrNull { it.ayah == ayah } ?: verses.first()
    val onShareVerse: () -> Unit = {
        shareText(context, "Fâtiha " + ayah + "\n" + readerVerse.arabic + "\n\n" +
            readerWords.joinToString("\n") { it.arabic + " — " + it.meaning(language) })
    }

    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = scheme.background,
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (screen != "home") {
                        IconButton(onClick = { screen = "home" }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Ana ekrana dön")
                        }
                    }
                    Text(
                        "Kur’an Kelime Atölyesi",
                        modifier = Modifier.weight(1f),
                        color = scheme.primary,
                        fontSize = if (screen == "home") 20.sp else 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box {
                        IconButton(onClick = { chooseLanguage = true }) {
                            Icon(Icons.Default.Language, contentDescription = "Çeviri dili")
                        }
                        DropdownMenu(expanded = chooseLanguage, onDismissRequest = { chooseLanguage = false }) {
                            for ((code, label) in languageLabels) {
                                DropdownMenuItem(
                                    text = { Text((if (code == language) "✓ " else "") + label) },
                                    onClick = { setLanguage(code) }
                                )
                            }
                        }
                    }
                    if (screen == "home") {
                        IconButton(onClick = { screen = "search" }) {
                            Icon(Icons.Default.Search, contentDescription = "Ara")
                        }
                        IconButton(onClick = { screen = "favorites" }) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorilerim")
                        }
                        IconButton(onClick = { dark = !dark; store.saveDark(dark) }) {
                            Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Gündüz / gece")
                        }
                    }
                }
            },
            bottomBar = {
                if (screen == "reader") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (dark) Panel else PaperCard),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            store.toggleVerseFavorite(ayah)
                            favoritesRevision++
                        }) {
                            Icon(
                                if (store.isFavoriteVerse(ayah)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Ayeti favorilere ekle",
                                tint = if (store.isFavoriteVerse(ayah)) Gold else scheme.onSurface
                            )
                        }
                        IconButton(onClick = onShareVerse) {
                            Icon(Icons.Default.Share, contentDescription = "Ayeti paylaş")
                        }
                        IconButton(onClick = { OfflineRecitation.verse(context, 1, ayah) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Ayeti baştan dinle")
                        }
                    }
                }
            }
        ) { inner ->
            when (screen) {
                "home" -> HomeBody(
                    lastAyah = store.lastAyah(), onContinue = { openAyah(store.lastAyah()) },
                    onOpen = ::openAyah,
                    modifier = Modifier.padding(inner)
                )
                "reader" -> ReaderBody(
                    ayah = ayah,
                    verse = readerVerse,
                    words = readerWords,
                    language = language,
                    dark = dark,
                    onWord = { token ->
                        store.saveProgress(ayah, token.position)
                        wordMenu = token
                    },
                    onVerseAudio = { OfflineRecitation.verse(context, 1, ayah) },
                    onPrevious = { openAyah(ayah - 1) },
                    onNext = { openAyah(ayah + 1) },
                    modifier = Modifier.padding(inner)
                )
                "search" -> SearchBody(
                    query = query,
                    onQuery = { query = it },
                    results = searchResults,
                    verses = verses,
                    onOpen = ::openAyah,
                    modifier = Modifier.padding(inner)
                )
                "favorites" -> FavoritesBody(
                    favoriteVerses = remember(favoritesRevision) { store.favoriteVerses() },
                    favoriteWords = remember(favoritesRevision) { store.favoriteWords() },
                    language = language,
                    onOpen = ::openAyah,
                    modifier = Modifier.padding(inner)
                )
            }
        }
        val chosen = wordMenu
        if (chosen != null) {
            WordActionSheet(
                word = chosen,
                language = language,
                favorite = store.isFavoriteWord(chosen.id),
                onDismiss = { wordMenu = null },
                onFavorite = {
                    store.toggleWordFavorite(chosen.id)
                    favoritesRevision++
                },
                onShare = {
                    shareText(context, "Fâtiha " + chosen.ayah + ":" + chosen.position + "\n" +
                        chosen.arabic + "\n" + chosen.meaning(language))
                },
                onLanguage = ::setLanguage,
                onListen = { OfflineRecitation.word(context, chosen.sura, chosen.ayah, chosen.position) }
            )
        }
    }
}

private val languageLabels = listOf(
    "tr" to "Türkçe", "en" to "English", "de" to "Deutsch",
    "es" to "Español", "zh" to "中文"
)

@Composable
private fun HomeBody(
    lastAyah: Int, onContinue: () -> Unit, onOpen: (Int) -> Unit, modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Zihinsel Huzur ve Odak", color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
        Card(onClick = onContinue, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Kaldığın yer", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
                    Text("Fâtiha Suresi · " + lastAyah + ". Ayet", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Text("Sûre / Cüz / Sayfa", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Card(
            onClick = { onOpen(1) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("1 · Fâtiha", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("7 ayet · 29 kelime · 5 dil", fontSize = 12.sp)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = "Fâtiha'yı aç", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Text(
            "Önizleme sürümü: Şimdilik yalnız Fâtiha vardır. 114 sûrenin doğrulanmış kelime verileri ve kıraat sesleri henüz eklenmemiştir.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .67f),
            fontSize = 12.sp
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReaderBody(
    ayah: Int,
    verse: Verse,
    words: List<WordToken>,
    language: String,
    dark: Boolean,
    onWord: (WordToken) -> Unit,
    onVerseAudio: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().clickable { onVerseAudio() }.padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("FÂTİHA SURESİ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(ayah.toString() + ". Ayet", fontSize = 13.sp)
                }
                Icon(Icons.Default.VolumeUp, contentDescription = "Ayetin tamamını dinle", tint = MaterialTheme.colorScheme.primary)
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        maxItemsInEachRow = 3
                    ) {
                        for (word in words) {
                            Column(
                                modifier = Modifier.widthIn(min = 85.dp, max = 150.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onWord(word) }
                                    .padding(vertical = 9.dp, horizontal = 3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    word.arabic,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 27.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 43.sp
                                )
                                Spacer(Modifier.height(7.dp))
                                Surface(
                                    color = when (word.kind) {
                                        "verb" -> if (dark) PastelBlue else Color(0xFFD8EAF5)
                                        "noun" -> if (dark) PastelGreen else Color(0xFFD4EDE3)
                                        else -> if (dark) PastelBeige else Color(0xFFE7E1D7)
                                    },
                                    shape = RoundedCornerShape(9.dp)
                                ) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(
                                            word.meaning(language),
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                            textAlign = TextAlign.Center,
                                            color = if (dark) Color.White else DarkText,
                                            fontSize = 13.sp,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onPrevious, enabled = ayah > 1) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = null)
                    Text("Önceki")
                }
                TextButton(onClick = onNext, enabled = ayah < 7) {
                    Text("Sonraki")
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
            Text(
                "Bir kelimeye dokun: favori, paylaş, çevir veya dinle.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .66f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordActionSheet(
    word: WordToken,
    language: String,
    favorite: Boolean,
    onDismiss: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onLanguage: (String) -> Unit,
    onListen: () -> Unit
) {
    var languagesExpanded by remember(word.id) { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(word.arabic, fontFamily = FontFamily.Serif, fontSize = 34.sp)
            Spacer(Modifier.height(6.dp))
            Text(word.meaning(language), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                WordAction(
                    if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    "Favori", onFavorite
                )
                WordAction(Icons.Default.Share, "Paylaş", onShare)
                WordAction(Icons.Default.Translate, "Çevir") { languagesExpanded = !languagesExpanded }
                WordAction(Icons.Default.VolumeUp, "Dinle", onListen)
            }
            if (languagesExpanded) {
                Spacer(Modifier.height(16.dp))
                Text("Kelimenin dilini seç", fontWeight = FontWeight.Medium)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((code, label) in languageLabels) {
                        FilterChip(
                            selected = language == code,
                            onClick = { onLanguage(code) },
                            label = { Text(label) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun WordAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(54.dp)) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        }
        Text(title, fontSize = 12.sp)
    }
}

@Composable
private fun SearchBody(
    query: String,
    onQuery: (String) -> Unit,
    results: List<SearchHit>,
    verses: List<Verse>,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query, onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Kelime ara: rahmet, mercy, رحمن…") },
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        if (query.isBlank()) {
            Text("Arapça veya seçili çeviri dilinde kelime ara.", fontSize = 13.sp)
        } else {
            val unique = results.distinctBy { it.ayah }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(unique, key = { it.ayah }) { hit ->
                    val verseArabic = verses.firstOrNull { it.ayah == hit.ayah }?.arabic.orEmpty()
                    Card(
                        onClick = { onOpen(hit.ayah) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text("Fâtiha Suresi · " + hit.ayah + ". Ayet",
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Text(verseArabic, fontFamily = FontFamily.Serif, fontSize = 22.sp,
                                textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            Text(hit.arabic + " · " + hit.meaning,
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesBody(
    favoriteVerses: List<Int>,
    favoriteWords: List<WordToken>,
    language: String,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Favorilerim", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        item { Text("Ayetler", fontSize = 15.sp, color = MaterialTheme.colorScheme.primary) }
        if (favoriteVerses.isEmpty()) {
            item { Text("Henüz favori ayet yok.", fontSize = 13.sp) }
        } else {
            items(favoriteVerses) { ayah ->
                Card(onClick = { onOpen(ayah) }) {
                    Text("Fâtiha · " + ayah + ". Ayet", Modifier.fillMaxWidth().padding(16.dp))
                }
            }
        }
        item { Text("Kelimeler", fontSize = 15.sp, color = MaterialTheme.colorScheme.primary) }
        if (favoriteWords.isEmpty()) {
            item { Text("Henüz favori kelime yok.", fontSize = 13.sp) }
        } else {
            items(favoriteWords, key = { "w" + it.id }) { word ->
                Card(onClick = { onOpen(word.ayah) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Fâtiha " + word.ayah + ":" + word.position, fontSize = 11.sp)
                            Text(word.meaning(language), color = MaterialTheme.colorScheme.primary)
                        }
                        Text(word.arabic, fontFamily = FontFamily.Serif, fontSize = 24.sp)
                    }
                }
            }
        }
    }
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Paylaş"))
}

private object OfflineRecitation {
    private var player: MediaPlayer? = null
    fun word(context: Context, sura: Int, ayah: Int, position: Int) {
        play(context, "audio/word/" + sura + "_" + ayah + "_" + position + ".mp3")
    }
    fun verse(context: Context, sura: Int, ayah: Int) {
        play(context, "audio/verse/" + sura + "_" + ayah + ".mp3")
    }
    private fun play(context: Context, file: String) {
        stop()
        try {
            val afd = context.assets.openFd(file)
            val mp = MediaPlayer()
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.prepare()
            player = mp
            mp.setOnCompletionListener {
                it.release()
                if (player === it) player = null
            }
            mp.start()
        } catch (_: Exception) {
            stop()
            Toast.makeText(context, "Doğrulanmış çevrimdışı kıraat sesi henüz eklenmedi.", Toast.LENGTH_LONG).show()
        }
    }
    fun stop() {
        player?.runCatching { stop(); release() }
        player = null
    }
}
