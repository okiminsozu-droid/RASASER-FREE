package com.rasaser.free.navigasyon
import org.junit.Assert.*
import org.junit.Test
class DestinationSpeechTest {
    @Test fun spokenDestinationsAreNotMistakenForAppCommands() {
        assertEquals("Banaz Devlet Hastanesine",
            DestinationSpeech.destination(NavLanguage.TR,"Banaz Devlet Hastanesine git"))
        assertEquals("Ağaoğlu Eczanesine",
            DestinationSpeech.destination(NavLanguage.TR,"Ağaoğlu Eczanesine götür"))
        assertEquals("Banaz State Hospital",
            DestinationSpeech.destination(NavLanguage.EN,"Navigate to Banaz State Hospital"))
        assertEquals("病院",
            DestinationSpeech.destination(NavLanguage.JA,"病院まで案内して"))
        assertNull(DestinationSpeech.destination(NavLanguage.TR,"rotayı iptal et"))
        assertNull(DestinationSpeech.destination(NavLanguage.EN,"mute"))
    }
    @Test fun turkishNormalizationAndSuffixFallback() {
        assertEquals("agaoglu eczanesine",PlaceQuery.normalize("Ağaoğlu Eczanesine"))
        val variants=PlaceQuery.tokens("Banaz Devlet Hastanesine")
        assertTrue(variants.any{it.last()=="hastane"})
        assertTrue(variants.all{it.isNotEmpty()})
    }
}
