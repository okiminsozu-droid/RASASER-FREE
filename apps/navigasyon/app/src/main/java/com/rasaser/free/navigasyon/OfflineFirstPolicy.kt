package com.rasaser.free.navigasyon

enum class Connection { OFFLINE, METERED, UNMETERED }
enum class NavigationMode { NEEDS_MAP, OFFLINE, ONLINE_ASSISTED }

data class NavigationDecision(
    val mode: NavigationMode,
    val localRouteEnabled: Boolean,
    val optionalOnlineEnabled: Boolean,
    val shouldCheckForUpdates: Boolean,
    val canOfferFirstDownload: Boolean,
    val canAutoDownloadLargeUpdate: Boolean
)

// The local map and routing engine are authoritative in every connectivity state.
object OfflineFirstPolicy {
    fun decide(
        mapVerified: Boolean,
        connection: Connection,
        onlineAllowed: Boolean,
        allowLargeUpdates: Boolean = false
    ): NavigationDecision {
        val connected = connection != Connection.OFFLINE && onlineAllowed
        val unmetered = connection == Connection.UNMETERED
        return NavigationDecision(
            mode = when {
                !mapVerified -> NavigationMode.NEEDS_MAP
                connected -> NavigationMode.ONLINE_ASSISTED
                else -> NavigationMode.OFFLINE
            },
            localRouteEnabled = mapVerified,
            optionalOnlineEnabled = connected,
            shouldCheckForUpdates = mapVerified && connected && unmetered,
            canOfferFirstDownload = !mapVerified && connected,
            canAutoDownloadLargeUpdate = mapVerified && connected && unmetered && allowLargeUpdates
        )
    }
}
