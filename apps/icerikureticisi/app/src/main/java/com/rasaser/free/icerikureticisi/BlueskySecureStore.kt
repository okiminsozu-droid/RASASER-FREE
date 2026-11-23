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

/** Public channel information only. No primary password is ever stored. */
data class BlueskyChannel(
    val id: String,
    val creatorId: Long,
    val did: String,
    val handle: String,
    val verifiedAt: Long
)

internal data class BlueskyCredential(
    val channel: BlueskyChannel,
    val accessJwt: String,
    val refreshJwt: String
)

/** Phone-only AES-256-GCM session vault, isolated from Telegram and local SQLite. */
object BlueskySecureStore {
    private const val KEY_ALIAS = "rasaser.icerikureticisi.bluesky.aes.v1"
    private const val PREFERENCES = "rasaser_bluesky_v1"
    private const val ENTRY = "accounts"
    private val lock = Any()

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply {
                init(KeyGenParameterSpec.Builder(
                    KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build())
            }.generateKey()
    }

    private fun load(context: Context): JSONArray {
        val value = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(ENTRY, null) ?: return JSONArray()
        return try {
            val bytes = Base64.decode(value, Base64.NO_WRAP)
            require(bytes.size > 28)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(
                128, bytes.copyOfRange(0, 12)
            ))
            JSONArray(String(cipher.doFinal(bytes, 12, bytes.size - 12), Charsets.UTF_8))
        } catch (_: Exception) {
            // Never replace a vault when the Keystore key is unavailable.
            throw IllegalStateException("Bluesky bağlantıları çözülemedi. Mevcut kayıtlar korunuyor.")
        }
    }

    private fun save(context: Context, content: JSONArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val bytes = cipher.iv + cipher.doFinal(
            content.toString().toByteArray(Charsets.UTF_8)
        )
        val value = Base64.encodeToString(bytes, Base64.NO_WRAP)
        check(context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit().putString(ENTRY, value).commit()) {
            "Şifreli Bluesky hesabı kaydedilemedi."
        }
    }

    private fun meta(j: JSONObject): BlueskyChannel = BlueskyChannel(
        j.getString("id"),
        j.getLong("creator_id"),
        j.getString("did"),
        j.getString("handle"),
        j.getLong("verified_at")
    )

    fun channels(context: Context, creatorId: Long): List<BlueskyChannel> =
        synchronized(lock) {
            val all = load(context)
            buildList {
                for (i in 0 until all.length()) {
                    val j = all.getJSONObject(i)
                    if (j.getLong("creator_id") == creatorId) add(meta(j))
                }
            }
        }

    internal fun credential(
        context: Context, creatorId: Long, localId: String
    ): BlueskyCredential? = synchronized(lock) {
        val all = load(context)
        for (i in 0 until all.length()) {
            val j = all.getJSONObject(i)
            if (j.getLong("creator_id") == creatorId && j.getString("id") == localId) {
                return@synchronized BlueskyCredential(
                    meta(j), j.getString("access_jwt"), j.getString("refresh_jwt")
                )
            }
        }
        null
    }

    fun upsert(
        context: Context, creatorId: Long, session: BlueskySession
    ): BlueskyChannel = synchronized(lock) {
        require(creatorId >= 0 && session.did.startsWith("did:"))
        val all = load(context)
        val updated = JSONArray()
        var previousId: String? = null
        for (i in 0 until all.length()) {
            val j = all.getJSONObject(i)
            if (j.getLong("creator_id") == creatorId && j.getString("did") == session.did) {
                previousId = j.getString("id")
            } else updated.put(j)
        }
        val item = BlueskyChannel(
            previousId ?: UUID.randomUUID().toString(),
            creatorId, session.did, session.handle, System.currentTimeMillis()
        )
        updated.put(JSONObject().apply {
            put("id", item.id)
            put("creator_id", creatorId)
            put("did", item.did)
            put("handle", item.handle)
            put("verified_at", item.verifiedAt)
            put("access_jwt", session.accessJwt)
            put("refresh_jwt", session.refreshJwt)
        })
        save(context, updated)
        item
    }

    internal fun renew(
        context: Context, creatorId: Long, localId: String, session: BlueskySession
    ): Boolean = synchronized(lock) {
        val all = load(context)
        var modified = false
        val updated = JSONArray()
        for (i in 0 until all.length()) {
            val j = all.getJSONObject(i)
            if (j.getLong("creator_id") == creatorId && j.getString("id") == localId) {
                require(j.getString("did") == session.did) {
                    "Yenilenen oturum farklı Bluesky hesabına ait."
                }
                j.put("access_jwt", session.accessJwt)
                j.put("refresh_jwt", session.refreshJwt)
                j.put("handle", session.handle)
                j.put("verified_at", System.currentTimeMillis())
                modified = true
            }
            updated.put(j)
        }
        if (modified) save(context, updated)
        modified
    }

    fun remove(context: Context, creatorId: Long, localId: String): Boolean =
        synchronized(lock) {
            val all = load(context)
            val updated = JSONArray()
            var removed = false
            for (i in 0 until all.length()) {
                val j = all.getJSONObject(i)
                if (j.getLong("creator_id") == creatorId && j.getString("id") == localId) {
                    removed = true
                } else updated.put(j)
            }
            if (removed) save(context, updated)
            removed
        }

    fun removeForCreator(context: Context, creatorId: Long) = synchronized(lock) {
        val all = load(context)
        val updated = JSONArray()
        for (i in 0 until all.length()) {
            val j = all.getJSONObject(i)
            if (j.getLong("creator_id") != creatorId) updated.put(j)
        }
        save(context, updated)
    }
}
