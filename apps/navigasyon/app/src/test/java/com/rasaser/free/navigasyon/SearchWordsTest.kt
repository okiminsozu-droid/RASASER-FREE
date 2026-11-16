package com.rasaser.free.navigasyon
import org.junit.Assert.*
import org.junit.Test
class SearchWordsTest {
 @Test fun allSixLanguagesComplete() {
    assertTrue(SearchWords.complete())
    for(lang in NavLanguage.entries)for(key in SearchNotice.entries)
        assertTrue(SearchWords.text(lang,key).isNotBlank())
 }
}
