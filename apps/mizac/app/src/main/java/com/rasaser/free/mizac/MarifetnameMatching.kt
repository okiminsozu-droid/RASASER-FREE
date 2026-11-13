package com.rasaser.free.mizac

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class BookEntry(
    val chapter: String,
    val group: String,
    val feature: String,
    val description: String,
    val otherSide: String?
)

private fun readBookFile(context: Context, file: String): List<BookEntry> =
    context.assets.open("history/$file").bufferedReader(Charsets.UTF_8).useLines { lines ->
        lines.filter { it.isNotBlank() && !it.startsWith("#") }.map { line ->
            val parts = line.split('\t')
            require(parts.size == if (file == "catalog_8.tsv") 5 else 4) {
                "Invalid Marifetname book data in $file"
            }
            BookEntry(parts[0], parts[1], parts[2], parts[3], parts.getOrNull(4))
        }.toList()
    }

private fun allBookEntries(context: Context): List<BookEntry> =
    listOf("catalog_4.tsv", "catalog_5.tsv", "catalog_6.tsv",
        "catalog_8.tsv", "catalog_mizac.tsv")
        .flatMap { readBookFile(context, it) }
        .also { require(it.size == 239) { "Missing book records: ${it.size}" } }

private fun visibleGroupKey(entry: BookEntry): String =
    entry.chapter + ":" + entry.group

@Composable
fun MarifetnameMatching(lang: MizacLang, strings: Map<String, String>) {
    val context = LocalContext.current
    val all = remember(context) { allBookEntries(context) }
    var mode by rememberSaveable { mutableStateOf("appearance") }
    var expanded by rememberSaveable { mutableStateOf("4:Yüz şekli") }

    val modeDefinitions = listOf(
        "appearance" to "catalogAppearance",
        "twitch" to "catalogTwitch",
        "mizac" to "catalogMizac"
    )

    val rows = when (mode) {
        "twitch" -> all.filter { it.chapter == "8" }
        "mizac" -> all.filter { it.chapter == "M" }
        else -> all.filter { it.chapter in listOf("4", "5", "6") }
    }

    val grouped = rows.groupBy(::visibleGroupKey)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF6)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                modeDefinitions.forEach { (id, labelKey) ->
                    Text(
                        ui(strings,labelKey),
                        modifier = Modifier.weight(1f)
                            .background(
                                if(mode == id)Color(0xFFDCC294) else Color(0xFFF5EDE3),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                mode = id
                                expanded = if(id=="appearance")"4:Yüz şekli" else ""
                            }
                            .padding(vertical=10.dp, horizontal=3.dp),
                        color=Color(0xFF241B13),
                        fontWeight=if(mode==id)FontWeight.Bold else FontWeight.Normal,
                        style=MaterialTheme.typography.labelSmall
                    )
                }
            }
            Text(
                rows.size.toString()+" "+ui(strings,"catalogItems"),
                color=Color(0xFF6D5943),
                style=MaterialTheme.typography.labelMedium
            )
            if(lang.code != "tr") {
                Text(ui(strings,"catalogOriginalLanguage"),
                    color=Color(0xFF6D5943),
                    style=MaterialTheme.typography.labelSmall)
            }
            grouped.forEach { (groupKey, groupRows) ->
                val current=expanded==groupKey
                Text(
                    (if(current)"▾ " else "▸ ") +
                        groupRows.first().group+"  (${groupRows.size})",
                    modifier=Modifier.fillMaxWidth().clickable {
                        expanded=if(current)"" else groupKey
                    }.padding(horizontal=8.dp,vertical=9.dp),
                    color=Color(0xFF1E1711),
                    fontWeight=FontWeight.SemiBold,
                    style=MaterialTheme.typography.titleSmall
                )
                if(current) {
                    groupRows.forEach { item ->
                        Row(
                            modifier=Modifier.fillMaxWidth().padding(
                                horizontal=6.dp,vertical=5.dp),
                            horizontalArrangement=Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                item.feature,
                                modifier=Modifier.weight(1f),
                                color=Color(0xFF1E1711),
                                fontWeight=FontWeight.Medium,
                                style=MaterialTheme.typography.bodySmall
                            )
                            Column(
                                modifier=Modifier.weight(1.4f),
                                verticalArrangement=Arrangement.spacedBy(3.dp)
                            ) {
                                if(item.otherSide == null) {
                                    Text(item.description,
                                        color=Color(0xFF594C3C),
                                        style=MaterialTheme.typography.bodySmall)
                                } else {
                                    Text(
                                        ui(strings,"catalogRight")+": "+item.description,
                                        color=Color(0xFF594C3C),
                                        style=MaterialTheme.typography.bodySmall)
                                    Text(
                                        ui(strings,"catalogLeft")+": "+item.otherSide,
                                        color=Color(0xFF594C3C),
                                        style=MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
            TextButton(onClick={
                val source=if(mode=="mizac")
                    "https://tr.wikisource.org/wiki/Marifetname/45"
                else "https://tr.wikisource.org/wiki/Marifetname/42"
                runCatching {
                    context.startActivity(Intent(
                        Intent.ACTION_VIEW, Uri.parse(source)))
                }
            }) {
                Text(ui(strings,"catalogSource"))
            }
        }
    }
}
