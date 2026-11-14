package com.rasaser.free.mizac

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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

private val QuickCream = Color(0xFFFFFBF6)
private val QuickInk = Color(0xFF261A12)
private val QuickMuted = Color(0xFF605343)

private data class SelfAnswer(val number: Int, val trait: String, val verse: String, val score: Int)

internal data class SelectedAdvice(val itemNo: Int, val score: Int, val action: String)

private fun readScores(context: Context, key: String): List<Int>? {
    val prefs=context.getSharedPreferences("mizac_quran_ethics_local",Context.MODE_PRIVATE)
    val scores=prefs.getString(key,null)?.split(",")?.mapNotNull {it.toIntOrNull()}
    return scores?.takeIf { it.size==88 && it.all {value->value in 1..5} }
}

private fun readEthicalAnswers(context: Context, section: String, scores: List<Int>): List<SelfAnswer> =
    context.assets.open("ethics/quran_176.tsv").bufferedReader().useLines { lines ->
        lines.filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val p=line.split('\t')
                if(p.size!=5 || p[0]!=section)return@mapNotNull null
                val index=p[1].toIntOrNull() ?: return@mapNotNull null
                if(index !in 1..88) return@mapNotNull null
                SelfAnswer(index,p[3],p[4],scores[index-1])
            }.toList()
    }

private fun readImprovementActions(context: Context): Map<Int,String> =
    context.assets.open("ethics/growth_actions_tr.tsv").bufferedReader().useLines { lines ->
        lines.filter { it.isNotBlank() && !it.startsWith("#") }.mapNotNull { row ->
            val fields=row.split('\t',limit=2)
            val number=fields.firstOrNull()?.toIntOrNull()
            if(fields.size!=2 || number==null) null
            else number to fields[1]
        }.toMap()
    }

internal fun selectSelfReportedConcerns(
    wrong: List<Int>, actions: Map<Int,String>
): List<SelectedAdvice> {
    require(wrong.size==88)
    return wrong.mapIndexedNotNull { index, rating ->
        if(rating>=4 && actions.containsKey(index+1))
            SelectedAdvice(index+1,rating,actions.getValue(index+1))
        else null
    }.sortedWith(
        compareByDescending<SelectedAdvice>{it.score}.thenBy{it.itemNo}
    ).take(5)
}

@Composable
fun QuickSelfReportStrengthsAndGrowth(
    strings: Map<String,String>,
    onQuestions: ()->Unit
) {
    val context=LocalContext.current
    val good=remember(context){readScores(context,"good")}
    val wrong=remember(context){readScores(context,"wrong")}
    val ready=good!=null && wrong!=null
    // The quick face analysis must provide a result without demanding another
    // 176 questions. Show optional personal growth insights only if the user
    // already completed the separate self-report.
    if(!ready) return

    Card(
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(20.dp),
        colors=CardDefaults.cardColors(containerColor=QuickCream)
    ) {
        Column(
            Modifier.padding(15.dp),
            verticalArrangement=Arrangement.spacedBy(9.dp)
        ) {
            Text(ui(strings,"quickGrowthHeading"),
                color=QuickInk,style=MaterialTheme.typography.titleLarge,
                fontWeight=FontWeight.Bold)
            if(ready) {
                val positive=remember(context,good) {
                    readEthicalAnswers(context,"+",good!!)
                        .filter{it.score>=4}
                        .sortedWith(compareByDescending<SelfAnswer>{it.score}
                            .thenBy{it.number}).take(5)
                }
                val negative=remember(context,wrong) {
                    readEthicalAnswers(context,"-",wrong!!)
                }
                val advice=remember(context) { readImprovementActions(context) }
                val concerns=selectSelfReportedConcerns(wrong!!,advice)
                Text(ui(strings,"quickFromYourAnswers"),color=QuickMuted,
                    style=MaterialTheme.typography.labelMedium)
                Text(ui(strings,"quickYourStrengths"),
                    color=QuickInk,fontWeight=FontWeight.SemiBold)
                if(positive.isEmpty()) Text(ui(strings,"quickNoneRatedHigh"),color=QuickMuted)
                positive.forEach { trait ->
                    Text("•  ${trait.trait} — ${trait.score}/5",
                        color=QuickInk,style=MaterialTheme.typography.bodyMedium)
                    Text(trait.verse,color=QuickMuted,
                        style=MaterialTheme.typography.labelSmall)
                }
                Text(ui(strings,"quickYourFocus"),
                    color=QuickInk,fontWeight=FontWeight.SemiBold)
                if(concerns.isEmpty())
                    Text(ui(strings,"quickNoHighRiskResponses"),color=QuickMuted,
                        style=MaterialTheme.typography.bodyMedium)
                concerns.forEach { note ->
                    val original=negative.firstOrNull{it.number==note.itemNo}
                    if(original!=null) {
                        Text("•  ${original.trait} — ${note.score}/5",
                            color=QuickInk,fontWeight=FontWeight.Medium,
                            style=MaterialTheme.typography.bodyMedium)
                        Text(note.action,color=QuickMuted,
                            style=MaterialTheme.typography.bodyMedium)
                        Text(original.verse,color=QuickMuted,
                            style=MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                Text(ui(strings,"quickNeedSelfAnswers"),color=QuickMuted,
                    style=MaterialTheme.typography.bodyMedium)
                Button(onClick=onQuestions,modifier=Modifier.fillMaxWidth()) {
                    Text(ui(strings,"ethicsEntry"))
                }
            }
        }
    }
}

@Composable
fun QuickFrameImprovements(m: FaceMeasurements, strings: Map<String,String>) {
    val keys=mutableListOf<String>()
    if(m.facesDetected>1)keys+="quickFrameSinglePerson"
    if(abs(m.yaw)>18f)keys+="quickFrameYaw"
    if(abs(m.roll)>15f)keys+="quickFrameRoll"
    if(m.width<160 || m.height<160)keys+="quickFrameCloser"
    if(m.eyeDistanceRatio==null || m.noseMouthRatio==null)
        keys+="quickFrameLighting"
    if(m.fromVideo)keys+="quickFrameVideoClip"

    if(keys.isEmpty())return
    Card(
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(20.dp),
        colors=CardDefaults.cardColors(containerColor=QuickCream)
    ) {
        Column(
            Modifier.padding(15.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ) {
            Text(ui(strings,"quickFrameImprovements"),
                color=QuickInk,fontWeight=FontWeight.Bold,
                style=MaterialTheme.typography.titleMedium)
            keys.take(4).forEach {key ->
                Text("•  "+ui(strings,key),color=QuickMuted,
                    style=MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
