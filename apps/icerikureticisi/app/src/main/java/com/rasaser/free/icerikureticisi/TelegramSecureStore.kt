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

/** Telegram channels owned by distinct creators. Never show or log bot tokens. */
data class TelegramChannel(
    val id: String,
    val creatorId: Long,
    val channelId: String,
    val channelTitle: String,
    val botUsername: String,
    val verifiedAt: Long
)

internal data class TelegramCredential(val channel: TelegramChannel, val token: String)

/**
 * Device-only encrypted credential vault.
 * Keystore AES-GCM key cannot be extracted; app backups are disabled in manifest.
 * Not a cloud session: phone must be running for manual Telegram publishing.
 */
object TelegramSecureStore {
    private const val PREF_NAME = "rasaser_telegram_v1"
    private const val KEY_ALIAS = "rasaser.icerikureticisi.telegram.aes.v1"
    private const val ENTRY = "encrypted_channels"
    private val monitor = Any()

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setRandomizedEncryptionRequired(true)
                        .build()
                )
            }.generateKey()
    }

    private fun read(context: Context): JSONArray {
        val encoded = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(ENTRY, null) ?: return JSONArray()
        try {
            val blob = Base64.decode(encoded, Base64.NO_WRAP)
            check(blob.size > 12 + 16)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, blob.copyOfRange(0, 12)))
            return JSONArray(String(cipher.doFinal(blob, 12, blob.size - 12), Charsets.UTF_8))
        } catch (_: Exception) {
            // Fail closed: never overwrite an undecryptable vault.
            throw IllegalStateException("Güvenli bağlantı kasası açılamadı. Kayıtlar korunuyor.")
        }
    }

    private fun write(context: Context, channels: JSONArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val ciphertext = cipher.doFinal(channels.toString().toByteArray(Charsets.UTF_8))
        val blob = cipher.iv + ciphertext
        val encoded = Base64.encodeToString(blob, Base64.NO_WRAP)
        check(context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(ENTRY, encoded).commit()) {
            "Şifreli bağlantılar kaydedilemedi."
        }
    }

    private fun fromJson(value: JSONObject): TelegramChannel = TelegramChannel(
        id = value.getString("id"),
        creatorId = value.getLong("creator_id"),
        channelId = value.getString("channel_id"),
        channelTitle = value.getString("channel_title"),
        botUsername = value.getString("bot_username"),
        verifiedAt = value.getLong("verified_at")
    )

    fun channels(context: Context, creatorId: Long): List<TelegramChannel> = synchronized(monitor) {
        val stored = read(context)
        buildList {
            for (i in 0 until stored.length()) {
                val item = stored.getJSONObject(i)
                if (item.getLong("creator_id") == creatorId) add(fromJson(item))
            }
        }
    }

    internal fun credential(context: Context, channelId: String, creatorId: Long): TelegramCredential? =
        synchronized(monitor) {
            val stored = read(context)
            for (i in 0 until stored.length()) {
                val item = stored.getJSONObject(i)
                if (item.getString("id") == channelId &&
                    item.getLong("creator_id") == creatorId) {
                    return@synchronized TelegramCredential(fromJson(item), item.getString("token"))
                }
            }
            null
        }

    fun add(
        context: Context,
        creatorId: Long,
        channelId: String,
        channelTitle: String,
        botUsername: String,
        verifiedAt: Long,
        token: String
    ): TelegramChannel = synchronized(monitor) {
        require(creatorId >= 0 && verifiedAt > 0)
        require(token.isNotBlank() && channelId.isNotBlank())
        val entries = read(context)
        // Identical bot+channel pair is updated, not silently duplicated.
        var id: String? = null
        val updated = JSONArray()
        for (i in 0 until entries.length()) {
            val item = entries.getJSONObject(i)
            if (item.getLong("creator_id") == creatorId &&
                item.getString("channel_id") == channelId &&
                item.getString("bot_username") == botUsername) {
                id = item.getString("id")
            } else updated.put(item)
        }
        val channel = TelegramChannel(
            id ?: UUID.randomUUID().toString(),
            creatorId, channelId, channelTitle.take(100),
            botUsername.take(100), verifiedAt
        )
        updated.put(JSONObject().apply {
            put("id", channel.id)
            put("creator_id", channel.creatorId)
            put("channel_id", channel.channelId)
            put("channel_title", channel.channelTitle)
            put("bot_username", channel.botUsername)
            put("verified_at", channel.verifiedAt)
            put("token", token)
        })
        write(context, updated)
        channel
    }

    fun remove(context: Context, creatorId: Long, connectionId: String): Boolean =
        synchronized(monitor) {
            val stored = read(context)
            val updated = JSONArray()
            var removed = false
            for (i in 0 until stored.length()) {
                val entry = stored.getJSONObject(i)
                if (entry.getString("id") == connectionId &&
                    entry.getLong("creator_id") == creatorId) removed = true
                else updated.put(entry)
            }
            if (removed) write(context, updated)
            removed
        }

    /** No Telegram channel token survives deletion of its associated creator. */
    fun removeForCreator(context: Context, creatorId: Long) = synchronized(monitor) {
        val stored = read(context)
        val updated = JSONArray()
        for (i in 0 until stored.length()) {
            val entry = stored.getJSONObject(i)
            if (entry.getLong("creator_id") != creatorId) updated.put(entry)
        }
        write(context, updated)
    }
}
