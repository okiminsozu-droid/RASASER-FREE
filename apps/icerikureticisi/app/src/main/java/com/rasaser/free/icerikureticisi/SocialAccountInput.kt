package com.rasaser.free.icerikureticisi

import java.net.URI
import java.util.Locale

/**
 * Converts a user-supplied profile link or handle into a bookmark.
 * This does NOT perform OAuth or verify that the phone user owns the account.
 */
object SocialAccountInput {
    private val validHandle = Regex("[A-Za-z0-9_.-]{1,80}")

    /** A stable HTTPS profile URL. Do not accept the platform homepage as an account. */
    fun profileUrl(platform: String, typed: String): String {
        val value = typed.trim()
        require(value.isNotBlank()) {
            "Önce hesabın kullanıcı adını veya resmî profil bağlantısını girin."
        }

        if (!OfficialSocialLinks.isRecognized(platform)) {
            require(value.startsWith("https://", ignoreCase = true)) {
                "Özel platform için https:// ile başlayan profil adresi girin."
            }
            return SocialCatalog.secureUrl(value)
        }

        val url = when {
            value.startsWith("http://", ignoreCase = true) ->
                throw IllegalArgumentException("Güvenlik için https:// profil bağlantısı kullanın.")
            value.startsWith("https://", ignoreCase = true) -> value
            value.startsWith("www.", ignoreCase = true) -> "https://$value"
            value.contains("/") && value.substringBefore("/").contains(".") -> "https://$value"
            else -> {
                val handle = value.removePrefix("@")
                require(validHandle.matches(handle)) {
                    "Geçerli kullanıcı adı veya resmî profil bağlantısı girin."
                }
                when (platform) {
                    "Instagram" -> "https://www.instagram.com/$handle/"
                    "Facebook" -> "https://www.facebook.com/$handle"
                    "TikTok" -> "https://www.tiktok.com/@$handle"
                    "YouTube" -> {
                        require(value.startsWith("@")) {
                            "YouTube için @kanaladı veya kanal bağlantısını girin."
                        }
                        "https://www.youtube.com/@$handle"
                    }
                    else -> throw IllegalArgumentException("Platform seçin.")
                }
            }
        }
        require(OfficialSocialLinks.isOfficialProfile(platform, url)) {
            "Seçtiğiniz platforma ait resmî profil bağlantısını girin."
        }
        return url
    }

    /** Auto-fill the display label for users who pasted only a profile URL. */
    fun displayLabel(platform: String, typedLabel: String, verifiedUrl: String): String {
        val explicit = typedLabel.trim()
        if (explicit.isNotEmpty()) return explicit
        val uri = URI(verifiedUrl)
        val path = uri.path.trim('/').split('/').lastOrNull().orEmpty()
        val result = when (platform) {
            "Facebook" -> if (uri.path.trim('/') == "profile.php") {
                uri.rawQuery.orEmpty().split('&').firstOrNull { it.startsWith("id=") }
                    ?.substringAfter("id=")?.takeIf { it.isNotBlank() }?.let { "Facebook $it" }
                    ?: "Facebook"
            } else path
            else -> path
        }
        require(result.isNotBlank()) { "Hesap adını veya profil bağlantısını girin." }
        return result.take(80)
    }
}
