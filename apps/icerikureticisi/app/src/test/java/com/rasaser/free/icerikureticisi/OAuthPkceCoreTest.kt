package com.rasaser.free.icerikureticisi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OAuthPkceCoreTest {
    @Test fun pkceS256MatchesRfc7636PublishedVector() {
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            OAuthPkceCore.challengeFor(verifier))
    }

    @Test fun everyAuthorizationHasIndependentLongRandomStateAndVerifier() {
        val first = OAuthPkceCore.xAuthorization(
            "clientID_12345", "com.rasaser.free.icerikureticisi://oauth-x"
        )
        val second = OAuthPkceCore.xAuthorization(
            "clientID_12345", "com.rasaser.free.icerikureticisi://oauth-x"
        )
        assertEquals("X", first.provider)
        assertNotEquals(first.state, second.state)
        assertNotEquals(first.codeVerifier, second.codeVerifier)
        assertEquals(43, first.state.length)
        assertTrue(first.codeVerifier.length in 43..128)
        assertEquals(OAuthPkceCore.challengeFor(first.codeVerifier), first.codeChallenge)
        assertTrue(first.authorizationUrl.startsWith("https://x.com/i/oauth2/authorize?"))
        assertTrue(first.authorizationUrl.contains("offline.access"))
        assertTrue(first.authorizationUrl.contains("code_challenge_method=S256"))
        assertTrue(OAuthPkceCore.stateMatches(first.state, first.state))
        assertFalse(OAuthPkceCore.stateMatches(first.state, second.state))
    }

    @Test fun refuseSpoofedProviderRedirectOrMissingClientId() {
        assertThrows(IllegalArgumentException::class.java) {
            OAuthPkceCore.xAuthorization("", "com.rasaser.free.icerikureticisi://oauth-x")
        }
        assertThrows(IllegalArgumentException::class.java) {
            OAuthPkceCore.xAuthorization("clientID_12345", "javascript:alert(1)")
        }
        assertThrows(IllegalArgumentException::class.java) {
            OAuthPkceCore.xAuthorization("clientID_12345", "http://unsafe.example/callback")
        }
        assertFalse(OAuthPkceCore.stateMatches("", "anything"))
    }

    @Test fun refreshThresholdNeverTreatsRevokedTokenAsUsable() {
        val future = System.currentTimeMillis() + 120_000
        val account = DeviceOAuthAccount(
            id = "connection-1", creatorId = 12L, provider = "X",
            remoteAccountId = "remote-1", displayName = "creator",
            scopes = setOf("tweet.write"), expiresAtMillis = future,
            lastVerifiedMillis = System.currentTimeMillis(),
            authorizationRequired = false
        )
        assertFalse(DeviceOAuthVault.needsRefresh(account, System.currentTimeMillis()))
        assertTrue(DeviceOAuthVault.needsRefresh(account, future - 30_000))
        assertFalse(DeviceOAuthVault.needsRefresh(
            account.copy(authorizationRequired = true), future
        ))
    }
}
