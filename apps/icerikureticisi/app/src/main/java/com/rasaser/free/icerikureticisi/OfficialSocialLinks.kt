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

    /** A platform homepage can be saved as an unverified app/web shortcut. */
    fun isPlatformShortcut(platform: String, link: String): Boolean {
        val homepage = platformHome(platform) ?: return false
        return link.trimEnd('/').equals(homepage.trimEnd('/'), ignoreCase = true)
    }

    /**
     * A social deep link may be captured by Instagram/Facebook rather than the
     * user's browser. The website action MUST use a real browser activity.
     */
    fun launchOfficialWebsite(context: Context, platform: String, profileUrl: String? = null): Boolean {
        val url = when {
            profileUrl.isNullOrBlank() -> platformHome(platform)
            !isRecognized(platform) -> profileUrl.takeIf {
                it.startsWith("https://", ignoreCase = true)
            }
            isOfficialProfile(platform, profileUrl) || isPlatformShortcut(platform, profileUrl) ->
                profileUrl
            else -> null
        }
        return url?.let { launchBrowser(context, it) } ?: false
    }

    fun launchPlatformHome(context: Context, platform: String): Boolean =
        launchOfficialWebsite(context, platform)

    fun launchPlatformApp(context: Context, platform: String): Boolean {
        val pkg = SocialCatalog.find(platform)?.packageName ?: return false
        return try {
            context.packageManager.getLaunchIntentForPackage(pkg)?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
                true
            } ?: if (android.os.Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getLaunchIntentSenderForPackage(pkg)
                    .sendIntent(context, 0, null, null, null)
                true
            } else false
        } catch (_: Exception) { false }
    }

    /** Prefer direct deep link to the SAVED profile, not the platform homepage. */
    fun launchProfileInApp(context: Context, account: SocialAccount): Boolean {
        if (isRecognized(account.platform) &&
            !isOfficialProfile(account.platform, account.profileUrl)) return false
        val pkg = account.packageName.takeIf { it.isNotBlank() } ?: return false
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(account.profileUrl)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (_: Exception) { false }
    }

    /** Safe browser route for the URL saved with each account. */
    fun launchProfile(context: Context, account: SocialAccount): Boolean {
        val url = account.profileUrl
        if (isRecognized(account.platform) &&
            !isOfficialProfile(account.platform, url) &&
            !isPlatformShortcut(account.platform, url)) return false
        if (!url.startsWith("https://", ignoreCase = true)) return false
        return launchBrowser(context, url)
    }

    private val browserPackages = listOf(
        "com.android.chrome", "com.vivo.browser", "com.heytap.browser",
        "com.sec.android.app.sbrowser", "com.mi.globalbrowser",
        "org.mozilla.firefox", "org.mozilla.fenix", "com.brave.browser",
        "com.microsoft.emmx", "com.opera.browser", "com.android.browser"
    )

    private fun launchBrowser(context: Context, url: String): Boolean {
        val uri = Uri.parse(url)
        if (uri.scheme != "https" || uri.host.isNullOrBlank()) return false
        val base = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Android FLAG_ACTIVITY_REQUIRE_NON_BROWSER was wrong here: that flag
        // explicitly REFUSES browsers. Try real browser packages, never a social
        // media package, before falling back to the Android selection screen.
        for (browser in browserPackages) {
            try {
                context.startActivity(Intent(base).setPackage(browser))
                return true
            } catch (_: Exception) { /* Browser not installed or unavailable. */ }
        }
        return try {
            context.startActivity(Intent.createChooser(base, "İnternet tarayıcısını seç").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (_: Exception) { false }
    }
}
