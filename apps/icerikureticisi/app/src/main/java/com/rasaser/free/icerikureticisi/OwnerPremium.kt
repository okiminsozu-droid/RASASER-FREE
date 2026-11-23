package com.rasaser.free.icerikureticisi

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * Offline Premium verification by owner-signed public-key voucher.
 * The signing private key is NEVER part of APK/GitHub.
 * Existing local HMAC receipts are preserved; no remote requests.
 */
object OwnerPremium {
    private const val PREF = "creator_premium"
    private const val KEY_ALIAS = "rasaser.creator.owner.premium.v1"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun installationId(context: Context): String {
        val p = prefs(context)
        p.getString("install", null)?.let { return it }
        val bytes = ByteArray(24).also { SecureRandom().nextBytes(it) }
        val id = Base64.encodeToString(
            bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        p.edit().putString("install", id).apply()
        return id
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
            "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            ).setDigests(KeyProperties.DIGEST_SHA256).build()
        )
        return generator.generateKey()
    }

    private fun seal(data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key())
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    fun active(context: Context): Boolean {
        val p = prefs(context)
        val payload = p.getString("receipt", null) ?: return false
        val signed = p.getString("receipt_hmac", null) ?: return false
        return try {
            if (!payload.startsWith(context.packageName + "|" + installationId(context) + "|")) {
                false
            } else {
                MessageDigest.isEqual(
                    seal(payload),
                    Base64.decode(signed, Base64.NO_WRAP)
                )
            }
        } catch (_: Exception) { false }
    }

    data class VerifyResult(val granted: Boolean, val message: String)

    /**
     * The voucher is signed OFFLINE by the owner's separate private key.
     * The APK only has a public key. Existing valid local Premium receipts remain valid.
     * No code, account identifier or installation identifier is sent anywhere.
     */
    suspend fun redeem(context: Context, input: String): VerifyResult =
        withContext(Dispatchers.Default) {
            if (!OfflinePremiumLicense.valid(input, context.packageName)) {
                return@withContext VerifyResult(false,
                    "Kod geçersiz, süresi dolmuş veya bu uygulama için düzenlenmemiş.")
            }
            return@withContext try {
                val installId = installationId(context)
                val receipt = context.packageName + "|" + installId + "|" +
                    System.currentTimeMillis() + "|owner_lifetime"
                val mac = Base64.encodeToString(seal(receipt), Base64.NO_WRAP)
                check(prefs(context).edit()
                    .putString("receipt", receipt)
                    .putString("receipt_hmac", mac)
                    .commit()) { "Premium kaydı diske yazılamadı." }
                VerifyResult(true, "Premium etkin • Çevrimdışı doğrulandı")
            } catch (_: Exception) {
                VerifyResult(false, "Cihazda Premium doğrulaması kaydedilemedi.")
            }
        }

    // Local-only build: no purchase website or external RaSaSeR account service.
    fun openGooglePlay(context: Context): Boolean = false
}

@Composable
fun OwnerPremiumDialog(context: Context, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(OwnerPremium.active(context)) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PremiumCrownMark(active = active, size = 50.dp)
                Text(tr("Premium"), style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column {
                Text(
                    if (active) tr("Premium etkin") else tr("Abone olun"),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = code, onValueChange = { code = it.take(1024) },
                    label = { Text(tr("Promosyon kodu")) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                TextButton(
                    enabled = !checking && code.isNotBlank(),
                    onClick = {
                        checking = true
                        scope.launch {
                            val r = OwnerPremium.redeem(context, code)
                            active = r.granted || OwnerPremium.active(context)
                            result = if (r.granted) Translations.text(AppLanguage.get(context), "Premium etkin") else r.message
                            code = ""
                            checking = false
                        }
                    }
                ) { Text(if (checking) "…" else tr("Doğrula")) }
                if (result.isNotBlank()) {
                    Text(result, color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Kapat")) } }
    )
}
