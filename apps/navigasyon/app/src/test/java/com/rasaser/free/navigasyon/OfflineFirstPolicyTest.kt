package com.rasaser.free.navigasyon

import org.junit.Test
import org.junit.Assert.*

class OfflineFirstPolicyTest {
    @Test fun offlineMapStillRoutes() {
        val p = OfflineFirstPolicy.decide(true, Connection.OFFLINE, true)
        assertEquals(NavigationMode.OFFLINE, p.mode)
        assertTrue(p.localRouteEnabled)
        assertFalse(p.optionalOnlineEnabled)
    }
    @Test fun wifiEnablesOptionalOnlineWithoutReplacingLocalRoute() {
        val p = OfflineFirstPolicy.decide(true, Connection.UNMETERED, true)
        assertEquals(NavigationMode.ONLINE_ASSISTED, p.mode)
        assertTrue(p.localRouteEnabled)
        assertTrue(p.shouldCheckForUpdates)
        assertFalse(p.canAutoDownloadLargeUpdate)
    }
    @Test fun mobileDataDoesNotFetchLargeFiles() {
        val p = OfflineFirstPolicy.decide(true, Connection.METERED, true, true)
        assertFalse(p.canAutoDownloadLargeUpdate)
        assertFalse(p.shouldCheckForUpdates)
    }
    @Test fun userCanDisableOnline() {
        val p = OfflineFirstPolicy.decide(true, Connection.UNMETERED, false)
        assertEquals(NavigationMode.OFFLINE, p.mode)
        assertFalse(p.optionalOnlineEnabled)
    }
    @Test fun firstDownloadRequiresMapAndConnectivity() {
        val a = OfflineFirstPolicy.decide(false, Connection.UNMETERED, true)
        assertEquals(NavigationMode.NEEDS_MAP, a.mode)
        assertTrue(a.canOfferFirstDownload)
        assertFalse(a.canAutoDownloadLargeUpdate)
    }
    @Test fun withoutMapOrInternetNoFakeRoute() {
        val p = OfflineFirstPolicy.decide(false, Connection.OFFLINE, true)
        assertEquals(NavigationMode.NEEDS_MAP, p.mode)
        assertFalse(p.localRouteEnabled)
        assertFalse(p.canOfferFirstDownload)
    }
    @Test fun largeUpdateOnlyWithExplicitOptInAndWifi() {
        val p = OfflineFirstPolicy.decide(true, Connection.UNMETERED, true, true)
        assertTrue(p.canAutoDownloadLargeUpdate)
    }
}
