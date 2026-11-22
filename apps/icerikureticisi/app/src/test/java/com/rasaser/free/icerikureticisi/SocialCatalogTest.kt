package com.rasaser.free.icerikureticisi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialCatalogTest {
    @Test fun fourPresetNetworksHaveRealAndroidPackages() {
        assertEquals(
            listOf("Instagram", "Facebook", "YouTube", "TikTok"),
            SocialCatalog.presets.map { it.name }
        )
        SocialCatalog.presets.forEach {
            assertTrue(SocialCatalog.validPackage(it.packageName))
            assertTrue(it.homeUrl.startsWith("https://"))
        }
    }

    @Test fun profileUrlsAreHttpsOnly() {
        assertEquals("https://www.instagram.com/hello",
            SocialCatalog.secureUrl("www.instagram.com/hello"))
        assertEquals("https://www.youtube.com/",
            SocialCatalog.secureUrl("", "https://www.youtube.com/"))
        assertEquals("", SocialCatalog.secureUrl(""))
        for (unsafe in listOf("http://example.com", "javascript:alert(1)",
                "https://user:pass@example.com", "invalid name")) {
            var rejected = false
            try { SocialCatalog.secureUrl(unsafe) }
            catch (_: IllegalArgumentException) { rejected = true }
            assertTrue("Should reject $unsafe", rejected)
        }
    }

    @Test fun customPackageValidationRejectsInvalidCharacters() {
        assertTrue(SocialCatalog.validPackage("com.other.socialapp"))
        assertTrue(SocialCatalog.validPackage(""))
        assertFalse(SocialCatalog.validPackage("com.example.app;rm"))
    }
}
