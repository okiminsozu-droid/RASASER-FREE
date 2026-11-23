package com.rasaser.free.icerikureticisi

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Independent private vault for Meta, Google and X OAuth grants.
 *
 * IMPORTANT: this vault is infrastructure only; it does NOT grant API permission.
 * The provider must first validate a real OAuth callback and remotely verify the
 * channel ID. Never populate this vault from a pasted URL, password or API key.
 *
 * No developer-owned server, analytics endpoint or password storage is involved.
 */
data class DeviceOAuthAccount(
    val id: String,
    val creatorId: Long,
    val provider: String,
    val remoteAccountId: String,
    val displayName: String,
    val scopes: Set<String>,
    val expiresAtMillis: Long,
    val lastVerifiedMillis: Long,
    val authorizationRequired: Boolean
)

internal data class DeviceOAuthCredential(
    val account: DeviceOAuthAccount,
    val accessToken: String,
    val refreshToken: String
)

internal data class ProviderVerifiedSession(
    val provider: String,
    val remoteAccountId: String,
    val displayName: String,
    val scopes: Set<String>,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long
) {
    init {
        require(provider in setOf("Facebook", "Instagram", "X", "YouTube"))
        require(remoteAccountId.isNotBlank() && remoteAccountId.length <= 256)
        require(displayName.isNotBlank() && displayName.length <= 140)
        require(accessToken.isNotBlank() && accessToken.length <= 16384)
        require(refreshToken.length <= 16384)
        require(scopes.size in 1..30 && scopes.all { it.length in 1..200 })
        require(expiresAtMillis > System.currentTimeMillis())
    }
}

/**
 * Android Keystore AES-256-GCM with random per-write IV. Unreadable ciphertext
 * is NEVER deleted or silently replaced, preventing accidental credential loss.
 */
object DeviceOAuthVault {
    private const val PREF = "rasaser_provider_oauth_v1"
    private const val CONTENT = "encrypted_accounts"
    private const val KEY = "rasaser.creator.oauth.accounts.aes.v1"
    private val lock = Any()
    private val providers = setOf("Facebook", "Instagram", "X", "YouTube")

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .setRandomizedEncryptionRequired(true)
                        .build()
                )
            }.generateKey()
    }

    private fun read(context: Context): JSONArray {
        val stored = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(CONTENT, null) ?: return JSONArray()
        return try {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            require(bytes.size > 28)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE, key(),
                GCMParameterSpec(128, bytes.copyOfRange(0, 12))
            )
            JSONArray(String(cipher.doFinal(bytes, 12, bytes.size - 12), Charsets.UTF_8))
        } catch (_: Exception) {
            throw IllegalStateException("Şifreli sosyal hesap kasası açılamadı; hesaplar korunuyor.")
        }
    }

    private fun save(context: Context, array: JSONArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val bytes = cipher.iv + cipher.doFinal(array.toString().toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        check(
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putString(CONTENT, encoded).commit()
        ) { "Bağlantı kasası diske kaydedilemedi." }
    }

    private fun meta(row: JSONObject): DeviceOAuthAccount =
        DeviceOAuthAccount(
            id = row.getString("id"),
            creatorId = row.getLong("creator_id"),
            provider = row.getString("provider"),
            remoteAccountId = row.getString("remote_account_id"),
            displayName = row.getString("display_name"),
            scopes = buildSet {
                val arr = row.getJSONArray("scopes")
                for (i in 0 until arr.length()) add(arr.getString(i))
            },
            expiresAtMillis = row.getLong("expires_at"),
            lastVerifiedMillis = row.getLong("last_verified_at"),
            authorizationRequired = row.optBoolean("reauthorization_required", false)
        )

    fun accounts(context: Context, creatorId: Long): List<DeviceOAuthAccount> =
        synchronized(lock) {
            val rows = read(context)
            buildList {
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    if (row.getLong("creator_id") == creatorId) add(meta(row))
                }
            }
        }

    internal fun credential(
        context: Context, creatorId: Long, id: String
    ): DeviceOAuthCredential? = synchronized(lock) {
        val rows = read(context)
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (row.getString("id") == id && row.getLong("creator_id") == creatorId) {
                return@synchronized DeviceOAuthCredential(
                    meta(row), row.getString("access"), row.optString("refresh")
                )
            }
        }
        null
    }

    /** Must only be called after remote OAuth code exchange AND /me identity verification. */
    internal fun persistVerified(
        context: Context, creatorId: Long, grant: ProviderVerifiedSession
    ): DeviceOAuthAccount = synchronized(lock) {
        require(creatorId >= 0)
        require(grant.provider in providers)
        val rows = read(context)
        val updated = JSONArray()
        var id: String? = null
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (row.getLong("creator_id") == creatorId &&
                row.getString("provider") == grant.provider &&
                row.getString("remote_account_id") == grant.remoteAccountId) {
                id = row.getString("id")
            } else updated.put(row)
        }
        val now = System.currentTimeMillis()
        val account = DeviceOAuthAccount(
            id ?: UUID.randomUUID().toString(), creatorId,
            grant.provider, grant.remoteAccountId, grant.displayName,
            grant.scopes, grant.expiresAtMillis, now, false
        )
        updated.put(JSONObject().apply {
            put("id", account.id)
            put("creator_id", creatorId)
            put("provider", account.provider)
            put("remote_account_id", account.remoteAccountId)
            put("display_name", account.displayName)
            put("scopes", JSONArray(grant.scopes.sorted()))
            put("expires_at", account.expiresAtMillis)
            put("last_verified_at", now)
            put("reauthorization_required", false)
            put("access", grant.accessToken)
            put("refresh", grant.refreshToken)
        })
        save(context, updated)
        account
    }

    /** Provider refused refresh or revoked access. Keep record, request consent again. */
    fun markReauthorizationRequired(
        context: Context, creatorId: Long, id: String
    ): Boolean = synchronized(lock) {
        val rows = read(context)
        var updated = false
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (row.getLong("creator_id") == creatorId && row.getString("id") == id) {
                row.put("reauthorization_required", true)
                updated = true
            }
        }
        if (updated) save(context, rows)
        updated
    }

    fun remove(context: Context, creatorId: Long, id: String): Boolean = synchronized(lock) {
        val rows = read(context)
        val filtered = JSONArray()
        var removed = false
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (row.getLong("creator_id") == creatorId && row.getString("id") == id) {
                removed = true
            } else filtered.put(row)
        }
        if (removed) save(context, filtered)
        removed
    }

    fun removeForCreator(context: Context, creatorId: Long) = synchronized(lock) {
        val rows = read(context)
        val filtered = JSONArray()
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (row.getLong("creator_id") != creatorId) filtered.put(row)
        }
        save(context, filtered)
    }

    fun needsRefresh(account: DeviceOAuthAccount, nowMillis: Long): Boolean =
        !account.authorizationRequired && account.expiresAtMillis <= nowMillis + 60_000
}
