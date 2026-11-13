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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        screen = when(screen) {
            "geometry" -> "home"
            "questions" -> "analysis-setup"
            "history-reflection" -> "history"
            else -> "home"
        }
    }
    CompositionLocalProvider(
        LocalLayoutDirection provides if (lang.code == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Column(Modifier.fillMaxSize().background(Sand)) {
            // Keep the flag selector at the physical top-right in every language,
            // while the page content mirrors for Arabic.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall)
                    ApprovedMizacLogo(208)
                } else if (screen != "analysis-setup") {
                    ApprovedMizacLogo(68)
                }
                when (screen) {
                    "home" -> HomeQuickActions(
                        dict = dict,
                        onStandard = { screen = "analysis-setup" },
                        onQuick = { screen = "geometry" }
                    )
                    "analysis-setup" -> AnalysisSelection(
                        dict = dict,
                        onQuestions = { number -> count = number; screen = "questions" }
                    )
                    "geometry" -> FaceGeometryScreen(
                        onBack = { screen = "home" }, lang = lang, strings = dict
                    )
                    "questions" -> OriginalQuestionnaire(
                        lang = lang, dict = dict, questionCount = count,
                        onBack = { screen = "analysis-setup" },
                        onHistory = { screen = "history" }
                    )
                    "history" -> MarifetnameScreen(
                        onBack = { screen = "home" }, lang = lang, strings = dict,
                        onReflection = { screen = "history-reflection" }
                    )
                    "history-reflection" -> HistoricalReflectionScreen(
                        lang, dict, onBack = { screen = "analysis-setup" }
                    )
                    "privacy" -> PrivacyPage(
                        dict = dict,
                        onBack = { screen = "home" },
                        onHistory = { screen = "history" }
                    )
                }
            }
            if (screen == "home") {
                Footer(dict, onLicense = { screen = "privacy" })
            }
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
private fun Footer(dict: Map<String, String>, onLicense: () -> Unit) {
    val context = LocalContext.current
    Box(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 18.dp, top = 1.dp)) {
        TextButton(
            modifier = Modifier.align(Alignment.BottomStart).size(44.dp),
            onClick = onLicense
        ) {
            Icon(Icons.Outlined.Info, contentDescription = ui(dict,"privacy"),
                tint = Ink, modifier = Modifier.size(19.dp))
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(ui(dict, "brand"), color = Ink, textAlign = TextAlign.Center,
                fontSize = 11.sp, maxLines = 1)
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.okiminsozu.com"))
                        )
                    }
                },
                modifier = Modifier.height(34.dp)
            ) {
                Text(ui(dict, "website"), fontSize = 12.sp,
                    color = Ink, textDecoration = TextDecoration.Underline)
            }
        }
    }
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
private fun HomeQuickActions(
    dict: Map<String, String>,
    onStandard: () -> Unit,
    onQuick: () -> Unit
) {
    val context = LocalContext.current
    var photoSaved by remember { mutableStateOf(File(context.filesDir,"mizac_photo.dat").exists()) }
    var videoSaved by remember { mutableStateOf(File(context.filesDir,"mizac_video.dat").exists()) }
    var feedback by remember { mutableStateOf("") }
    var pendingQuick by remember { mutableStateOf(false) }
    var videoOptions by remember { mutableStateOf(false) }

    fun save(uri: Uri?, kind: String) {
        if (uri == null) {
            pendingQuick = false
            return
        }
        runCatching { saveMediaLocally(context, uri, kind) }
            .onSuccess {
                if (kind == "photo") photoSaved = true else videoSaved = true
                feedback = ui(dict,if(kind=="photo")"savePhoto" else "saveVideo")
                if (pendingQuick) {
                    pendingQuick = false
                    onQuick()
                }
            }
            .onFailure {
                pendingQuick = false
                feedback = ui(dict,when(it.message) {
                    "tooLong" -> "tooLong"
                    "tooLarge" -> "tooLarge"
                    else -> "saveFailure"
                })
            }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        save(it,"photo")
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        save(it,"video")
    }
    val captureFile = remember(context) { File(context.cacheDir,"mizac_video_capture.mp4") }
    val captureUri = remember(context) {
        FileProvider.getUriForFile(context,context.packageName + ".fileprovider",captureFile)
    }
    val capture = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val uri = if(captureFile.exists() && captureFile.length()>0L) captureUri
                else it.data?.data
            if(uri!=null) save(uri,"video") else feedback=ui(dict,"saveFailure")
        } else feedback=ui(dict,"recordCancelled")
        captureFile.delete()
    }
    fun recordVideo() {
        captureFile.delete()
        val intent=Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_DURATION_LIMIT,30)
            putExtra(MediaStore.EXTRA_VIDEO_QUALITY,1)
            putExtra(MediaStore.EXTRA_OUTPUT,captureUri)
            clipData=ClipData.newUri(context.contentResolver,"MIZAC",captureUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        runCatching { capture.launch(intent) }.onFailure {
            feedback=ui(dict,"cameraUnavailable")
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp,Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MediaCircle(
            isSaved = photoSaved,
            label = ui(dict,"photoChoose"),
            icon = { Icon(Icons.Outlined.AddPhotoAlternate,contentDescription = null,
                tint = Ink,modifier=Modifier.size(34.dp)) },
            onClick = { pendingQuick=false; photoPicker.launch("image/*") }
        )
        MediaCircle(
            isSaved = videoSaved,
            label = ui(dict,"videoChoose"),
            icon = { Icon(Icons.Outlined.VideoLibrary,contentDescription = null,
                tint = Ink,modifier=Modifier.size(34.dp)) },
            onClick = { videoOptions=true }
        )
    }
    WhiteAction(ui(dict,"analyze"),onClick=onStandard)
    WhiteAction(ui(dict,"quickAnalysis"),onClick={
        when {
            photoSaved || videoSaved -> onQuick()
            else -> { pendingQuick=true; photoPicker.launch("image/*") }
        }
    })
    if (feedback.isNotBlank()) {
        Text(feedback, color = Muted, textAlign=TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),style=MaterialTheme.typography.bodySmall)
    }
    if (videoOptions) {
        AlertDialog(
            onDismissRequest = { videoOptions=false },
            title = { Text(ui(dict,"videoTitle")) },
            text = { Text(ui(dict,"videoHint")) },
            confirmButton = {
                TextButton(onClick = { videoOptions=false; videoPicker.launch("video/*") }) {
                    Text(ui(dict,"videoChoose"))
                }
            },
            dismissButton = {
                Column {
                    TextButton(onClick={videoOptions=false; recordVideo()}) {
                        Text(ui(dict,"videoRecord"))
                    }
                    TextButton(onClick={videoOptions=false}) {
                        Text(ui(dict,"cancel"))
                    }
                }
            }
        )
    }
}

@Composable
private fun MediaCircle(
    isSaved: Boolean,
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Column(horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.size(76.dp).border(
                width=if(isSaved)3.dp else 1.dp,
                color=if(isSaved) Color(0xFF9D742C) else Color(0xFFA68D6C),
                shape=CircleShape
            ).background(Ivory,CircleShape).clickable(onClick=onClick),
            contentAlignment=Alignment.Center
        ) {
            icon()
            if(isSaved) Text("✓",color=Color(0xFF47763E),
                modifier=Modifier.align(Alignment.TopEnd).padding(end=6.dp),
                fontWeight=FontWeight.Bold)
        }
        Text(label,color=Ink,textAlign=TextAlign.Center,
            fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis,
            modifier=Modifier.width(125.dp))
    }
}

@Composable
private fun AnalysisSelection(
    dict: Map<String, String>,
    onQuestions: (Int) -> Unit
) {
    val context=LocalContext.current
    val prefs=remember(context) {
        context.getSharedPreferences("mizac_local_setup",Context.MODE_PRIVATE)
    }
    var count by rememberSaveable {
        mutableStateOf(prefs.getInt("question_count",10).takeIf { it in listOf(10,50,120) } ?: 10)
    }
    Column(
        modifier=Modifier.fillMaxWidth().padding(top=56.dp),
        verticalArrangement=Arrangement.spacedBy(42.dp),
        horizontalAlignment=Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(),
            horizontalArrangement=Arrangement.SpaceEvenly,
            verticalAlignment=Alignment.CenterVertically) {
            listOf(10,50,120).forEach { value ->
                Box(
                    modifier=Modifier.size(88.dp)
                        .border(
                            width=if(count==value)3.dp else 1.dp,
                            color=if(count==value)Color(0xFF98702D) else Color(0xFFCCB48E),
                            shape=CircleShape
                        )
                        .background(Ivory,CircleShape)
                        .clickable { count=value },
                    contentAlignment=Alignment.Center
                ) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(value.toString(),color=Ink,fontWeight=FontWeight.Bold,
                            fontSize=28.sp)
                        Text(ui(dict,"questions"),color=Ink,fontSize=11.sp)
                    }
                }
            }
        }
        WhiteAction(ui(dict,"start"),onClick={
            prefs.edit().putInt("question_count",count).apply()
            onQuestions(count)
        })
    }
}

@Composable
private fun PrivacyPage(
    dict: Map<String, String>,
    onBack: () -> Unit,
    onHistory: () -> Unit
) {
    val context = LocalContext.current
    var confirm by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    Text(ui(dict,"privacyTitle"),color=Ink,
        style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold)
    Card(colors = CardDefaults.cardColors(containerColor=Ivory)) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(ui(dict,"localPrivacy"),color=Ink,fontWeight=FontWeight.Bold)
            Text(ui(dict,"privacyText"),color=Muted)
            Text(ui(dict,"licenseTitle"),color=Ink,fontWeight=FontWeight.Bold)
            Text(ui(dict,"licenseNotice"),color=Muted)
        }
    }
    OutlinedButton(onClick=onHistory,modifier=Modifier.fillMaxWidth()) {
        Text(ui(dict,"history"))
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
