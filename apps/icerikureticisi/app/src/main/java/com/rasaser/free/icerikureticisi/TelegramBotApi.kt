package com.rasaser.free.icerikureticisi

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** Telegram Bot API uses BotFather credentials, not OAuth user-account sign-in. */
data class TelegramVerification(
    val channelId: String,
    val channelTitle: String,
    val botUsername: String
)

object TelegramBotApi {
    private const val PHOTO_MAX_BYTES: Long = 10L * 1024L * 1024L
    private val TOKEN = Regex("^[0-9]{5,20}:[A-Za-z0-9_-]{25,}$")
    private val CHANNEL_USERNAME = Regex("^@[A-Za-z0-9_]{5,32}$")
    private val CHANNEL_NUMERIC = Regex("^-100[0-9]{5,20}$")

    fun validChannelId(value: String): Boolean =
        CHANNEL_USERNAME.matches(value.trim()) || CHANNEL_NUMERIC.matches(value.trim())

    fun validBotToken(value: String): Boolean = TOKEN.matches(value.trim())

    suspend fun verify(token: String, requestedChat: String): TelegramVerification =
        withContext(Dispatchers.IO) {
            require(validBotToken(token)) { "BotFather tarafından verilen geçerli bot tokenını girin." }
            require(validChannelId(requestedChat)) {
                "Kanal kullanıcı adını @kanaladi veya -100 ile başlayan kanal ID'si olarak girin."
            }
            val cleanToken = token.trim()
            val chat = requestedChat.trim()
            val me = call(cleanToken, "getMe", JSONObject())
                .getJSONObject("result")
            require(me.optBoolean("is_bot", false)) { "Girilen token Telegram botuna ait değil." }
            val botId = me.getLong("id")
            val username = me.optString("username", "")
            val chatResult = call(cleanToken, "getChat", JSONObject().put("chat_id", chat))
                .getJSONObject("result")
            require(chatResult.optString("type") == "channel") {
                "Bu hedef bir Telegram kanalı değil. Botu kanal yöneticisi olarak ekleyin."
            }
            val member = call(
                cleanToken, "getChatMember",
                JSONObject().put("chat_id", chat).put("user_id", botId)
            ).getJSONObject("result")
            val status = member.optString("status")
            require(status == "creator" ||
                (status == "administrator" && member.optBoolean("can_post_messages", false))) {
                "Botun bu kanalda mesaj yayınlama yönetici izni bulunmuyor."
            }
            TelegramVerification(
                channelId = chatResult.getLong("id").toString(),
                channelTitle = chatResult.optString("title", chat).take(100),
                botUsername = username
            )
        }

    suspend fun publishText(
        token: String, chatId: String, text: String
    ): Long = withContext(Dispatchers.IO) {
        require(text.isNotBlank()) { "Önce gönderi metnini yazın." }
        require(text.length <= 4096) { "Telegram metni en fazla 4096 karakter olabilir." }
        val result = call(token, "sendMessage", JSONObject()
            .put("chat_id", chatId).put("text", text))
        val post = result.getJSONObject("result")
        require(post.optLong("message_id") > 0L) { "Telegram yayın onayı döndürmedi." }
        post.getLong("message_id")
    }

    suspend fun publishPhoto(
        context: Context, token: String, chatId: String, photo: Uri, caption: String
    ): Long = withContext(Dispatchers.IO) {
        require(photo.scheme == "content") { "Yalnızca telefonunuzdan seçilen fotoğraflar gönderilebilir." }
        require(caption.length <= 1024) { "Telegram fotoğraf açıklaması en fazla 1024 karakter olabilir." }
        val mime = context.contentResolver.getType(photo).orEmpty()
        require(mime in setOf("image/jpeg", "image/png", "image/webp")) {
            "Lütfen JPG, PNG veya WEBP biçiminde bir fotoğraf seçin."
        }
        val descriptor = context.contentResolver.openAssetFileDescriptor(photo, "r")
        try {
            if (descriptor != null && descriptor.length > PHOTO_MAX_BYTES) {
                throw IllegalArgumentException("Telegram fotoğrafı 10 MB'den küçük olmalı.")
            }
        } finally { descriptor?.close() }

        val boundary = "rasaser-" + UUID.randomUUID().toString()
        val connection = open(token, "sendPhoto", "multipart/form-data; boundary=$boundary")
        try {
            connection.setChunkedStreamingMode(65536)
            connection.outputStream.use { out ->
                fun field(name: String, value: String) {
                    out.write(("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n" +
                        value + "\r\n").toByteArray(Charsets.UTF_8))
                }
                field("chat_id", chatId)
                if (caption.isNotBlank()) field("caption", caption)
                out.write(("--$boundary\r\nContent-Disposition: form-data; " +
                    "name=\"photo\"; filename=\"creator-photo.jpg\"\r\n" +
                    "Content-Type: $mime\r\n\r\n").toByteArray(Charsets.UTF_8))
                var copied = 0L
                val input = context.contentResolver.openInputStream(photo)
                    ?: throw IllegalArgumentException("Seçilen fotoğraf okunamadı.")
                input.use {
                    val buffer = ByteArray(65536)
                    while (true) {
                        val length = it.read(buffer)
                        if (length == -1) break
                        copied += length
                        require(copied <= PHOTO_MAX_BYTES) { "Fotoğraf en fazla 10 MB olabilir." }
                        out.write(buffer, 0, length)
                    }
                }
                out.write("\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8))
            }
            val parsed = parseResponse(connection)
            val message = parsed.getJSONObject("result").getLong("message_id")
            require(message > 0L) { "Telegram fotoğraf yayını doğrulanmadı." }
            message
        } finally { connection.disconnect() }
    }

    private fun call(token: String, endpoint: String, args: JSONObject): JSONObject {
        require(validBotToken(token)) { "Bot tokenı geçersiz." }
        val conn = open(token, endpoint, "application/json; charset=utf-8")
        try {
            conn.outputStream.use {
                it.write(args.toString().toByteArray(Charsets.UTF_8))
            }
            return parseResponse(conn)
        } finally { conn.disconnect() }
    }

    private fun open(token: String, endpoint: String, contentType: String): HttpURLConnection {
        require(endpoint in setOf("getMe", "getChat", "getChatMember", "sendMessage", "sendPhoto"))
        require(validBotToken(token))
        // Fixed Telegram host avoids SSRF; never put tokens into logs, error messages or UI.
        return (URL("https://api.telegram.org/bot$token/$endpoint")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 25000
            doOutput = true
            useCaches = false
            instanceFollowRedirects = false
            setRequestProperty("Content-Type", contentType)
            setRequestProperty("Accept", "application/json")
        }
    }

    private fun parseResponse(conn: HttpURLConnection): JSONObject {
        try {
            val status = conn.responseCode
            val input = if (status in 200..299) conn.inputStream else conn.errorStream
            val bytes = input?.use { stream ->
                ByteArrayOutputStream().use { output ->
                    val buffer = ByteArray(4096)
                    var total = 0
                    while (true) {
                        val n = stream.read(buffer)
                        if (n < 0) break
                        total += n
                        require(total <= 65536) { "Telegram yanıtı beklenenden büyük." }
                        output.write(buffer, 0, n)
                    }
                    output.toByteArray()
                }
            } ?: ByteArray(0)
            val parsed = JSONObject(String(bytes, Charsets.UTF_8))
            if (status !in 200..299 || !parsed.optBoolean("ok")) {
                val message = when (status) {
                    400 -> "Kanal bulunamadı veya bilgileri hatalı."
                    401 -> "Bot tokenı geçersiz veya iptal edilmiş."
                    403 -> "Botun kanala erişimi ya da yayınlama izni yok."
                    429 -> "Telegram işlem sınırına ulaşıldı. Daha sonra tekrar deneyin."
                    else -> "Telegram API işlemi başarısız (HTTP $status)."
                }
                throw IllegalStateException(message)
            }
            return parsed
        } catch (e: IllegalArgumentException) { throw e }
        catch (e: IllegalStateException) { throw e }
        catch (_: IOException) {
            throw IllegalStateException("Telegram'a erişilemiyor. İnternet bağlantısını kontrol edin.")
        } catch (_: Exception) {
            throw IllegalStateException("Telegram'dan geçerli bir yanıt alınamadı.")
        }
    }
}
