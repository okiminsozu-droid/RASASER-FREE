package com.rasaser.free.rasaserwidgets

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec

/**
 * RaSaSeR OWNER PREMIUM — independently verifies a server-issued owner_lifetime
 * grant. Never trust a browser success screen or a caller-provided Gmail string.
 *
 * This APK contains NO private signing key, Google OAuth secret or password.
 * Only the public verification key may be embedded once the Cloudflare keypair
 * has been securely provisioned. Until then activation always fails CLOSED.
 */
object OwnerPremium {
    private const val KEY_ALIAS = "rasaser_owner_device_key_v1"
    private const val PREFS = "rasaser_verified_owner_grant"

    // PUBLIC verification key, NOT a secret. Fill only after Cloudflare generates
    // and permanently stores the paired PRIVATE signer outside public GitHub.
    private const val TRUSTED_SERVER_PUBLIC_KEY_SPKI_B64URL = ""

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    private fun decode(value: String): ByteArray =
        Base64.decode(value, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    fun isConfigured(): Boolean = TRUSTED_SERVER_PUBLIC_KEY_SPKI_B64URL.isNotBlank()

    private fun keyPair(): KeyPair {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val privateKey = store.getKey(KEY_ALIAS, null) as? java.security.PrivateKey
        val publicKey = store.getCertificate(KEY_ALIAS)?.publicKey
        if (privateKey != null && publicKey != null) return KeyPair(publicKey, privateKey)

        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore"
        )
        generator.initialize(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
        )
        return generator.generateKeyPair()
    }

    /** The AndroidKeystore PRIVATE key never leaves the phone. */
    fun devicePublicKey(): String = encode(keyPair().public.encoded)

    fun signSession(requestId: String, nonce: String): String {
        require(requestId.length in 32..64 && nonce.length in 32..64)
        val bytes = "rasaser-owner-v1|$requestId|$nonce".toByteArray(Charsets.UTF_8)
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(keyPair().private)
        signature.update(bytes)
        return encode(signature.sign())
    }

    fun isActive(context: Context): Boolean {
        if (!isConfigured()) return false
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("grant", null) ?: return false
        return verify(context, value)
    }

    fun acceptSignedGrant(context: Context, grant: String): Boolean {
        if (!verify(context, grant)) return false
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("grant", grant).commit()
    }

    private fun verify(context: Context, grant: String): Boolean = try {
        if (!isConfigured() || grant.length > 12_000) false
        else {
            val sections = grant.split('.')
            if (sections.size != 2 || sections.any { it.isBlank() }) false
            else {
                val message = sections[0]
                val payload = JSONObject(String(decode(message), Charsets.UTF_8))
                if (payload.optInt("v") != 1 ||
                    payload.optString("scope") != "owner_lifetime" ||
                    payload.optString("packageName") != context.packageName ||
                    payload.optString("devicePublicKey") != devicePublicKey()
                ) false
                else {
                    val signer = KeyFactory.getInstance("EC").generatePublic(
                        X509EncodedKeySpec(decode(TRUSTED_SERVER_PUBLIC_KEY_SPKI_B64URL))
                    )
                    val signature = Signature.getInstance("SHA256withECDSA")
                    signature.initVerify(signer)
                    signature.update(message.toByteArray(Charsets.US_ASCII))
                    signature.verify(decode(sections[1]))
                }
            }
        }
    } catch (_: Exception) {
        false
    }
}
