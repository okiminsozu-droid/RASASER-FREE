package com.rasaser.free.icerikureticisi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialConnectionCapabilitiesTest {
    @Test fun allEightBrandProvidersHaveExplicitPolicy() {
        assertEquals(8, SocialCatalog.presets.size)
        val names = SocialCatalog.presets.map { it.name }.toSet()
        assertEquals(
            setOf("Instagram", "Facebook", "X", "YouTube", "TikTok",
                "Pinterest", "Bluesky", "Telegram"),
            names
        )
        names.forEach { name ->
            assertFalse(SocialConnectionCapabilities.capability(name).storesSocialAccountPassword)
        }
    }

    @Test fun onlyTrueNativeConnectorsCanPublishOnDevice() {
        assertTrue(SocialConnectionCapabilities.isDirect("Bluesky"))
        assertTrue(SocialConnectionCapabilities.isDirect("Telegram"))
        assertTrue(SocialConnectionCapabilities.capability("Bluesky").canDirectPublishPhoto)
        assertTrue(SocialConnectionCapabilities.capability("Telegram").canDirectPublishText)
        for (name in listOf("Instagram", "Facebook", "X", "YouTube", "TikTok", "Pinterest")) {
            assertFalse(SocialConnectionCapabilities.isDirect(name))
            assertFalse(SocialConnectionCapabilities.capability(name).canDirectPublishPhoto)
            assertFalse(SocialConnectionCapabilities.capability(name).canDirectPublishText)
            assertTrue(SocialConnectionCapabilities.capability(name).requiresProviderApproval)
        }
    }

    @Test fun openingAProfileOrEnteringAnApiKeyCannotCreateFakeOAuthSession() {
        for (provider in SocialCatalog.presets.map { it.name }) {
            if (provider !in setOf("Telegram", "Bluesky")) {
                assertEquals("Kısayol • Resmî bağlantı yok",
                    SocialConnectionCapabilities.connectedLabel(provider, true))
            }
        }
        assertEquals("Bağlı • Telefonda şifreli",
            SocialConnectionCapabilities.connectedLabel("Telegram", true))
        assertEquals("Henüz bağlı değil",
            SocialConnectionCapabilities.connectedLabel("Bluesky", false))
        assertEquals(DeviceConnectionMode.MANUAL_SHORTCUT,
            SocialConnectionCapabilities.capability("Other service").mode)
    }
}
