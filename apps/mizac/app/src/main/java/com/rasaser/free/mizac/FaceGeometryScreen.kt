package com.rasaser.free.mizac

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private val InkColor = Color(0xFF1E1711)
private val WhiteCard = Color(0xFFFFFBF6)
private val MutedColor = Color(0xFF65584A)

data class FaceMeasurements(
    val fromVideo: Boolean,
    val facesDetected: Int,
    val width: Int,
    val height: Int,
    val eyeDistanceRatio: Float?,
    val noseMouthRatio: Float?,
    val yaw: Float,
    val roll: Float,
    val smileProbability: Float?,
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?
)

private sealed interface FaceStatus {
    data object Loading : FaceStatus
    data class Done(val measurement: FaceMeasurements): FaceStatus
    data class Failed(val reasonKey: String) : FaceStatus
}

private fun readRotatedImage(file: File): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val sample = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / 1440)
    val bitmap = BitmapFactory.decodeFile(file.path,
        BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val angle = try {
        ExifInterface(file.path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        )
    } catch (_: Exception) { ExifInterface.ORIENTATION_NORMAL }
    val matrix = Matrix()
    when (angle) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f,1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f,-1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.preScale(-1f,1f)
            matrix.postRotate(90f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.preScale(-1f,1f)
            matrix.postRotate(270f)
        }
        else -> return bitmap
    }
    val result = Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)
    if (result !== bitmap) bitmap.recycle()
    return result
}

private fun readVideoFrame(file: File): Bitmap? {
    val reader = MediaMetadataRetriever()
    return try {
        reader.setDataSource(file.absolutePath)
        val duration = reader.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        )?.toLongOrNull() ?: 0L
        val centerTimeUs = (duration.coerceIn(0L,30_000L) / 2L) * 1000L
        reader.getFrameAtTime(centerTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    } finally { reader.release() }
}

private suspend fun inspectFaceLocally(context: Context): FaceStatus {
    val photo = File(context.filesDir,"mizac_photo.dat")
    val video = File(context.filesDir,"mizac_video.dat")
    val prefs = context.getSharedPreferences("mizac_local_setup", Context.MODE_PRIVATE)
    val preferred = prefs.getString("last_media_kind", "photo")
    val file = when {
        preferred == "video" && video.exists() -> video
        preferred == "photo" && photo.exists() -> photo
        photo.exists() -> photo
        video.exists() -> video
        else -> return FaceStatus.Failed("needPhoto")
    }
    val fromVideo = file == video
    val bitmap = try {
        withContext(Dispatchers.IO) {
            if (fromVideo) readVideoFrame(file) else readRotatedImage(file)
        }
    } catch (_: Exception) { null }
        ?: return FaceStatus.Failed("geometryError")

    val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )
    return try {
        suspendCancellableCoroutine { continuation ->
            detector.process(InputImage.fromBitmap(bitmap,0))
                .addOnSuccessListener { faces ->
                    val biggest = faces.maxByOrNull {
                        it.boundingBox.width() * it.boundingBox.height()
                    }
                    val result = if (biggest == null) FaceStatus.Failed("geometryFailed")
                    else {
                        val rect=biggest.boundingBox
                        val w=rect.width().coerceAtLeast(1)
                        val h=rect.height().coerceAtLeast(1)
                        val l=biggest.getLandmark(FaceLandmark.LEFT_EYE)?.position
                        val r=biggest.getLandmark(FaceLandmark.RIGHT_EYE)?.position
                        val nose=biggest.getLandmark(FaceLandmark.NOSE_BASE)?.position
                        val mouth=biggest.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
                        val eyes=if(l!=null&&r!=null){
                            kotlin.math.hypot(
                                (r.x-l.x).toDouble(),(r.y-l.y).toDouble()
                            ).toFloat()/w
                        } else null
                        val noseMouth=if(nose!=null&&mouth!=null) {
                            kotlin.math.hypot(
                                (mouth.x-nose.x).toDouble(),
                                (mouth.y-nose.y).toDouble()
                            ).toFloat()/h
                        } else null
                        FaceStatus.Done(FaceMeasurements(fromVideo, faces.size, w, h,
                            eyes, noseMouth, biggest.headEulerAngleY,biggest.headEulerAngleZ,
                            biggest.smilingProbability,
                            biggest.leftEyeOpenProbability,
                            biggest.rightEyeOpenProbability))
                    }
                    if(continuation.isActive)continuation.resume(result)
                }
                .addOnFailureListener {
                    if(continuation.isActive)
                        continuation.resume(FaceStatus.Failed("geometryError"))
                }
                .addOnCompleteListener {
                    detector.close()
                    bitmap.recycle()
                }
        }
    } catch (_: Exception) {
        detector.close()
        if(!bitmap.isRecycled)bitmap.recycle()
        FaceStatus.Failed("geometryError")
    }
}

@Composable
fun FaceGeometryScreen(
    onBack: () -> Unit,
    lang: MizacLang,
    strings: Map<String, String>,
    onHistory: () -> Unit
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<FaceStatus>(FaceStatus.Loading) }
    LaunchedEffect(Unit) { state = inspectFaceLocally(context) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            ui(strings, "photoReportHistory"), color = InkColor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        MarifetnameMatching(lang, strings)

        Text(ui(strings,"photoReportScience"),color=InkColor,
            style=MaterialTheme.typography.headlineSmall,
            fontWeight=FontWeight.Bold)
        when(val result = state) {
            FaceStatus.Loading -> {
                CircularProgressIndicator()
                Text(ui(strings,"geometryLoading"),color=InkColor)
            }
            is FaceStatus.Failed -> {
                Card(colors=CardDefaults.cardColors(containerColor=WhiteCard)) {
                    Text(ui(strings,result.reasonKey),color=InkColor,
                        modifier=Modifier.padding(16.dp))
                }
            }
            is FaceStatus.Done -> {
                val v = result.measurement
                Card(
                    modifier=Modifier.fillMaxWidth(),
                    colors=CardDefaults.cardColors(containerColor=WhiteCard),
                    shape=RoundedCornerShape(22.dp)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement=Arrangement.spacedBy(9.dp)
                    ) {
                        GeometryLabel(strings,"geometrySource",
                            ui(strings,if(v.fromVideo)"geometryVideo" else "geometryPhoto"))
                        GeometryLabel(strings,"geometryFaces",v.facesDetected.toString())
                        GeometryLabel(strings,"geometryPixels","${v.width} × ${v.height}")
                        GeometryLabel(strings,"geometryRatio",
                            formatLocalNumber(v.width.toFloat()/v.height,lang))
                        GeometryLabel(strings,"geometryEyes",
                            v.eyeDistanceRatio?.let { formatLocalNumber(it,lang) }
                                ?: ui(strings,"geometryUnavailable"))
                        GeometryLabel(strings,"geometryNose",
                            v.noseMouthRatio?.let { formatLocalNumber(it,lang) }
                                ?: ui(strings,"geometryUnavailable"))
                        GeometryLabel(strings,"geometryYaw",
                            formatLocalNumber(v.yaw,lang) + "°")
                        GeometryLabel(strings,"geometryRoll",
                            formatLocalNumber(v.roll,lang) + "°")
                        GeometryLabel(strings,"photoReportSmile",
                            v.smileProbability?.let { formatLocalNumber(it*100f,lang)+"%" }
                                ?:ui(strings,"geometryUnavailable"))
                        GeometryLabel(strings,"photoReportLeftEye",
                            v.leftEyeOpenProbability?.let {
                                formatLocalNumber(it*100f,lang)+"%"
                            } ?:ui(strings,"geometryUnavailable"))
                        GeometryLabel(strings,"photoReportRightEye",
                            v.rightEyeOpenProbability?.let {
                                formatLocalNumber(it*100f,lang)+"%"
                            } ?:ui(strings,"geometryUnavailable"))
                        if(v.facesDetected>1)
                            Text(ui(strings,"geometryMultiple"),color=MutedColor)
                    }
                }
                if(v.fromVideo)
                    Text(ui(strings,"photoReportVideoNote"),color=MutedColor,
                        style=MaterialTheme.typography.bodySmall)
                PhotoObservationSummary(strings,v)
                PersonalizedPhotoSuggestions(strings,v)
            }
        }

    }
}

@Composable
private fun GeometryLabel(strings: Map<String,String>, key: String, value: String) {
    Row(
        modifier=Modifier.fillMaxWidth().padding(vertical=3.dp),
        horizontalArrangement=Arrangement.spacedBy(12.dp)
    ) {
        Text(ui(strings,key),modifier=Modifier.weight(1.7f),
            color=MutedColor,style=MaterialTheme.typography.bodyMedium)
        Text(value,modifier=Modifier.weight(1f),
            color=InkColor,fontWeight=FontWeight.SemiBold,
            style=MaterialTheme.typography.bodyMedium)
    }
}
