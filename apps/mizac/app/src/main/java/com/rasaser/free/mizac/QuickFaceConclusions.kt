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
import androidx.compose.runtime.remember
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
    val reliable=m.facesDetected==1 && abs(m.yaw)<=18f
    val left=if(reliable)safeProbability(m.leftEyeOpenProbability,m.yaw) else null
    val right=if(reliable)safeProbability(m.rightEyeOpenProbability,m.yaw) else null
    val smile=if(reliable)safeProbability(m.smileProbability,m.yaw) else null
    val results=mutableListOf<Pair<String,String>>()
    results+=ui(strings,"quickFaceDetectedLabel") to
        ui(strings,if(m.facesDetected==1)"quickOneFace" else "quickMoreFaces")
    results+=ui(strings,"quickFaceDirectionLabel") to ui(strings,when {
        abs(m.yaw)<=10f->"quickFaceFront"
        abs(m.yaw)<=22f->"quickFaceSlightAngle"
        else->"quickFaceSideAngle"
    })
    results+=ui(strings,"quickHeadPositionLabel") to
        ui(strings,if(abs(m.roll)<=10f)"quickHeadAligned" else "quickHeadTilted")
    if(left!=null&&right!=null) {
        results+=ui(strings,"quickEyeObservationLabel") to
            ui(strings,when {
                left>=0.85f&&right>=0.85f -> "quickEyesOpen"
                left<=0.15f||right<=0.15f -> "quickEyePartClosed"
                else -> "quickEyesUnclear"
            })
    }
    if(smile!=null){
        results+=ui(strings,"quickSmileObservationLabel") to ui(strings,when{
            smile>=0.7f->"quickSmilePresent"
            smile<=0.25f->"quickSmileNotVisible"
            else->"quickSmileUncertain"
        })
    }
    results+=ui(strings,"geometryRatio") to
        formatLocalNumber(m.width.toFloat()/m.height,lang)
    if(m.eyeDistanceRatio!=null)
        results+=ui(strings,"geometryEyes") to formatLocalNumber(m.eyeDistanceRatio,lang)
    if(m.noseMouthRatio!=null)
        results+=ui(strings,"geometryNose") to formatLocalNumber(m.noseMouthRatio,lang)

    Card(
        modifier=Modifier.fillMaxWidth(),
        colors=CardDefaults.cardColors(containerColor=quickWhite),
        shape=RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.padding(16.dp),
            verticalArrangement=Arrangement.spacedBy(9.dp)) {
            Text(ui(strings,"quickMeasuredFacts"),color=quickDark,
                style=MaterialTheme.typography.titleLarge,
                fontWeight=FontWeight.Bold)
            results.forEach{(label,value)->
                Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    Text(label,color=quickGray,
                        style=MaterialTheme.typography.labelMedium)
                    Text(value,color=quickDark,
                        style=MaterialTheme.typography.bodyMedium,
                        fontWeight=FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Source-first report. The work can be quoted accurately without assigning
 * morality, intelligence or trustworthiness to a photographed person.
 * Book descriptors have no author-defined numeric cutoffs.
 */
@Composable
fun QuickBookReading(
    measurement: FaceMeasurements,
    strings: Map<String,String>
) {
    val context=LocalContext.current
    val hasOnePerson=measurement.facesDetected==1
    val prob=safeProbability(measurement.smileProbability,measurement.yaw)
    val smilingInFrame=hasOnePerson && prob!=null && prob>=0.7f
    // Never display an empty pseudo-result or tell the user to choose a
    // source item as though it were the automatic analysis outcome.
    if(!smilingInFrame) return
    val book=remember(context){ readSourceFaceEntries(context) }

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

            if(!hasOnePerson) {
                Text(ui(strings,"quickNeedsOneFace"),color=quickGray,
                    style=MaterialTheme.typography.bodyMedium)
            } else if(smilingInFrame) {
                Text(ui(strings,"quickSmilePresent"),color=quickDark,
                    style=MaterialTheme.typography.bodyMedium)
            } else if(prob!=null && prob<=0.25f) {
                Text(ui(strings,"quickSmileNotVisible"),color=quickDark,
                    style=MaterialTheme.typography.bodyMedium)
            } else {
                Text(ui(strings,"quickSmileNotClassified"),color=quickGray,
                    style=MaterialTheme.typography.bodyMedium)
            }

            // No fixed three-item result for every photograph. Display only
            // the source passage corresponding to an actually observed smile.
            // Other body descriptions require the user's explicit selection.
            if(smilingInFrame) {
                book.firstOrNull { it.first=="Tebessümlü yüz" }?.let { (trait,meaning) ->
                    Text(trait,color=quickDark,fontWeight=FontWeight.SemiBold)
                    Text(meaning,color=quickGray,
                        style=MaterialTheme.typography.bodyMedium)
                    Text(ui(strings,"quickBookCitation"),color=quickGray,
                        style=MaterialTheme.typography.labelMedium)
                }
            } else {
                Text(ui(strings,"quickBookChooseFeature"),color=quickGray,
                    style=MaterialTheme.typography.bodyMedium)
            }
            Text(ui(strings,"quickBookOneLineLimit"),color=quickGray,
                style=MaterialTheme.typography.labelSmall)
        }
    }
}

private fun readSourceFaceEntries(context: Context): List<Pair<String,String>> =
    runCatching {
        val terms=setOf("Tebessümlü yüz","Yuvarlak, güleç yüz",
            "Güleç yüz ve tatlı söz")
        context.assets.open("history/catalog_4.tsv")
            .bufferedReader().useLines { lines ->
                lines.filter{it.isNotBlank() && !it.startsWith("#")}
                    .map{it.split('\t')}
                    .filter{it.size==4 && it[2] in terms}
                    .map{it[2] to it[3]}.toList()
            }
    }.getOrDefault(emptyList())
