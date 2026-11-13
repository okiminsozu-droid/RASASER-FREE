package com.rasaser.free.kurankelime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel

/** ML Kit Translation models are download-only. The English pivot is built in.
 *  No private Quran content or previously signed APK is changed here.
 */
internal object OfflineLanguagePacks {
    internal data class Pack(val code: String, val name: String)

    // Arabic source + five target languages; English is embedded in the ML Kit SDK.
    val downloadable = listOf(
        Pack("ar", "Arapça (kaynak)"),
        Pack("tr", "Türkçe"),
        Pack("de", "Almanca"),
        Pack("es", "İspanyolca"),
        Pack("ja", "Japonca")
    )

    fun downloaded(onResult: (Set<String>?, String?) -> Unit) {
        RemoteModelManager.getInstance()
            .getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                onResult(models.map { it.language }.toSet() + "en", null)
            }
            .addOnFailureListener { error ->
                onResult(null, error.localizedMessage ?: "Model durumu okunamadı")
            }
    }

    /** The SDK's real model state, not a cached preference, controls readiness. */
    fun pairReady(source: String, target: String, onResult: (Boolean) -> Unit) {
        downloaded { available, _ ->
            onResult(available != null && source in available && target in available)
        }
    }
}

@Composable
internal fun OfflineLanguagePacksDialog(onDismiss: () -> Unit) {
    val manager = remember { RemoteModelManager.getInstance() }
    var installed by remember { mutableStateOf(setOf("en")) }
    var reading by remember { mutableStateOf(true) }
    var running by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf("") }

    fun refresh() {
        reading = true
        OfflineLanguagePacks.downloaded { current, error ->
            reading = false
            if (current != null) {
                installed = current
            } else {
                problem = error ?: "Paketler kontrol edilemedi"
            }
        }
    }

    fun downloadAll(ready: Set<String> = installed) {
        if (running || reading) return
        val missing = OfflineLanguagePacks.downloadable.filter { it.code !in ready }
        if (missing.isEmpty()) {
            status = "Tüm dil modelleri zaten cihazda."
            return
        }
        running = true
        problem = ""
        val conditions = DownloadConditions.Builder().requireWifi().build()

        fun downloadNext(index: Int) {
            if (index >= missing.size) {
                running = false
                status = "Tüm dil modelleri indirildi. Artık internetsiz çeviri kullanılabilir."
                refresh()
                return
            }
            val pack = missing[index]
            status = pack.name + " modeli indiriliyor (" + (index + 1) + "/" + missing.size + "). Wi-Fi gerekli."
            manager.download(TranslateRemoteModel.Builder(pack.code).build(), conditions)
                .addOnSuccessListener {
                    installed = installed + pack.code
                    downloadNext(index + 1)
                }
                .addOnFailureListener { error ->
                    running = false
                    problem = pack.name + " indirilemedi: " +
                        (error.localizedMessage ?: "Wi-Fi bağlantısını kontrol edin")
                    refresh()
                }
        }

        downloadNext(0)
    }

    LaunchedEffect(Unit) {
        // One tap on the home screen starts all missing downloads automatically.
        reading = true
        OfflineLanguagePacks.downloaded { current, error ->
            reading = false
            if (current != null) {
                installed = current
                downloadAll(current)
            } else {
                problem = error ?: "Mevcut dil paketleri denetlenemedi"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Çevrimdışı Dil Paketleri") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Modeller Google ML Kit tarafından Wi-Fi ile bir kez indirilir. " +
                        "Sonrasında cihazda saklanır ve çeviri için internet gerekmez.",
                    fontSize = 13.sp
                )
                Text("İngilizce · Hazır (yerleşik)", fontSize = 13.sp)
                OfflineLanguagePacks.downloadable.forEach { pack ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(pack.name, fontSize = 14.sp)
                        Text(
                            if (pack.code in installed) "✓ İndirildi" else "İndirilecek",
                            fontSize = 12.sp
                        )
                    }
                }
                if (reading || running) {
                    CircularProgressIndicator()
                }
                if (status.isNotBlank()) Text(status, fontSize = 12.sp)
                if (problem.isNotBlank()) Text(problem, fontSize = 12.sp)
                Text(
                    "Not: Otomatik çeviri Kur’an kelime meali yerine geçmez. " +
                        "Doğrulanmış kelime anlamları varsa öncelikle onlar gösterilir.",
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { downloadAll() }, enabled = !running && !reading) {
                Text("Eksikleri indir / yeniden dene")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}
