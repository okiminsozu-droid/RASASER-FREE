package com.rasaser.free.icerikureticisi

import android.content.Context
import android.os.Build
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PIN never leaves the phone and is never stored in plaintext.
 * PBKDF2 salt/hash live in private preferences, excluded from Android backups.
 * A 5-failure local delay limits online guessing of the PIN.
 */
object AppLock {
    private const val PREF = "creator_lock"
    private const val ITERATIONS = 180000
    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun enabled(context: Context): Boolean =
        prefs(context).getString("hash", null) != null

    fun biometricEnabled(context: Context): Boolean =
        enabled(context) && prefs(context).getBoolean("biometric", false)

    fun setBiometric(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean("biometric", value && enabled(context)).apply()
    }

    fun setPin(context: Context, pin: String) {
        require(pin.length in 6..12 && pin.all { it.isDigit() }) {
            "PIN 6-12 rakam olmalı."
        }
        val salt = ByteArray(24).also { SecureRandom().nextBytes(it) }
        val hash = derive(pin, salt)
        prefs(context).edit()
            .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .putInt("failures", 0).putLong("locked_until", 0L)
            .putBoolean("biometric", false)
            .apply()
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
        } finally { spec.clearPassword() }
    }

    fun secondsRemaining(context: Context): Long {
        val remaining = prefs(context).getLong("locked_until", 0L) -
            System.currentTimeMillis()
        return if (remaining > 0) (remaining + 999) / 1000 else 0L
    }

    fun verify(context: Context, pin: String): Boolean {
        if (!enabled(context) || secondsRemaining(context) > 0) return false
        val p = prefs(context)
        return try {
            val salt = Base64.decode(p.getString("salt", "").orEmpty(), Base64.DEFAULT)
            val expected = Base64.decode(p.getString("hash", "").orEmpty(), Base64.DEFAULT)
            val valid = MessageDigest.isEqual(expected, derive(pin, salt))
            if (valid) {
                p.edit().putInt("failures", 0).putLong("locked_until", 0L).apply()
            } else {
                val failures = p.getInt("failures", 0) + 1
                val lockUntil = if (failures >= 5) System.currentTimeMillis() + 30000L else 0L
                p.edit().putInt("failures", if (failures >= 5) 0 else failures)
                    .putLong("locked_until", lockUntil).apply()
            }
            valid
        } catch (_: Exception) { false }
    }

    fun disable(context: Context, currentPin: String): Boolean {
        if (!verify(context, currentPin)) return false
        prefs(context).edit().clear().apply()
        return true
    }
}

object FingerprintGate {
    fun available(activity: FragmentActivity): Boolean =
        BiometricManager.from(activity).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!available(activity)) {
            onError("Parmak izi cihazda etkin değil.")
            return
        }
        val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle("RaSaSeR İçerik Üreticisi")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText(Translations.text(AppLanguage.get(activity), "PIN kullan"))
            .build()
        prompt.authenticate(info)
    }
}

@Composable
fun UnlockScreen(activity: FragmentActivity, onUnlock: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var launched by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!launched && AppLock.biometricEnabled(activity) &&
            FingerprintGate.available(activity)) {
            launched = true
            FingerprintGate.authenticate(activity, "Parmak iziyle aç", onUnlock) {
                message = it
            }
        }
    }
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 64.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(tr("RaSaSeR • İçerik Üreticisi"), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(22.dp))
            Text(tr("Uygulama kilidi"), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = pin, onValueChange = { pin = it.filter(Char::isDigit).take(12) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(tr("PIN gir")) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true
            )
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                if (AppLock.verify(activity, pin)) onUnlock()
                else {
                    val seconds = AppLock.secondsRemaining(activity)
                    message = if (seconds > 0) "Çok fazla deneme: $seconds saniye sonra yeniden deneyin."
                        else "PIN yanlış."
                    pin = ""
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(tr("Kilidi aç")) }
            if (AppLock.biometricEnabled(activity) &&
                FingerprintGate.available(activity)) {
                TextButton(onClick = {
                    FingerprintGate.authenticate(activity, "Parmak iziyle aç", onUnlock) {
                        message = it
                    }
                }) { Text(tr("Parmak iziyle aç")) }
            }
            Text(
                "PIN unutulursa ve parmak izi kullanılamazsa uygulama verilerini silmeden " +
                    "güvenli bir sıfırlama yapılamaz.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun AppLockSettings(activity: FragmentActivity, onDismiss: () -> Unit) {
    var requiresOldPin by remember { mutableStateOf(AppLock.enabled(activity)) }
    var oldPin by remember { mutableStateOf("") }
    var verified by remember { mutableStateOf(!requiresOldPin) }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isEnabled by remember { mutableStateOf(AppLock.enabled(activity)) }
    var bio by remember { mutableStateOf(AppLock.biometricEnabled(activity)) }
    val fingerprintTitle = tr("Parmak iziyle aç")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Uygulama kilidi")) },
        text = {
            Column {
                if (!verified) {
                    Text(tr("Değişiklik için mevcut PIN'i doğrulayın."))
                    OutlinedTextField(oldPin, { oldPin = it.filter(Char::isDigit).take(12) },
                        label = { Text(tr("PIN gir")) }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                    Button(onClick = {
                        verified = AppLock.verify(activity, oldPin)
                        if (!verified) message = "PIN yanlış veya geçici kilit etkin."
                    }) { Text(tr("Doğrula")) }
                } else {
                    Text(tr(if (isEnabled) "Şifreyi değiştir" else "Şifre oluştur"))
                    if (!isEnabled) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            tr("Önce PIN oluşturun; ardından telefonunuzun parmak izi özelliğini etkinleştirin."),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(12) },
                        label = { Text(tr("Yeni PIN")) }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                    OutlinedTextField(confirm, { confirm = it.filter(Char::isDigit).take(12) },
                        label = { Text(tr("PIN tekrarı")) }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                    Button(onClick = {
                        if (pin.length !in 6..12 || pin != confirm) {
                            message = "PIN 6-12 rakam olmalı ve iki PIN aynı olmalı."
                        } else {
                            AppLock.setPin(activity, pin)
                            isEnabled = true
                            bio = false
                            pin = ""; confirm = ""
                            message = if (FingerprintGate.available(activity))
                                "PIN kaydedildi. Şimdi aşağıdan parmak iziyle açmayı etkinleştirebilirsiniz."
                            else "PIN kaydedildi. Bir sonraki açılışta istenecek."
                        }
                    }) { Text(tr("Kaydet")) }
                    if (isEnabled) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tr("Parmak iziyle aç"), modifier = Modifier.weight(1f))
                            Switch(checked = bio, enabled = FingerprintGate.available(activity),
                                onCheckedChange = { enabled ->
                                    if (!enabled) {
                                        AppLock.setBiometric(activity, false)
                                        bio = false
                                        message = "Parmak izi devre dışı."
                                    } else {
                                        FingerprintGate.authenticate(
                                            activity = activity,
                                            title = fingerprintTitle,
                                            onSuccess = {
                                                AppLock.setBiometric(activity, true)
                                                bio = AppLock.biometricEnabled(activity)
                                                message = "Parmak izi etkinleştirildi."
                                            },
                                            onError = { message = it }
                                        )
                                    }
                                })
                        }
                        if (!FingerprintGate.available(activity)) Text(
                            "Bu cihazda güçlü parmak izi hazır değil.",
                            style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = {
                            if (AppLock.disable(activity, oldPin)) {
                                onDismiss()
                            } else {
                                message = "Kilidi kapatmak için mevcut PIN'i yeniden girin."
                                verified = false
                            }
                        }) { Text(tr("Kilidi kapat")) }
                    }
                }
                if (message.isNotBlank()) Text(
                    message, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Kapat")) } }
    )
}
