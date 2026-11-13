package com.rasaser.free.mizac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
    onBack: () -> Unit,
    onHistory: () -> Unit
) {
    val context = LocalContext.current
    val questions = remember(questionCount) {
        selectOriginalQuestions(loadOriginalQuestions(context), questionCount)
    }
    var answers by rememberSaveable(questionCount) {
        mutableStateOf(List(questions.size) { 0 })
    }
    var finished by rememberSaveable(questionCount) { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { questions.size })
    val scope = rememberCoroutineScope()
    val selectedPage = pagerState.currentPage

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (finished && answers.all { it in 1..5 }) {
            Text(
                ui(dict,"resultTitle"), color=dark,
                style=MaterialTheme.typography.headlineSmall,
                fontWeight=FontWeight.Bold
            )
            val labels=mapOf(
                "O" to "factorO","C" to "factorC","E" to "factorE",
                "A" to "factorA","S" to "factorS"
            )
            MIZAC_FACTORS.forEach { factor ->
                val score=originalScore(questions,answers,factor)
                Card(
                    colors=CardDefaults.cardColors(containerColor=ivory),
                    shape=RoundedCornerShape(22.dp),
                    modifier=Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement=Arrangement.spacedBy(8.dp)
                    ) {
                        Text(ui(dict,labels.getValue(factor)),color=dark,
                            style=MaterialTheme.typography.titleMedium)
                        LinearProgressIndicator(
                            progress={score/5f}, modifier=Modifier.fillMaxWidth()
                        )
                        Text(formatLocalNumber(score,lang)+" / 5",
                            color=secondary,style=MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Text(ui(dict,"resultDisclaimer"),color=secondary,
                style=MaterialTheme.typography.bodySmall)
            Text(ui(dict,"noNorms"),color=secondary,
                style=MaterialTheme.typography.bodySmall)
            OutlinedButton(
                onClick=onHistory,
                modifier=Modifier.fillMaxWidth()
            ) { Text(ui(dict,"history")) }
        } else {
            Text(
                "${selectedPage+1} / ${questions.size}",
                modifier=Modifier.fillMaxWidth(),
                color=dark,
                textAlign=TextAlign.Center,
                style=MaterialTheme.typography.titleLarge,
                fontWeight=FontWeight.Bold
            )
            LinearProgressIndicator(
                progress={(selectedPage+1f)/questions.size},
                modifier=Modifier.fillMaxWidth()
            )
            HorizontalPager(
                state=pagerState,
                modifier=Modifier.fillMaxWidth().height(286.dp),
                key={ page -> questions[page].id }
            ) { page ->
                Card(
                    colors=CardDefaults.cardColors(containerColor=ivory),
                    shape=RoundedCornerShape(26.dp),
                    modifier=Modifier.fillMaxWidth().padding(horizontal=3.dp)
                ) {
                    Column(
                        modifier=Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement=Arrangement.SpaceBetween
                    ) {
                        Text(
                            questions[page].text(lang),
                            modifier=Modifier.fillMaxWidth().height(135.dp),
                            color=dark,
                            textAlign=TextAlign.Center,
                            style=MaterialTheme.typography.titleLarge
                        )
                        Row(
                            modifier=Modifier.fillMaxWidth(),
                            horizontalArrangement=Arrangement.SpaceBetween
                        ) {
                            (1..5).forEach { value ->
                                val isSelected=answers[page]==value
                                Box(
                                    modifier=Modifier.size(47.dp)
                                        .background(
                                            if(isSelected)Color(0xFFD8B57B) else Color.White,
                                            RoundedCornerShape(11.dp)
                                        )
                                        .border(
                                            if(isSelected)2.dp else 1.dp,
                                            if(isSelected)Color(0xFF8D6428) else Color(0xFFD1C7B8),
                                            RoundedCornerShape(11.dp)
                                        )
                                        .clickable {
                                            answers=answers.toMutableList().also{it[page]=value}
                                        },
                                    contentAlignment=Alignment.Center
                                ) {
                                    Text(
                                        value.toString(),
                                        color=dark,
                                        fontWeight=if(isSelected)FontWeight.Bold else FontWeight.Medium,
                                        style=MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Button(
                onClick={
                    if(selectedPage == questions.lastIndex) {
                        val missing=answers.indexOfFirst{it==0}
                        if(missing>=0) {
                            scope.launch { pagerState.animateScrollToPage(missing) }
                        } else {
                            finished=true
                        }
                    } else {
                        scope.launch { pagerState.animateScrollToPage(selectedPage+1) }
                    }
                },
                enabled=answers[selectedPage]>0,
                modifier=Modifier.fillMaxWidth().height(60.dp),
                shape=RoundedCornerShape(27.dp),
                colors=ButtonDefaults.buttonColors(
                    containerColor=ivory,contentColor=dark
                )
            ) {
                Text(
                    ui(dict,"next"),
                    style=MaterialTheme.typography.titleLarge,
                    fontWeight=FontWeight.SemiBold
                )
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
