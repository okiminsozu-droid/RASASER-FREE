package com.rasaser.free.icerikureticisi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthorizedChannelRulesTest {
    @Test fun copiedProfileUrlCannotGrantPublicationRights() {
        val allowed = setOf(ChannelOperation.PUBLISH_IMAGE)
        assertFalse(AuthorizedChannelRules.mayPublish(
            ChannelAuthState.MANUAL, "12345", allowed, ChannelOperation.PUBLISH_IMAGE))
        assertFalse(AuthorizedChannelRules.mayPublish(
            ChannelAuthState.AUTHORIZING, "12345", allowed, ChannelOperation.PUBLISH_IMAGE))
    }

    @Test fun verifiedChannelNeedsRemoteIdentityAndCorrectScope() {
        val allowed = setOf(ChannelOperation.PROFILE, ChannelOperation.PUBLISH_IMAGE)
        assertFalse(AuthorizedChannelRules.mayPublish(
            ChannelAuthState.CONNECTED, "", allowed, ChannelOperation.PUBLISH_IMAGE))
        assertFalse(AuthorizedChannelRules.mayPublish(
            ChannelAuthState.CONNECTED, "insta-1", allowed, ChannelOperation.PUBLISH_VIDEO))
        assertFalse(AuthorizedChannelRules.mayPublish(
            ChannelAuthState.CONNECTED, "insta-1", allowed, ChannelOperation.READ_SUPPORTED_EVENTS))
        assertTrue(AuthorizedChannelRules.mayPublish(
            ChannelAuthState.CONNECTED, "insta-1", allowed, ChannelOperation.PUBLISH_IMAGE))
    }

    @Test fun connectionLossImmediatelyDisablesPublish() {
        for (state in listOf(
            ChannelAuthState.REAUTH_REQUIRED,
            ChannelAuthState.REVOKED,
            ChannelAuthState.FAILED
        )) {
            assertFalse(AuthorizedChannelRules.mayPublish(
                state, "id", setOf(ChannelOperation.PUBLISH_VIDEO), ChannelOperation.PUBLISH_VIDEO))
        }
    }

    @Test fun legacyRowsRemainManualAndProviderMappingStaysExact() {
        assertEquals(ChannelAuthState.MANUAL, ChannelAuthState.fromStored("new_unknown_value"))
        assertEquals(ChannelAuthState.MANUAL, ChannelAuthState.fromStored(null))
        assertEquals(RaSaSeRProvider.FACEBOOK, RaSaSeRProvider.fromPlatform("facebook"))
        assertEquals(null, RaSaSeRProvider.fromPlatform("Other"))
    }
}
