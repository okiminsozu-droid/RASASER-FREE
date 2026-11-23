package com.rasaser.free.icerikureticisi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SocialAccountInputTest {
    @Test fun instagramUsernameBecomesOfficialProfileUrl() {
        assertEquals("https://www.instagram.com/creator.64/",
            SocialAccountInput.profileUrl("Instagram", "@creator.64"))
        assertEquals("creator.64",
            SocialAccountInput.displayLabel("Instagram", "", "https://www.instagram.com/creator.64/"))
    }

    @Test fun xTwitterAccountsHaveStableOfficialUrls() {
        assertEquals("https://x.com/example_user",
            SocialAccountInput.profileUrl("X", "@example_user"))
        assertEquals("example_user", SocialAccountInput.displayLabel(
            "X", "", "https://x.com/example_user"))
    }

    @Test fun twoAccountsOnOnePlatformRemainSeparateUrls() {
        val first = SocialAccountInput.profileUrl("Instagram", "firstuser")
        val second = SocialAccountInput.profileUrl("Instagram", "seconduser")
        assertEquals("https://www.instagram.com/firstuser/", first)
        assertEquals("https://www.instagram.com/seconduser/", second)
    }

    @Test fun youtubeAndTikTokNormalizeHandles() {
        assertEquals("https://www.youtube.com/@mychannel",
            SocialAccountInput.profileUrl("YouTube", "@mychannel"))
        assertEquals("https://www.tiktok.com/@mycreator",
            SocialAccountInput.profileUrl("TikTok", "@mycreator"))
        assertEquals("@mycreator",
            SocialAccountInput.displayLabel("TikTok", "", "https://www.tiktok.com/@mycreator"))
    }

    @Test fun rejectsBlankAndImpersonationUrlsWithClearErrors() {
        assertThrows(IllegalArgumentException::class.java) {
            SocialAccountInput.profileUrl("Instagram", "")
        }
        assertThrows(IllegalArgumentException::class.java) {
            SocialAccountInput.profileUrl("Instagram", "https://instagram.com.evil.test/alice")
        }
        assertThrows(IllegalArgumentException::class.java) {
            SocialAccountInput.profileUrl("YouTube", "not-an-at-handle")
        }
    }

    @Test fun recognizesOfficialFacebookProfileAndUserChoice() {
        assertEquals("https://www.facebook.com/alice",
            SocialAccountInput.profileUrl("Facebook", "alice"))
        assertEquals("Ali's Facebook",
            SocialAccountInput.displayLabel("Facebook", "Ali's Facebook", "https://www.facebook.com/alice"))
        assertEquals("Facebook 12345",
            SocialAccountInput.displayLabel("Facebook", "", "https://www.facebook.com/profile.php?id=12345"))
    }
}
