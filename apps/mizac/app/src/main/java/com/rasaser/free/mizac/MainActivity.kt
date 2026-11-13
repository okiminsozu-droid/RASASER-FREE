package com.rasaser.free.mizac

import android.graphics.BitmapFactory
import android.os.Bundle
import android.content.Context
import android.content.ClipData
import androidx.core.content.FileProvider
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Alignment
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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

private val gold = Color(0xFFD4A34F)
private val black = Color(0xFF1E1711)
private val sand = Color(0xFFEBCFAC)
private val muted = Color(0xFF574B3F)
private val ivory = Color(0xFFFFFBF6)

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
                colorScheme = lightColorScheme(
                    primary = ivory, onPrimary = black, background = sand,
                    surface = ivory, onSurface = black, onBackground = black
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
    var activeQuestionCount by rememberSaveable { mutableStateOf(10) }
    var answers by rememberSaveable { mutableStateOf(List(items.size) { 0 }) }
    BackHandler(enabled = screen != "home") { screen = "home" }

    Column(
        modifier = Modifier.fillMaxSize().background(sand)
            .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (screen) {
            "home" -> Home(
                onReflect = { screen = "analysis-setup" },
                onHistory = { screen = "history" },
                onPrivacy = { screen = "privacy" }
            )
            "analysis-setup" -> AnalysisSetupScreen(
                onBack = { screen = "home" },
                onGeometry = { screen = "geometry" },
                onQuestionnaire = { count -> activeQuestionCount = count; screen = "questionnaire" }
            )
            "geometry" -> FaceGeometryScreen(onBack = { screen = "analysis-setup" })
            "questionnaire" -> PersonalityTestScreen(
                questionCount = activeQuestionCount,
                onBack = { screen = "analysis-setup" }
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
            "history" -> MarifetnameScreen(onBack = { screen = "home" })
            "privacy" -> PrivacyScreen(
                onBack = { screen = "home" },
                onDeleteAnswers = { answers = List(items.size) { 0 } }
            )
        }
    }
}

@Composable
private fun Home(onReflect: () -> Unit, onHistory: () -> Unit, onPrivacy: () -> Unit) {
    Text(
        "❦ Görünüşün, karakterinin anahtarıdır. ❦",
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        color = black, fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.titleMedium
    )
    Text(
        "Your appearance is the key to your character.",
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        color = black, style = MaterialTheme.typography.bodyMedium
    )
    ApprovedLogo()
    Button(
        onClick = onReflect, modifier = Modifier.fillMaxWidth().height(68.dp),
        shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ivory, contentColor = black)
    ) { Text("Analiz Et", style = MaterialTheme.typography.headlineSmall) }
    OutlinedButton(
        onClick = onHistory, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = ivory, contentColor = black)
    ) { Text("Bilgi & Rehber") }
    OutlinedButton(
        onClick = onPrivacy, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = ivory, contentColor = black)
    ) { Text("Gizlilik ve Lisanslar") }
    Text(
        "RaSaSeR APPS & TECHNOLOGY",
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        color = black, style = MaterialTheme.typography.titleSmall
    )
    val context = LocalContext.current
    androidx.compose.material3.TextButton(
        onClick = {
            val browser = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.okiminsozu.com"))
            runCatching { context.startActivity(browser) }
        }, modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "www.okiminsozu.com", textAlign = TextAlign.Center, color = black,
            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
        )
    }
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
            color = black, modifier = Modifier.fillMaxWidth().height(120.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    OutlinedButton(onClick = onBack) { Text("‹ Ana sayfa") }
    Text(title, color = gold, style = MaterialTheme.typography.headlineMedium)
}

/**
 * No analytics pipeline is connected here: local media is never uploaded
 * or used to infer personality. Selected counts are stored only on-device.
 */
private fun storeMediaLocally(context: Context, uri: Uri, kind: String) {
    require(kind == "photo" || kind == "video") { "Geçersiz medya türü." }
    val mime = context.contentResolver.getType(uri)
    if (mime != null) {
        require(mime.startsWith(if (kind == "photo") "image/" else "video/")) {
            "Seçilen dosya beklenen türde değil."
        }
    }
    if (kind == "video") {
        val metadata = MediaMetadataRetriever()
        try {
            metadata.setDataSource(context, uri)
            val duration = metadata.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: error("Video süresi okunamadı.")
            require(duration in 1L..30_000L) {
                "Video en fazla 30 saniye olabilir."
            }
        } finally {
            metadata.release()
        }
    }
    val file = File(context.filesDir, "mizac_" + kind + ".dat")
    val temp = File(context.filesDir, "mizac_" + kind + ".tmp")
    val maxBytes = if (kind == "video") 80L * 1024L * 1024L else 15L * 1024L * 1024L
    try {
        val source = context.contentResolver.openInputStream(uri)
            ?: error("Dosya açılamadı.")
        source.use { input ->
            temp.outputStream().use { output ->
                val buffer = ByteArray(16 * 1024)
                var size = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    size += read
                    require(size <= maxBytes) { "Dosya çok büyük." }
                    output.write(buffer, 0, read)
                }
                require(size > 0) { "Boş dosya kaydedilemez." }
            }
        }
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
    } finally {
        temp.delete()
    }
}

@Composable
private fun AnalysisSetupScreen(
    onBack: () -> Unit,
    onGeometry: () -> Unit,
    onQuestionnaire: (Int) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("mizac_local_setup", Context.MODE_PRIVATE)
    }
    var questionCount by rememberSaveable {
        mutableStateOf(prefs.getInt("question_count", 10).takeIf { it in listOf(10, 50, 120) } ?: 10)
    }
    var hasPhoto by remember { mutableStateOf(File(context.filesDir, "mizac_photo.dat").exists()) }
    var hasVideo by remember { mutableStateOf(File(context.filesDir, "mizac_video.dat").exists()) }
    var feedback by rememberSaveable { mutableStateOf("") }
    fun save(uri: Uri?, kind: String) {
        if (uri == null) return
        runCatching { storeMediaLocally(context, uri, kind) }
            .onSuccess {
                if (kind == "photo") hasPhoto = true else hasVideo = true
                feedback = if (kind == "photo") "Fotoğraf telefona kaydedildi." else "Video telefona kaydedildi."
            }
            .onFailure {
                feedback = it.message ?: "Dosya kaydedilemedi."
            }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        save(it, "photo")
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        save(it, "video")
    }
    val captureFile = remember(context) { File(context.cacheDir, "mizac_video_capture.mp4") }
    val captureUri = remember(context) {
        FileProvider.getUriForFile(
            context, context.packageName + ".fileprovider", captureFile
        )
    }
    val videoRecorder = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == android.app.Activity.RESULT_OK) {
            val uri = if (captureFile.exists() && captureFile.length() > 0L) {
                captureUri
            } else {
                it.data?.data
            }
            if (uri == null) feedback = "Kamera kayıt dosyası bulunamadı."
            else save(uri, "video")
        } else {
            feedback = "Video kaydı iptal edildi."
        }
        captureFile.delete()
    }
    PageHeader("Kişilik Analizi", onBack)
    Card(
        colors = CardDefaults.cardColors(containerColor = ivory),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("🔒 Verileriniz yalnızca size aittir.", color = black,
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                "Fotoğraf, video, soru seçimi ve test yanıtlarınız RaSaSeR'a gönderilmez. " +
                    "MİZAÇ hesabı veya bulut depolaması yoktur; kaydedilen dosyalar yalnızca " +
                    "telefonunuzun uygulama alanında tutulur. İsterseniz silebilirsiniz.",
                color = muted, style = MaterialTheme.typography.bodySmall
            )
        }
    }
    Text(
        "İstersen fotoğraf veya 30 saniyeye kadar video ekleyebilirsin. Hiçbiri zorunlu değil.",
        color = muted, style = MaterialTheme.typography.bodyMedium
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = ivory),
        shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("▧  Fotoğraf", color = black, style = MaterialTheme.typography.titleLarge)
            Text(
                if (hasPhoto) "Fotoğraf cihazda kayıtlı." else "İstersen galeriden bir fotoğraf seç.",
                color = muted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { photoPicker.launch("image/*") }) {
                    Text(if (hasPhoto) "Değiştir" else "Fotoğraf Seç")
                }
                if (hasPhoto) OutlinedButton(onClick = {
                    File(context.filesDir, "mizac_photo.dat").delete()
                    hasPhoto = false
                    feedback = "Fotoğraf silindi."
                }) { Text("Kaldır") }
            }
        }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = ivory),
        shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("▷  Video (en fazla 30 sn)", color = black,
                style = MaterialTheme.typography.titleLarge)
            Text(
                if (hasVideo) "Video cihazda kayıtlı." else "Galeriden seç veya kamerayla kaydet.",
                color = muted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { videoPicker.launch("video/*") }) {
                    Text("Video Seç")
                }
                OutlinedButton(onClick = {
                    captureFile.delete()
                    val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                        putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30)
                        putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
                        putExtra(MediaStore.EXTRA_OUTPUT, captureUri)
                        clipData = ClipData.newUri(context.contentResolver, "MİZAÇ video", captureUri)
                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                    runCatching { videoRecorder.launch(intent) }
                        .onFailure { feedback = "Video kamerası açılamadı." }
                }) {
                    Text("Video Çek")
                }
            }
            if (hasVideo) OutlinedButton(onClick = {
                File(context.filesDir, "mizac_video.dat").delete()
                hasVideo = false
                feedback = "Video silindi."
            }) { Text("Videoyu Kaldır") }
        }
    }
    Button(
        onClick = onGeometry,
        enabled = hasPhoto || hasVideo,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = ivory, contentColor = black)
    ) {
        Text("Fotoğrafın / Videonun Yüz Geometrisini İncele")
    }
    Text(
        "Görüntünün yüz ölçümü yalnız cihazda yapılır; görünüşten karakter tahmini üretilmez.",
        color = muted, style = MaterialTheme.typography.bodySmall
    )
    Text("Kaç soru ile ilerlemek istersin?", color = black,
        style = MaterialTheme.typography.titleLarge)
    listOf(
        10 to "Hızlı Analiz",
        50 to "Standart Analiz",
        120 to "Ayrıntılı Analiz"
    ).forEach { (count, title) ->
        Card(
            modifier = Modifier.fillMaxWidth().clickable {
                questionCount = count
                feedback = ""
            },
            colors = CardDefaults.cardColors(containerColor = ivory),
            shape = RoundedCornerShape(26.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = questionCount == count,
                    onClick = { questionCount = count; feedback = "" }
                )
                Column(Modifier.padding(start = 8.dp)) {
                    Text("$count Soru", style = MaterialTheme.typography.titleLarge,
                        color = black)
                    Text(title, color = muted)
                }
            }
        }
    }
    Button(
        onClick = {
            prefs.edit().putInt("question_count", questionCount).apply()
            feedback = "$questionCount soruluk seçenek kaydedildi. Analiz motoruna hiçbir bilgi gönderilmedi."
        },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ivory, contentColor = black)
    ) {
        Text("Seçimi Kaydet", style = MaterialTheme.typography.titleMedium)
    }
    if (feedback.isNotEmpty()) {
        Text(feedback, color = black, style = MaterialTheme.typography.bodyMedium)
    }
    Button(
        onClick = {
            prefs.edit().putInt("question_count", questionCount).apply()
            onQuestionnaire(questionCount)
        },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ivory, contentColor = black)
    ) { Text("Soruları Başlat", style = MaterialTheme.typography.titleMedium) }
    Text(
        "Verilerin sahibi sizsiniz. MİZAÇ bu aşamada fotoğraf ve videoları analiz etmez; " +
            "analiz motoruna, RaSaSeR'a veya herhangi bir sunucuya göndermez. " +
            "Seçtiğiniz dosyaların telefon galerinizdeki asılları uygulamadan bağımsızdır.",
        color = muted, style = MaterialTheme.typography.bodySmall
    )
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
private fun PrivacyScreen(onBack: () -> Unit, onDeleteAnswers: () -> Unit) {
    PageHeader("Gizlilik ve Lisanslar", onBack)
    val context = LocalContext.current
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var deletionStatus by rememberSaveable { mutableStateOf("") }
    Card(
        colors = CardDefaults.cardColors(containerColor = ivory),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Verileriniz yalnızca size aittir.",
                color = black, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Text(
                "Fotoğraflarınız, videolarınız, kişilik testi cevaplarınız ve analiz sonuçlarınız " +
                    "yalnızca kendi cihazınızda saklanır. RaSaSeR bu verileri toplamaz, " +
                    "sunucularına yüklemez, görüntülemez veya başka amaçlarla kullanmaz.",
                color = black
            )
            Text(
                "MİZAÇ'ta hesap, bulut senkronizasyonu, izleme veya reklam amaçlı " +
                    "kişisel veri aktarımı yoktur. İlk sürümde test yanıtları yalnızca " +
                    "açık oturumda tutulur; kalıcı analiz geçmişi henüz oluşturulmaz.",
                color = muted, style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Galeriden seçtiğiniz orijinal dosyalar ve telefonun kendi kamera " +
                    "uygulaması tarafından saklanmış kopyalar MİZAÇ'ın silme işleminin " +
                    "dışındadır. Web sitesi bağlantısına dokunursanız tarayıcı ayrı açılır.",
                color = muted, style = MaterialTheme.typography.bodySmall
            )
        }
    }
    OutlinedButton(
        onClick = { confirmDelete = true },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = ivory, contentColor = black)
    ) {
        Text("Tüm Yerel MİZAÇ Verilerimi Sil")
    }
    if (deletionStatus.isNotEmpty()) Text(deletionStatus, color = black)
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Yerel veriler silinsin mi?") },
            text = { Text(
                "MİZAÇ'ın kendi depolama alanındaki fotoğraf, video, geçici çekim " +
                    "ve soru sayısı tercihleri silinecektir. Bu işlem geri alınamaz. " +
                    "Telefon galerisindeki orijinal dosyalar silinmez."
            ) },
            confirmButton = {
                TextButton(onClick = {
                    val fileNames = listOf(
                        "mizac_photo.dat", "mizac_video.dat",
                        "mizac_photo.tmp", "mizac_video.tmp"
                    )
                    var success = true
                    fileNames.forEach { name ->
                        val file = File(context.filesDir, name)
                        if (file.exists() && !file.delete()) success = false
                    }
                    val captureFile = File(context.cacheDir, "mizac_video_capture.mp4")
                    if (captureFile.exists() && !captureFile.delete()) success = false
                    val prefs = context.getSharedPreferences(
                        "mizac_local_setup", Context.MODE_PRIVATE
                    )
                    if (!prefs.edit().clear().commit()) success = false
                    onDeleteAnswers()
                    deletionStatus = if (success) {
                        "MİZAÇ'ın bu cihazda sakladığı veriler silindi."
                    } else {
                        "Bazı yerel dosyalar silinemedi. Lütfen yeniden deneyin."
                    }
                    confirmDelete = false
                }) { Text("Verilerimi Sil") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç") }
            }
        )
    }
    val licenses = remember(context) {
        runCatching {
            context.assets.open("legal/licenses.txt").bufferedReader().use { it.readText() }
        }.getOrDefault("Lisans bilgisi okunamadı.")
    }
    Text(licenses, style = MaterialTheme.typography.bodySmall, color = muted)
}
