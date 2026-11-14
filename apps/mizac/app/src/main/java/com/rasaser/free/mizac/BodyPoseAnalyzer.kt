package com.rasaser.free.mizac

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.hypot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Genuine 2D body-image measurements only. Does not infer moral traits,
 * intelligence, health, disease, personality, or physical height in cm.
 * All metrics are conditional on visible, high-confidence landmarks.
 */
data class BodyMetric(val labelKey: String, val value: Float, val degrees: Boolean = false)
data class BodyPoseReport(
    val fromVideo: Boolean,
    val visibleLandmarks: Int,
    val fullyFramed: Boolean,
    val metrics: List<BodyMetric>,
    val recognized: Boolean
)

private fun confidentLandmark(
    pose: Pose, key: Int, width: Int, height: Int
): PointF? {
    val landmark = pose.getPoseLandmark(key) ?: return null
    val p = landmark.position
    if (landmark.inFrameLikelihood < 0.70f ||
        p.x < 0 || p.y < 0 || p.x >= width || p.y >= height) return null
    return p
}

private fun distance(a: PointF, b: PointF): Float =
    hypot((a.x-b.x).toDouble(), (a.y-b.y).toDouble()).toFloat()

private fun jointAngle(a: PointF, b: PointF, c: PointF): Float? {
    val vx = (a.x-b.x).toDouble()
    val vy = (a.y-b.y).toDouble()
    val wx = (c.x-b.x).toDouble()
    val wy = (c.y-b.y).toDouble()
    val product = hypot(vx,vy)*hypot(wx,wy)
    if(product < 0.0001) return null
    val cosine = ((vx*wx+vy*wy)/product).coerceIn(-1.0,1.0)
    return Math.toDegrees(acos(cosine)).toFloat()
}

private fun imageTilt(a: PointF,b: PointF): Float =
    Math.toDegrees(atan2(abs(a.y-b.y).toDouble(),
        abs(a.x-b.x).coerceAtLeast(0.1f).toDouble())).toFloat()

internal fun reportPose(
    pose: Pose, width: Int, height: Int, fromVideo: Boolean
): BodyPoseReport {
    val count=pose.allPoseLandmarks.count {
        it.inFrameLikelihood>=0.7f &&
            it.position.x in 0f..width.toFloat() &&
            it.position.y in 0f..height.toFloat()
    }
    fun point(key: Int) = confidentLandmark(pose,key,width,height)
    val ls=point(PoseLandmark.LEFT_SHOULDER)
    val rs=point(PoseLandmark.RIGHT_SHOULDER)
    val lh=point(PoseLandmark.LEFT_HIP)
    val rh=point(PoseLandmark.RIGHT_HIP)
    val le=point(PoseLandmark.LEFT_ELBOW)
    val re=point(PoseLandmark.RIGHT_ELBOW)
    val lw=point(PoseLandmark.LEFT_WRIST)
    val rw=point(PoseLandmark.RIGHT_WRIST)
    val lk=point(PoseLandmark.LEFT_KNEE)
    val rk=point(PoseLandmark.RIGHT_KNEE)
    val la=point(PoseLandmark.LEFT_ANKLE)
    val ra=point(PoseLandmark.RIGHT_ANKLE)

    val metrics=mutableListOf<BodyMetric>()
    fun add(key: String, value: Float?, degrees: Boolean=false) {
        if(value!=null && value.isFinite() && value>=0f)
            metrics+=BodyMetric(key,value,degrees)
    }
    val shoulders=if(ls!=null && rs!=null) distance(ls,rs) else null
    val hips=if(lh!=null && rh!=null) distance(lh,rh) else null
    val torso=if(ls!=null && rs!=null && lh!=null && rh!=null) {
        val shoulderMid=PointF((ls.x+rs.x)/2,(ls.y+rs.y)/2)
        val hipMid=PointF((lh.x+rh.x)/2,(lh.y+rh.y)/2)
        distance(shoulderMid,hipMid).takeIf{it>1f}
    } else null
    if(shoulders!=null && torso!=null)
        add("bodyShoulderTorso",shoulders/torso)
    if(hips!=null && torso!=null)
        add("bodyHipTorso",hips/torso)
    if(shoulders!=null && hips!=null && hips>1f)
        add("bodyShoulderHip",shoulders/hips)
    if(ls!=null && rs!=null)add("bodyShoulderTilt",imageTilt(ls,rs),true)
    if(lh!=null && rh!=null)add("bodyHipTilt",imageTilt(lh,rh),true)
    if(ls!=null && le!=null && lw!=null)
        add("bodyLeftElbow",jointAngle(ls,le,lw),true)
    if(rs!=null && re!=null && rw!=null)
        add("bodyRightElbow",jointAngle(rs,re,rw),true)
    if(lh!=null && lk!=null && la!=null)
        add("bodyLeftKnee",jointAngle(lh,lk,la),true)
    if(rh!=null && rk!=null && ra!=null)
        add("bodyRightKnee",jointAngle(rh,rk,ra),true)
    if(torso!=null){
        if(ls!=null && lw!=null) add("bodyLeftArmTorso",distance(ls,lw)/torso)
        if(rs!=null && rw!=null) add("bodyRightArmTorso",distance(rs,rw)/torso)
        if(lh!=null && la!=null) add("bodyLeftLegTorso",distance(lh,la)/torso)
        if(rh!=null && ra!=null) add("bodyRightLegTorso",distance(rh,ra)/torso)
    }
    val full=ls!=null && rs!=null && lh!=null && rh!=null &&
        lk!=null && rk!=null && la!=null && ra!=null
    return BodyPoseReport(fromVideo,count,full,metrics,count>=4)
}

suspend fun inspectBodyLocally(context:Context): BodyPoseReport? {
    val photo=File(context.filesDir,"mizac_photo.dat")
    val video=File(context.filesDir,"mizac_video.dat")
    val selected=context.getSharedPreferences("mizac_local_setup",Context.MODE_PRIVATE)
        .getString("last_media_kind","photo")
    val file=when{
        selected=="video" && video.exists()->video
        selected=="photo" && photo.exists()->photo
        photo.exists()->photo
        video.exists()->video
        else->return null
    }
    val fromVideo=file==video
    val bitmap=try {
        withContext(Dispatchers.IO) {
            if(fromVideo) readVideoFrame(file) else readRotatedImage(file)
        }
    }catch(_:Exception){null} ?:return null

    val options=PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE).build()
    val detector=PoseDetection.getClient(options)
    return try {
        suspendCancellableCoroutine { continuation ->
            detector.process(InputImage.fromBitmap(bitmap,0))
                .addOnSuccessListener{pose->
                    if(continuation.isActive)
                        continuation.resume(
                            reportPose(pose,bitmap.width,bitmap.height,fromVideo)
                        )
                }
                .addOnFailureListener{
                    if(continuation.isActive)continuation.resume(null)
                }
                .addOnCompleteListener {
                    detector.close()
                    bitmap.recycle()
                }
        }
    }catch(_:Exception){
        detector.close()
        if(!bitmap.isRecycled)bitmap.recycle()
        null
    }
}

@Composable
fun BodyPoseResults(
    report: BodyPoseReport?,
    strings: Map<String,String>,
    lang: MizacLang,
    multiFace: Boolean
) {
    Card(
        modifier=Modifier.fillMaxWidth(),
        colors=CardDefaults.cardColors(containerColor=Color(0xFFFFFBF6)),
        shape=RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.padding(15.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(ui(strings,"bodyReportTitle"),
                style=MaterialTheme.typography.titleLarge,
                color=Color(0xFF211911),fontWeight=FontWeight.Bold)
            when {
                report==null || !report.recognized -> {
                    Text(ui(strings,"bodyNoPose"),
                        color=Color(0xFF645749),
                        style=MaterialTheme.typography.bodyMedium)
                }
                else -> {
                    if(multiFace) Text(ui(strings,"bodyMultiplePeople"),
                        color=Color(0xFF645749),
                        style=MaterialTheme.typography.bodyMedium)
                    Text(ui(strings,"bodyLandmarks")+": "+report.visibleLandmarks+"/33",
                        color=Color(0xFF645749),
                        style=MaterialTheme.typography.bodyMedium)
                    Text(ui(strings,if(report.fullyFramed)"bodyFull"
                        else "bodyPartlyVisible"),color=Color(0xFF645749))
                    report.metrics.forEach{metric ->
                        Row(
                            modifier=Modifier.fillMaxWidth()
                                .padding(vertical=3.dp),
                            horizontalArrangement=Arrangement.spacedBy(10.dp)
                        ){
                            Text(ui(strings,metric.labelKey),
                                modifier=Modifier.weight(1.7f),
                                color=Color(0xFF645749),
                                style=MaterialTheme.typography.bodyMedium)
                            Text(
                                formatLocalNumber(metric.value,lang) +
                                    if(metric.degrees)"°" else "",
                                modifier=Modifier.weight(1f),
                                color=Color(0xFF211911),
                                fontWeight=FontWeight.SemiBold,
                                style=MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text(ui(strings,"bodyMeasurementLimit"),
                        color=Color(0xFF645749),
                        style=MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
