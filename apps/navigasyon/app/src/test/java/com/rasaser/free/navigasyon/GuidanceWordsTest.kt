package com.rasaser.free.navigasyon
import org.junit.Assert.*
import org.junit.Test
class GuidanceWordsTest {
 @Test fun sixLanguagesCovered() {
    assertTrue(GuidanceWords.complete())
    for(lang in NavLanguage.entries) for(word in GuidanceLabel.entries)
        assertTrue(GuidanceWords.text(lang,word).isNotBlank())
 }
}
