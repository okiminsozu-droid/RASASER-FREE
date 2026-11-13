package com.rasaser.free.mizac

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private val Sand = Color(0xFFEBCFAC)
private val Ink = Color(0xFF1E1711)
private val Ivory = Color(0xFFFFFBF6)
private val Muted = Color(0xFF65584A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Ink, onPrimary = Ivory, background = Sand,
                    onBackground = Ink, surface = Ivory, onSurface = Ink
                )
            ) {
                MizacRoot()
            }
        }
    }
}

@Composable
private fun MizacRoot() {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("mizac_local_setup", Context.MODE_PRIVATE)
    }
    var languageCode by rememberSaveable {
        mutableStateOf(prefs.getString("ui_language", "tr") ?: "tr")
    }
    val lang = mizacLang(languageCode)
    val dict = remember(lang.code) { uiDictionary(context, lang) }
    var screen by rememberSaveable { mutableStateOf("home") }
    var count by rememberSaveable { mutableStateOf(10) }
    BackHandler(screen != "home") {
        screen = if (screen == "geometry" || screen == "questions" || screen == "history-reflection") {
            "analysis-setup"
        } else "home"
    }
    CompositionLocalProvider(
        LocalLayoutDirection provides if (lang.code == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Column(Modifier.fillMaxSize().background(Sand)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageFlags(lang, dict) { selected ->
                    languageCode = selected.code
                    prefs.edit().putString("ui_language", selected.code).apply()
                }
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (screen == "home") {
                    Text("❦ Görünüşün, karakterinin anahtarıdır. ❦",
                        color = Ink, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
                    Text("Your appearance is the key to your character.",
                        color = Ink, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth())
                }
                ApprovedMizacLogo(if (screen == "home") 230 else 95)
                when (screen) {
                    "home" -> {
                        WhiteAction(ui(dict, "analyze")) { screen = "analysis-setup" }
                        WhiteAction(ui(dict, "history")) { screen = "history" }
                        WhiteAction(ui(dict, "privacy")) { screen = "privacy" }
                    }
                    "analysis-setup" -> AnalysisSelection(
                        lang, dict,
                        onBack = { screen = "home" },
                        onGeometry = { screen = "geometry" },
                        onQuestions = { number -> count = number; screen = "questions" },
                        onHistory = { screen = "history-reflection" }
                    )
                    "geometry" -> FaceGeometryScreen(
                        onBack = { screen = "analysis-setup" }, lang = lang, strings = dict
                    )
                    "questions" -> OriginalQuestionnaire(
                        lang, dict, count, onBack = { screen = "analysis-setup" }
                    )
                    "history" -> MarifetnameScreen(
                        onBack = { screen = "home" }, lang = lang, strings = dict
                    )
                    "history-reflection" -> HistoricalReflectionScreen(
                        lang, dict, onBack = { screen = "analysis-setup" }
                    )
                    "privacy" -> PrivacyPage(dict, onBack = { screen = "home" })
                }
            }
            Footer(dict)
        }
    }
}

@Composable
private fun LanguageFlags(
    current: MizacLang,
    dict: Map<String, String>,
    onChange: (MizacLang) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text("${current.flag}  ${ui(dict, "language")}  ▾",
                color = Ink, style = MaterialTheme.typography.titleMedium)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MIZAC_LANGUAGES.forEach { option ->
                DropdownMenuItem(
                    text = { Text("${option.flag}  ${option.name}", color = Ink) },
                    onClick = { expanded = false; onChange(option) }
                )
            }
        }
    }
}

@Composable
private fun ApprovedMizacLogo(heightDp: Int) {
    val context = LocalContext.current
    val bitmap = remember(context) {
        runCatching {
            val raw = context.assets.open("brand/mizac_logo.webp.b64")
                .bufferedReader().use { it.readText() }
            val bytes = Base64.decode(raw, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = "MİZAÇ — RaSaSeR",
            modifier = Modifier.fillMaxWidth().height(heightDp.dp),
            contentScale = ContentScale.Fit)
    } else {
        Text("MİZAÇ", color = Ink, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineLarge)
    }
}

@Composable
private fun WhiteAction(title: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(62.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Ivory, contentColor = Ink)
    ) { Text(title, style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun Footer(dict: Map<String, String>) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(ui(dict, "brand"), color = Ink, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium)
        TextButton(onClick = {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.okiminsozu.com")))
            }
        }) {
            Text(ui(dict, "website"), color = Ink, textDecoration = TextDecoration.Underline)
        }
    }
}

@Composable
private fun Heading(title: String, back: String, onBack: () -> Unit) {
    OutlinedButton(onClick = onBack, colors = ButtonDefaults.outlinedButtonColors(
        containerColor = Ivory, contentColor = Ink
    )) { Text(back) }
    Text(title, color = Ink, fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.headlineSmall)
}

private fun saveMediaLocally(context: Context, uri: Uri, kind: String): Boolean {
    require(kind == "photo" || kind == "video")
    val mime = context.contentResolver.getType(uri)
    if (mime != null) require(mime.startsWith(if (kind == "photo") "image/" else "video/"))
    if (kind == "video") {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: error("Missing duration")
            require(durationMs in 1L..30_000L) { "tooLong" }
        } finally { retriever.release() }
    }
    val destination = File(context.filesDir, "mizac_$kind.dat")
    val temp = File(context.filesDir, "mizac_$kind.tmp")
    val maxBytes = if (kind == "video") 80L * 1024 * 1024 else 15L * 1024 * 1024
    try {
        val source = context.contentResolver.openInputStream(uri) ?: error("Unable to open")
        source.use { input ->
            temp.outputStream().use { output ->
                val bytes = ByteArray(16384)
                var total = 0L
                while (true) {
                    val read = input.read(bytes)
                    if (read < 0) break
                    total += read
                    require(total <= maxBytes) { "tooLarge" }
                    output.write(bytes, 0, read)
                }
                require(total > 0L)
            }
        }
        Files.move(temp.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return true
    } finally { temp.delete() }
}

@Composable
private fun AnalysisSelection(
    lang: MizacLang,
    dict: Map<String, String>,
    onBack: () -> Unit,
    onGeometry: () -> Unit,
    onQuestions: (Int) -> Unit,
    onHistory: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("mizac_local_setup", Context.MODE_PRIVATE)
    }
    var count by rememberSaveable {
        mutableStateOf(prefs.getInt("question_count", 10).takeIf { it in listOf(10,50,120) } ?: 10)
    }
    var photo by remember { mutableStateOf(File(context.filesDir, "mizac_photo.dat").exists()) }
    var video by remember { mutableStateOf(File(context.filesDir, "mizac_video.dat").exists()) }
    var feedback by remember { mutableStateOf("") }
    fun save(uri: Uri?, kind: String) {
        if (uri == null) return
        runCatching { saveMediaLocally(context, uri, kind) }.onSuccess {
            if (kind == "photo") photo = true else video = true
            feedback = ui(dict, if (kind == "photo") "savePhoto" else "saveVideo")
        }.onFailure {
            feedback = ui(dict, when (it.message) {
                "tooLong" -> "tooLong"
                "tooLarge" -> "tooLarge"
                else -> "saveFailure"
            })
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
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", captureFile)
    }
    val capture = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val uri = if (captureFile.exists() && captureFile.length() > 0L) captureUri
                else it.data?.data
            if (uri != null) save(uri, "video") else feedback = ui(dict, "saveFailure")
        } else feedback = ui(dict, "recordCancelled")
        captureFile.delete()
    }
    Heading(ui(dict,"setupTitle"),ui(dict,"backHome"),onBack)
    Card(colors = CardDefaults.cardColors(containerColor = Ivory)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("🔒 " + ui(dict,"localPrivacy"), color = Ink, fontWeight = FontWeight.Bold)
            Text(ui(dict,"localPrivacyBody"), color = Muted)
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = Ivory)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui(dict,"photoTitle"), color = Ink, style = MaterialTheme.typography.titleMedium)
            Text(ui(dict,if(photo) "photoSaved" else "photoHint"), color = Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { photoPicker.launch("image/*") }) {
                    Text(ui(dict,if(photo) "photoReplace" else "photoChoose"))
                }
                if (photo) {
                    OutlinedButton(onClick = {
                        File(context.filesDir,"mizac_photo.dat").delete()
                        photo = false
                        feedback = ui(dict,"removeSuccess")
                    }) { Text(ui(dict,"photoDelete")) }
                }
            }
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = Ivory)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui(dict,"videoTitle"),color = Ink,style = MaterialTheme.typography.titleMedium)
            Text(ui(dict,if(video) "videoSaved" else "videoHint"),color = Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { videoPicker.launch("video/*") }) {
                    Text(ui(dict,"videoChoose"))
                }
                OutlinedButton(onClick = {
                    captureFile.delete()
                    val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                        putExtra(MediaStore.EXTRA_DURATION_LIMIT,30)
                        putExtra(MediaStore.EXTRA_VIDEO_QUALITY,1)
                        putExtra(MediaStore.EXTRA_OUTPUT,captureUri)
                        clipData = ClipData.newUri(context.contentResolver, "MIZAC",captureUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                    runCatching { capture.launch(intent) }.onFailure {
                        feedback = ui(dict,"cameraUnavailable")
                    }
                }) { Text(ui(dict,"videoRecord")) }
            }
            if (video) {
                OutlinedButton(onClick = {
                    File(context.filesDir,"mizac_video.dat").delete()
                    video = false
                    feedback = ui(dict,"removeSuccess")
                }) { Text(ui(dict,"videoDelete")) }
            }
        }
    }
    WhiteAction(ui(dict,"geometry"),onClick = {
        if (photo || video) onGeometry() else feedback = ui(dict,"needPhoto")
    })
    Text(ui(dict,"noCameraPersonality"),color = Muted,
        style = MaterialTheme.typography.bodySmall)
    Text(ui(dict,"chooseCount"),color = Ink,style = MaterialTheme.typography.titleMedium)
    listOf(10,50,120).forEach { option ->
        Card(
            modifier = Modifier.fillMaxWidth().clickable { count = option },
            colors = CardDefaults.cardColors(containerColor = Ivory),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = count == option, onClick = { count = option })
                Text("$option " + ui(dict,"questions") + " · " + ui(dict,
                    when(option){10->"fast";50->"normal";else->"deep"}), color=Ink)
            }
        }
    }
    OutlinedButton(onClick = {
        prefs.edit().putInt("question_count",count).apply()
        feedback = ui(dict,"saveFeedback")
    },modifier=Modifier.fillMaxWidth()) {
        Text(ui(dict,"saveChoice"))
    }
    WhiteAction(ui(dict,"startQuestions"),onClick = {
        prefs.edit().putInt("question_count",count).apply()
        onQuestions(count)
    })
    OutlinedButton(onClick = onHistory,modifier=Modifier.fillMaxWidth()) {
        Text(ui(dict,"historyButton"))
    }
    if (feedback.isNotBlank()) Text(feedback,color=Ink)
}

@Composable
private fun PrivacyPage(dict: Map<String, String>, onBack: () -> Unit) {
    val context = LocalContext.current
    var confirm by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    Heading(ui(dict,"privacyTitle"),ui(dict,"backHome"),onBack)
    Card(colors = CardDefaults.cardColors(containerColor=Ivory)) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(ui(dict,"localPrivacy"),color=Ink,fontWeight=FontWeight.Bold)
            Text(ui(dict,"privacyText"),color=Muted)
            Text(ui(dict,"licenseTitle"),color=Ink,fontWeight=FontWeight.Bold)
            Text(ui(dict,"licenseNotice"),color=Muted)
        }
    }
    OutlinedButton(onClick = { confirm=true },modifier=Modifier.fillMaxWidth()) {
        Text(ui(dict,"deleteAll"))
    }
    if(feedback.isNotBlank()) Text(feedback,color=Ink)
    if(confirm) AlertDialog(
        onDismissRequest = {confirm=false},
        title = {Text(ui(dict,"deleteConfirmTitle"))},
        text = {Text(ui(dict,"deleteConfirm"))},
        confirmButton = {
            TextButton(onClick = {
                var ok = true
                listOf("mizac_photo.dat","mizac_photo.tmp","mizac_video.dat","mizac_video.tmp")
                    .forEach { name ->
                        val f=File(context.filesDir,name)
                        if(f.exists()&&!f.delete()) ok=false
                    }
                val pending=File(context.cacheDir,"mizac_video_capture.mp4")
                if(pending.exists()&&!pending.delete())ok=false
                val prefs=context.getSharedPreferences("mizac_local_setup",Context.MODE_PRIVATE)
                val previousLanguage=prefs.getString("ui_language","tr")
                if(!prefs.edit().clear().putString("ui_language",previousLanguage).commit())ok=false
                feedback=ui(dict,if(ok)"deleted" else "deletePartial")
                confirm=false
            }){Text(ui(dict,"deleteYes"))}
        },
        dismissButton={TextButton(onClick={confirm=false}){Text(ui(dict,"cancel"))}}
    )
}
