package com.rasaser.free.mizac

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Only facts available from ML Kit are attributed to this uploaded image.
 * Mârifetnâme beliefs are never presented as a verified moral/personality
 * classification or a claim about the intrinsic qualities of the subject.
 *
 * ML Kit classifications require a near-frontal face for reliability.
 * Cf. developers.google.com/ml-kit/vision/face-detection/face-detection-concepts
 */
private val quickDark=Color(0xFF211A14)
private val quickGray=Color(0xFF5F554A)
private val quickWhite=Color(0xFFFFFBF6)

/** Prevent interpreting out-of-range or unavailable classifiers as observed traits. */
private fun safeProbability(p: Float?, yaw: Float): Float? =
    if(p!=null && p.isFinite() && p in 0f..1f && abs(yaw)<=18f) p else null

@Composable
fun QuickMeasuredConclusions(
    measurement: FaceMeasurements,
    lang: MizacLang,
    strings: Map<String,String>
) {
    val m=measurement
    val facts=mutableListOf<String>()
    facts+=ui(strings,if(m.fromVideo)"quickFromMiddleFrame" else "quickFromPhoto")
    facts+=ui(strings,if(m.facesDetected==1)"quickOneFace" else "quickMoreFaces")
    facts+=ui(strings,when {
        abs(m.yaw)<=10f -> "quickFaceFront"
        abs(m.yaw)<=22f -> "quickFaceSlightAngle"
        else -> "quickFaceSideAngle"
    })
    facts+=ui(strings,if(abs(m.roll)<=10f)"quickHeadAligned" else "quickHeadTilted")
    val eyeL=safeProbability(m.leftEyeOpenProbability,m.yaw)
    val eyeR=safeProbability(m.rightEyeOpenProbability,m.yaw)
    if(eyeL!=null && eyeR!=null) {
        val key=when {
            eyeL>=0.85f && eyeR>=0.85f -> "quickEyesOpen"
            eyeL<=0.15f || eyeR<=0.15f -> "quickEyePartClosed"
            else -> "quickEyesUnclear"
        }
        facts+=ui(strings,key)
    }
    val smile=safeProbability(m.smileProbability,m.yaw)
    if(smile!=null) {
        facts+=ui(strings,when {
            smile>=0.7f -> "quickSmilePresent"
            smile<=0.25f -> "quickSmileNotVisible"
            else -> "quickSmileUncertain"
        })
    }
    if(m.eyeDistanceRatio!=null && m.noseMouthRatio!=null)
        facts+=ui(strings,"quickLandmarksFound")
    else facts+=ui(strings,"quickLandmarksMissing")

    Card(
        modifier=Modifier.fillMaxWidth(),
        colors=CardDefaults.cardColors(containerColor=quickWhite),
        shape=RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier=Modifier.padding(16.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp)
        ) {
            Text(ui(strings,"quickMeasuredFacts"),
                color=quickDark,style=MaterialTheme.typography.titleLarge,
                fontWeight=FontWeight.Bold)
            facts.forEach { line ->
                Text("•  "+line,color=quickGray,
                    style=MaterialTheme.typography.bodyMedium)
            }
            Text(
                ui(strings,"geometryRatio")+": "+
                    formatLocalNumber(m.width.toFloat()/m.height.toFloat(),lang),
                color=quickDark,fontWeight=FontWeight.SemiBold,
                style=MaterialTheme.typography.bodyMedium
            )
            Text(
                ui(strings,"geometryYaw")+": "+
                    formatLocalNumber(m.yaw,lang)+"°   •   "+
                    ui(strings,"geometryRoll")+": "+
                    formatLocalNumber(m.roll,lang)+"°",
                color=quickDark,style=MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** One automatic book-title match is possible from a still smiling expression.
 *  Other permanent facial morphology labels have no author-supplied numeric
 *  thresholds and cannot be automatically inferred by ML Kit.
 */
@Composable
fun QuickBookReading(
    measurement: FaceMeasurements,
    strings: Map<String,String>
) {
    val context=LocalContext.current
    val smile=safeProbability(measurement.smileProbability,measurement.yaw)
    val isSmileVisible=smile!=null && smile>=0.7f
    val entry=if(isSmileVisible) {
        readSmileEntry(context)
    } else null

    Card(
        modifier=Modifier.fillMaxWidth(),
        colors=CardDefaults.cardColors(containerColor=quickWhite),
        shape=RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier=Modifier.padding(16.dp),
            verticalArrangement=Arrangement.spacedBy(9.dp)
        ) {
            Text(ui(strings,"quickBookReport"),
                color=quickDark,style=MaterialTheme.typography.titleLarge,
                fontWeight=FontWeight.Bold)
            if(entry!=null) {
                Text(ui(strings,"quickBookSmilingObserved"),color=quickGray)
                Text(entry.first+" — "+entry.second,
                    color=quickDark,fontWeight=FontWeight.SemiBold)
                Text(ui(strings,"quickBookCitation"),color=quickGray,
                    style=MaterialTheme.typography.labelMedium)
            } else {
                Text(ui(strings,"quickBookUnmatched"),color=quickGray,
                    style=MaterialTheme.typography.bodyMedium)
            }
            Text(ui(strings,"quickBookScope"),color=quickGray,
                style=MaterialTheme.typography.labelSmall)
        }
    }
}

private fun readSmileEntry(context: Context): Pair<String,String>? = runCatching {
    context.assets.open("history/catalog_4.tsv").bufferedReader().useLines { lines ->
        lines.filter{it.isNotBlank() && !it.startsWith("#")}
            .map{it.split('\t')}
            .firstOrNull { it.size==4 && it[2]=="Tebessümlü yüz" }
            ?.let { it[2] to it[3] }
    }
}.getOrNull()
