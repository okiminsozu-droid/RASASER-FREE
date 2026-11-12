package com.rasaser.free.kurankelime

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.content.ClipData
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
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
    @Suppress("DEPRECATION")
    private fun enableImmersiveReading() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveReading()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveReading()
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

fun openBookPage(context: Context, page: Int = 0, highlightAyah: Int = 0, highlightWordId: Int = 0,
                 hitX: Float? = null, hitY: Float? = null) {
    context.startActivity(Intent(context, StableReaderActivity::class.java).apply {
        putExtra("page", page)
        putExtra("highlight_ayah", highlightAyah)
        putExtra("highlight_word_id", highlightWordId)
        if (hitX != null && hitY != null) {
            putExtra("hit_x", hitX)
            putExtra("hit_y", hitY)
        }
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

internal fun installedBook(context: Context): File? {
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

private fun pageAssetPath(page: Int): String =
    "quran/rendered/page_" + page.toString().padStart(4, '0') + ".webp"

private fun hasRenderedPages(context: Context): Boolean = try {
    context.assets.open(pageAssetPath(1)).use {
        it.read() >= 0
    }
} catch (_: Exception) { false }

/**
 * Prefer pre-rendered pages: decoding one small WebP doesn't initialize Android PdfRenderer
 * or allocate a whole PDF document's native bitmaps. Keep original PDF as archival source.
 */
private fun renderPage(context: Context, file: File?, page: Int, width: Int): Bitmap {
    require(page in 1..EXPECTED_PAGES)
    try {
        context.assets.open(pageAssetPath(page)).use { stream ->
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val decoded = BitmapFactory.decodeStream(stream, null, opts)
            if (decoded != null && decoded.width > 0 && decoded.height > 0) return decoded
        }
    } catch (_: Exception) { /* Older, source-only APK: PDF fallback below */ }
    val existing = file ?: throw IllegalStateException("Yerleşik sayfa verisi bulunamadı.")
    ParcelFileDescriptor.open(existing, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { pdf ->
            require(page in 1..pdf.pageCount)
            pdf.openPage(page - 1).use { p ->
                val renderWidth = width.coerceIn(600, 1200)
                val h = (renderWidth.toFloat() * p.height / p.width).toInt()
                val image = Bitmap.createBitmap(renderWidth, h, Bitmap.Config.ARGB_8888)
                image.eraseColor(android.graphics.Color.WHITE)
                try {
                    p.render(image, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return image
                } catch (e: Exception) {
                    image.recycle()
                    throw e
                }
            }
        }
    }
}

private fun buildPageShare(context: Context, file: File?, page: Int): Intent {
    val original = renderPage(context, file, page, 1200)
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
    val privateIndex = remember { PageWordIndex(context.applicationContext) }
    val bundledIndex = remember { BundledWordIndex(context.applicationContext) }
    val localEngine = remember { PdfTextEngine(context.applicationContext) }
    var page by rememberSaveable {
        mutableIntStateOf(if (initialPage in 1..EXPECTED_PAGES) initialPage else prefs.lastPage())
    }
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var pageDialog by remember { mutableStateOf(false) }
    var pageDraft by remember { mutableStateOf(page.toString()) }
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var renderedPagesReady by remember { mutableStateOf(false) }
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
        // The complete Quran is already inside the APK as rendered pages.
        // Do not duplicate its 90 MB PDF in internal phone storage.
        renderedPagesReady = withContext(Dispatchers.IO) { hasRenderedPages(context) }
        if (!renderedPagesReady) {
            pdfFile = withContext(Dispatchers.IO) { installedBook(context) }
        }
        loading = false
    }
    LaunchedEffect(page, highlightWordId) {
        chosen = if (highlightWordId != 0) {
            withContext(Dispatchers.IO) {
                (if (highlightWordId < 0) localEngine.byId(highlightWordId)
                 else if (highlightWordId >= 1000000) bundledIndex.byId(highlightWordId)
                 else privateIndex.byId(highlightWordId))?.takeIf { it.page == page }
            }
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
    LaunchedEffect(pdfFile, renderedPagesReady, page, zoom) {
        if (pdfFile == null && !renderedPagesReady) return@LaunchedEffect
        bitmap = null
        val width = when {
            zoom >= 2f -> 2400
            zoom >= 1.5f -> 1800
            else -> 1400
        }
        try {
            bitmap = withContext(Dispatchers.IO) { renderPage(context, pdfFile, page, width) }
            error = null
        } catch (e: Exception) {
            error = "Sayfa açılamadı: " + e.message
        }
    }
    if (pageDialog) {
        AlertDialog(
            onDismissRequest = { pageDialog = false },
            title = { Text("Sayfaya git") },
            text = {
                OutlinedTextField(
                    value = pageDraft,
                    onValueChange = { pageDraft = it.filter(Char::isDigit).take(3) },
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
                        zoom = 1f
                        pageDialog = false
                    }
                }, enabled = (pageDraft.toIntOrNull() ?: 0) in 1..EXPECTED_PAGES) { Text("Git") }
            },
            dismissButton = { TextButton(onClick = { pageDialog = false }) { Text("İptal") } }
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Paper)) {
        // Toolbar overlays DO NOT subtract height from the reading page.
        // Preserve the real PDF aspect ratio rather than distorting letters.
        val aspect = bitmap?.let { it.width.toFloat() / it.height } ?: (595f / 842f)
        val fittingWidth = minOf(maxWidth, maxHeight * aspect)
        val paperWidth = fittingWidth * zoom
        val paperHeight = paperWidth / aspect
        val containerWidth = maxOf(maxWidth, paperWidth)
        val containerHeight = maxOf(maxHeight, paperHeight)
        val panning = if (zoom > 1.01f)
            Modifier.horizontalScroll(horizontal).verticalScroll(vertical)
        else Modifier

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Forest)
            }
        } else if (pdfFile == null && !renderedPagesReady) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error ?: "304 sayfalık Kur’an PDF bu APK içinde bulunmuyor. Eksiksiz çevrimdışı paketle kurulmalıdır.")
                Spacer(Modifier.height(12.dp))
                Text("Kullanıcıdan PDF seçmesi istenmez. Çevrimdışı paket yeniden oluşturulmalıdır.",
                    modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        } else if (error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(error ?: "", color = Color.Red) }
        } else {
            Box(
                Modifier.fillMaxSize()
                    .pointerInput(page, zoom) {
                        if (zoom <= 1.01f) detectHorizontalDragGestures(
                            onDragStart = { dragDistance = 0f },
                            onHorizontalDrag = { change, dx ->
                                dragDistance += dx
                                change.consume()
                            },
                            onDragEnd = {
                                if (dragDistance < -65f && page < EXPECTED_PAGES) {
                                    page++; zoom = 1f; controlsVisible = false
                                } else if (dragDistance > 65f && page > 1) {
                                    page--; zoom = 1f; controlsVisible = false
                                }
                                dragDistance = 0f
                            },
                            onDragCancel = { dragDistance = 0f }
                        )
                    }
                    .then(panning)
            ) {
                Box(Modifier.width(containerWidth).height(containerHeight),
                    contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.width(paperWidth).height(paperHeight)
                            .pointerInput(page, zoom, paperWidth, paperHeight) {
                                detectTapGestures(
                                    onDoubleTap = { controlsVisible = !controlsVisible },
                                    onTap = { touch ->
                                        val x = (touch.x / paperWidth.toPx() * 595f).coerceIn(0f, 595f)
                                        val y = (touch.y / paperHeight.toPx() * 842f).coerceIn(0f, 842f)
                                        val tappedPage = page
                                        chosen = null
                                        manualMarker = Offset(x / 595f, y / 842f)
                                        scope.launch {
                                            val result = withContext(Dispatchers.IO) {
                                                privateIndex.nearestWord(tappedPage, x, y)
                                                    ?: bundledIndex.nearest(tappedPage, x, y)
                                                    ?: if (Build.VERSION.SDK_INT >= 35 && !bundledIndex.isBundled)
                                                        pdfFile?.let { localEngine.selectAt(it, tappedPage, x, y) }
                                                       else null
                                            }
                                            if (page == tappedPage) {
                                                chosen = result
                                                if (result != null) {
                                                    manualMarker = null
                                                    controlsVisible = true
                                                }
                                            }
                                        }
                                    },
                                    onLongPress = { touch ->
                                        val x = (touch.x / paperWidth.toPx() * 595f).coerceIn(0f, 595f)
                                        val y = (touch.y / paperHeight.toPx() * 842f).coerceIn(0f, 842f)
                                        val tappedPage = page
                                        chosen = null
                                        scope.launch {
                                            val result = withContext(Dispatchers.IO) {
                                                privateIndex.nearestWord(tappedPage, x, y)
                                                    ?: bundledIndex.nearest(tappedPage, x, y)
                                                    ?: if (Build.VERSION.SDK_INT >= 35)
                                                        pdfFile?.let { localEngine.selectAt(it, tappedPage, x, y) }
                                                       else null
                                            }
                                            if (page == tappedPage) {
                                                chosen = result
                                                controlsVisible = true
                                                if (result != null) manualMarker = null
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        val image = bitmap
                        if (image != null) {
                            Image(image.asImageBitmap(),
                                contentDescription = "Kur'an PDF, sayfa $page",
                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                            val selected = chosen
                            if (selected != null) {
                                val left = (paperWidth * (selected.x / 595f) - 3.dp).coerceAtLeast(0.dp)
                                val top = (paperHeight * ((selected.y - 4f) / 842f) - 2.dp).coerceAtLeast(0.dp)
                                val width = paperWidth * (selected.width / 595f) + 7.dp
                                val height = paperHeight * ((selected.height + 8f) / 842f) + 4.dp
                                Box(Modifier.offset(x = left, y = top).width(width).height(height)
                                    .border(2.dp, Color(0xFFBE8C22),
                                        androidx.compose.foundation.shape.RoundedCornerShape(5.dp))
                                    .background(Color(0x40F5D66D)))
                            } else if (manualMarker != null) {
                                val point = manualMarker!!
                                Box(Modifier.offset(
                                    x = paperWidth * point.x - 12.dp,
                                    y = paperHeight * point.y - 12.dp
                                ).size(24.dp)
                                    .border(2.dp, Color(0xFFBE8C22),
                                        androidx.compose.foundation.shape.CircleShape))
                            } else if (page == 1 && highlightAyah in 1..7) {
                                val starts = listOf(.134f,.239f,.328f,.421f,.516f,.608f,.698f,.813f)
                                Box(Modifier.offset(y = paperHeight * starts[highlightAyah-1])
                                    .fillMaxWidth().height(paperHeight *
                                        (starts[highlightAyah]-starts[highlightAyah-1]))
                                    .background(Color(0x30F5D66D)))
                            }
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Forest)
                            }
                        }
                    }
                }
            }
        }

        if (controlsVisible) {
            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth()
                    .background(Color(0xF5F9F3E7)).height(56.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (page < EXPECTED_PAGES) { page++; zoom = 1f } },
                    enabled = page < EXPECTED_PAGES) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Sonraki sayfa")
                }
                TextButton(onClick = { pageDraft = page.toString(); pageDialog = true }) {
                    Text("$page / $EXPECTED_PAGES", color = Forest)
                }
                IconButton(onClick = { prefs.toggleFavorite(page); favoriteRevision++ }) {
                    Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favori",
                        tint = if (isFavorite) Color(0xFFB58A36) else Forest)
                }
                IconButton(onClick = home) {
                    Icon(Icons.Default.Home, contentDescription = "Ana sayfa", tint = Forest)
                }
                IconButton(onClick = { if (page > 1) { page--; zoom = 1f } },
                    enabled = page > 1) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Önceki sayfa")
                }
            }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color(0xF5FFFFFF))) {
                val selected = chosen
                if (selected != null) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(selected.gloss, color = Forest,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f), maxLines = 2)
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Kur'an seçili metin", selected.gloss))
                            Toast.makeText(context, "Seçili metin kopyalandı", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Metni kopyala")
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { controlsVisible = false }) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Tam ekran oku")
                    }
                    IconButton(onClick = {
                        val file = pdfFile
                        if (file == null && !renderedPagesReady) {
                            Toast.makeText(context, "Önce PDF'yi seçin.", Toast.LENGTH_SHORT).show()
                        } else scope.launch {
                            try {
                                val send = withContext(Dispatchers.IO) {
                                    buildPageShare(context, file, page)
                                }
                                context.startActivity(Intent.createChooser(send, "Sayfayı paylaş"))
                            } catch (_: Exception) {
                                Toast.makeText(context, "Paylaşım hazırlanamadı.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Sayfayı paylaş")
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { zoom = (zoom - .5f).coerceAtLeast(1f) }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Küçült")
                    }
                    Text((zoom * 100).toInt().toString() + "%",
                        style = MaterialTheme.typography.labelSmall)
                    IconButton(onClick = { zoom = (zoom + .5f).coerceAtMost(3f) }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Büyüt")
                    }
                }
            }
        } else {
            IconButton(
                onClick = { controlsVisible = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    .background(Color(0xDDF9F3E7), androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(Icons.Default.FullscreenExit, contentDescription = "Okuma araçlarını göster",
                    tint = Forest)
            }
        }
    }
}
