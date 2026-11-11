package com.rasaser.free.kurankelime

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    "ar" to "🇸🇦 العربية", "es" to "🇪🇸 Español"
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
    var language by remember { mutableStateOf(settings.getString("lang", "tr") ?: "tr") }
    var menu by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf("home") }
    var query by remember { mutableStateOf("") }
    var updated by remember { mutableIntStateOf(0) }
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
            Text(l.slogan, modifier=Modifier.weight(1f).padding(horizontal=6.dp),
                fontSize=12.sp, textAlign=TextAlign.Center, color=HomeBrown)
            if (logo != null) {
                Image(logo, contentDescription="Kurani Keşfet logosu",
                    modifier=Modifier.size(68.dp), contentScale=ContentScale.Fit)
            } else {
                Icon(Icons.Default.AutoStories, contentDescription="Kurani Keşfet", tint=HomeGreen,
                    modifier=Modifier.size(46.dp))
            }
        }
        when(screen) {
            "home" -> {
                Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement=Arrangement.Center,
                    horizontalAlignment=Alignment.CenterHorizontally) {
                    if(logo!=null) {
                        Image(logo, contentDescription=l.title,
                            modifier=Modifier.size(192.dp), contentScale=ContentScale.Fit)
                    } else {
                        Icon(Icons.Default.AutoStories, contentDescription=null,
                            modifier=Modifier.size(105.dp), tint=HomeGreen)
                    }
                    Text(l.title, color=HomeGreen, fontSize=30.sp, fontWeight=FontWeight.Bold,
                        textAlign=TextAlign.Center)
                    Text(l.subtitle, color=HomeBrown, fontSize=13.sp,
                        modifier=Modifier.padding(top=8.dp,bottom=28.dp), textAlign=TextAlign.Center)
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
                Text("Kelime dizini: şu an Fâtiha örneği. PDF görsel içeriğinin tamamı henüz aranabilir değil.",
                    fontSize=11.sp, color=HomeBrown, modifier=Modifier.padding(vertical=8.dp))
                val hits = remember(query,language) {
                    if(query.isBlank()) emptyList() else quranIndex.search(query,language)
                }
                LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    items(hits.distinctBy { it.ayah }) { hit ->
                        Card(onClick={openBookPage(context,1,hit.ayah)}, shape=RoundedCornerShape(14.dp)) {
                            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                                Text("Fâtiha • " + hit.ayah + ". Ayet • PDF sayfa 1",color=HomeGreen)
                                Text(hit.arabic + " · " + hit.meaning, fontSize=16.sp)
                            }
                        }
                    }
                    if(query.isNotBlank() && hits.isEmpty()) {
                        item { Text(l.empty, modifier=Modifier.padding(16.dp)) }
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
                if(pages.isEmpty()) {
                    Text("Henüz favori sayfa yok.",modifier=Modifier.padding(20.dp))
                } else LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    items(pages) { page ->
                        Card(onClick={openBookPage(context,page)}) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment=Alignment.CenterVertically) {
                                Icon(Icons.Default.Star,color=Color(0xFFC69C51), contentDescription=null)
                                Spacer(Modifier.width(12.dp))
                                Text("PDF Sayfa " + page,color=HomeGreen)
                            }
                        }
                    }
                }
            }
        }
        Text("RaSaSeR APPS & TECHNOLOGY",modifier=Modifier.fillMaxWidth().padding(top=10.dp),
            textAlign=TextAlign.Center,color=HomeBrown,fontSize=12.sp,letterSpacing=1.sp)
        Text(l.free,modifier=Modifier.fillMaxWidth().padding(top=6.dp,bottom=14.dp),
            textAlign=TextAlign.Center,color=HomeBrown,fontSize=12.sp)
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
