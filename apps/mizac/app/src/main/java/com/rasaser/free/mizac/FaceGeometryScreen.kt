package com.rasaser.free.mizac

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private val ink = Color(0xFF1E1711)
private val beigeCard = Color(0xFFFFFBF6)
private val softInk = Color(0xFF65584A)

data class GeometryReport(
    val origin: String,
    val people: Int,
    val widthPx: Int,
    val heightPx: Int,
    val widthHeightRatio: Float,
    val eyeDistanceRatio: Float?,
    val noseMouthRatio: Float?,
    val yawDegrees: Float,
    val rollDegrees: Float
)

private sealed interface GeometryState {
    data object Loading : GeometryState
    data class Success(val report: GeometryReport) : GeometryState
    data class Error(val message: String) : GeometryState
}

private fun orientedPhoto(file: File): Bitmap? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null
    val sample = maxOf(1, maxOf(options.outWidth, options.outHeight) / 1440)
    val bitmap = BitmapFactory.decodeFile(
        file.path, BitmapFactory.Options().apply { inSampleSize = sample }
    ) ?: return null
    val orientation = try {
        ExifInterface(file.path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        )
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.preScale(-1f, 1f); matrix.postRotate(90f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.preScale(-1f, 1f); matrix.postRotate(270f) }
        else -> return bitmap
    }
    val corrected = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (corrected !== bitmap) bitmap.recycle()
    return corrected
}

private fun videoFrame(file: File): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(file.absolutePath)
        val durationMs = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        )?.toLongOrNull() ?: 0L
        val center = (durationMs.coerceIn(0L, 30_000L) / 2L) * 1_000L
        retriever.getFrameAtTime(center, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    } finally {
        retriever.release()
    }
}

private suspend fun scanLocally(context: Context): GeometryState {
    val photo = File(context.filesDir, "mizac_photo.dat")
    val video = File(context.filesDir, "mizac_video.dat")
    val fromPhoto = photo.exists()
    val source = when {
        fromPhoto -> photo
        video.exists() -> video
        else -> return GeometryState.Error("Önce fotoğraf veya video seçmelisiniz.")
    }
    val bitmap = try {
        withContext(Dispatchers.IO) {
            if (fromPhoto) orientedPhoto(source) else videoFrame(source)
        }
    } catch (_: Exception) { null }
        ?: return GeometryState.Error("Görüntü okunamadı. Daha net bir fotoğraf deneyin.")

    val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
    )
    return try {
        suspendCancellableCoroutine { continuation ->
            detector.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { faces ->
                    val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                    val report = if (face == null) {
                        GeometryState.Error("Yüz belirlenemedi. Önden, iyi ışıkta çekilmiş fotoğraf seçin.")
                    } else {
                        val box = face.boundingBox
                        val width = box.width().coerceAtLeast(1)
                        val height = box.height().coerceAtLeast(1)
                        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
                        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
                        val eyeDistance = if (leftEye != null && rightEye != null) {
                            kotlin.math.hypot(
                                (rightEye.x - leftEye.x).toDouble(),
                                (rightEye.y - leftEye.y).toDouble()
                            ).toFloat() / width.toFloat()
                        } else null
                        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
                        val mouth = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
                        val noseMouthDistance = if (nose != null && mouth != null) {
                            kotlin.math.hypot(
                                (mouth.x - nose.x).toDouble(),
                                (mouth.y - nose.y).toDouble()
                            ).toFloat() / height.toFloat()
                        } else null
                        GeometryState.Success(
                            GeometryReport(
                                origin = if (fromPhoto) "Fotoğraf" else "Video (tek kare)",
                                people = faces.size,
                                widthPx = width,
                                heightPx = height,
                                widthHeightRatio = width.toFloat() / height,
                                eyeDistanceRatio = eyeDistance,
                                noseMouthRatio = noseMouthDistance,
                                yawDegrees = face.headEulerAngleY,
                                rollDegrees = face.headEulerAngleZ
                            )
                        )
                    }
                    if (continuation.isActive) continuation.resume(report)
                }
                .addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume(GeometryState.Error("Cihaz içi yüz ölçümü başarısız oldu."))
                    }
                }
                .addOnCompleteListener {
                    detector.close()
                    bitmap.recycle()
                }
        }
    } catch (_: Exception) {
        detector.close()
        if (!bitmap.isRecycled) bitmap.recycle()
        GeometryState.Error("Yüz ölçümü tamamlanamadı.")
    }
}

@Composable
fun FaceGeometryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<GeometryState>(GeometryState.Loading) }
    LaunchedEffect(Unit) {
        state = scanLocally(context)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onBack) { Text("‹ Analiz Seçimi") }
        Text("Gerçek Yüz Geometrisi", color = ink,
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Google ML Kit görüntü üzerindeki ölçülebilir noktaları cihazınızda tespit eder. " +
                "Bu bilgilerden kişilik, zeka, dürüstlük veya niyet çıkarılmaz.",
            color = softInk
        )
        when (val current = state) {
            GeometryState.Loading -> {
                CircularProgressIndicator(color = ink)
                Text("Fotoğraf telefonunuzda inceleniyor...", color = ink)
            }
            is GeometryState.Error -> {
                Card(colors = CardDefaults.cardColors(containerColor = beigeCard)) {
                    Text(current.message, color = ink, modifier = Modifier.padding(16.dp))
                }
            }
            is GeometryState.Success -> {
                val report = current.report
                Card(shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = beigeCard)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Kaynak: " + report.origin, color = ink)
                        Text("Algılanan yüz sayısı: " + report.people, color = ink)
                        ReportValue("Yüz bölgesi (piksel)", "${report.widthPx} × ${report.heightPx}")
                        ReportValue("2B genişlik / yükseklik oranı",
                            String.format(Locale.US, "%.3f", report.widthHeightRatio))
                        ReportValue("Gözler arası mesafe / yüz genişliği",
                            report.eyeDistanceRatio?.let { String.format(Locale.US, "%.3f", it) }
                                ?: "İşaret noktaları yeterli değil")
                        ReportValue("Burun - alt dudak / yüz yüksekliği",
                            report.noseMouthRatio?.let { String.format(Locale.US, "%.3f", it) }
                                ?: "İşaret noktaları yeterli değil")
                        ReportValue("Başın sağa-sola dönüşü",
                            String.format(Locale.US, "%.1f°", report.yawDegrees))
                        ReportValue("Başın yatay eksende eğimi",
                            String.format(Locale.US, "%.1f°", report.rollDegrees))
                        if (report.people > 1) Text(
                            "Birden fazla yüz var: en büyük görünen yüz ölçüldü.",
                            color = softInk
                        )
                    }
                }
                Text(
                    "Bunlar tıbbi ölçüm veya gerçek dünya santimetreleri değildir; " +
                        "kameranın açısı, ışık ve yüz pozisyonundan etkilenir. " +
                        "Video için yalnızca temsilî bir kare incelenir.",
                    color = softInk, style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Gizlilik: Görüntü ve ölçüm sonucu sunucuya gönderilmez. Sonuç sadece bu ekranda hesaplanır.",
            color = ink, style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ReportValue(name: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(name, color = softInk, style = MaterialTheme.typography.labelMedium)
        Text(value, color = ink, style = MaterialTheme.typography.titleMedium)
    }
}
