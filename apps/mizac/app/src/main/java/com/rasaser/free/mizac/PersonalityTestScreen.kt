package com.rasaser.free.mizac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val dark = Color(0xFF211911)
private val ivory = Color(0xFFFFFBF6)
private val secondary = Color(0xFF5A4C40)

private fun originalScore(list: List<OriginalQuestion>, answers: List<Int>, factor: String): Float {
    val values = list.mapIndexedNotNull { idx, item ->
        if (item.category != factor) null
        else (if (item.reverse) 6 - answers[idx] else answers[idx]).toFloat()
    }
    return values.average().toFloat()
}

@Composable
fun OriginalQuestionnaire(
    lang: MizacLang,
    dict: Map<String, String>,
    questionCount: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val all = remember(context) { loadOriginalQuestions(context) }
    val questions = remember(questionCount, all) {
        selectOriginalQuestions(all, questionCount)
    }
    var answers by rememberSaveable(questionCount) {
        mutableStateOf(List(questions.size) { 0 })
    }
    var index by rememberSaveable(questionCount) { mutableStateOf(0) }
    var finished by rememberSaveable(questionCount) { mutableStateOf(false) }
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick=onBack) { Text(ui(dict,"backSetup")) }
        Text(ui(dict,"scienceHeader"),color=dark,
            style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text(ui(dict,"unvalidated"),color=secondary,
            style=MaterialTheme.typography.bodySmall)
        if(finished && answers.all { it in 1..5 }) {
            Text(ui(dict,"resultTitle"),color=dark,
                style=MaterialTheme.typography.titleLarge)
            val labels=mapOf(
                "O" to "factorO","C" to "factorC","E" to "factorE",
                "A" to "factorA","S" to "factorS"
            )
            MIZAC_FACTORS.forEach { factor ->
                val score=originalScore(questions,answers,factor)
                Card(colors=CardDefaults.cardColors(containerColor=ivory),
                    shape=RoundedCornerShape(19.dp),modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp),
                        verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Text(ui(dict,labels.getValue(factor)),color=dark,
                            style=MaterialTheme.typography.titleMedium)
                        LinearProgressIndicator(progress={score / 5f},modifier=Modifier.fillMaxWidth())
                        Text(formatLocalNumber(score,lang) + " / 5",
                            color=secondary,style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text(ui(dict,"resultDisclaimer"),color=secondary)
            Text(ui(dict,"noNorms"),color=secondary,
                style=MaterialTheme.typography.bodySmall)
            Button(onClick={
                answers=List(questions.size){0}
                index=0
                finished=false
            },modifier=Modifier.fillMaxWidth(),
                colors=ButtonDefaults.buttonColors(containerColor=ivory,contentColor=dark)) {
                Text(ui(dict,"restart"))
            }
        } else {
            Text(
                ui(dict,"questions") + ": ${index+1} / ${questions.size}",
                color=dark,style=MaterialTheme.typography.titleMedium
            )
            LinearProgressIndicator(progress={(index+1f)/questions.size},
                modifier=Modifier.fillMaxWidth())
            Card(colors=CardDefaults.cardColors(containerColor=ivory),
                shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(questions[index].text(lang),color=dark,
                        style=MaterialTheme.typography.titleLarge)
                    Text(ui(dict,"agreement"),color=secondary,
                        style=MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                        verticalAlignment=Alignment.CenterVertically) {
                        (1..5).forEach { value ->
                            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                RadioButton(selected=answers[index]==value,
                                    onClick={
                                        answers=answers.toMutableList().also { it[index]=value }
                                    })
                                Text(value.toString(),color=dark)
                            }
                        }
                    }
                }
            }
            Text(ui(dict,"localPrivacy"),color=secondary,
                style=MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled=index>0,onClick={index--},modifier=Modifier.weight(1f)
                ){Text(ui(dict,"back"))}
                Button(
                    enabled=answers[index]>0,
                    onClick={
                        if(index==questions.lastIndex){
                            val firstMissing=answers.indexOfFirst{it==0}
                            if(firstMissing>=0)index=firstMissing else finished=true
                        }else index++
                    },modifier=Modifier.weight(1f),
                    colors=ButtonDefaults.buttonColors(containerColor=ivory,contentColor=dark)
                ){
                    Text(ui(dict,if(index==questions.lastIndex)"showResult" else "next"))
                }
            }
        }
    }
}

@Composable
fun HistoricalReflectionScreen(
    lang: MizacLang,
    dict: Map<String, String>,
    onBack: () -> Unit
) {
    val context=LocalContext.current
    val questions=remember(lang.code) { loadHistoryReflection(context,lang) }
    var answers by rememberSaveable {
        mutableStateOf(List(10){0})
    }
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick=onBack) { Text(ui(dict,"backSetup")) }
        Text(ui(dict,"historyReflectionTitle"),color=dark,
            style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text(ui(dict,"historyReflectionNotice"),color=secondary)
        questions.forEachIndexed { idx, prompt ->
            Card(colors=CardDefaults.cardColors(containerColor=ivory),
                shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("${idx+1}. $prompt",color=dark,
                        style=MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement=Arrangement.SpaceBetween,
                        modifier=Modifier.fillMaxWidth()) {
                        (1..5).forEach { score ->
                            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                RadioButton(
                                    selected=answers[idx]==score,
                                    onClick={
                                        answers=answers.toMutableList().also{it[idx]=score}
                                    }
                                )
                                Text(score.toString(),color=dark)
                            }
                        }
                    }
                }
            }
        }
        Text(ui(dict,"agreement"),color=secondary,
            style=MaterialTheme.typography.bodySmall)
        Text(ui(dict,"historyReflectionEnd"),color=secondary,
            style=MaterialTheme.typography.bodySmall)
    }
}
