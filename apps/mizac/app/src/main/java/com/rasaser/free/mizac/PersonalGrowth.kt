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

@Composable
fun GeneralGrowthSuggestions(strings: Map<String, String>) {
    GrowthCard(
        ui(strings,"personalGuidance"),
        listOf(
            "adviceKindness","adviceHonesty","adviceService",
            "advicePatience","adviceLearning"
        ).map { ui(strings,it) }
    )
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
    // Relative ranking within the respondent's own self-report,
    // without population norms or judgments about inherent worth.
    val sorted=MIZAC_FACTORS.sortedWith(
        compareByDescending<String> { scores[it] ?: 0f }.thenBy{it}
    )
    val high=sorted.take(2)
    val low=sorted.takeLast(2).reversed()

    Text(ui(strings,"personalAnswerProfile"),color=GrowthInk,
        style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
    Text(ui(strings,"personalFromAnswers"),color=GrowthMuted,
        style=MaterialTheme.typography.bodySmall)
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
        (low.map { ui(strings,adviceKey(it)) } +
            listOf("adviceKindness","adviceHonesty","adviceService")
                .map { ui(strings,it) }).distinct()
    )
}
