package com.rasaser.free.rasaserwidgets

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Shared RaSaSeR Google WEB owner login, invoked by seven taps on the brand.
 * Does not require a new Android OAuth/SHA-1 registration for each APK.
 *
 * Login uses the system browser; Google credentials are never collected here.
 * Premium activates ONLY after the Cloudflare server has approved Google OIDC,
 * verified Android Keystore possession and returned a cryptographically
 * validated package+device-bound owner_lifetime grant.
 */
class OwnerBrowserLogin(
    private val activity: Activity,
    private val onVerified: () -> Unit
) {
    private val endpoint = "https://rasaser-bagimsiz-kullanici-girisi.okiminsozu.workers.dev"
    private val prefs = activity.getSharedPreferences("rasaser_owner_pending", Context.MODE_PRIVATE)
    private var taps = 0
    private var firstTap = 0L
    @Volatile private var busy = false

    fun onBrandTap() {
        if (OwnerPremium.isActive(activity)) return
        val now = SystemClock.elapsedRealtime()
        if (firstTap == 0L || now - firstTap > 4_200L) {
            firstTap = now
            taps = 0
        }
        taps++
        if (taps >= 7) {
            taps = 0
            firstTap = 0L
            start()
        }
    }

    private fun clearPending() = prefs.edit().clear().apply()

    private fun toast(messageKey: String) {
        if (!activity.isFinishing && !activity.isDestroyed) {
            AlertDialog.Builder(activity)
                .setTitle(OwnerLoginText.t(activity, "title"))
                .setMessage(OwnerLoginText.t(activity, messageKey))
                .setPositiveButton("OK", null).show()
        }
    }

    private fun post(path: String, body: JSONObject): JSONObject {
        val url = URL(endpoint + path)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.instanceFollowRedirects = false
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Cache-Control", "no-store")
            conn.outputStream.use { stream ->
                stream.write(body.toString().toByteArray(Charsets.UTF_8))
            }
            if (conn.responseCode != 200) throw IllegalStateException("Owner service not ready")
            val response = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (response.length > 16_000) throw IllegalStateException("Invalid response size")
            val data = JSONObject(response)
            if (!data.optBoolean("ok")) throw IllegalStateException("Owner verification declined")
            return data
        } finally {
            conn.disconnect()
        }
    }

    private fun start() {
        if (busy || OwnerPremium.isActive(activity)) return
        // Fail closed until Cloudflare has a secure signer and only the PUBLIC
        // verification key has been explicitly installed in the Android source.
        if (!OwnerPremium.isConfigured()) {
            toast("inactive")
            return
        }
        busy = true
        Thread {
            try {
                val publicKey = OwnerPremium.devicePublicKey()
                val data = post("/v1/owner/browser/start", JSONObject()
                    .put("packageName", activity.packageName)
                    .put("devicePublicKey", publicKey))
                val id = data.getString("requestId")
                val nonce = data.getString("nonce")
                val expiry = data.getLong("expiresAtEpochSeconds")
                val uri = Uri.parse(data.getString("loginUrl"))
                require(id.length in 32..64 && nonce.length in 32..64)
                require(expiry > System.currentTimeMillis() / 1_000)
                require(uri.scheme == "https" &&
                    uri.host == "rasaser-bagimsiz-kullanici-girisi.okiminsozu.workers.dev" &&
                    uri.path == "/v1/owner/browser/authorize")
                prefs.edit().putString("id", id).putString("nonce", nonce)
                    .putLong("expiresAt", expiry).commit()
                activity.runOnUiThread {
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        try {
                            activity.startActivity(
                                Intent(Intent.ACTION_VIEW, uri)
                                    .addCategory(Intent.CATEGORY_BROWSABLE)
                            )
                        } catch (_: Exception) {
                            clearPending()
                            toast("failed")
                        }
                    }
                }
            } catch (_: Exception) {
                activity.runOnUiThread { toast("failed") }
            } finally {
                busy = false
            }
        }.start()
    }

    /** Called when user returns from the system Google browser to VARYOK. */
    fun onAppResumed() {
        if (busy || OwnerPremium.isActive(activity) || !OwnerPremium.isConfigured()) return
        val id = prefs.getString("id", null) ?: return
        val nonce = prefs.getString("nonce", null) ?: return
        if (prefs.getLong("expiresAt", 0L) <= System.currentTimeMillis() / 1_000) {
            clearPending()
            return
        }
        busy = true
        Thread {
            try {
                val signature = OwnerPremium.signSession(id, nonce)
                val proof = JSONObject()
                    .put("requestId", id).put("nonce", nonce)
                    .put("deviceSignature", signature)
                val state = post("/v1/owner/browser/status", proof)
                    .getString("status")
                if (state == "approved") {
                    val grant = post("/v1/owner/browser/finish", proof).getString("grant")
                    if (!OwnerPremium.acceptSignedGrant(activity, grant)) {
                        throw IllegalStateException("Grant not trusted")
                    }
                    clearPending()
                    activity.runOnUiThread {
                        if (!activity.isFinishing && !activity.isDestroyed) onVerified()
                    }
                } else if (state == "pending") {
                    activity.runOnUiThread {
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            AlertDialog.Builder(activity)
                                .setTitle(OwnerLoginText.t(activity, "title"))
                                .setMessage(OwnerLoginText.t(activity, "pending"))
                                .setPositiveButton(OwnerLoginText.t(activity, "check")) { _, _ ->
                                    onAppResumed()
                                }
                                .setNegativeButton(OwnerLoginText.t(activity, "later"), null)
                                .show()
                        }
                    }
                } else {
                    clearPending()
                    activity.runOnUiThread { toast("failed") }
                }
            } catch (_: Exception) {
                // Do not activate Premium on network, verification or signature errors.
                activity.runOnUiThread { toast("failed") }
            } finally {
                busy = false
            }
        }.start()
    }
}
