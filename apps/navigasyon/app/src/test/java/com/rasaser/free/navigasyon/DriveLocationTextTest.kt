package com.rasaser.free.navigasyon
import org.junit.Assert.*
import org.junit.Test
class DriveLocationTextTest {
    @Test fun sixLanguagesHaveEachLabel() {
        assertTrue(DriveLocationText.complete())
        for(lang in NavLanguage.entries)for(word in DriveLocationWord.entries)
            assertTrue(DriveLocationText.get(lang,word).isNotBlank())
    }
}
