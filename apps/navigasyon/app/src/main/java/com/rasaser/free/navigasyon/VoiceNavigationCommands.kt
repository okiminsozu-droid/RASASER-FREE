package com.rasaser.free.navigasyon

import java.util.Locale

/** Device-only recognition result routing. Never send transcripts off-device. */
enum class VoiceNavigationCommand {
    SETTINGS, ALERTS, HOME, MUTE, UNMUTE, CLEAR_DESTINATION
}

object VoiceNavigationCommands {
    private val phrases:Map<NavLanguage,Map<VoiceNavigationCommand,List<String>>> = mapOf(
        NavLanguage.TR to mapOf(
            VoiceNavigationCommand.SETTINGS to listOf("ayarlar","ayarları aç"),
            VoiceNavigationCommand.ALERTS to listOf("uyarılar","uyarıları aç"),
            VoiceNavigationCommand.HOME to listOf("ana ekran","ana ekrana dön"),
            VoiceNavigationCommand.MUTE to listOf("sesi kapat","sessize al"),
            VoiceNavigationCommand.UNMUTE to listOf("sesi aç","sesi etkinleştir"),
            VoiceNavigationCommand.CLEAR_DESTINATION to listOf("hedefi sil","rotayı iptal et")
        ),
        NavLanguage.EN to mapOf(
            VoiceNavigationCommand.SETTINGS to listOf("settings","open settings"),
            VoiceNavigationCommand.ALERTS to listOf("alerts","show alerts"),
            VoiceNavigationCommand.HOME to listOf("home","go home"),
            VoiceNavigationCommand.MUTE to listOf("mute","mute voice"),
            VoiceNavigationCommand.UNMUTE to listOf("unmute","enable voice"),
            VoiceNavigationCommand.CLEAR_DESTINATION to listOf("clear destination","cancel route")
        ),
        NavLanguage.DE to mapOf(
            VoiceNavigationCommand.SETTINGS to listOf("einstellungen","einstellungen öffnen"),
            VoiceNavigationCommand.ALERTS to listOf("warnungen","warnungen öffnen"),
            VoiceNavigationCommand.HOME to listOf("startseite","zur startseite"),
            VoiceNavigationCommand.MUTE to listOf("ton aus","stumm schalten"),
            VoiceNavigationCommand.UNMUTE to listOf("ton an","stumm aus"),
            VoiceNavigationCommand.CLEAR_DESTINATION to listOf("ziel löschen","route abbrechen")
        ),
        NavLanguage.AR to mapOf(
            VoiceNavigationCommand.SETTINGS to listOf("الإعدادات","افتح الإعدادات"),
            VoiceNavigationCommand.ALERTS to listOf("التحذيرات","اعرض التحذيرات"),
            VoiceNavigationCommand.HOME to listOf("الرئيسية","الصفحة الرئيسية"),
            VoiceNavigationCommand.MUTE to listOf("كتم الصوت","أوقف الصوت"),
            VoiceNavigationCommand.UNMUTE to listOf("تشغيل الصوت","أعد الصوت"),
            VoiceNavigationCommand.CLEAR_DESTINATION to listOf("مسح الوجهة","إلغاء المسار")
        ),
        NavLanguage.ES to mapOf(
            VoiceNavigationCommand.SETTINGS to listOf("ajustes","abrir ajustes"),
            VoiceNavigationCommand.ALERTS to listOf("alertas","mostrar alertas"),
            VoiceNavigationCommand.HOME to listOf("inicio","ir al inicio"),
            VoiceNavigationCommand.MUTE to listOf("silenciar","desactivar voz"),
            VoiceNavigationCommand.UNMUTE to listOf("activar voz","quitar silencio"),
            VoiceNavigationCommand.CLEAR_DESTINATION to listOf("borrar destino","cancelar ruta")
        ),
        NavLanguage.JA to mapOf(
            VoiceNavigationCommand.SETTINGS to listOf("設定","設定を開く"),
            VoiceNavigationCommand.ALERTS to listOf("警告","警告を表示"),
            VoiceNavigationCommand.HOME to listOf("ホーム","ホームに戻る"),
            VoiceNavigationCommand.MUTE to listOf("音声を消す","ミュート"),
            VoiceNavigationCommand.UNMUTE to listOf("音声をつける","ミュート解除"),
            VoiceNavigationCommand.CLEAR_DESTINATION to listOf("目的地を消去","ルートをキャンセル")
        )
    )
    private fun clean(s:String):String=s.trim().replace(Regex("[.!?,؛،。！？]+$"),"")
        .replace(Regex("\\s+")," ").lowercase(Locale.ROOT)
    fun match(language:NavLanguage, recognized:String):VoiceNavigationCommand? {
        val message=clean(recognized)
        if(message.isBlank() || message.length>120) return null
        return phrases[language]?.entries?.firstOrNull { (_,aliases)->
            aliases.any { clean(it)==message }
        }?.key
    }
    fun complete():Boolean=NavLanguage.entries.all { lang ->
        VoiceNavigationCommand.entries.all { command ->
            phrases[lang]?.get(command)?.isNotEmpty()==true
        }
    }
}
