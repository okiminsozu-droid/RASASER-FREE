package com.rasaser.free.navigasyon

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The 99% screen must never pretend installation is stuck downloading. */
class TurkeyInstallProgressTest {
    @Test fun verifyStageExplainsLastPercentWithoutFalseCompletion() {
        val status=TurkeyPackageUi.workerStatus(
            NavLanguage.TR,TurkeyCountryPackWorker.STAGE_SEARCH_VERIFY,99,false)
        assertTrue(status.contains("Yer arama doğrulanıyor"))
        assertTrue(status.contains("İndirme tamamlandı"))
        assertFalse(status.contains("99%"))
    }

    @Test fun resumeButtonPreservesAlreadyDownloadedPacks() {
        val label=TurkeyPackageUi.button(NavLanguage.TR,false,true)
        assertTrue(label.contains("Kaldığı yerden devam"))
    }

    @Test fun providerAddressIsNeverShownInUserFacingProgress() {
        val display=TurkeyPackageUi.workerStatus(
            NavLanguage.TR,TurkeyCountryPackWorker.STAGE_SEARCH,95,false)
        assertFalse(display.contains("https"))
        assertFalse(display.contains("workers.dev"))
        assertFalse(display.contains("Cloudflare"))
    }

    @Test fun installedMapCanBeReportedBeforePlaceIndexFinishes() {
        assertTrue(TurkeyCountryPackWorker.STAGE_SEARCH_VERIFY >
            TurkeyCountryPackWorker.STAGE_SEARCH)
        assertTrue(TurkeyCountryPackWorker.MAP_READY.isNotEmpty())
    }
}
