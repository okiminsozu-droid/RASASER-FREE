package com.rasaser.free.template

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val gold = Color(0xFFD4B996)
private val charcoal = Color(0xFF111111)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = gold, background = charcoal)) {
                FreeHome()
            }
        }
    }
}

@Composable
private fun FreeHome() {
    val context = LocalContext.current
    var licensesOpen by rememberSaveable { mutableStateOf(false) }
    val licenses = context.assets.open("legal/licenses.txt").bufferedReader().use { it.readText() }
    Column(
        modifier = Modifier.fillMaxSize().background(charcoal).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "RaSaSeR", color = gold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(text = "FREE Uygulama Şablonu", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Text(text = "Bu örnek arayüz bir ürün değildir. Yeni uygulamayı burada bağımsız şekilde geliştirin.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = { licensesOpen = !licensesOpen }, modifier = Modifier.fillMaxWidth()) {
            Text(if (licensesOpen) "Lisansları kapat" else "Ayarlar > Lisanslar")
        }
        if (licensesOpen) {
            Spacer(Modifier.height(16.dp))
            Text(licenses, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
