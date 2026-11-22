package com.rasaser.free.icerikureticisi

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.json.JSONObject

object CreatorAppearance {
    private const val FILE = "creator_settings"
    fun dark(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean("dark", false)
    fun setDark(context: Context, isDark: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean("dark", isDark).apply()
    }
}

@Composable
fun CreatorTopControls(
    language: String,
    isDark: Boolean,
    onLanguage: (String) -> Unit,
    onDark: (Boolean) -> Unit,
    onPremium: () -> Unit,
    onLock: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = onPremium) {
            Text("♛", color = Color(0xFFD1AB5C),
                style = MaterialTheme.typography.headlineMedium)
        }
        Image(painter = painterResource(R.drawable.ic_launcher_creator),
            contentDescription = "İçerik Üreticisi logosu",
            modifier = Modifier.size(42.dp))
        IconButton(onClick = onLock) {
            Text("🔐", style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground)
        }
        IconButton(onClick = { onDark(!isDark) }) {
            Text(if (isDark) "☼" else "☾",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineSmall)
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Text(AppLanguage.choices.first { it.code == language }.flag + " ▾",
                    style = MaterialTheme.typography.titleMedium)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                AppLanguage.choices.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text("${choice.flag} ${choice.title}") },
                        onClick = {
                            onLanguage(choice.code)
                            menu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CreatorAvatar(context: Context, logoFile: String, fallback: String, size: Int = 52) {
    val bitmap = remember(logoFile) {
        if (logoFile.isBlank()) null else LocalLogos.load(context, logoFile)?.asImageBitmap()
    }
    val modifier = Modifier.size(size.dp).clip(RoundedCornerShape(14.dp))
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = "Profil resmi", modifier = modifier)
    } else {
        Box(
            modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(fallback.take(2).uppercase(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CreatorNameTranslationsDialog(
    creator: Creator,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    val original = remember(creator.id, creator.translatedNamesJson) {
        try { JSONObject(creator.translatedNamesJson) } catch (_: Exception) { JSONObject() }
    }
    val languages = AppLanguage.choices.filter { it.code != "tr" }
    var names by remember(creator.id) {
        mutableStateOf(languages.associate { it.code to original.optString(it.code) })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("İçerik adı çevirileri")) },
        text = {
            Column(
                modifier = Modifier.height(400.dp)
                    .then(Modifier),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(creator.name, style = MaterialTheme.typography.bodySmall)
                androidx.compose.foundation.lazy.LazyColumn {
                    items(languages.size) { index ->
                        val language = languages[index]
                        OutlinedTextField(
                            value = names[language.code].orEmpty(),
                            onValueChange = { value ->
                                names = names + (language.code to value.take(80))
                            },
                            label = { Text("${language.flag} ${language.title}") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(names) }) { Text(tr("Kaydet")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Vazgeç")) }
        }
    )
}

@Composable
fun CreatorFooter(context: Context) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("RaSaSeR APPS & TECHNOLOGY",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold)
        Text(
            "www.okiminsozu.com",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.okiminsozu.com")))
                } catch (_: Exception) {}
            }
        )
    }
}
