package com.rasaser.free.mizac

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

private val DarkText = Color(0xFF231A13)
private val CardWhite = Color(0xFFFFFBF6)
private val Dim = Color(0xFF605344)

@Composable
fun MarifetnameScreen(
    onBack: () -> Unit,
    lang: MizacLang,
    strings: Map<String, String>,
    onReflection: () -> Unit
) {
    val context = LocalContext.current
    val sections = remember(lang.code) { loadHistory(context, lang) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(ui(strings,"historyTitle"), color = DarkText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        Card(shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)) {
            Column(Modifier.padding(16.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ui(strings,"historyNotice"), color = DarkText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
                Text(ui(strings,"historyOrigin"), color = Dim)
            }
        }
        sections.forEach { section ->
            Card(shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardWhite)) {
                Column(Modifier.padding(16.dp),verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(section.title,color=DarkText,
                        fontWeight=FontWeight.SemiBold,
                        style=MaterialTheme.typography.titleMedium)
                    Text(section.explanation,color=Dim)
                }
            }
        }
        Button(onClick=onReflection,modifier=Modifier.fillMaxWidth()) {
            Text(ui(strings,"historyReflectionTitle"))
        }
        Text(ui(strings,"historySources"),color=DarkText,
            style=MaterialTheme.typography.titleMedium)
        Card(colors=CardDefaults.cardColors(containerColor=CardWhite)) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(ui(strings,"historyPrimaryReference"),
                    color=Dim, style=MaterialTheme.typography.bodySmall)
                Text(ui(strings,"historyArchiveReference"),
                    color=Dim, style=MaterialTheme.typography.bodySmall)
                Text("https://acikerisim.tbmm.gov.tr/items/37ceaca9-2499-4d5b-a0df-be6a757513c9",
                    color=DarkText, style=MaterialTheme.typography.bodySmall)
                Text(ui(strings,"historyCopyright"),
                    color=Dim,style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}
