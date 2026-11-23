package com.rasaser.free.icerikureticisi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class BlueskySession(
    val did: String,
    val handle: String,
    val accessJwt: String,
    val refreshJwt: String
)

data class BlueskyPublishedPost(
    val uri: String,
    val cid: String
)

/**
 * PDS entryway for Bluesky-hosted accounts. Strict allowlist:
 * - no RaSaSeR services, proxies or callback URLs
 * - no user-provided domains (SSRF prevention)
 * - only app-specific password accepted by the UI, never primary credentials
 *
 * NOTE: This is the ATProto app-password path for Bluesky-hosted accounts,
 * NOT OAuth. Formal ATProto OAuth requires public client metadata hosting.
 */
object BlueskyNativeApi {
    private const val ENTRYWAY = "https://bsky.social/xrpc/"
    private const val MAX_REPLY = 65536
    private const val MAX_UTF8 = 3000
    private const val MAX_CODEPOINTS = 300

    fun validHandle(value: String): Boolean {
        val handle = value.trim().removePrefix("@")
        return handle.length in 4..253 &&
            Regex("^[A-Za-z0-9][A-Za-z0-9.-]*\\.[A-Za-z]{2,63}$").matches(handle) &&
            !handle.contains("..")
    }

    fun validAppPassword(value: String): Boolean =
        Regex("(?i)^[a-z0-9]{4}(-[a-z0-9]{4}){3}$").matches(value.trim())

    fun validateText(text: String): String {
        val trimmed = text.trim()
        require(trimmed.isNotBlank()) { "Bluesky gönderisi boş olamaz." }
        require(trimmed.codePointCount(0, trimmed.length) <= MAX_CODEPOINTS &&
            trimmed.toByteArray(Charsets.UTF_8).size <= MAX_UTF8) {
            "Bluesky metni en fazla 300 karakter ve 3000 bayt olabilir."
        }
        return trimmed
    }

    suspend fun connect(handle: String, appPassword: String): BlueskySession =
        withContext(Dispatchers.IO) {
            require(validHandle(handle)) {
                "Bluesky kullanıcı adını @kullanici.bsky.social biçiminde girin."
            }
            require(validAppPassword(appPassword)) {
                "Yalnız Bluesky ayarlarından üretilmiş 4 gruplu uygulama parolası kullanın."
            }
            val j = request("com.atproto.server.createSession", JSONObject().apply {
                put("identifier", handle.trim().removePrefix("@"))
                put("password", appPassword.trim())
            })
            session(j)
        }

    suspend fun publish(
        stored: BlueskyCredential,
        onRenewed: (BlueskySession) -> Unit,
        text: String
    ): BlueskyPublishedPost = withContext(Dispatchers.IO) {
        val content = validateText(text)
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        val body = JSONObject().apply {
            put("repo", stored.channel.did)
            put("collection", "app.bsky.feed.post")
            put("record", JSONObject().apply {
                put("\u0024type", "app.bsky.feed.post")
                put("text", content)
                put("createdAt", now)
            })
        }
        val response = try {
            request("com.atproto.repo.createRecord", body, stored.accessJwt)
        } catch (e: BlueskyHttpException) {
            if (e.status != 401) throw e
            // A 401 means the initial post was NOT accepted. Refresh before retry.
            val refreshed = session(request(
                "com.atproto.server.refreshSession",
                JSONObject(), stored.refreshJwt
            ))
            require(refreshed.did == stored.channel.did) {
                "Bluesky oturum kimliği değişti. Yeniden hesap bağlayın."
            }
            // Persist rotated refresh token BEFORE retry to avoid lockout on crash.
            onRenewed(refreshed)
            request("com.atproto.repo.createRecord", body, refreshed.accessJwt)
        }
        val uri = response.optString("uri")
        require(uri.startsWith("at://" + stored.channel.did + "/app.bsky.feed.post/")) {
            "Bluesky geçerli yayın kimliği döndürmedi."
        }
        BlueskyPublishedPost(uri, response.optString("cid"))
    }

    private fun session(obj: JSONObject): BlueskySession {
        val did = obj.getString("did")
        val handle = obj.getString("handle")
        require(did.startsWith("did:") && validHandle(handle)) {
            "Bluesky hesap bilgisi doğrulanamadı."
        }
        val access = obj.getString("accessJwt")
        val refresh = obj.getString("refreshJwt")
        require(access.isNotBlank() && refresh.isNotBlank())
        return BlueskySession(did, handle, access, refresh)
    }

    private class BlueskyHttpException(val status: Int, message: String) :
        IllegalStateException(message)

    private fun request(
        endpoint: String,
        body: JSONObject,
        bearer: String = ""
    ): JSONObject {
        require(endpoint in setOf(
            "com.atproto.server.createSession",
            "com.atproto.server.refreshSession",
            "com.atproto.repo.createRecord"
        ))
        val conn = URL(ENTRYWAY + endpoint).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 25000
            conn.doOutput = true
            conn.instanceFollowRedirects = false
            conn.useCaches = false
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")
            if (bearer.isNotBlank()) conn.setRequestProperty("Authorization", "Bearer $bearer")
            conn.outputStream.use { out ->
                out.write(body.toString().toByteArray(Charsets.UTF_8))
            }
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val bytes = stream?.use { input ->
                ByteArrayOutputStream().use { out ->
                    val buffer = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_REPLY) { "Bluesky yanıtı beklenenden büyük." }
                        out.write(buffer, 0, count)
                    }
                    out.toByteArray()
                }
            } ?: ByteArray(0)
            if (status !in 200..299) {
                val message = when (status) {
                    400 -> "Bluesky hesap bilgileri veya gönderi biçimi geçersiz."
                    401 -> "Bluesky oturumu sona erdi. Yeniden bağlayın."
                    403 -> "Bluesky hesabının bu işlem için yetkisi bulunmuyor."
                    429 -> "Bluesky istek sınırı doldu. Daha sonra deneyin."
                    else -> "Bluesky işlemi başarısız (HTTP $status)."
                }
                throw BlueskyHttpException(status, message)
            }
            return JSONObject(String(bytes, Charsets.UTF_8))
        } catch (e: BlueskyHttpException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: IllegalStateException) {
            throw e
        } catch (_: IOException) {
            throw IllegalStateException("Bluesky'ye bağlanılamıyor. İnterneti kontrol edin.")
        } catch (_: Exception) {
            throw IllegalStateException("Bluesky yanıtı doğrulanamadı.")
        } finally {
            conn.disconnect()
        }
    }

    /** Never show bearer tokens or endpoint paths in UI, logs or notifications. */
    fun safeMessage(e: Throwable): String {
        val m = e.message.orEmpty()
        if (e !is IllegalStateException && e !is IllegalArgumentException)
            return "Bluesky işlemi başarısız. Bağlantıyı kontrol edin."
        if (m.length > 240 || m.contains("eyJ") || m.contains("Bearer ", true) ||
            m.contains("accessJwt") || m.contains("refreshJwt"))
            return "Bluesky bağlantısı doğrulanamadı. Hesabı yeniden bağlayın."
        return m.ifBlank { "Bluesky işlemi başarısız." }
    }
}
