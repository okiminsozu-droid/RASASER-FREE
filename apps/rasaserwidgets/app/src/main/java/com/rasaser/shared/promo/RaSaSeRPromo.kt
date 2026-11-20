package com.rasaser.shared.promo

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.text.InputFilter
import android.text.InputType
import android.util.Base64
import android.widget.EditText
import android.widget.Toast
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * Shared RaSaSeR universal promotion entitlement for all Android apps.
 *
 * One private promo code is verified ONCE online by the RaSaSeR D1 backend;
 * neither code nor its SHA-256 hash is shipped in the APK.
 * The successful result is then protected with an AndroidKeyStore non-exportable
 * HMAC key. Subsequent launches verify it offline (unlimited validity).
 *
 * This is a free publisher-granted entitlement, NOT a Play subscription.
 * A copied SharedPreferences file cannot activate another installation.
 *
 * Each newly registered RaSaSeR app must add its own packageName to the server
 * allowlist and snapshot this source + Premium UI integration into that app.
 */
object RaSaSeRPromo {
    private const val SERVER = "https://rasaser-bagimsiz-kullanici-girisi.okiminsozu.workers.dev"
    private const val PREFS = "rasaser_promo_entitlement_v1"
    private const val HMAC_ALIAS = "rasaser_universal_promo_hmac_v1"
    private const val FIELD_ID = "install_id"
    private const val FIELD_DATA = "grant"
    private const val FIELD_MAC = "mac"

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private fun decode(input: String): ByteArray =
        Base64.decode(input, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    private fun installationId(ctx: Context): String {
        val preferences = prefs(ctx)
        preferences.getString(FIELD_ID, null)?.let { return it }
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        val id = encode(bytes)
        if (!preferences.edit().putString(FIELD_ID, id).commit())
            throw IllegalStateException("Cannot save installation ID")
        return id
    }

    private fun key(create: Boolean): SecretKey? {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(HMAC_ALIAS, null) as? SecretKey)?.let { return it }
        if (!create) return null
        val maker = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
            "AndroidKeyStore")
        maker.init(KeyGenParameterSpec.Builder(
            HMAC_ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        ).setDigests(KeyProperties.DIGEST_SHA256).build())
        return maker.generateKey()
    }

    @Synchronized
    private fun mac(payload: String, create: Boolean): ByteArray? {
        val secret = key(create) ?: return null
        val engine = Mac.getInstance("HmacSHA256")
        engine.init(secret)
        return engine.doFinal(payload.toByteArray(Charsets.UTF_8))
    }

    /**
     * Free owner promotion: allow offline creation only if a successful
     * Cloudflare redemption was sealed on this same device and app package.
     */
    fun isActive(ctx: Context): Boolean {
        return try {
        val data = prefs(ctx).getString(FIELD_DATA, null) ?: return false
        val signature = prefs(ctx).getString(FIELD_MAC, null) ?: return false
        val fields = data.split('|')
        if (fields.size != 4 ||
            fields[0] != "v1" ||
            fields[1] != ctx.packageName ||
            fields[2] != prefs(ctx).getString(FIELD_ID, null) ||
            fields[3] != "owner_lifetime") return false
        val actual = mac(data, false) ?: return false
        MessageDigest.isEqual(actual, decode(signature))
        } catch (_: Exception) { false }
    }

    @Synchronized
    private fun seal(ctx: Context, packageName: String, installId: String): Boolean {
        if(packageName!=ctx.packageName || installId!=installationId(ctx)) return false
        val payload = "v1|$packageName|$installId|owner_lifetime"
        val tag = mac(payload, true) ?: return false
        return prefs(ctx).edit()
            .putString(FIELD_DATA, payload)
            .putString(FIELD_MAC, encode(tag))
            .commit()
    }

    /** Network is used ONLY on first code activation, not on normal use. */
    fun redeem(activity: Activity, value: String, onFinished: (Boolean, String) -> Unit) {
        val code = value.trim().uppercase(Locale.ROOT).replace(Regex("\\s+"), "")
        val language = Locale.getDefault().language
        val messages = labels(language)
        if (!code.matches(Regex("RSR(-[A-HJ-NP-Z2-9]{6}){4}"))) {
            onFinished(false, messages[3])
            return
        }
        Thread {
            var conn: HttpURLConnection? = null
            val result: Pair<Boolean, String> = try {
                val id = installationId(activity.applicationContext)
                val url = URL("$SERVER/v1/promo/redeem")
                conn = url.openConnection() as HttpURLConnection
                conn!!.requestMethod = "POST"
                conn!!.connectTimeout = 10_000
                conn!!.readTimeout = 15_000
                conn!!.instanceFollowRedirects = false
                conn!!.setRequestProperty("Content-Type", "application/json")
                conn!!.setRequestProperty("Accept", "application/json")
                conn!!.setRequestProperty("Cache-Control", "no-store")
                conn!!.doOutput = true
                val payload = JSONObject()
                    .put("packageName", activity.packageName)
                    .put("installationId", id)
                    .put("code", code)
                conn!!.outputStream.use {
                    it.write(payload.toString().toByteArray(Charsets.UTF_8))
                }
                if (conn!!.responseCode != 200) {
                    Pair(false, if (conn!!.responseCode == 429) messages[4] else messages[3])
                } else {
                    val response = conn!!.inputStream.bufferedReader(Charsets.UTF_8).use {
                        it.readText()
                    }
                    if (response.length > 2048) throw IllegalStateException("Unexpected reply")
                    val verified = JSONObject(response)
                    val allowed = verified.optBoolean("ok") &&
                        verified.optBoolean("active") &&
                        verified.optString("scope") == "owner_lifetime" &&
                        verified.optString("packageName") == activity.packageName &&
                        verified.optString("installationId") == id
                    if (allowed && seal(activity.applicationContext,activity.packageName,id))
                        Pair(true,messages[2])
                    else Pair(false,messages[3])
                }
            } catch (_: Exception) {
                Pair(false,messages[5])
            } finally {
                conn?.disconnect()
            }
            activity.runOnUiThread {
                if(!activity.isFinishing && !activity.isDestroyed)
                    onFinished(result.first,result.second)
            }
        }.start()
    }

    /**
     * One accessible, named UI entry, not a hidden tap gesture.
     * language is the APP language, not necessarily the Android OS language.
     */
    fun showCodeDialog(activity: Activity, language: String, onActivated: () -> Unit) {
        val t = labels(language)
        val input = EditText(activity).apply {
            hint = "RSR-XXXXXX-XXXXXX-XXXXXX-XXXXXX"
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            filters = arrayOf(InputFilter.LengthFilter(60))
            setSingleLine(true)
            setPadding(38,22,38,22)
        }
        val alert = AlertDialog.Builder(activity)
            .setTitle(t[0])
            .setView(input)
            .setPositiveButton(t[1]) { _, _ ->
                redeem(activity,input.text.toString()) { ok,msg ->
                    Toast.makeText(activity,msg,Toast.LENGTH_LONG).show()
                    if(ok) onActivated()
                }
            }.setNegativeButton(t[6],null).create()
        alert.show()
    }

    fun title(language: String): String = labels(language)[0]

    private fun labels(language: String): List<String> = when(language) {
        "tr" -> listOf("Promosyon Kodu Gir","Etkinleştir","Premium süresiz etkinleştirildi.",
            "Promosyon kodu geçersiz.","Çok fazla deneme. Daha sonra tekrar deneyin.",
            "Bağlantı kurulamadı. İnternetinizi kontrol edin.","İptal")
        "en" -> listOf("Enter Promo Code","Activate","Lifetime Premium activated.",
            "Invalid promo code.","Too many attempts. Try later.",
            "Cannot connect. Check your internet connection.","Cancel")
        "de" -> listOf("Promocode eingeben","Aktivieren","Premium dauerhaft aktiviert.",
            "Ungültiger Promocode.","Zu viele Versuche. Später erneut versuchen.",
            "Verbindung fehlgeschlagen. Internet prüfen.","Abbrechen")
        "es" -> listOf("Introducir código promocional","Activar","Premium activado de por vida.",
            "Código no válido.","Demasiados intentos. Vuelve más tarde.",
            "Sin conexión. Comprueba internet.","Cancelar")
        "ja" -> listOf("プロモーションコードを入力","有効化","Premiumを無期限で有効にしました。",
            "コードが無効です。","試行回数が多すぎます。後でもう一度お試しください。",
            "接続できません。インターネットを確認してください。","キャンセル")
        "ar" -> listOf("أدخل الرمز الترويجي","تفعيل","تم تفعيل Premium مدى الحياة.",
            "الرمز غير صالح.","محاولات كثيرة. حاول لاحقًا.",
            "فشل الاتصال. تحقق من الإنترنت.","إلغاء")
        else -> labels("en")
    }
}
