package com.rasaser.free.icerikureticisi

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URI
import java.util.Locale

/**
 * Official platform website links are NOT account authorization.
 * Without platform OAuth + approved publish scopes no app can prove publication
 * or obtain private account notifications by following a profile URL.
 */
object OfficialSocialLinks {
    private val officialHosts = mapOf(
        "Instagram" to setOf("instagram.com", "www.instagram.com"),
        "Facebook" to setOf("facebook.com", "www.facebook.com", "m.facebook.com", "web.facebook.com"),
        "YouTube" to setOf("youtube.com", "www.youtube.com", "m.youtube.com"),
        "TikTok" to setOf("tiktok.com", "www.tiktok.com", "m.tiktok.com")
    )

    fun platformHome(platform: String): String? =
        SocialCatalog.find(platform)?.homeUrl

    fun isRecognized(platform: String): Boolean = officialHosts.containsKey(platform)

    /** Verifies known platform host, without relying on suffixes or deceptive subdomains. */
    fun isOfficialProfile(platform: String, link: String): Boolean {
        val hosts = officialHosts[platform] ?: return false
        val parsed = try { URI(link.trim()) } catch (_: Exception) { return false }
        val host = parsed.host?.lowercase(Locale.ROOT) ?: return false
        if (parsed.scheme?.lowercase(Locale.ROOT) != "https" ||
            parsed.userInfo != null || (parsed.port != -1 && parsed.port != 443) ||
            host !in hosts) return false
        val path = parsed.path?.trim('/').orEmpty()
        if (path.isBlank()) return false
        if (platform == "YouTube" &&
            !listOf("@", "channel/", "user/", "c/").any { path.startsWith(it) }) return false
        if (platform == "TikTok" && !path.startsWith("@")) return false
        if (platform == "Facebook" && path in setOf("login", "home.php", "watch", "marketplace")) return false
        if (platform == "Instagram" && path in setOf("accounts", "explore", "reels", "p")) return false
        return true
    }

    fun launchPlatformHome(context: Context, platform: String): Boolean {
        val uri = platformHome(platform) ?: return false
        return launchTrusted(context, uri)
    }

    fun launchProfile(context: Context, account: SocialAccount): Boolean {
        val url = account.profileUrl
        if (isRecognized(account.platform) &&
            !isOfficialProfile(account.platform, url)) return false
        if (!url.startsWith("https://", ignoreCase = true)) return false
        return launchTrusted(context, url)
    }

    private fun launchTrusted(context: Context, url: String): Boolean = try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: Exception) { false }
}
