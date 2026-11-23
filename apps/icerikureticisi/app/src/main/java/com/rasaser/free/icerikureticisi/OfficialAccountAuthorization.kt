package com.rasaser.free.icerikureticisi

/**
 * Independent RaSaSeR channel connection contract.
 *
 * Real OAuth grants must originate from the platform's own consent screen and be
 * verified by a dedicated trusted backend. Never place client secrets or
 * access/refresh tokens in the Android APK or in creator_hub.db.
 */
enum class ChannelAuthState(val storedValue: String) {
    MANUAL("manual"),
    AUTHORIZING("authorizing"),
    CONNECTED("connected"),
    REAUTH_REQUIRED("reauth_required"),
    REVOKED("revoked"),
    FAILED("failed");

    companion object {
        fun fromStored(value: String?): ChannelAuthState =
            entries.firstOrNull { it.storedValue == value } ?: MANUAL
    }
}

enum class RaSaSeRProvider(val platformName: String) {
    INSTAGRAM("Instagram"),
    FACEBOOK("Facebook"),
    TIKTOK("TikTok"),
    YOUTUBE("YouTube"),
    PINTEREST("Pinterest"),
    BLUESKY("Bluesky"),
    TELEGRAM("Telegram");

    companion object {
        fun fromPlatform(name: String): RaSaSeRProvider? =
            entries.firstOrNull { it.platformName.equals(name, ignoreCase = true) }
    }
}

enum class ChannelOperation {
    PROFILE,
    PUBLISH_IMAGE,
    PUBLISH_VIDEO,
    PUBLISH_TEXT,
    READ_SUPPORTED_EVENTS
}

data class VerifiedChannel(
    val provider: RaSaSeRProvider,
    val remoteChannelId: String,
    val displayLabel: String,
    val grantedOperations: Set<ChannelOperation>,
    val connectionId: String,
    val lastVerifiedMillis: Long
) {
    init {
        require(remoteChannelId.isNotBlank() && remoteChannelId.length <= 256)
        require(connectionId.isNotBlank() && connectionId.length <= 256)
        require(lastVerifiedMillis > 0)
    }
}

/**
 * A profile URL is only a bookmark. Authorization is a separate step; this
 * predicate cannot be bypassed by adding a URL, by selecting the account or
 * by opening the official Android application.
 */
object AuthorizedChannelRules {
    fun mayPublish(
        state: ChannelAuthState,
        remoteChannelId: String,
        permittedOperations: Set<ChannelOperation>,
        requested: ChannelOperation
    ): Boolean = state == ChannelAuthState.CONNECTED &&
        remoteChannelId.isNotBlank() &&
        requested in permittedOperations &&
        requested in setOf(
            ChannelOperation.PUBLISH_IMAGE,
            ChannelOperation.PUBLISH_VIDEO,
            ChannelOperation.PUBLISH_TEXT
        )

    /** The local app can show this metadata, but never accepts the grant itself. */
    fun verifiedLabel(state: ChannelAuthState): String = when (state) {
        ChannelAuthState.MANUAL -> "Manuel profil bağlantısı"
        ChannelAuthState.AUTHORIZING -> "Resmî yetki bekleniyor"
        ChannelAuthState.CONNECTED -> "Resmî olarak bağlandı"
        ChannelAuthState.REAUTH_REQUIRED -> "Yeniden bağlantı gerekli"
        ChannelAuthState.REVOKED -> "Bağlantı kaldırıldı"
        ChannelAuthState.FAILED -> "Bağlantı hatası"
    }
}
