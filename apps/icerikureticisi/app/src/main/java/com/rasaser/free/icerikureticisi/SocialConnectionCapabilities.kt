package com.rasaser.free.icerikureticisi

/**
 * Policy wall separating a verified first-party API session from a bookmark.
 * No native connector may infer an OAuth grant from a password, URL, API key,
 * package install, or the mere act of opening another social media application.
 */
enum class DeviceConnectionMode {
    TELEGRAM_CHANNEL_BOT,
    BLUESKY_APP_PASSWORD,
    OFFICIAL_OAUTH_NOT_CONFIGURED,
    MANUAL_SHORTCUT
}

data class SocialProviderCapability(
    val name: String,
    val mode: DeviceConnectionMode,
    val canDirectPublishText: Boolean,
    val canDirectPublishPhoto: Boolean,
    val storesSocialAccountPassword: Boolean,
    val requiresProviderApproval: Boolean
)

/**
 * Credentials for real connected accounts are in provider-specific Android
 * Keystore vaults. Unavailable providers only create local shortcuts.
 */
object SocialConnectionCapabilities {
    private val providers = listOf(
        SocialProviderCapability("Instagram", DeviceConnectionMode.OFFICIAL_OAUTH_NOT_CONFIGURED,
            false, false, false, true),
        SocialProviderCapability("Facebook", DeviceConnectionMode.OFFICIAL_OAUTH_NOT_CONFIGURED,
            false, false, false, true),
        SocialProviderCapability("X", DeviceConnectionMode.OFFICIAL_OAUTH_NOT_CONFIGURED,
            false, false, false, true),
        SocialProviderCapability("YouTube", DeviceConnectionMode.OFFICIAL_OAUTH_NOT_CONFIGURED,
            false, false, false, true),
        SocialProviderCapability("TikTok", DeviceConnectionMode.OFFICIAL_OAUTH_NOT_CONFIGURED,
            false, false, false, true),
        SocialProviderCapability("Pinterest", DeviceConnectionMode.OFFICIAL_OAUTH_NOT_CONFIGURED,
            false, false, false, true),
        SocialProviderCapability("Bluesky", DeviceConnectionMode.BLUESKY_APP_PASSWORD,
            true, true, false, false),
        SocialProviderCapability("Telegram", DeviceConnectionMode.TELEGRAM_CHANNEL_BOT,
            true, true, false, false)
    )

    fun capability(platform: String): SocialProviderCapability =
        providers.firstOrNull { it.name == platform } ?:
            SocialProviderCapability(platform, DeviceConnectionMode.MANUAL_SHORTCUT,
                false, false, false, false)

    fun isDirect(platform: String): Boolean =
        capability(platform).mode in setOf(
            DeviceConnectionMode.TELEGRAM_CHANNEL_BOT,
            DeviceConnectionMode.BLUESKY_APP_PASSWORD
        )

    /** A bookmark or OAuth-unconfigured platform is NEVER connected. */
    fun connectedLabel(platform: String, hasVerifiedProviderGrant: Boolean): String {
        if (!isDirect(platform)) return "Kısayol • Resmî bağlantı yok"
        return if (hasVerifiedProviderGrant) "Bağlı • Telefonda şifreli"
            else "Henüz bağlı değil"
    }
}
