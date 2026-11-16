package com.rasaser.free.navigasyon
import org.junit.Assert.*
import org.junit.Test
class SystemSpeechWordsTest {
  @Test fun sixLanguagesHavePrivacyNotice() {
    assertTrue(SystemSpeechWords.complete())
    for(lang in NavLanguage.entries)for(key in SystemSpeechWord.entries)
      assertTrue(SystemSpeechWords.get(lang,key).isNotBlank())
  }
}