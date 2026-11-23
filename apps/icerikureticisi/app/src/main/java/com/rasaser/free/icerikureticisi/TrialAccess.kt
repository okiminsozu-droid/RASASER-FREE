package com.rasaser.free.icerikureticisi

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import kotlin.math.max

/** Offline-first fifteen-day trial. This is not a server-side licensing guarantee. */
object TrialWindow {
    const val DAY_MS: Long = 24L * 60 * 60 * 1000
    const val DURATION_MS: Long = 15L * DAY_MS

    fun daysLeft(startMillis: Long, observedMillis: Long): Int {
        if (startMillis <= 0L || observedMillis < startMillis) return 0
        val elapsed = observedMillis - startMillis
        if (elapsed >= DURATION_MS) return 0
        return ((DURATION_MS - elapsed + DAY_MS - 1) / DAY_MS).toInt()
    }
}

object TrialAccess {
    private const val PREF = "rasaser_creator_trial_v1"
    private const val KEY_ALIAS = "rasaser.creator.trial.hmac.v1"
    private const val CLOCK_TOLERANCE_MS = 2L * 60 * 1000

    data class Status(val remainingDays: Int, val clockInvalid: Boolean = false) {
        val expired: Boolean get() = remainingDays == 0
    }

    private fun secret(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore")
            .apply {
                init(KeyGenParameterSpec.Builder(
                    KEY_ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                ).setDigests(KeyProperties.DIGEST_SHA256).build())
            }.generateKey()
    }

    private fun signature(body: String): ByteArray =
        Mac.getInstance("HmacSHA256").apply { init(secret()) }
            .doFinal(body.toByteArray(Charsets.UTF_8))

    private fun save(context: Context, start: Long, lastSeen: Long) {
        val body = "$start|$lastSeen"
        val seal = Base64.encodeToString(signature(body), Base64.NO_WRAP)
        check(context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("trial", body).putString("seal", seal).commit())
    }

    @Synchronized
    fun status(context: Context): Status {
        return try {
            val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val body = prefs.getString("trial", null)
            if (body == null) {
                // Creating the first receipt is the only moment the trial begins.
                // If the seal is missing but a trial existed, fail closed.
                if (prefs.contains("seal")) return Status(0, clockInvalid = true)
                save(context, now, now)
                return Status(15)
            }
            val seal = prefs.getString("seal", null) ?: return Status(0, true)
            val valid = MessageDigest.isEqual(
                signature(body), Base64.decode(seal, Base64.DEFAULT))
            if (!valid) return Status(0, true)
            val parts = body.split("|")
            if (parts.size != 2) return Status(0, true)
            val start = parts[0].toLongOrNull() ?: return Status(0, true)
            val lastSeen = parts[1].toLongOrNull() ?: return Status(0, true)
            if (start <= 0L || lastSeen < start || now < lastSeen - CLOCK_TOLERANCE_MS) {
                return Status(0, true)
            }
            val observed = max(now, lastSeen)
            if (observed > lastSeen) save(context, start, observed)
            Status(TrialWindow.daysLeft(start, observed))
        } catch (_: Exception) {
            Status(0, clockInvalid = true)
        }
    }
}

@Composable
fun TrialExpiredScreen(onActivated: () -> Unit) {
    val context = LocalContext.current
    var showPremium by remember { mutableStateOf(true) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF7F1E7)) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PremiumCrownMark(active = false, size = 78.dp)
                Spacer(Modifier.height(16.dp))
                Text("RaSaSeR PREMIUM",
                    color = Color(0xFF8E6731),
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    tr("Deneme süresi doldu"),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF262521),
                    textAlign = TextAlign.Center
                )
                Text(
                    tr("15 günlük ücretsiz denemeniz tamamlandı. İçerikleriniz cihazınızda saklanmaya devam eder."),
                    modifier = Modifier.padding(vertical = 22.dp),
                    color = Color(0xFF55524D),
                    textAlign = TextAlign.Center
                )
                Button(onClick = { showPremium = true }) {
                    Text(tr("Premium'u etkinleştir"))
                }
            }
        }
        if (showPremium) OwnerPremiumDialog(context, onDismiss = {
            showPremium = false
            onActivated()
        })
    }
}
