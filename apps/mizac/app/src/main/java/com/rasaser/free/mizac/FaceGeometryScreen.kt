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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.google.mlkit.vision.face.FaceContour
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
    val rightEyeOpenProbability: Float?,
    val mouthWidthRatio: Float? = null,
    val noseWidthRatio: Float? = null,
    val eyeOutlineRatio: Float? = null,
    val eyebrowGapRatio: Float? = null
)

private sealed interface FaceStatus {
    data object Loading : FaceStatus
    data class Done(val measurement: FaceMeasurements): FaceStatus
    data class Failed(val reasonKey: String) : FaceStatus
}

internal fun readRotatedImage(file: File): Bitmap? {
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

internal fun readVideoFrame(file: File): Bitmap? {
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
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
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
                        // Contours quantify appearance on this 2D image;
                        // they do not provide physique-to-character thresholds.
                        fun contourWidth(kind: Int): Float? {
                            val pts=biggest.getContour(kind)?.points.orEmpty()
                            if(pts.size<2)return null
                            return (pts.maxOf{it.x}-pts.minOf{it.x})
                                .takeIf{it>0f}?.div(w)
                        }
                        val mouthL=biggest.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
                        val mouthR=biggest.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
                        val mouthWidth=if(mouthL!=null&&mouthR!=null)
                            kotlin.math.hypot(
                                (mouthL.x-mouthR.x).toDouble(),
                                (mouthL.y-mouthR.y).toDouble()
                            ).toFloat()/w else null
                        val leftEyeW=contourWidth(FaceContour.LEFT_EYE)
                        val rightEyeW=contourWidth(FaceContour.RIGHT_EYE)
                        val eyeW=if(leftEyeW!=null&&rightEyeW!=null)
                            (leftEyeW+rightEyeW)/2f else null
                        val leftBrow=biggest.getContour(
                            FaceContour.LEFT_EYEBROW_BOTTOM)?.points.orEmpty()
                        val rightBrow=biggest.getContour(
                            FaceContour.RIGHT_EYEBROW_BOTTOM)?.points.orEmpty()
                        val browGap=if(leftBrow.isNotEmpty()&&rightBrow.isNotEmpty())
                            leftBrow.minOf{a->rightBrow.minOf{b->
                                kotlin.math.abs(a.x-b.x)}}/w else null
                        FaceStatus.Done(FaceMeasurements(fromVideo, faces.size, w, h,
                            eyes, noseMouth, biggest.headEulerAngleY,biggest.headEulerAngleZ,
                            biggest.smilingProbability,
                            biggest.leftEyeOpenProbability,
                            biggest.rightEyeOpenProbability,
                            mouthWidth,contourWidth(FaceContour.NOSE_BOTTOM),
                            eyeW,browGap))
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
    onHistory: () -> Unit,
    onEthics: () -> Unit
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<FaceStatus>(FaceStatus.Loading) }
    var showBookCatalog by rememberSaveable { mutableStateOf(false) }
    var showTechnical by rememberSaveable { mutableStateOf(false) }
    var bodyReport by remember { mutableStateOf<BodyPoseReport?>(null) }
    var bodyReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        state = inspectFaceLocally(context)
        bodyReport = inspectBodyLocally(context)
        bodyReady = true
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            ui(strings, "quickResultHeading"), color=InkColor,
            style=MaterialTheme.typography.headlineSmall,
            fontWeight=FontWeight.Bold
        )
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
                // ML Kit eye/smile classifiers are valid chiefly near frontal yaw.
                val reliableClassification = kotlin.math.abs(v.yaw) <= 18f
                QuickMeasuredConclusions(v,lang,strings)
                TextButton(
                    modifier=Modifier.fillMaxWidth(),
                    onClick={showTechnical=!showTechnical}
                ) {
                    Text(ui(strings,if(showTechnical)"quickHideTech" else "quickShowTech"),
                        color=MutedColor)
                }
                if(showTechnical) Card(
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
                        listOf(
                            "faceMouthWidth" to v.mouthWidthRatio,
                            "faceNoseWidth" to v.noseWidthRatio,
                            "faceEyeOutline" to v.eyeOutlineRatio,
                            "faceBrowGap" to v.eyebrowGapRatio
                        ).forEach { (key,value) ->
                            if(value!=null)GeometryLabel(strings,key,
                                formatLocalNumber(value,lang))
                        }
                        GeometryLabel(strings,"geometryYaw",
                            formatLocalNumber(v.yaw,lang) + "°")
                        GeometryLabel(strings,"geometryRoll",
                            formatLocalNumber(v.roll,lang) + "°")
                        GeometryLabel(strings,"photoReportSmile",
                            v.smileProbability?.takeIf { reliableClassification && it in 0f..1f }
                                ?.let { formatLocalNumber(it*100f,lang)+"%" }
                                ?:ui(strings,"geometryUnavailable"))
                        GeometryLabel(strings,"photoReportLeftEye",
                            v.leftEyeOpenProbability?.takeIf { reliableClassification && it in 0f..1f }
                                ?.let {
                                formatLocalNumber(it*100f,lang)+"%"
                            } ?:ui(strings,"geometryUnavailable"))
                        GeometryLabel(strings,"photoReportRightEye",
                            v.rightEyeOpenProbability?.takeIf { reliableClassification && it in 0f..1f }
                                ?.let {
                                formatLocalNumber(it*100f,lang)+"%"
                            } ?:ui(strings,"geometryUnavailable"))
                        if(v.facesDetected>1)
                            Text(ui(strings,"geometryMultiple"),color=MutedColor)
                    }
                }
                if(v.fromVideo)
                    Text(ui(strings,"photoReportVideoNote"),color=MutedColor,
                        style=MaterialTheme.typography.bodySmall)
                if(v.facesDetected==1 &&
                    kotlin.math.abs(v.yaw)<=18f &&
                    (v.smileProbability?:0f)>=0.7f) {
                    Text(ui(strings,"photoReportHistory"),color=InkColor,
                        style=MaterialTheme.typography.titleLarge,
                        fontWeight=FontWeight.Bold)
                    QuickBookReading(v,strings)
                }
                QuickFrameImprovements(v,strings)
                QuickSelfReportStrengthsAndGrowth(strings,onEthics)
            }
        }
        if(bodyReady) {
            BodyPoseResults(
                bodyReport, strings, lang,
                multiFace=(state as? FaceStatus.Done)?.measurement?.facesDetected
                    ?.let{ it>1 } ?: false
            )
        } else {
            Text(ui(strings,"bodyLoading"),color=MutedColor,
                style=MaterialTheme.typography.bodySmall)
        }
        TextButton(
            modifier=Modifier.fillMaxWidth(),
            onClick={ showBookCatalog=!showBookCatalog }
        ) {
            Text(ui(strings,if(showBookCatalog)"quickHideBook" else "quickBrowseBook"),
                color=MutedColor)
        }
        if(showBookCatalog) MarifetnameMatching(lang, strings, selectable=true)
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
