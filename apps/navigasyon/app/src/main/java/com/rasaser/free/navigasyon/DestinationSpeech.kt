package com.rasaser.free.navigasyon

/** Extracts a spoken destination name. No remote NLP service is ever used. */
object DestinationSpeech {
    private val prefix:Map<NavLanguage,List<Regex>> = mapOf(
        NavLanguage.TR to listOf("^(?:beni\\s+)?(?:götür\\s+|navigasyon\\s+|yol\\s+tarifi\\s+|hedef\\s+)"),
        NavLanguage.EN to listOf("^(?:take\\s+me\\s+to|navigate\\s+to|go\\s+to|directions\\s+to)\\s+"),
        NavLanguage.DE to listOf("^(?:navigiere\\s+zu|fahre\\s+zu|bringe\\s+mich\\s+zu)\\s+"),
        NavLanguage.AR to listOf("^(?:اذهب\\s+إلى|اذهب\\s+الى|خذني\\s+إلى|خذني\\s+الى)\\s*"),
        NavLanguage.ES to listOf("^(?:llévame\\s+a|ir\\s+a|navegar\\s+a)\\s+"),
        NavLanguage.JA to emptyList()
    ).mapValues { (_,patterns)->patterns.map{Regex(it,RegexOption.IGNORE_CASE)} }
    private val suffix:Map<NavLanguage,List<Regex>> = mapOf(
        NavLanguage.TR to listOf("\\s+(?:git|götür|gidelim|rota\\s+oluştur)(?:\\s+lütfen)?$"),
        NavLanguage.EN to listOf("\\s+(?:please|now)$"),
        NavLanguage.DE to listOf("\\s+(?:bitte|jetzt)$"),
        NavLanguage.AR to emptyList(),
        NavLanguage.ES to listOf("\\s+(?:por\\s+favor)$"),
        NavLanguage.JA to listOf("(?:まで案内して|まで行って|に行く|へ行く)$")
    ).mapValues { (_,patterns)->patterns.map {Regex(it,RegexOption.IGNORE_CASE)} }

    fun destination(language:NavLanguage,transcript:String):String? {
        if(VoiceNavigationCommands.match(language,transcript)!=null)return null
        var text=transcript.trim().trim('!', '?', '.', '،', '。','！','？')
        for(pattern in prefix[language].orEmpty())text=text.replace(pattern,"")
        for(pattern in suffix[language].orEmpty())text=text.replace(pattern,"")
        text=text.trim()
        if(text.length<3 || text.length>110)return null
        // Do not infer place names from sentences or arbitrary actions.
        if(text.contains('\n') || text.contains(';'))return null
        return text
    }
}
