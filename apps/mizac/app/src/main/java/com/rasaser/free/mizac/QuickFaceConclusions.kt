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

private val quickDark = Color(0xFF211A14)
private val quickGray = Color(0xFF5F554A)
private val quickWhite = Color(0xFFFFFBF6)

private fun safeProbability(value: Float?, yaw: Float): Float? =
    value?.takeIf { it.isFinite() && it in 0f..1f && abs(yaw) <= 18f }

/**
 * A smile is the one book descriptor that the existing classifier can
 * directly observe. Never derive a moral trait from the book's statement.
 */
internal fun hasObservedSmile(m: FaceMeasurements): Boolean =
    m.facesDetected == 1 &&
        (safeProbability(m.smileProbability, m.yaw) ?: 0f) >= 0.70f

@Composable
fun QuickMeasuredConclusions(
    measurement: FaceMeasurements,
    lang: MizacLang,
    strings: Map<String, String>
) {
    val m = measurement
    val reliable = m.facesDetected == 1 && abs(m.yaw) <= 18f
    val left = if(reliable) safeProbability(m.leftEyeOpenProbability, m.yaw) else null
    val right = if(reliable) safeProbability(m.rightEyeOpenProbability, m.yaw) else null
    val smile = if(reliable) safeProbability(m.smileProbability, m.yaw) else null

    val observations = mutableListOf<Pair<String, String>>()
    observations += ui(strings, "quickFaceDetectedLabel") to
        ui(strings, if(m.facesDetected == 1) "quickOneFace" else "quickMoreFaces")
    observations += ui(strings, "quickFaceDirectionLabel") to ui(strings, when {
        abs(m.yaw) <= 10f -> "quickFaceFront"
        abs(m.yaw) <= 22f -> "quickFaceSlightAngle"
        else -> "quickFaceSideAngle"
    })
    observations += ui(strings, "quickHeadPositionLabel") to
        ui(strings, if(abs(m.roll) <= 10f) "quickHeadAligned" else "quickHeadTilted")
    if(left != null && right != null) {
        observations += ui(strings, "quickEyeObservationLabel") to ui(strings, when {
            left >= 0.85f && right >= 0.85f -> "quickEyesOpen"
            left <= 0.15f || right <= 0.15f -> "quickEyePartClosed"
            else -> "quickEyesUnclear"
        })
    }
    if(smile != null) {
        observations += ui(strings, "quickSmileObservationLabel") to ui(strings, when {
            smile >= 0.70f -> "quickSmilePresent"
            smile <= 0.25f -> "quickSmileNotVisible"
            else -> "quickSmileUncertain"
        })
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = quickWhite),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(ui(strings, "quickMeasuredFacts"), color = quickDark,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Text(
                ui(strings, if(m.fromVideo) "quickFromMiddleFrame" else "quickFromPhoto"),
                color = quickGray, style = MaterialTheme.typography.bodySmall
            )
            observations.forEach { (label, value) ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, color = quickGray, style = MaterialTheme.typography.labelMedium)
                    Text(value, color = quickDark,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold)
                }
            }
            Text(ui(strings, "photoReportAppearanceNote"), color = quickGray,
                style = MaterialTheme.typography.labelSmall)
        }
    }
}

private data class HistoricalSnippet(
    val title: String,
    val historicalClaim: String,
    val article: String
)

/**
 * Fetch source entries by their exact title. We must not fabricate a threshold
 * for "wide face", "short arm", etc. The book provides no pixel-ratio cutoff.
 */
private fun readHistoricalSnippets(context: Context): Map<String, HistoricalSnippet> {
    val names = setOf(
        "Tebessümlü yüz",
        "Kaşların açık ve ayrı durması",
        "Orta biçimli göz",
        "Orta büyüklükte çene",
        "Ölçülü omuz",
        "Geniş göğüs",
        "Uzun parmaklar"
    )
    return listOf("catalog_4.tsv" to "4", "catalog_5.tsv" to "5")
        .flatMap { (file, article) ->
            context.assets.open("history/$file").bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() && !it.startsWith("#") }
                    .map { it.split('\t') }
                    .filter { it.size == 4 && it[2] in names }
                    .map { HistoricalSnippet(it[2], it[3], article) }
                    .toList()
            }
        }.associateBy { it.title }
}

@Composable
private fun HistoricalRow(item: HistoricalSnippet, strings: Map<String, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(item.title, color = quickDark, fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium)
        Text(item.historicalClaim, color = quickGray,
            style = MaterialTheme.typography.bodyMedium)
        Text(ui(strings, if(item.article == "4") "quickHistoryArticle4" else "quickHistoryArticle5"),
            color = quickGray, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * First section of the quick analysis, always shown before the measurements.
 * Only visible smile can be connected to a source description automatically.
 * Unmatched catalogue examples are explicitly not attributed to the subject.
 */
@Composable
fun QuickBookReading(
    measurement: FaceMeasurements,
    bodyReport: BodyPoseReport?,
    strings: Map<String, String>
) {
    val context = LocalContext.current
    val source = remember(context) { readHistoricalSnippets(context) }
    val observedSmile = hasObservedSmile(measurement)
    val singleFace = measurement.facesDetected == 1
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = quickWhite),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(ui(strings, "photoReportHistory"), color = quickDark,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Text(ui(strings, "photoReportHistoryIntro"), color = quickGray,
                style = MaterialTheme.typography.bodySmall)

            when {
                !singleFace -> Text(ui(strings, "quickNeedsOneFace"), color = quickGray)
                observedSmile -> {
                    source["Tebessümlü yüz"]?.let { record ->
                        Text(ui(strings, "quickBookSmilingObserved"), color = quickDark,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold)
                        HistoricalRow(record, strings)
                    }
                }
                else -> Text(ui(strings, "quickBookUnmatched"), color = quickGray,
                    style = MaterialTheme.typography.bodyMedium)
            }

            // Relevant material from the book, not classifications of this face.
            Text(ui(strings, "quickHistoryFaceExamples"), color = quickDark,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold)
            listOf("Kaşların açık ve ayrı durması", "Orta biçimli göz",
                "Orta büyüklükte çene").forEach { name ->
                source[name]?.let { HistoricalRow(it, strings) }
            }
            if(bodyReport?.recognized == true) {
                Text(ui(strings, "quickHistoryBodyExamples"), color = quickDark,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                listOf("Ölçülü omuz", "Geniş göğüs", "Uzun parmaklar")
                    .forEach { name ->
                        source[name]?.let { HistoricalRow(it, strings) }
                    }
            }

            Text(ui(strings, "quickHistoryExamplesNotMatched"), color = quickGray,
                style = MaterialTheme.typography.labelSmall)
            Text(ui(strings, "photoReportSource"), color = quickGray,
                style = MaterialTheme.typography.labelSmall)
        }
    }
}
