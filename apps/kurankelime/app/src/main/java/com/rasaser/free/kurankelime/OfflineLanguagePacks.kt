package com.rasaser.free.kurankelime

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel

/**
 * Explicit, free on-device ML Kit translation model management.
 * All models are downloaded and retained on device; private Quran media is not uploaded.
 * Independently dispatch each missing language: a stalled Arabic model must not block
 * Turkish, German, Spanish or Japanese downloads.
 */
internal object OfflineLanguagePacks {
    internal data class Pack(val code: String, val name: String)

    val downloadable = listOf(
        Pack("ar", "Arapça"),
        Pack("tr", "Türkçe"),
        Pack("de", "Almanca"),
        Pack("es", "İspanyolca"),
        Pack("ja", "Japonca")
    )

    fun hasWifiInternet(context: Context): Boolean {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return false
        // Look at all networks, including the underlying Wi-Fi of an active VPN.
        return connectivity.allNetworks.any { network ->
            val caps = connectivity.getNetworkCapabilities(network) ?: return@any false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }

    fun downloaded(onResult: (Set<String>?, String?) -> Unit) {
        RemoteModelManager.getInstance()
            .getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                onResult(models.map { it.language }.toSet() + "en", null)
            }
            .addOnFailureListener { error ->
                onResult(null, error.localizedMessage ?: "Model listesi okunamadı")
            }
    }

    fun pairReady(source: String, target: String, onResult: (Boolean) -> Unit) {
        downloaded { available, _ ->
            onResult(available != null && source in available && target in available)
        }
    }
}

@Composable
internal fun OfflineLanguagePacksDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { RemoteModelManager.getInstance() }
    val handler = remember { Handler(Looper.getMainLooper()) }
    val states = remember { mutableStateMapOf<String, String>() }
    var installed by remember { mutableStateOf(setOf("en")) }
    var reading by remember { mutableStateOf(true) }
    var pending by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("Cihazdaki dil paketleri kontrol ediliyor…") }
    var problem by remember { mutableStateOf("") }

    fun downloadAll(ready: Set<String>) {
        if (pending > 0 || reading) return
        val missing = OfflineLanguagePacks.downloadable.filter { it.code !in ready }
        if (missing.isEmpty()) {
            status = "6 dil hazır. Çeviri için artık internet gerekmiyor."
            return
        }
        if (!OfflineLanguagePacks.hasWifiInternet(context)) {
            problem = "Wi-Fi internet bağlantısı doğrulanamadı. Wi-Fi bağlantısını ve " +
                "internet erişimini kontrol edip tekrar deneyin."
            status = "İndirilemeyen paketler korunuyor; tekrar deneyebilirsiniz."
            return
        }

        problem = ""
        pending = missing.size
        val settled = mutableSetOf<String>()
        var errors = 0
        status = "${missing.size} eksik dil paketi için indirme başlatıldı."

        fun finish(pack: OfflineLanguagePacks.Pack, success: Boolean, reason: String = "") {
            if (!settled.add(pack.code)) {
                // An SDK task may finish after the 3-minute watchdog; record late success.
                if (success) {
                    installed = installed + pack.code
                    states[pack.code] = "✓ İndirildi"
                }
                return
            }
            pending = (pending - 1).coerceAtLeast(0)
            if (success) {
                installed = installed + pack.code
                states[pack.code] = "✓ İndirildi"
            } else {
                errors++
                states[pack.code] = "Tekrar dene"
                problem = "${pack.name}: $reason"
            }
            if (pending == 0) {
                status = if (errors == 0) {
                    "Tüm dil paketleri indirildi. Çevrimdışı çeviri hazır."
                } else {
                    "${installed.size}/6 dil hazır. Eksikler için yeniden dene tuşunu kullan."
                }
            }
        }

        // Never queue the other languages behind the first unfinished model.
        missing.forEach { pack ->
            val model = TranslateRemoteModel.Builder(pack.code).build()
            states[pack.code] = "İndiriliyor…"
            // Wi-Fi has already been validated above. Do not add requireWifi():
            // the SDK could otherwise wait silently on its own background constraint.
            val conditions = DownloadConditions.Builder().build()
            try {
                manager.download(model, conditions)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            manager.isModelDownloaded(model)
                                .addOnCompleteListener { verified ->
                                    if (verified.isSuccessful && verified.result == true) {
                                        finish(pack, true)
                                    } else {
                                        finish(pack, false, "Model kaydı doğrulanamadı.")
                                    }
                                }
                        } else {
                            finish(
                                pack, false,
                                task.exception?.localizedMessage ?: "ML Kit indirme hatası"
                            )
                        }
                    }
            } catch (error: Exception) {
                finish(pack, false, error.localizedMessage ?: "İndirme başlatılamadı")
            }
            // An SDK download may remain pending without invoking either callback.
            // Do not leave the UI disabled forever; real model downloads may continue.
            handler.postDelayed({
                if (pack.code !in settled) {
                    manager.isModelDownloaded(model)
                        .addOnCompleteListener { checked ->
                            if (checked.isSuccessful && checked.result == true) {
                                finish(pack, true)
                            } else {
                                finish(
                                    pack, false,
                                    "3 dakika içinde yanıt alınmadı. Google Play hizmetleri, " +
                                        "Wi-Fi ve depolama alanını kontrol edin."
                                )
                            }
                        }
                }
            }, 180_000L)
        }
    }

    LaunchedEffect(Unit) {
        OfflineLanguagePacks.downloaded { current, error ->
            reading = false
            if (current == null) {
                problem = error ?: "Yüklü paketler kontrol edilemedi."
                status = "Tekrar dene tuşuyla yeniden kontrol edebilirsiniz."
            } else {
                installed = current
                OfflineLanguagePacks.downloadable.forEach { pack ->
                    states[pack.code] = if (pack.code in current) "✓ İndirildi" else "Eksik"
                }
                downloadAll(current)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Çevrimdışı Dil Paketleri") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    "Wi-Fi üzerinden tek seferlik indirme. Yüklenen modeller cihazda " +
                        "kalır; sonraki çeviriler internet gerektirmez.",
                    fontSize = 13.sp
                )
                Text("İngilizce    ✓ Hazır (yerleşik)", fontSize = 13.sp)
                OfflineLanguagePacks.downloadable.forEach { pack ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(pack.name, fontSize = 14.sp)
                        Text(
                            states[pack.code] ?: if (pack.code in installed) "✓ İndirildi" else "Eksik",
                            fontSize = 12.sp
                        )
                    }
                }
                if (pending > 0 || reading) CircularProgressIndicator()
                Text(status, fontSize = 12.sp)
                if (problem.isNotBlank()) Text(problem, fontSize = 12.sp)
                Text(
                    "Otomatik çeviri dinî açıdan doğrulanmış Kur’an kelime meali değildir.",
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pending == 0 && !reading) {
                        reading = true
                        OfflineLanguagePacks.downloaded { current, error ->
                            reading = false
                            if (current != null) {
                                installed = current
                                downloadAll(current)
                            } else {
                                problem = error ?: "Model listesi okunamadı."
                            }
                        }
                    }
                },
                enabled = pending == 0 && !reading
            ) {
                Text("Eksikleri indir / yeniden dene")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}
