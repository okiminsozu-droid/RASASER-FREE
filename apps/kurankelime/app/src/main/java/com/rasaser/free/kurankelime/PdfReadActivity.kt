package com.rasaser.free.kurankelime

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

private const val EXPECTED_PAGES = 304
private const val EXPECTED_LENGTH = 90889211L
private const val EXPECTED_SHA256 = "387674c9a3bfb2c501c2dcca4b63946ea05f386dcd4d6fa4b63b4060b01397ea"
private const val BOOK_ASSET = "quran/kelime_mealli.pdf"
private val Paper = Color(0xFFFFFFFF)
private val GoldSand = Color(0xFFE8CEAC)
private val Forest = Color(0xFF17422F)

class PdfReadActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val first = intent.getIntExtra("page", 0)
        val highlight = intent.getIntExtra("highlight_ayah", 0)
        val wordId = intent.getIntExtra("highlight_word_id", 0)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(background = Paper, primary = Forest)) {
                PdfReaderScreen(first, highlight, wordId) {
                    startActivity(Intent(this, DiscoverHomeActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                    finish()
                }
            }
        }
    }
}

fun openBookPage(context: Context, page: Int = 0, highlightAyah: Int = 0, highlightWordId: Int = 0) {
    context.startActivity(Intent(context, PdfReadActivity::class.java).apply {
        putExtra("page", page)
        putExtra("highlight_ayah", highlightAyah)
        putExtra("highlight_word_id", highlightWordId)
    })
}

private fun verifyPdf(file: File): Boolean {
    return try {
    if (file.length() != EXPECTED_LENGTH) return false
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(65536)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count > 0) digest.update(buffer, 0, count)
        }
    }
    val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    if (actual != EXPECTED_SHA256) return false
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { it.pageCount == EXPECTED_PAGES }
    }
} catch (_: Exception) { false }
}

private fun installedBook(context: Context): File? {
    val target = File(context.filesDir, "quran_original_304.pdf")
    if (target.isFile && verifyPdf(target)) return target
    val temp = File(context.filesDir, "quran_original_304.pending")
    return try {
        context.assets.open(BOOK_ASSET).use { input ->
            temp.outputStream().use { out -> input.copyTo(out, 65536) }
        }
        if (!verifyPdf(temp)) throw IllegalArgumentException("Invalid bundled Quran PDF")
        if (target.exists()) target.delete()
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
        target
    } catch (_: Exception) {
        temp.delete()
        null
    }
}

private fun importBook(context: Context, uri: android.net.Uri): File {
    val temp = File(context.filesDir, "quran_import.pending")
    val target = File(context.filesDir, "quran_original_304.pdf")
    try {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Dosya açılamadı")
        input.use { source -> temp.outputStream().use { source.copyTo(it, 65536) } }
        require(verifyPdf(temp)) { "304 sayfalık doğru PDF dosyasını seçin." }
        if (target.exists()) target.delete()
        if (!temp.renameTo(target)) { temp.copyTo(target, overwrite = true); temp.delete() }
        return target
    } finally { temp.delete() }
}

private fun renderPage(file: File, page: Int, width: Int): Bitmap {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { pdf ->
            require(page in 1..pdf.pageCount)
            pdf.openPage(page - 1).use { p ->
                val bitmap = Bitmap.createBitmap(width, (width.toFloat() * p.height / p.width).toInt(), Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                p.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return bitmap
            }
        }
    }
}

private fun buildPageShare(context: Context, file: File, page: Int): Intent {
    val original = renderPage(file, page, 1200)
    val output = Bitmap.createBitmap(original.width, original.height + 180, Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(output)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(18, 68, 39)
            textSize = 39f
            isFakeBoldText = true
        }
        canvas.drawText("KURANI KEŞFET", 28f, 60f, paint)
        try {
            context.assets.open("brand/logo.webp").use { input ->
                val logo = BitmapFactory.decodeStream(input)
                if (logo != null) {
                    canvas.drawBitmap(logo, null, Rect(output.width - 110, 6, output.width - 8, 108), Paint(Paint.FILTER_BITMAP_FLAG))
                    logo.recycle()
                }
            }
        } catch (_: Exception) { /* the name remains on the shared image */ }
        canvas.drawBitmap(original, 0f, 100f, null)
        paint.isFakeBoldText = false
        paint.textSize = 28f
        canvas.drawText("Kelime Mealli Kur'an - Sayfa " + page, 25f, (output.height - 42).toFloat(), paint)
        canvas.drawText("RaSaSeR APPS & TECHNOLOGY", 25f, (output.height - 12).toFloat(), paint.apply { textSize = 22f })
        val shareDirectory = File(context.cacheDir, "shares").apply { mkdirs() }
        val outputFile = File(shareDirectory, "kurani_kesfet_sayfa_" + page + ".png")
        outputFile.outputStream().use { output.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".provider", outputFile)
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } finally {
        original.recycle()
        output.recycle()
    }
}

@Composable
private fun PdfReaderScreen(initialPage: Int, highlightAyah: Int, highlightWordId: Int, home: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PageStore(context) }
    val wordIndex = remember { PageWordIndex(context.applicationContext) }
    var page by rememberSaveable { mutableIntStateOf(if (initialPage in 1..EXPECTED_PAGES) initialPage else prefs.lastPage()) }
    var pageDialog by remember { mutableStateOf(false) }
    var pageDraft by remember { mutableStateOf(page.toString()) }
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var favoriteRevision by remember { mutableIntStateOf(0) }
    val isFavorite = remember(page, favoriteRevision) { prefs.isFavorite(page) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var chosen by remember(page) { mutableStateOf<PageWord?>(null) }
    var manualMarker by remember(page) { mutableStateOf<Offset?>(null) }
    var dragDistance by remember(page) { mutableFloatStateOf(0f) }
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()

    LaunchedEffect(Unit) {
        pdfFile = withContext(Dispatchers.IO) { installedBook(context) }
        loading = false
    }
    LaunchedEffect(page, highlightWordId) {
        chosen = if (highlightWordId > 0) {
            withContext(Dispatchers.Default) { wordIndex.byId(highlightWordId)?.takeIf { it.page == page } }
        } else null
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            loading = true
            scope.launch {
                try {
                    pdfFile = withContext(Dispatchers.IO) { importBook(context, uri) }
                    error = null
                } catch (e: Exception) {
                    error = e.message ?: "PDF dosyası açılamadı."
                } finally { loading = false }
            }
        }
    }
    LaunchedEffect(page) {
        prefs.setLastPage(page)
        vertical.scrollTo(0)
        horizontal.scrollTo(0)
    }
    LaunchedEffect(pdfFile, page, zoom) {
        val file = pdfFile ?: return@LaunchedEffect
        bitmap = null
        val desiredWidth = when {
            zoom >= 2f -> 2400
            zoom >= 1.5f -> 1800
            else -> 1400
        }
        try {
            bitmap = withContext(Dispatchers.IO) { renderPage(file, page, desiredWidth) }
            error = null
        } catch (e: Exception) { error = "Sayfa açılamadı: " + e.message }
    }
    if (pageDialog) {
        AlertDialog(
            onDismissRequest = { pageDialog = false },
            title = { Text("Sayfaya git") },
            text = {
                OutlinedTextField(
                    value = pageDraft,
                    onValueChange = { value -> pageDraft = value.filter(Char::isDigit).take(3) },
                    label = { Text("1–304 arasında sayfa") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = pageDraft.toIntOrNull()
                    if (target != null && target in 1..EXPECTED_PAGES) {
                        page = target
                        pageDialog = false
                        zoom = 1f
                    }
                }, enabled = (pageDraft.toIntOrNull() ?: 0) in 1..EXPECTED_PAGES) { Text("Git") }
            },
            dismissButton = { TextButton(onClick = { pageDialog = false }) { Text("İptal") } }
        )
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().background(Color(0xFFF9F3E7)).height(54.dp).padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (page < EXPECTED_PAGES) { page++; zoom = 1f } }, enabled = page < EXPECTED_PAGES) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Sonraki sayfa - sola git")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (page == 1) "Fâtiha Sûresi" else if (page == 2) "Bakara Sûresi" else "KURANI KEŞFET",
                    color = Forest, style = MaterialTheme.typography.labelMedium
                )
                TextButton(onClick = { pageDraft = page.toString(); pageDialog = true },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(27.dp)) {
                    Text("Sayfa " + page + " / " + EXPECTED_PAGES, style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = home) { Icon(Icons.Default.Home, contentDescription = "Ana sayfa", tint = Forest) }
            IconButton(onClick = { if (page > 1) { page--; zoom = 1f } }, enabled = page > 1) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Önceki sayfa - sağa git")
            }
        }
        Row(
            Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { prefs.toggleFavorite(page); favoriteRevision++ }) {
                Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Sayfayı favorilere kaydet",
                    tint = if (isFavorite) Color(0xFFB58A36) else Forest)
            }
            IconButton(onClick = {
                val file = pdfFile
                if (file == null) {
                    Toast.makeText(context, "Önce PDF'yi seçin.", Toast.LENGTH_SHORT).show()
                } else {
                    scope.launch {
                        try {
                            val send = withContext(Dispatchers.IO) { buildPageShare(context, file, page) }
                            context.startActivity(Intent.createChooser(send, "Sayfayı paylaş"))
                        } catch (_: Exception) {
                            Toast.makeText(context, "Paylaşım hazırlanamadı.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }) { Icon(Icons.Default.Share, contentDescription = "Sayfayı logolu görsel olarak paylaş") }
            val selected = chosen
            if (selected != null) {
                Text(selected.gloss, modifier = Modifier.weight(1f).padding(horizontal = 5.dp),
                    maxLines = 1, style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF885E20))
            } else Spacer(Modifier.weight(1f))
            IconButton(onClick = { zoom = (zoom - .5f).coerceAtLeast(1f) }) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Küçült")
            }
            Text((zoom * 100).toInt().toString() + "%", style = MaterialTheme.typography.labelSmall)
            IconButton(onClick = { zoom = (zoom + .5f).coerceAtMost(3f) }) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Büyüt")
            }
        }
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Forest)
            }
        } else if (pdfFile == null) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error ?: "304 sayfalık PDF henüz cihazda bulunmuyor.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = { picker.launch(arrayOf("application/pdf")) },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldSand, contentColor = Forest)) {
                    Text("PDF'yi bir kez seç")
                }
                Text("PDF yerel saklanır; tekrar seçim veya internet gerekmez.",
                    modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        } else if (error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error ?: "", color = Color.Red)
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Fit the complete A4 page in the remaining screen without cropping.
                val fittingWidth = minOf(maxWidth, maxHeight * (595f / 842f))
                val paperWidth = fittingWidth * zoom
                val paperHeight = paperWidth * (842f / 595f)
                val containerWidth = maxOf(maxWidth, paperWidth)
                val containerHeight = maxOf(maxHeight, paperHeight)

                Box(Modifier.fillMaxSize().horizontalScroll(horizontal).verticalScroll(vertical)) {
                    Box(
                        Modifier.width(containerWidth).height(containerHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.width(paperWidth).height(paperHeight)
                                .pointerInput(page, zoom) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { dragDistance = 0f },
                                        onHorizontalDrag = { change, drag ->
                                            if (zoom <= 1.01f) {
                                                dragDistance += drag
                                                change.consume()
                                            }
                                        },
                                        onDragEnd = {
                                            if (zoom <= 1.01f && dragDistance < -75f && page < EXPECTED_PAGES) {
                                                page++
                                            } else if (zoom <= 1.01f && dragDistance > 75f && page > 1) {
                                                page--
                                            }
                                            dragDistance = 0f
                                        }
                                    )
                                }
                                .pointerInput(page, zoom) {
                                    detectTapGestures { point ->
                                        val x = point.x / paperWidth.toPx() * 595f
                                        val y = point.y / paperHeight.toPx() * 842f
                                        val word = wordIndex.nearestWord(page, x, y)
                                        chosen = word
                                        manualMarker = if (word == null) Offset(point.x / paperWidth.toPx(),
                                            point.y / paperHeight.toPx()) else null
                                    }
                                }
                        ) {
                            val image = bitmap
                            if (image != null) {
                                Image(
                                    bitmap = image.asImageBitmap(),
                                    contentDescription = "Kelime mealli Kur'an sayfa " + page,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.FillBounds
                                )
                                if (chosen != null) {
                                    val word = chosen!!
                                    val left = (paperWidth * (word.x / 595f) - 3.dp).coerceAtLeast(0.dp)
                                    val top = (paperHeight * ((word.y - 20f) / 842f) - 2.dp).coerceAtLeast(0.dp)
                                    val width = paperWidth * (word.width / 595f) + 7.dp
                                    val height = paperHeight * ((word.height + 25f) / 842f) + 4.dp
                                    Box(
                                        Modifier.offset(x = left, y = top).width(width).height(height)
                                            .border(2.dp, Color(0xFFBE8C22), androidx.compose.foundation.shape.RoundedCornerShape(5.dp))
                                            .background(Color(0x50F5D66D))
                                    )
                                } else if (manualMarker != null) {
                                    val point = manualMarker!!
                                    Box(
                                        Modifier.offset(
                                            x = paperWidth * point.x - 12.dp,
                                            y = paperHeight * point.y - 12.dp
                                        ).size(24.dp)
                                            .border(2.dp, Color(0xFFBE8C22),
                                                androidx.compose.foundation.shape.CircleShape)
                                            .background(Color(0x55F5D66D),
                                                androidx.compose.foundation.shape.CircleShape)
                                    )
                                } else if (page == 1 && highlightAyah in 1..7) {
                                    // Safe fallback for the historical Fatiha-only demo index.
                                    val starts = listOf(.134f,.239f,.328f,.421f,.516f,.608f,.698f,.813f)
                                    val top = starts[highlightAyah - 1]
                                    val height = starts[highlightAyah] - top
                                    Box(
                                        Modifier.offset(y = paperHeight * top).fillMaxWidth()
                                            .height(paperHeight * height)
                                            .background(Color(0x30F5D66D))
                                    )
                                }
                            } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Forest)
                            }
                        }
                    }
                }
            }
        }
    }
}
