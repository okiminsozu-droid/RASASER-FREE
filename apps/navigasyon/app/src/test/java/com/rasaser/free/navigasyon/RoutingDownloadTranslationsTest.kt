package com.rasaser.free.navigasyon

import org.junit.Assert.*
import org.junit.Test
class RoutingDownloadTranslationsTest {
 @Test fun sixLanguagesComplete() {
   assertTrue(RoutingDownloadText.complete())
   for(language in NavLanguage.entries)
     for(key in RoutingDownloadStatus.entries)
       assertTrue(RoutingDownloadText.get(language,key).isNotBlank())
 }
}
