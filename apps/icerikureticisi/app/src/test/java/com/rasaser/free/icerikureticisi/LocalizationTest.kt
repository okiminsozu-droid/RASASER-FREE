package com.rasaser.free.icerikureticisi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizationTest {
    @Test fun everyRequiredLanguageHasFlagAndName() {
        assertEquals(listOf("tr", "en", "de", "es", "ja", "ar"),
            AppLanguage.choices.map { it.code })
        assertTrue(AppLanguage.choices.all { it.flag.isNotBlank() && it.title.isNotBlank() })
    }

    @Test fun knownInterfaceTextGetsTranslated() {
        for (language in AppLanguage.choices.filter { it.code != "tr" }) {
            assertTrue(
                "Missing Content Creator translation for ${language.code}",
                Translations.text(language.code, "İçerik Üreticisi") != "İçerik Üreticisi"
            )
            assertTrue(Translations.text(language.code, "Premium").isNotBlank())
        }
        assertEquals("İçerik adı", Translations.text("tr", "İçerik adı"))
    }

    @Test fun unknownPersonalContentIsNotInventedOrMachineTranslated() {
        assertEquals("Kişisel özel içerik adı",
            Translations.text("ja", "Kişisel özel içerik adı"))
        assertEquals("tr", AppLanguage.normalize("unknown"))
    }
}
