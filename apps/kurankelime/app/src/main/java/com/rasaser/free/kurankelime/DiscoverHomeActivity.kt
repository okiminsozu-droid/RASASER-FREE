package com.rasaser.free.kurankelime

import android.graphics.BitmapFactory
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HomeSand = Color(0xFFE8CEAC)
private val HomeGreen = Color(0xFF17422F)
private val HomeBrown = Color(0xFF6D5036)
private val languages = listOf(
    "tr" to "🇹🇷 Türkçe", "en" to "🇬🇧 English", "de" to "🇩🇪 Deutsch",
    "ar" to "🇸🇦 العربية", "es" to "🇪🇸 Español", "zh" to "🇨🇳 中文"
)
private data class UiLabels(
    val title: String, val subtitle: String, val slogan: String,
    val read: String, val search: String, val favorites: String,
    val searchHint: String, val empty: String, val free: String
)
private fun labels(code: String) = when(code) {
    "en" -> UiLabels("DISCOVER THE QURAN","WORD-BY-WORD MEANING APP","A letter to you from your Creator",
        "Read","Search","Favorites","Search a word","No matches in the current preview index",
        "Free of charge. We have no commercial purpose.")
    "de" -> UiLabels("ENTDECKE DEN KORAN","WORT-FÜR-WORT-BEDEUTUNG","Ein Brief deines Schöpfers an dich",
        "Lesen","Suchen","Favoriten","Wort suchen","Keine Treffer im aktuellen Vorschauindex",
        "Kostenlos. Wir verfolgen keine kommerziellen Ziele.")
    "es" -> UiLabels("DESCUBRE EL CORÁN","SIGNIFICADO PALABRA POR PALABRA","Una carta para ti de tu Creador",
        "Leer","Buscar","Favoritos","Buscar palabra","Sin resultados en el índice preliminar",
        "Es gratis. No tenemos fines comerciales.")
    "ar" -> UiLabels("اكتشف القرآن","معاني الكلمات","رسالة إليك من خالقك",
        "اقرأ","ابحث","المفضلة","ابحث عن كلمة","لا توجد نتائج في الفهرس التجريبي",
        "تطبيق مجاني، وليس لنا أي هدف تجاري.")
    else -> UiLabels("KURANI KEŞFET","KELİME MEALLİ UYGULAMA","Yaratıcından sana bir mektup",
        "Oku","Ara","Favoriler","Kelime ara","Mevcut önizleme dizininde sonuç yok",
        "Ücretsizdir, hiçbir maddi amacımız yoktur.")
}

class DiscoverHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme=lightColorScheme(
                background=Color.White, surface=Color(0xFFFCF8F1), primary=HomeGreen
            )) { DiscoverHome() }
        }
    }
}

@Composable
private fun DiscoverHome() {
    val context = LocalContext.current
    val settings = remember { context.getSharedPreferences("discovery_home", android.content.Context.MODE_PRIVATE) }
    val store = remember { PageStore(context) }
    val quranIndex = remember { QuranStore(context) }
    val fullIndex = remember { PageWordIndex(context.applicationContext) }
    val localEngine = remember { PdfTextEngine(context.applicationContext) }
    val bundledIndex = remember { BundledWordIndex(context.applicationContext) }
    val bundledReady = remember { bundledIndex.isBundled }
    var language by remember { mutableStateOf(settings.getString("lang", "tr") ?: "tr") }
    var menu by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf(if ((context as? android.app.Activity)?.intent?.getBooleanExtra("open_search", false) == true) "search" else "home") }
    var query by remember { mutableStateOf("") }
    var updated by remember { mutableIntStateOf(0) }
    var savedWordHits by remember { mutableStateOf(emptyList<PageWord>()) }
    var fullHits by remember { mutableStateOf(emptyList<PageWord>()) }
    var onDeviceReady by remember { mutableStateOf(false) }
    var indexing by remember { mutableStateOf(false) }
    var scannedPages by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(304) }
    var noTextLayer by remember { mutableStateOf(false) }
    var bookPresent by remember { mutableStateOf(false) }
    LaunchedEffect(screen) {
        if (screen != "search") return@LaunchedEffect
        val file = if (bundledReady) null
            else withContext(Dispatchers.IO) { installedBook(context) }
        bookPresent = bundledReady || file != null
        if (file != null && Build.VERSION.SDK_INT >= 35 && !bundledReady && !fullIndex.hasFullIndex) {
            indexing = true
            try {
                onDeviceReady = withContext(Dispatchers.IO) {
                    localEngine.buildIfNeeded(file) { done, total ->
                        scannedPages = done
                        pageCount = total
                    }
                }
                noTextLayer = !onDeviceReady
            } catch (_: Exception) {
                // Malformed or image-only PDFs can still use an explicitly supplied private index.
                noTextLayer = true
            } finally {
                indexing = false
            }
        }
    }
    LaunchedEffect(screen, query, onDeviceReady, bundledReady) {
        fullHits = if (screen == "search" && query.isNotBlank()) {
            withContext(Dispatchers.IO) {
                when {
                    bundledReady -> bundledIndex.search(query)
                    onDeviceReady -> localEngine.search(query)
                    fullIndex.hasFullIndex -> fullIndex.search(query)
                    else -> emptyList()
                }
            }
        } else emptyList()
    }
    LaunchedEffect(screen, updated, bundledReady, onDeviceReady) {
        if (screen == "favorites") {
            savedWordHits = withContext(Dispatchers.IO) {
                store.wordFavorites().mapNotNull { id ->
                    when {
                        id in -10007..-10001 -> null // Preview Fâtiha ayah; displayed separately.
                        id >= 1000000 -> bundledIndex.byId(id)
                        id < 0 -> localEngine.byId(id)
                        else -> fullIndex.byId(id)
                    }
                }
            }
        }
    }
    val l = labels(language)
    val logo = remember {
        try {
            context.assets.open("brand/logo.webp").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        } catch (_: Exception) { null }
    }
    BackHandler(screen != "home") { screen = "home" }
    Column(Modifier.fillMaxSize().background(Color.White).padding(horizontal=18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top=14.dp), verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.SpaceBetween) {
            Box {
                OutlinedButton(onClick={ menu = true }, contentPadding=PaddingValues(horizontal=10.dp)) {
                    Text(languages.firstOrNull { it.first == language }?.second ?: "🇹🇷 Türkçe",
                        color=HomeGreen, fontSize=12.sp)
                    Icon(Icons.Default.ArrowDropDown, contentDescription="Dil listesi")
                }
                DropdownMenu(expanded=menu, onDismissRequest={menu=false}) {
                    languages.forEach { (code, label) ->
                        DropdownMenuItem(text={Text((if(language==code) "✓ " else "") + label)},
                            onClick={ language=code; settings.edit().putString("lang",code).apply(); menu=false })
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (logo != null) {
                Image(logo, contentDescription="Kurani Keşfet logosu",
                    modifier=Modifier.size(68.dp), contentScale=ContentScale.Fit)
            } else {
                Icon(Icons.Default.AutoStories, contentDescription="Kurani Keşfet", tint=HomeGreen,
                    modifier=Modifier.size(46.dp))
            }
        }
        if (screen == "home") {
            Column(Modifier.fillMaxWidth().padding(top=10.dp, bottom=6.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text("❦  Yaratıcından Sana Bir Mektup  ❦",
                    color=HomeBrown, fontSize=21.sp, fontFamily=FontFamily.Serif,
                    fontWeight=FontWeight.Bold, textAlign=TextAlign.Center, lineHeight=26.sp)
                Text("A Letter to You from Your Creator",
                    color=HomeBrown.copy(alpha=.86f), fontSize=14.sp,
                    fontFamily=FontFamily.Serif, fontStyle=FontStyle.Italic,
                    textAlign=TextAlign.Center, modifier=Modifier.padding(top=4.dp))
            }
        }
        when(screen) {
            "home" -> {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement=Arrangement.Center, horizontalAlignment=Alignment.CenterHorizontally) {
                    if(logo!=null) {
                        Image(logo, contentDescription=l.title,
                            modifier=Modifier.size(154.dp), contentScale=ContentScale.Fit)
                    } else {
                        Icon(Icons.Default.AutoStories, contentDescription=null,
                            modifier=Modifier.size(90.dp), tint=HomeGreen)
                    }
                    Text(l.title, color=HomeGreen, fontSize=27.sp, fontWeight=FontWeight.Bold,
                        textAlign=TextAlign.Center)
                    Text(l.subtitle, color=HomeBrown, fontSize=13.sp,
                        modifier=Modifier.padding(top=5.dp,bottom=18.dp), textAlign=TextAlign.Center)
                    HomeAction(Icons.Default.MenuBook, l.read) { openBookPage(context) }
                    Spacer(Modifier.height(12.dp))
                    HomeAction(Icons.Default.Search, l.search) { screen="search" }
                    Spacer(Modifier.height(12.dp))
                    HomeAction(Icons.Default.StarBorder, l.favorites) { updated++; screen="favorites" }
                }
            }
            "search" -> {
                Row(Modifier.padding(top=18.dp), verticalAlignment=Alignment.CenterVertically) {
                    IconButton(onClick={ screen="home" }) {
                        Icon(Icons.Default.ArrowBack, contentDescription="Ana sayfa")
                    }
                    Text(l.search, color=HomeGreen, fontSize=20.sp)
                }
                OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),
                    placeholder={Text(l.searchHint)}, singleLine=true)
                val hasFullSearch = bundledReady || onDeviceReady || fullIndex.hasFullIndex
                val searchMessage = when {
                    indexing -> "Çevrimdışı metin dizini hazırlanıyor: $scannedPages / $pageCount sayfa"
                    bundledReady -> "304 sayfalık çevrimdışı dizin hazır · OCR sonuçları doğrulama gerektirir"
                    onDeviceReady -> "PDF'den oluşturulan yerel arama dizini hazır · internet gerekmez"
                    fullIndex.hasFullIndex -> "Uygulamaya eklenmiş 304 sayfalık arama dizini hazır"
                    noTextLayer -> "Bu PDF'nin metin katmanı okunamadı. Kelime dizini eklenmeli."
                    !bookPresent -> "Eksiksiz çevrimdışı paket bu APK içinde bulunmuyor."
                    Build.VERSION.SDK_INT < 35 -> "Android 15 altında tam arama için yerel kelime dizini gereklidir."
                    else -> "Tam arama verisi henüz hazır değil; Fâtiha örnek araması açık."
                }
                Text(searchMessage, fontSize=11.sp, color=HomeBrown,
                    modifier=Modifier.padding(vertical=8.dp))
                if (indexing) LinearProgressIndicator(
                    progress = { scannedPages.toFloat() / pageCount.coerceAtLeast(1) },
                    modifier=Modifier.fillMaxWidth()
                )
                val fallback = remember(query, language, hasFullSearch) {
                    if (query.isBlank() || hasFullSearch) emptyList()
                    else quranIndex.search(query, if (language=="ar") "tr" else language)
                }
                LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    if (hasFullSearch) {
                        items(fullHits, key={ "hit" + it.id }) { hit ->
                            val isSaved = remember(hit.id, updated) { store.isWordFavorite(hit.id) }
                            Card(onClick={openBookPage(context,hit.page,highlightWordId=hit.id,
                                hitX=hit.x+hit.width*.5f,hitY=hit.y+hit.height*.5f)},
                                shape=RoundedCornerShape(14.dp)) {
                                Row(Modifier.fillMaxWidth().padding(start=15.dp, top=10.dp, end=6.dp, bottom=10.dp),
                                    verticalAlignment=Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        val verifiedRange = remember(hit.id,hit.page) {
                                            if (bundledReady) bundledIndex.referenceLabel(hit.id,hit.page)
                                            else null
                                        }
                                        Text(verifiedRange ?: ("Kur’anı Keşfet · Sayfa " + hit.page),
                                            color=HomeGreen,fontWeight=FontWeight.SemiBold,fontSize=13.sp)
                                        Spacer(Modifier.height(5.dp))
                                        Text(hit.gloss,fontSize=17.sp,color=Color(0xFF885E20),
                                            fontWeight=FontWeight.Bold)
                                        Text("Sayfa " + hit.page + " · Kelimeye git",
                                            color=HomeBrown,fontSize=11.sp)
                                    }
                                    IconButton(onClick={
                                        store.toggleWordFavorite(hit.id)
                                        updated++
                                    }) {
                                        Icon(if(isSaved) Icons.Default.Star else Icons.Default.StarBorder,
                                            tint=if(isSaved) Color(0xFFC69C51) else HomeGreen,
                                            contentDescription=if(isSaved) "Favorilerden çıkar" else "Favorilere ekle")
                                    }
                                }
                            }
                        }
                    } else {
                        items(fallback.distinctBy { it.ayah }) { hit ->
                            val id = -10000-hit.ayah
                            val isSaved = remember(id,updated) { store.isWordFavorite(id) }
                            Card(onClick={openBookPage(context,1,hit.ayah)},
                                shape=RoundedCornerShape(14.dp)) {
                                Row(Modifier.fillMaxWidth().padding(start=15.dp,top=10.dp,end=6.dp,bottom=10.dp),
                                    verticalAlignment=Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Fâtiha · " + hit.ayah + ". Ayet · Sayfa 1",color=HomeGreen)
                                        Text(hit.arabic + " · " + hit.meaning,fontSize=16.sp)
                                    }
                                    IconButton(onClick={
                                        store.toggleWordFavorite(id)
                                        updated++
                                    }) {
                                        Icon(if(isSaved) Icons.Default.Star else Icons.Default.StarBorder,
                                            tint=if(isSaved) Color(0xFFC69C51) else HomeGreen,
                                            contentDescription=if(isSaved) "Favorilerden çıkar" else "Favorilere ekle")
                                    }
                                }
                            }
                        }
                    }
                    if (query.isNotBlank() && !indexing && fullHits.isEmpty() && fallback.isEmpty()) {
                        item { Text(l.empty,modifier=Modifier.padding(16.dp)) }
                    }
                }
            }
            else -> {
                Row(Modifier.padding(top=18.dp), verticalAlignment=Alignment.CenterVertically) {
                    IconButton(onClick={screen="home"}) {
                        Icon(Icons.Default.ArrowBack, contentDescription="Ana sayfa")
                    }
                    Text(l.favorites,color=HomeGreen,fontSize=20.sp)
                }
                val pages = remember(updated) { store.favorites() }
                val previewAyahs = remember(updated) {
                    store.wordFavorites().filter { it in -10007..-10001 }.map { -10000-it }.sorted()
                }
                if (pages.isEmpty() && savedWordHits.isEmpty() && previewAyahs.isEmpty()) {
                    Text("Henüz favori sayfa veya kelime yok.",modifier=Modifier.padding(20.dp))
                } else LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    items(savedWordHits,key={"saved-word-"+it.id}) { word ->
                        Card(onClick={openBookPage(context,word.page,highlightWordId=word.id,
                            hitX=word.x+word.width*.5f,hitY=word.y+word.height*.5f)}) {
                            Row(Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment=Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(word.gloss,color=HomeGreen,fontSize=17.sp,fontWeight=FontWeight.Bold)
                                    Text("Sayfa "+word.page+" · Kelimeye git",
                                        color=HomeBrown,fontSize=12.sp)
                                }
                                IconButton(onClick={
                                    store.toggleWordFavorite(word.id)
                                    updated++
                                }) {
                                    Icon(Icons.Default.Star,tint=Color(0xFFC69C51),
                                        contentDescription="Kelimeyi favorilerden çıkar")
                                }
                            }
                        }
                    }
                    items(previewAyahs,key={"saved-fatiha-"+it}) { ayah ->
                        Card(onClick={openBookPage(context,1,ayah)}) {
                            Row(Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment=Alignment.CenterVertically) {
                                Text("Fâtiha · "+ayah+". Ayet",color=HomeGreen,
                                    modifier=Modifier.weight(1f))
                                IconButton(onClick={
                                    store.toggleWordFavorite(-10000-ayah)
                                    updated++
                                }) {
                                    Icon(Icons.Default.Star,tint=Color(0xFFC69C51),
                                        contentDescription="Ayeti favorilerden çıkar")
                                }
                            }
                        }
                    }
                    items(pages,key={"saved-page-"+it}) { page ->
                        Card(onClick={openBookPage(context,page)}) {
                            Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                                Icon(Icons.Default.Star,tint=Color(0xFFC69C51),contentDescription=null)
                                Spacer(Modifier.width(12.dp))
                                Text("Sayfa " + page,color=HomeGreen)
                            }
                        }
                    }
                }
            }
        }
        if (screen == "home") {
            Column(Modifier.fillMaxWidth().padding(top=8.dp,bottom=14.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Row(verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.Center) {
                    Icon(Icons.Default.AutoAwesome,contentDescription=null,
                        tint=Color(0xFFB38C4D),modifier=Modifier.size(23.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("RaSaSeR",fontFamily=FontFamily.Serif,
                        fontWeight=FontWeight.ExtraBold,fontSize=24.sp,
                        color=HomeGreen)
                    Spacer(Modifier.width(7.dp))
                    Icon(Icons.Default.AutoAwesome,contentDescription=null,
                        tint=Color(0xFFB38C4D),modifier=Modifier.size(23.dp))
                }
                Text("APPS & TECHNOLOGY",letterSpacing=2.sp,
                    fontSize=13.sp,fontWeight=FontWeight.Bold,color=HomeBrown)
                Spacer(Modifier.height(8.dp))
                Text("ÜCRETSİZDİR · HİÇBİR MADDİ AMACIMIZ YOKTUR",
                    fontFamily=FontFamily.Serif,fontSize=14.sp,
                    fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,color=HomeGreen)
                Text("COMPLETELY FREE · NO COMMERCIAL PURPOSE",
                    fontFamily=FontFamily.Serif,fontSize=12.sp,
                    fontStyle=FontStyle.Italic,textAlign=TextAlign.Center,color=HomeBrown)
                Text("www.okiminsozu.com",
                    modifier=Modifier.padding(top=5.dp).clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.okiminsozu.com")))
                    },fontSize=16.sp,fontWeight=FontWeight.SemiBold,
                    color=Color(0xFF92652E),textAlign=TextAlign.Center)
            }
        }
    }
}
@Composable
private fun HomeAction(icon: androidx.compose.ui.graphics.vector.ImageVector,label:String,onClick:()->Unit) {
    Surface(onClick=onClick, modifier=Modifier.fillMaxWidth().height(66.dp),
        shape=RoundedCornerShape(22.dp), color=HomeSand) {
        Row(Modifier.fillMaxSize().padding(horizontal=22.dp),verticalAlignment=Alignment.CenterVertically) {
            Icon(icon,contentDescription=null,tint=HomeBrown,modifier=Modifier.size(30.dp))
            Spacer(Modifier.width(22.dp))
            Text(label,color=HomeGreen,fontSize=25.sp,fontWeight=FontWeight.SemiBold,
                modifier=Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription=null,tint=HomeBrown)
        }
    }
}
