package com.rasaser.free.mizac

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Source: user's PDF Kuranda_176_Iyi_ve_Kotu_Insan_Ozellikleri_Ayetli.pdf.
 * 176 independent thematic titles with verse references. Not literal translations.
 * This assessment is self-report, not an automatic moral diagnosis of a human.
 */
private data class EthicalItem(
    val kind: String,
    val number: Int,
    val category: String,
    val feature: String,
    val ayets: String
)

private fun loadEthicalItems(context: Context): List<EthicalItem> =
    context.assets.open("ethics/quran_176.tsv").bufferedReader().useLines { lines ->
        lines.filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line ->
                val t=line.split('\t')
                require(t.size==5) { "Corrupt ethics question" }
                EthicalItem(t[0],t[1].toInt(),t[2],t[3],t[4])
            }.toList()
    }.also { all ->
        require(all.size == 176 && all.count{it.kind=="+"}==88 &&
            all.count{it.kind=="-"}==88) { "Incomplete ethics data" }
        for(kind in listOf("+","-"))
            require(all.filter{it.kind==kind}.map{it.number} == (1..88).toList())
    }

private fun parseAnswers(p: SharedPreferences, key: String): List<Int> {
    val read=p.getString(key,null)?.split(",")?.mapNotNull{it.toIntOrNull()}
    return if(read!=null && read.size==88 && read.all{it in 0..5})
        read else List(88){0}
}

private val ethicsBg=Color(0xFFFFFBF6)
private val ethicsInk=Color(0xFF251B13)
private val ethicsMuted=Color(0xFF65584A)
private val ethicsGold=Color(0xFFD5B784)

@Composable
fun QuranEthicsQuestionnaire(
    lang: MizacLang,
    dict: Map<String,String>
) {
    val context=androidx.compose.ui.platform.LocalContext.current
    val entries=remember(context){loadEthicalItems(context)}
    val goodItems=remember(entries){entries.filter{it.kind=="+"}}
    val wrongItems=remember(entries){entries.filter{it.kind=="-"}}
    val pref=remember(context){
        context.getSharedPreferences("mizac_quran_ethics_local",Context.MODE_PRIVATE)
    }
    var good by rememberSaveable { mutableStateOf(parseAnswers(pref,"good")) }
    var wrong by rememberSaveable { mutableStateOf(parseAnswers(pref,"wrong")) }
    var section by rememberSaveable { mutableStateOf("") }
    val goodDone=good.all{it in 1..5}
    val wrongDone=wrong.all{it in 1..5}

    Column(
        modifier=Modifier.fillMaxWidth(),
        verticalArrangement=Arrangement.spacedBy(14.dp)
    ) {
        if(goodDone && wrongDone) {
            EthicsResults(goodItems,wrongItems,good,wrong,dict,onReset={
                pref.edit().remove("good").remove("wrong").apply()
                good=List(88){0}
                wrong=List(88){0}
                section=""
            })
        } else if(section.isEmpty()) {
            Text(ui(dict,"ethicsTitle"),color=ethicsInk,
                style=MaterialTheme.typography.headlineSmall,
                fontWeight=FontWeight.Bold)
            val choices=listOf(
                Triple("+","ethicsGoodTitle",good),
                Triple("-","ethicsWrongTitle",wrong)
            )
            choices.forEach { (kind,labelKey,answers) ->
                Card(
                    modifier=Modifier.fillMaxWidth().clickable { section=kind },
                    shape=RoundedCornerShape(22.dp),
                    colors=CardDefaults.cardColors(containerColor=ethicsBg)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement=Arrangement.spacedBy(9.dp)
                    ) {
                        Text(ui(dict,labelKey),color=ethicsInk,
                            style=MaterialTheme.typography.titleLarge,
                            fontWeight=FontWeight.SemiBold)
                        val count=answers.count{it>0}
                        Text("$count / 88",color=ethicsMuted)
                        LinearProgressIndicator(
                            progress={count/88f},modifier=Modifier.fillMaxWidth())
                    }
                }
            }
            Text(ui(dict,"ethicsChooseInfo"),color=ethicsMuted,
                style=MaterialTheme.typography.bodySmall)
            if(lang.code!="tr")
                Text(ui(dict,"catalogOriginalLanguage"),color=ethicsMuted,
                    style=MaterialTheme.typography.labelSmall)
        } else {
            // Both sections use exactly the user's 88 items.
            val items=if(section=="+")goodItems else wrongItems
            val answers=if(section=="+")good else wrong
            val initial=answers.indexOfFirst{it==0}.let{if(it<0)0 else it}
            androidx.compose.runtime.key(section) {
                val pager=rememberPagerState(initialPage=initial,pageCount={items.size})
                val scope=rememberCoroutineScope()
                val page=pager.currentPage
                Text(ui(dict,if(section=="+")"ethicsGoodTitle" else "ethicsWrongTitle"),
                    color=ethicsInk,style=MaterialTheme.typography.titleLarge,
                    fontWeight=FontWeight.Bold)
                Text("${page+1} / 88",color=ethicsMuted,
                    modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center)
                LinearProgressIndicator(progress={answers.count{it>0}/88f},
                    modifier=Modifier.fillMaxWidth())
                HorizontalPager(
                    state=pager,
                    modifier=Modifier.fillMaxWidth().height(350.dp),
                    key={index->items[index].kind+items[index].number}
                ) { i ->
                    val item=items[i]
                    Card(
                        modifier=Modifier.fillMaxWidth(),
                        shape=RoundedCornerShape(23.dp),
                        colors=CardDefaults.cardColors(containerColor=ethicsBg)
                    ) {
                        Column(
                            modifier=Modifier.fillMaxWidth().padding(15.dp),
                            verticalArrangement=Arrangement.spacedBy(13.dp)
                        ) {
                            Text(item.category,color=ethicsMuted,
                                style=MaterialTheme.typography.labelLarge)
                            Text(item.feature,color=ethicsInk,
                                style=MaterialTheme.typography.titleLarge,
                                fontWeight=FontWeight.Bold)
                            Text(ui(dict,"ethicsQuestion"),color=ethicsInk,
                                style=MaterialTheme.typography.bodyMedium)
                            Text(item.ayets,color=ethicsMuted,
                                style=MaterialTheme.typography.bodySmall)
                            Row(
                                modifier=Modifier.fillMaxWidth(),
                                horizontalArrangement=Arrangement.SpaceBetween
                            ) {
                                (1..5).forEach{answer->
                                    val chosen=answers[i]==answer
                                    Box(
                                        modifier=Modifier.size(47.dp)
                                            .background(
                                                if(chosen)ethicsGold else Color.White,
                                                RoundedCornerShape(11.dp))
                                            .border(
                                                if(chosen)2.dp else 1.dp,
                                                if(chosen)Color(0xFF8D6428) else Color(0xFFD1C7B8),
                                                RoundedCornerShape(11.dp))
                                            .clickable {
                                                val next=answers.toMutableList().also{
                                                    it[i]=answer
                                                }
                                                if(section=="+")good=next else wrong=next
                                                pref.edit().putString(
                                                    if(section=="+")"good" else "wrong",
                                                    next.joinToString(",")
                                                ).apply()
                                            },
                                        contentAlignment=Alignment.Center
                                    ) {
                                        Text(answer.toString(),color=ethicsInk,
                                            style=MaterialTheme.typography.titleMedium,
                                            fontWeight=if(chosen)FontWeight.Bold
                                                else FontWeight.Normal)
                                    }
                                }
                            }
                            Text(ui(dict,"ethicsScale"),color=ethicsMuted,
                                style=MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Button(
                    enabled=answers[page]!=0,
                    onClick={
                        if(page==items.lastIndex) {
                            val missing=answers.indexOfFirst{it==0}
                            if(missing>=0)
                                scope.launch{pager.animateScrollToPage(missing)}
                            else section=""
                        } else {
                            scope.launch{pager.animateScrollToPage(page+1)}
                        }
                    },
                    colors=ButtonDefaults.buttonColors(
                        containerColor=ethicsBg,contentColor=ethicsInk),
                    modifier=Modifier.fillMaxWidth(),
                    shape=RoundedCornerShape(22.dp)
                ) {
                    Text(ui(dict,if(page==items.lastIndex)"ethicsFinishSection" else "next"))
                }
                TextButton(onClick={section=""},modifier=Modifier.fillMaxWidth()) {
                    Text(ui(dict,"ethicsSections"),color=ethicsMuted)
                }
            }
        }
    }
}

private fun scorePercent(answers: List<Int>): Int =
    ((answers.sumOf{(it-1).coerceIn(0,4)} / (88f*4f))*100f).roundToInt()

@Composable
private fun RankedEthicList(
    title: String,
    items: List<Pair<EthicalItem,Int>>,
    dict: Map<String,String>,
    emptyLabel: String
) {
    var showAll by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier=Modifier.fillMaxWidth(),
        colors=CardDefaults.cardColors(containerColor=ethicsBg),
        shape=RoundedCornerShape(20.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(15.dp),
            verticalArrangement=Arrangement.spacedBy(9.dp)
        ) {
            Text(title,color=ethicsInk,
                style=MaterialTheme.typography.titleMedium,
                fontWeight=FontWeight.Bold)
            if(items.isEmpty()) Text(emptyLabel,color=ethicsMuted)
            (if(showAll)items else items.take(10)).forEachIndexed{idx,(item,value)->
                Column(verticalArrangement=Arrangement.spacedBy(2.dp)){
                    Text("${idx+1}. ${item.feature} — $value/5",
                        color=ethicsInk,style=MaterialTheme.typography.bodyMedium)
                    Text(item.ayets,color=ethicsMuted,
                        style=MaterialTheme.typography.labelSmall)
                }
            }
            if(items.size>10)
                TextButton(onClick={showAll=!showAll}) {
                    Text(ui(dict,if(showAll)"ethicsLess" else "ethicsAll")+
                        " (${items.size})")
                }
        }
    }
}

@Composable
private fun EthicsResults(
    goodItems: List<EthicalItem>,
    wrongItems: List<EthicalItem>,
    good: List<Int>,
    wrong: List<Int>,
    dict: Map<String,String>,
    onReset: ()->Unit
) {
    val g=scorePercent(good)
    val w=scorePercent(wrong)
    val dominant=when{
        g>w->"ethicsGoodDominant"
        w>g->"ethicsWrongDominant"
        else->"ethicsEqual"
    }
    Text(ui(dict,"ethicsResult"),color=ethicsInk,
        style=MaterialTheme.typography.headlineSmall,
        fontWeight=FontWeight.Bold)
    Card(
        modifier=Modifier.fillMaxWidth(),
        colors=CardDefaults.cardColors(containerColor=ethicsBg),
        shape=RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier=Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp)
        ) {
            Text(ui(dict,"ethicsGoodTitle")+"   %$g",color=ethicsInk,
                style=MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(progress={g/100f},
                modifier=Modifier.fillMaxWidth())
            Text(ui(dict,"ethicsWrongTitle")+"   %$w",color=ethicsInk,
                style=MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(progress={w/100f},
                modifier=Modifier.fillMaxWidth())
            Text(ui(dict,dominant),color=ethicsInk,
                style=MaterialTheme.typography.titleLarge,
                fontWeight=FontWeight.Bold)
            Text(ui(dict,"ethicsResultMethod"),color=ethicsMuted,
                style=MaterialTheme.typography.bodySmall)
        }
    }
    val strengths=goodItems.zip(good)
        .filter{it.second>=4}
        .sortedWith(compareByDescending<Pair<EthicalItem,Int>>{it.second}
            .thenBy{it.first.number})
    val concerns=wrongItems.zip(wrong)
        .filter{it.second>=4}
        .sortedWith(compareByDescending<Pair<EthicalItem,Int>>{it.second}
            .thenBy{it.first.number})
    val improvements=goodItems.zip(good)
        .filter{it.second<=2}
        .sortedWith(compareBy<Pair<EthicalItem,Int>>{it.second}
            .thenBy{it.first.number})
    RankedEthicList(ui(dict,"ethicsStrengths"),strengths,dict,ui(dict,"ethicsNoEntries"))
    RankedEthicList(ui(dict,"ethicsConcerns"),concerns,dict,ui(dict,"ethicsNoEntries"))
    RankedEthicList(ui(dict,"ethicsImprovements"),improvements,dict,ui(dict,"ethicsNoEntries"))
    Text(ui(dict,"ethicsSelfReport"),color=ethicsMuted,
        style=MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick=onReset,modifier=Modifier.fillMaxWidth()) {
        Text(ui(dict,"ethicsRestart"))
    }
}
