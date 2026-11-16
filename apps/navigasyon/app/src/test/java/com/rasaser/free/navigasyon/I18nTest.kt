package com.rasaser.free.navigasyon
import org.junit.Assert.*
import org.junit.Test

class I18nTest {
 @Test fun allSixLanguagesComplete() {
   assertEquals(6, NavLanguage.entries.size)
   assertTrue(L10n.complete())
   for (lang in NavLanguage.entries) for (key in TextKey.entries) assertTrue(
      "$lang/$key", L10n.get(lang,key).isNotBlank()
   )
 }
 @Test fun defaultLanguageIsTurkish() {
    assertEquals(NavLanguage.TR,NavLanguage.fromTag("unknown"))
    assertEquals(NavLanguage.JA,NavLanguage.fromTag("ja"))
 }
}
