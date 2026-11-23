package com.rasaser.free.icerikureticisi

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialSocialLinksTest {
    @Test fun supportsMultipleOfficialAccountsOnOnePlatform() {
        assertTrue(OfficialSocialLinks.isOfficialProfile("Instagram", "https://www.instagram.com/account_1/"))
        assertTrue(OfficialSocialLinks.isOfficialProfile("Instagram", "https://www.instagram.com/account_2/"))
        assertTrue(OfficialSocialLinks.isOfficialProfile("Facebook", "https://www.facebook.com/profile.php?id=12345"))
        assertTrue(OfficialSocialLinks.isOfficialProfile("YouTube", "https://www.youtube.com/@channel_name"))
        assertTrue(OfficialSocialLinks.isOfficialProfile("TikTok", "https://www.tiktok.com/@creator"))
    }

    @Test fun rejectsSpoofedSitesAndPlainHomeLinks() {
        assertFalse(OfficialSocialLinks.isOfficialProfile("Instagram", "https://www.instagram.com.evil.example/me"))
        assertFalse(OfficialSocialLinks.isOfficialProfile("Instagram", "https://www.instagram.com/"))
        assertFalse(OfficialSocialLinks.isOfficialProfile("TikTok", "https://www.tiktok.com/"))
        assertFalse(OfficialSocialLinks.isOfficialProfile("YouTube", "https://www.youtube.com/watch?v=test"))
        assertFalse(OfficialSocialLinks.isOfficialProfile("Facebook", "https://facebook.com@evil.example/me"))
        assertFalse(OfficialSocialLinks.isOfficialProfile("Instagram", "http://www.instagram.com/me"))
    }

    @Test fun preventsCrossPlatformProfileMixups() {
        assertFalse(OfficialSocialLinks.isOfficialProfile("Facebook", "https://www.instagram.com/account/"))
        assertFalse(OfficialSocialLinks.isOfficialProfile("TikTok", "https://www.youtube.com/@channel"))
    }
    @Test fun homepageShortcutsRemainUnverifiedAndPlatformSpecific() {
        assertTrue(OfficialSocialLinks.isPlatformShortcut("Instagram", "https://www.instagram.com/"))
        assertTrue(OfficialSocialLinks.isPlatformShortcut("Facebook", "https://www.facebook.com"))
        assertFalse(OfficialSocialLinks.isPlatformShortcut("TikTok", "https://www.instagram.com/"))
        assertFalse(OfficialSocialLinks.isPlatformShortcut("Instagram", "https://www.instagram.com/creator/"))
        assertFalse(OfficialSocialLinks.isPlatformShortcut("Instagram", "https://instagram.com.evil.example/"))
        assertFalse(OfficialSocialLinks.isOfficialProfile("Instagram", "https://www.instagram.com/"))
    }

}
