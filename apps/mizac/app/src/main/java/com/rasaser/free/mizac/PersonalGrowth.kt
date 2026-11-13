package com.rasaser.free.mizac

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val GrowthInk = Color(0xFF211911)
private val GrowthMuted = Color(0xFF645749)
private val GrowthWhite = Color(0xFFFFFBF6)

private fun factorLabel(factor: String): String = when (factor) {
    "O" -> "factorO"
    "C" -> "factorC"
    "E" -> "factorE"
    "A" -> "factorA"
    "S" -> "factorS"
    else -> "factorO"
}

private fun adviceKey(factor: String): String = when (factor) {
    "O" -> "adviceO"
    "C" -> "adviceC"
    "E" -> "adviceE"
    "A" -> "adviceA"
    "S" -> "adviceS"
    else -> "adviceLearning"
}

@Composable
private fun GrowthCard(title: String, items: List<String>) {
    Card(
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(20.dp),
        colors=CardDefaults.cardColors(containerColor=GrowthWhite)
    ) {
        Column(
            modifier=Modifier.padding(14.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ) {
            Text(title,color=GrowthInk, fontWeight=FontWeight.Bold,
                style=MaterialTheme.typography.titleMedium)
            items.forEach { item ->
                Text("•  "+item,color=GrowthMuted,
                    style=MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun photoAdviceKeys(v: FaceMeasurements): List<String> {
    val chosen=mutableListOf<String>()
    // All advice is conditional on OBSERVABLE frame attributes only;
    // no morality, intelligence or personality inferred from physiognomy.
    if(v.facesDetected>1) {
        chosen += "visualAdviceGroup"
    }
    when {
        abs(v.yaw)>22f -> chosen+="visualAdviceFaceAngle"
        abs(v.roll)>16f -> chosen+="visualAdviceLevel"
        abs(v.yaw)<=10f && abs(v.roll)<=10f -> chosen+="visualAdviceGoodAngle"
        else -> chosen+="visualAdviceSlightTurn"
    }
    val eyeProb=listOfNotNull(v.leftEyeOpenProbability,
        v.rightEyeOpenProbability)
    when {
        eyeProb.isEmpty() -> chosen+="visualAdviceLight"
        eyeProb.minOrNull()!!<0.55f -> chosen+="visualAdviceEyesVisible"
        eyeProb.minOrNull()!!>=0.85f -> chosen+="visualAdviceEyeContact"
        else -> chosen+="visualAdviceEyePosition"
    }
    when(val smile=v.smileProbability) {
        null -> chosen+="visualAdviceExpressionClear"
        else -> when {
            smile>=0.75f -> chosen+="visualAdviceSmileVisible"
            smile>=0.35f -> chosen+="visualAdviceSmileModerate"
            else -> chosen+="visualAdviceSmileOptional"
        }
    }
    when {
        v.eyeDistanceRatio==null || v.noseMouthRatio==null ->
            chosen+="visualAdviceCameraQuality"
        v.width<165 || v.height<165 -> chosen+="visualAdviceCloserFrame"
        v.fromVideo -> chosen+="visualAdviceVideoSingleFrame"
        else -> chosen+="visualAdviceNaturalFrame"
    }
    return chosen.distinct().take(5)
}

@Composable
fun PersonalizedPhotoSuggestions(
    strings: Map<String,String>,
    measurement: FaceMeasurements
) {
    GrowthCard(ui(strings,"visualAdviceTitle"),
        photoAdviceKeys(measurement).map{ui(strings,it)})
    Text(ui(strings,"visualAdviceScope"),color=GrowthMuted,
        style=MaterialTheme.typography.bodySmall)
}

@Composable
fun PhotoObservationSummary(
    strings: Map<String, String>,
    measurement: FaceMeasurements
) {
    val positives=mutableListOf<String>()
    val limits=mutableListOf<String>()
    if (measurement.facesDetected==1)
        positives.add(ui(strings,"photoFaceClear"))
    else limits.add(ui(strings,"photoFaceMultiple"))
    if (abs(measurement.yaw)<=15f && abs(measurement.roll)<=15f)
        positives.add(ui(strings,"photoNearFront"))
    else limits.add(ui(strings,"photoTurned"))
    if(measurement.eyeDistanceRatio!=null && measurement.noseMouthRatio!=null)
        positives.add(ui(strings,"photoLandmarksMeasured"))
    else limits.add(ui(strings,"photoLandmarksMissing"))

    GrowthCard(ui(strings,"photoQualityStrengths"),positives)
    if(limits.isNotEmpty())
        GrowthCard(ui(strings,"photoQualityLimits"),limits)
    Text(
        ui(strings,"personalFromPhoto"),
        color=GrowthMuted,style=MaterialTheme.typography.bodySmall
    )
}

@Composable
fun QuestionnaireGrowthSummary(
    language: MizacLang,
    strings: Map<String,String>,
    scores: Map<String,Float>
) {
    // Compare the person's answers with their own answers, not
    // population norms or purported moral/clinical classifications.
    val sorted=MIZAC_FACTORS.sortedWith(
        compareByDescending<String> { scores[it] ?: 0f }.thenBy{it}
    )
    val top=sorted.first()
    val bottom=sorted.last()
    val spread=(scores[top]?:0f)-(scores[bottom]?:0f)
    val distinctEnough=spread>=0.35f
    val high=if(distinctEnough)
        sorted.filter { (scores[top]?:0f)-(scores[it]?:0f)<=0.25f }.take(2)
        else emptyList()
    val low=if(distinctEnough)
        sorted.reversed().filter { (scores[it]?:0f)-(scores[bottom]?:0f)<=0.25f }
            .take(2) else emptyList()

    Text(ui(strings,"personalAnswerProfile"),color=GrowthInk,
        style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
    Text(ui(strings,"personalFromAnswers"),color=GrowthMuted,
        style=MaterialTheme.typography.bodySmall)
    if(!distinctEnough) {
        GrowthCard(ui(strings,"personalBalanceTitle"),
            listOf(ui(strings,"personalBalanceMessage")))
        GrowthCard(ui(strings,"personalGuidance"),
            listOf(ui(strings,"personalBalanceAdvice")))
    } else {
        GrowthCard(
            ui(strings,"personalStrengths"),
            high.map { factor ->
                ui(strings,factorLabel(factor))+" • "+
                    formatLocalNumber(scores.getValue(factor),language)+" / 5"
            }
        )
        GrowthCard(
            ui(strings,"personalGrowthAreas"),
            low.map { factor ->
                ui(strings,factorLabel(factor))+" • "+
                    formatLocalNumber(scores.getValue(factor),language)+" / 5"
            }
        )
        GrowthCard(
            ui(strings,"personalGuidance"),
            (low.flatMap { factor ->
                listOf(ui(strings,adviceKey(factor)),
                    ui(strings,"adviceExtra"+factor))
            }+listOf(ui(strings,"adviceStrength"+top))).distinct()
        )
    }
}
