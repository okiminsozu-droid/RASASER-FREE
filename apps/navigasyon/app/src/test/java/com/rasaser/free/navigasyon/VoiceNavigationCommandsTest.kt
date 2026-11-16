package com.rasaser.free.navigasyon

import org.junit.Assert.*
import org.junit.Test

class VoiceNavigationCommandsTest {
    @Test fun allSixLanguagesHaveEachCommand() {
        assertTrue(VoiceNavigationCommands.complete())
        val pairs = listOf(
            Triple(NavLanguage.TR,"rotayı iptal et",VoiceNavigationCommand.CLEAR_DESTINATION),
            Triple(NavLanguage.EN,"mute",VoiceNavigationCommand.MUTE),
            Triple(NavLanguage.DE,"EINSTELLUNGEN",VoiceNavigationCommand.SETTINGS),
            Triple(NavLanguage.AR,"الإعدادات",VoiceNavigationCommand.SETTINGS),
            Triple(NavLanguage.ES,"mostrar alertas",VoiceNavigationCommand.ALERTS),
            Triple(NavLanguage.JA,"目的地を消去",VoiceNavigationCommand.CLEAR_DESTINATION)
        )
        for((lang,spoken,expected) in pairs)
            assertEquals(expected,VoiceNavigationCommands.match(lang,spoken))
    }
    @Test fun recognizedSpeechMustBeAnExactCommand() {
        assertNull(VoiceNavigationCommands.match(NavLanguage.TR,"ayarlar açık ama rotam kalsın"))
        assertNull(VoiceNavigationCommands.match(NavLanguage.EN,"don't cancel route"))
        assertNull(VoiceNavigationCommands.match(NavLanguage.EN,"unmute the wrong thing"))
        assertNull(VoiceNavigationCommands.match(NavLanguage.TR,""))
        assertNull(VoiceNavigationCommands.match(NavLanguage.JA,"目的地"))
    }
}
