package com.rasaser.free.icerikureticisi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NativePrivateEngineTest {
    @Test fun blueskyDirectInputUsesOnlyAppPasswords() {
        assertTrue(BlueskyNativeApi.validHandle("@creator.bsky.social"))
        assertTrue(BlueskyNativeApi.validHandle("custom.example"))
        assertFalse(BlueskyNativeApi.validHandle("https://evil.example/callback"))
        assertFalse(BlueskyNativeApi.validHandle("something"))
        assertTrue(BlueskyNativeApi.validAppPassword("abcd-efgh-ijkl-mnop"))
        assertFalse(BlueskyNativeApi.validAppPassword("regular-account-password"))
        assertFalse(BlueskyNativeApi.validAppPassword(""))
    }

    @Test fun blueskyPostLimitHandlesUnicodeAndOptionalPhoto() {
        assertEquals("Selam", BlueskyNativeApi.validateText(" Selam "))
        assertThrows(IllegalArgumentException::class.java) {
            BlueskyNativeApi.validateText("")
        }
        assertEquals("", BlueskyNativeApi.validateText(" ", hasPhoto = true))
        assertEquals("a".repeat(300), BlueskyNativeApi.validateText("a".repeat(300)))
        assertThrows(IllegalArgumentException::class.java) {
            BlueskyNativeApi.validateText("a".repeat(301))
        }
        assertThrows(IllegalArgumentException::class.java) {
            BlueskyNativeApi.validateText("🌹".repeat(301))
        }
    }

    @Test fun unsignedOrTamperedOfflineLicenseIsNeverAccepted() {
        assertFalse(OfflinePremiumLicense.valid("", "com.rasaser.free.icerikureticisi"))
        assertFalse(OfflinePremiumLicense.valid(
            "RSRL1.eyJleHBpcmVzIjowfQ.invalid",
            "com.rasaser.free.icerikureticisi"
        ))
        assertFalse(OfflinePremiumLicense.valid(
            "THIS-IS-NOT-A-SIGNED-LICENSE", "com.rasaser.free.icerikureticisi"
        ))
    }

    @Test fun otherSocialPlatformBookmarksCannotClaimPublicationGrants() {
        for (provider in RaSaSeRProvider.entries) {
            assertFalse(AuthorizedChannelRules.mayPublish(
                ChannelAuthState.MANUAL,
                provider.platformName,
                setOf(ChannelOperation.PUBLISH_TEXT),
                ChannelOperation.PUBLISH_TEXT
            ))
        }
    }
}
