package com.rasaser.free.icerikureticisi

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import org.json.JSONObject
import java.net.URL
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.net.ssl.HttpsURLConnection

/**
 * Owner promo is validated ONLY on the private Cloudflare Worker, never against
 * a value/hash embedded into APK or GitHub. Activation is sealed by Android Keystore.
 * No plain promo code is persisted locally; offline entitlement is a signed local receipt.
 */
object OwnerPremium {
    private const val PREF = "creator_premium"
    private const val KEY_ALIAS = "rasaser.creator.owner.premium.v1"
    private const val ENDPOINT =
        "https://rasaser-bagimsiz-kullanici-girisi.okiminsozu.workers.dev/v1/promo/redeem"

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

    suspend fun redeem(context: Context, input: String): VerifyResult = withContext(Dispatchers.IO) {
        val code = input.trim().uppercase().replace(Regex("\\s+"), "")
        if (code.length !in 15..64) {
            return@withContext VerifyResult(false, "Geçerli promosyon kodunu girin.")
        }
        var connection: HttpsURLConnection? = null
        try {
            val id = installationId(context)
            val body = JSONObject().apply {
                put("packageName", context.packageName)
                put("installationId", id)
                put("code", code)
            }.toString()
            connection = (URL(ENDPOINT).openConnection() as HttpsURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12000
                readTimeout = 12000
                instanceFollowRedirects = false
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val content = (
                if (status in 200..299) connection.inputStream else connection.errorStream
            )?.bufferedReader()?.use { it.readText().take(4096) }.orEmpty()
            val response = JSONObject(content)
            val accepted = status == 200 &&
                response.optBoolean("ok", false) &&
                response.optBoolean("active", false) &&
                response.optString("scope") == "owner_lifetime" &&
                response.optString("packageName") == context.packageName &&
                response.optString("installationId") == id &&
                !response.optBoolean("renewalRequired", true)
            if (!accepted) {
                val reason = response.optString("error", "INVALID_CODE")
                val notice = when (reason) {
                    "APP_NOT_AUTHORIZED" -> "Bu uygulama henüz ortak promosyon servisine yetkilendirilmedi."
                    "INVALID_CODE_OR_RATE_LIMIT" -> "Kod geçersiz veya deneme limiti doldu."
                    "SERVICE_UNAVAILABLE" -> "Doğrulama servisi kullanılamıyor."
                    else -> "Promosyon doğrulanamadı."
                }
                return@withContext VerifyResult(false, notice)
            }
            val receipt = context.packageName + "|" + id + "|" +
                response.getLong("issuedAt") + "|owner_lifetime"
            val mac = Base64.encodeToString(seal(receipt), Base64.NO_WRAP)
            prefs(context).edit()
                .putString("receipt", receipt)
                .putString("receipt_hmac", mac).apply()
            VerifyResult(true, "Premium etkin")
        } catch (_: Exception) {
            VerifyResult(false, "Bağlantı veya güvenli doğrulama hatası. İnternetinizi kontrol edin.")
        } finally {
            connection?.disconnect()
        }
    }

    fun openGooglePlay(context: Context): Boolean {
        // The new app may not be published yet, so we search rather than invent a product page.
        val targets = listOf(
            "market://search?q=RaSaSeR",
            "https://play.google.com/store/search?q=RaSaSeR&c=apps"
        )
        for (target in targets) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (target.startsWith("market:")) setPackage("com.android.vending")
                })
                return true
            } catch (_: ActivityNotFoundException) {
                // Fall back from the native store to its website.
            } catch (_: SecurityException) {}
        }
        return false
    }
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
                    value = code, onValueChange = { code = it.take(64) },
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
