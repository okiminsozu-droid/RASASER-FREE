package com.rasaser.free.navigasyon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TurkeyInstallSpaceTest {
    @Test fun completedAndVerifiedSearchZipDoesNotCountAsMissingAgain() {
        assertEquals(260_000_000L,
            TurkeyInstallSpace.requiredBytes(66_446_365L,0L,true,280_000_000L,true))
    }

    @Test fun partialZipCountsOnlyItsMissingBytesAndSupportsResume() {
        assertEquals(260_000_000L+16_446_365L,
            TurkeyInstallSpace.requiredBytes(66_446_365L,50_000_000L,false,280_000_000L,true))
    }

    @Test fun previouslyInstalledMapDiskCalculationIsUnchanged() {
        val bytes=TurkeyInstallSpace.requiredBytes(
            605_992_151L,0L,false,669_908_992L+150_000_000L,false)
        assertTrue(bytes>1_000_000_000L)
        assertEquals(605_992_151L+669_908_992L+150_000_000L+80_000_000L,bytes)
    }
}
