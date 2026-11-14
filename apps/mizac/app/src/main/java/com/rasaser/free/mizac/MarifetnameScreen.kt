package com.rasaser.free.mizac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MarifetnameScreen(
    onBack: () -> Unit,
    lang: MizacLang,
    strings: Map<String, String>,
    onReflection: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            ui(strings,"historyBookMethod"),
            color=Color(0xFF231A13),
            style=MaterialTheme.typography.headlineSmall,
            fontWeight=FontWeight.Bold
        )
        MarifetnameMatching(lang, strings)
        Button(onClick=onReflection,modifier=Modifier.fillMaxWidth()) {
            Text(ui(strings,"historyReflectionTitle"))
        }
    }
}
