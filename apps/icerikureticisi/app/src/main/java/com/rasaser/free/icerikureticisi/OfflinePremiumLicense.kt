package com.rasaser.free.icerikureticisi

import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Offline public-key license verifier. Only the public EC P-256 key is in APK.
 * Signing PRIVATE key is strictly an external owner-held file, NEVER shipped.
 * No account identity, device ID or redeem code is transmitted to RaSaSeR.
 *
 * Code: RSRL1.<base64url JSON payload>.<base64url DER ECDSA signature>
 * ECDSA signed bytes: UTF8("RSRL1." + payloadB64url)
 */
object OfflinePremiumLicense {
    private const val PUBLIC_KEY_SPKI = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEKVJ7zq2Xh8bjOnxrJ91XiP4k4E1rSPMqSJ+2WR739zUnwCjLjdovEDFavK09n8/L5nsKjT4wo0U46+/9vdnqNA=="
    private val SERIAL = Regex("^[a-zA-Z0-9_-]{8,80}$")
    private val ENCODED = Regex("^[A-Za-z0-9_-]{8,800}$")

    fun valid(code: String, currentPackage: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val input = code.trim().replace(Regex("\\s+"), "")
        if (input.length !in 70..1024 || currentPackage.isBlank()) return false
        val chunks = input.split(".")
        if (chunks.size != 3 || chunks[0] != "RSRL1" ||
            !ENCODED.matches(chunks[1]) || !ENCODED.matches(chunks[2])) return false
        return try {
            val pub = KeyFactory.getInstance("EC").generatePublic(
                X509EncodedKeySpec(Base64.decode(PUBLIC_KEY_SPKI, Base64.DEFAULT))
            )
            val verified = Signature.getInstance("SHA256withECDSA").apply {
                initVerify(pub)
                update(("RSRL1." + chunks[1]).toByteArray(StandardCharsets.US_ASCII))
            }.verify(Base64.decode(chunks[2], Base64.URL_SAFE or Base64.NO_WRAP))
            if (!verified) return false

            val data = Base64.decode(chunks[1], Base64.URL_SAFE or Base64.NO_WRAP)
            if (data.size > 1024) return false
            val payload = JSONObject(String(data, Charsets.UTF_8))
            val expires = payload.getLong("expires")
            val issued = payload.getLong("issued")
            val nowSeconds = nowMillis / 1000L
            payload.getInt("v") == 1 &&
                payload.getString("scope") == "owner_lifetime" &&
                payload.getString("package").let { it == "*" || it == currentPackage } &&
                SERIAL.matches(payload.getString("serial")) &&
                issued > 0L && issued <= nowSeconds + 600L &&
                expires >= 0L && (expires == 0L || expires >= nowSeconds)
        } catch (_: Exception) { false }
    }
}
