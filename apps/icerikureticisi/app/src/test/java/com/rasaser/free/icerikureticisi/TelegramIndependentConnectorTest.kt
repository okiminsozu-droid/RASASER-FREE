package com.rasaser.free.icerikureticisi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramIndependentConnectorTest {
    @Test fun acceptChannelHandlesAndNumericIds() {
        assertTrue(TelegramBotApi.validChannelId("@mychannel"))
        assertTrue(TelegramBotApi.validChannelId("@test_public_channel"))
        assertTrue(TelegramBotApi.validChannelId("-1001234567890"))
        assertFalse(TelegramBotApi.validChannelId("https://t.me/channel"))
        assertFalse(TelegramBotApi.validChannelId("@x"))
        assertFalse(TelegramBotApi.validChannelId("1234567890"))
    }

    @Test fun rejectUnstructuredBotTokensWithoutNetworkCalls() {
        assertFalse(TelegramBotApi.validBotToken(""))
        assertFalse(TelegramBotApi.validBotToken("123456:abc"))
        assertFalse(TelegramBotApi.validBotToken("https://example.com"))
        assertTrue(TelegramBotApi.validBotToken("123456789:" + "x".repeat(35)))
    }

    @Test fun allEightPlatformsHaveDistinctLocalCatalogRecords() {
        val values = setOf("Instagram", "Facebook", "X", "YouTube", "TikTok",
            "Pinterest", "Bluesky", "Telegram")
        assertEquals(values, SocialCatalog.presets.map { it.name }.toSet())
        assertEquals(8, SocialCatalog.presets.size)
        assertEquals(8, RaSaSeRProvider.entries.size)
    }

    @Test fun telegramAndBlueskyAreNotAuthorizedFromJustUrl() {
        assertTrue(OfficialSocialLinks.isOfficialProfile("Telegram", "https://t.me/mychannel"))
        assertTrue(OfficialSocialLinks.isOfficialProfile("Bluesky", "https://bsky.app/profile/my.bsky.social"))
        assertFalse(OfficialSocialLinks.isOfficialProfile("Telegram", "https://t.me/"))
        assertFalse(OfficialSocialLinks.isOfficialProfile("Telegram", "https://t.me.evil.test/mychannel"))
        assertFalse(AuthorizedChannelRules.mayPublish(
            ChannelAuthState.MANUAL, "channel-id",
            setOf(ChannelOperation.PUBLISH_TEXT), ChannelOperation.PUBLISH_TEXT))
    }
}
