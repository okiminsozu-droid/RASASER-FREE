package com.rasaser.free.navigasyon

enum class SystemSpeechWord { TITLE, DESCRIPTION, ALLOW, TYPE_INSTEAD }

/**
 * Explicit per-device consent: the system recognizer may transmit audio to
 * its provider when an offline recognizer does not exist.
 */
object SystemSpeechWords {
    private val words:Map<NavLanguage,List<String>> = mapOf(
        NavLanguage.TR to listOf(
            "Sesli hedef arama",
            "Çevrimdışı ses tanıma yok. Telefonun ses servisi internet kullanabilir.",
            "Sesle ara","Yazarak ara"),
        NavLanguage.EN to listOf(
            "Voice destination search",
            "Offline recognition is unavailable. Your phone's speech service may use the internet.",
            "Use voice","Type instead"),
        NavLanguage.DE to listOf(
            "Sprachzielsuche",
            "Offline-Spracherkennung fehlt. Der Sprachdienst Ihres Telefons kann das Internet nutzen.",
            "Sprache verwenden","Stattdessen tippen"),
        NavLanguage.AR to listOf(
            "البحث الصوتي عن الوجهة",
            "التعرف دون اتصال غير متاح. قد تستخدم خدمة الصوت على هاتفك الإنترنت.",
            "استخدم الصوت","اكتب بدلاً من ذلك"),
        NavLanguage.ES to listOf(
            "Búsqueda de destino por voz",
            "No hay reconocimiento sin conexión. El servicio de voz del teléfono puede usar internet.",
            "Usar voz","Escribir"),
        NavLanguage.JA to listOf(
            "音声で目的地を検索",
            "オフライン音声認識がありません。端末の音声サービスがインターネットを使用する場合があります。",
            "音声で検索","文字で検索")
    )
    fun get(language:NavLanguage,key:SystemSpeechWord):String =
        requireNotNull(words[language]?.getOrNull(key.ordinal))
    fun complete():Boolean=NavLanguage.entries.all{words[it]?.size==SystemSpeechWord.entries.size}
}
