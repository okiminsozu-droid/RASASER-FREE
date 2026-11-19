package com.rasaser.free.rasaserwidgets

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * No online accounts, no biometric templates collected by the app.
 * PIN is stored only as a locally salted PBKDF2 hash, never plain text.
 * Biometrics are verified by Android's trusted system prompt.
 */
object AppSecurity {
    private const val PREFS = "varyok_app_security"
    private const val MODE = "security_mode"
    private const val SALT = "pin_salt"
    private const val DIGEST = "pin_digest"
    private const val ITERATIONS = 150_000
    const val MODE_PIN = "pin"
    const val MODE_BIOMETRIC = "biometric"
    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun mode(context: Context): String = preferences(context).getString(MODE, "") ?: ""
    fun isEnabled(context: Context): Boolean = mode(context) in setOf(MODE_PIN, MODE_BIOMETRIC)

    @Synchronized
    fun enablePin(context: Context, pin: String): Boolean {
        if (isEnabled(context) || pin.length !in 4..12 || pin.any { !it.isDigit() }) return false
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val derived = derive(pin, salt)
        return preferences(context).edit()
            .putString(SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(DIGEST, Base64.encodeToString(derived, Base64.NO_WRAP))
            .putString(MODE, MODE_PIN)
            .commit()
    }

    @Synchronized
    fun enableBiometric(context: Context): Boolean {
        if (isEnabled(context)) return false
        // Called ONLY after a successful system BiometricPrompt confirmation.
        return preferences(context).edit().putString(MODE, MODE_BIOMETRIC).commit()
    }

    fun checkPin(context: Context, pin: String): Boolean {
        if (mode(context) != MODE_PIN) return false
        return try {
            val prefs = preferences(context)
            val salt = Base64.decode(prefs.getString(SALT, ""), Base64.NO_WRAP)
            val expected = Base64.decode(prefs.getString(DIGEST, ""), Base64.NO_WRAP)
            if (salt.size < 16 || expected.size != 32) return false
            MessageDigest.isEqual(derive(pin, salt), expected)
        } catch (_: Exception) { false }
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}
