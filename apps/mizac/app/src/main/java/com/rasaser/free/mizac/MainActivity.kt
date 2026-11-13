package com.rasaser.free.mizac

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale

private val gold = Color(0xFFE1BE78)
private val black = Color(0xFF090909)
private val muted = Color(0xFFCECBC4)

private data class ReflectionItem(val text: String, val dimension: Int)

private val dimensionNames = listOf(
    "Keşfetme ve yeniliğe açıklık",
    "Planlama ve düzenlilik",
    "Sosyal etkileşim eğilimi",
    "İş birliği ve anlayış",
    "Zorluklarda dengeyi koruma"
)

// These items were composed for an early UX prototype; NOT a validated psychometric test.
private val items = listOf(
    ReflectionItem("Yeni fikirleri keşfetmekten hoşlanırım.", 0),
    ReflectionItem("Bilmediğim konuları öğrenmek bana keyif verir.", 0),
    ReflectionItem("Günümü önceden planlamaya çalışırım.", 1),
    ReflectionItem("Başladığım işleri tamamlamak için düzenli çaba gösteririm.", 1),
    ReflectionItem("Yeni insanlarla sohbet başlatmak bana kolay gelir.", 2),
    ReflectionItem("Kalabalık ortamlarda bulunmaktan genellikle hoşlanırım.", 2),
    ReflectionItem("Görüş ayrılığında karşımdakini anlamaya çalışırım.", 3),
    ReflectionItem("İhtiyaç duyulduğunda başkalarına destek olmaya çalışırım.", 3),
    ReflectionItem("Beklenmedik bir sorunla karşılaşınca sakinleşebilirim.", 4),
    ReflectionItem("Zor bir günün ardından yeniden dengemi bulabilirim.", 4)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = gold, onPrimary = black, background = black,
                    surface = Color(0xFF1C1C1C), onSurface = Color.White,
                    onBackground = Color.White
                )
            ) {
                MizacApp()
            }
        }
    }
}

@Composable
private fun MizacApp() {
    var screen by rememberSaveable { mutableStateOf("home") }
    var answers by rememberSaveable { mutableStateOf(List(items.size) { 0 }) }
    BackHandler(enabled = screen != "home") { screen = "home" }

    Column(
        modifier = Modifier.fillMaxSize().background(black)
            .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (screen) {
            "home" -> Home(
                onReflect = { screen = "reflection" },
                onHistory = { screen = "history" },
                onPrivacy = { screen = "privacy" }
            )
            "reflection" -> ReflectionScreen(
                answers = answers,
                onAnswer = { index, answer ->
                    answers = answers.toMutableList().also { it[index] = answer }
                },
                onResults = { screen = "results" },
                onBack = { screen = "home" }
            )
            "results" -> ResultsScreen(answers, onBack = { screen = "home" },
                onRetry = { answers = List(items.size) { 0 }; screen = "reflection" })
            "history" -> HistoryScreen(onBack = { screen = "home" })
            "privacy" -> PrivacyScreen(onBack = { screen = "home" })
        }
    }
}

@Composable
private fun Home(onReflect: () -> Unit, onHistory: () -> Unit, onPrivacy: () -> Unit) {
    Text("RaSaSeR APPS & TECHNOLOGY", color = gold, style = MaterialTheme.typography.labelMedium)
    ApprovedLogo()
    Text(
        "MİZAÇ", modifier = Modifier.fillMaxWidth(), color = gold,
        textAlign = TextAlign.Center, fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.displayMedium
    )
    Text(
        "Kendini tanı • İnsanı anla",
        modifier = Modifier.fillMaxWidth(), color = muted, textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Button(onClick = onReflect, modifier = Modifier.fillMaxWidth()) {
        Text("Kendimi Tanıyorum")
    }
    OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) {
        Text("Mârifetnâme • Tarihî Bilgiler")
    }
    OutlinedButton(onClick = onPrivacy, modifier = Modifier.fillMaxWidth()) {
        Text("Gizlilik ve Lisanslar")
    }
    Text(
        "Bilimsel kişilik değerlendirmesi ile tarihî yorumlar birbirinden ayrı tutulur.",
        color = muted, style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun ApprovedLogo() {
    val context = LocalContext.current
    val bitmap = remember(context) {
        runCatching {
            val encoded = context.assets.open("brand/mizac_logo.webp.b64")
                .bufferedReader().use { it.readText() }
            val data = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(data, 0, data.size)?.asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Onaylanan MİZAÇ altın-siyah marka görseli",
            modifier = Modifier.fillMaxWidth().height(258.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        Text(
            "MİZAÇ · RaSaSeR",
            color = gold, modifier = Modifier.fillMaxWidth().height(120.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    OutlinedButton(onClick = onBack) { Text("‹ Ana sayfa") }
    Text(title, color = gold, style = MaterialTheme.typography.headlineMedium)
}

@Composable
private fun ReflectionScreen(
    answers: List<Int>,
    onAnswer: (Int, Int) -> Unit,
    onResults: () -> Unit,
    onBack: () -> Unit
) {
    PageHeader("Kendimi Tanıyorum", onBack)
    Text(
        "Bu 10 soru bir kişisel farkındalık örneğidir. Doğrulanmış psikolojik test, teşhis veya kesin karakter tespiti değildir.",
        color = muted, style = MaterialTheme.typography.bodyMedium
    )
    Text("1: Hiç katılmıyorum     5: Tamamen katılıyorum", color = gold,
        style = MaterialTheme.typography.bodySmall)
    items.forEachIndexed { index, item ->
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text((index + 1).toString() + ". " + item.text,
                style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..5).forEach { value ->
                    val selected = answers[index] == value
                    if (selected) {
                        Button(onClick = { onAnswer(index, value) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)) {
                            Text(value.toString())
                        }
                    } else {
                        OutlinedButton(onClick = { onAnswer(index, value) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)) {
                            Text(value.toString())
                        }
                    }
                }
            }
        }
    }
    val completed = answers.count { it in 1..5 }
    Text("Tamamlanan: " + completed + " / " + items.size, color = muted)
    Button(onClick = onResults, modifier = Modifier.fillMaxWidth(),
        enabled = completed == items.size) {
        Text("Yanıtlarımı İncele")
    }
}

@Composable
private fun ResultsScreen(answers: List<Int>, onBack: () -> Unit, onRetry: () -> Unit) {
    PageHeader("Kişisel Farkındalık Özeti", onBack)
    Text(
        "Bu puanlar yalnız verdiğiniz yanıtların ortalamasıdır. Başka insanlarla karşılaştırma veya bilimsel kişilik skoru değildir.",
        color = muted
    )
    dimensionNames.forEachIndexed { dimension, name ->
        val values = items.mapIndexedNotNull { index, item ->
            if (item.dimension == dimension) answers[index].toFloat() else null
        }
        val score = if (values.isNotEmpty()) values.average().toFloat() else 0f
        Text(name, color = gold, style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(
            progress = { score / 5f },
            modifier = Modifier.fillMaxWidth()
        )
        Text("Kendi yanıt ortalamanız: " +
            String.format(Locale.getDefault(), "%.1f", score) + " / 5",
            color = muted, style = MaterialTheme.typography.bodySmall)
    }
    Text(
        "Bu alanlar geliştirilebilir eğilimleri düşünmeniz içindir; yüksek veya düşük puan iyi-kötü insan anlamına gelmez.",
        color = muted
    )
    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
        Text("Yeniden Yanıtla")
    }
}

@Composable
private fun HistoryScreen(onBack: () -> Unit) {
    PageHeader("Mârifetnâme • İlm-i Kıyâfet", onBack)
    Text(
        "Erzurumlu İbrahim Hakkı'nın Mârifetnâme eserinde, insanın dış görünüşünden huy ve mizaca dair yorumlar içeren bir tarihî gelenek aktarılır.",
        style = MaterialTheme.typography.bodyLarge
    )
    Text(
        "Bu bölüm kültür ve düşünce tarihi içindir. Yüz, göz, boy veya beden biçiminden bir kişinin güvenilirliği, zekâsı, karakteri ya da niyeti bilimsel olarak belirlenemez.",
        color = muted
    )
    Text(
        "İlerleyen sürümlerde, doğrulanmış eser nüshalarından kaynaklı ve açıklamalı metinler ayrı bir kütüphaneye eklenecektir.",
        color = gold
    )
}

@Composable
private fun PrivacyScreen(onBack: () -> Unit) {
    PageHeader("Gizlilik ve Lisanslar", onBack)
    Text(
        "Bu ilk sürümde hesap, internet izni, kamera veya mikrofon erişimi bulunmaz. Yanıtlar cihaz dışına gönderilmez. Test yanıtları kalıcı olarak kaydedilmez.",
        color = muted
    )
    val context = LocalContext.current
    val licenses = remember(context) {
        runCatching { context.assets.open("legal/licenses.txt")
            .bufferedReader().use { it.readText() } }.getOrDefault("Lisans bilgisi okunamadı.")
    }
    Text(licenses, style = MaterialTheme.typography.bodySmall)
}
