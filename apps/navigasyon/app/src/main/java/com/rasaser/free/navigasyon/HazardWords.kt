package com.rasaser.free.navigasyon

/** All warnings are from OSM/Valhalla maneuver tags, not a live incident feed. */
object HazardWords {
    private val entries:Map<NavLanguage,List<String>> = mapOf(
        NavLanguage.TR to listOf("Ücretli yol veya geçiş", "Bozuk veya stabilize yol",
            "Feribot geçişi", "Kapı veya bariyer"),
        NavLanguage.EN to listOf("Toll road or crossing", "Rough or unpaved road",
            "Ferry crossing", "Gate or barrier"),
        NavLanguage.DE to listOf("Gebührenpflichtige Straße", "Schlechte oder unbefestigte Straße",
            "Fährüberfahrt", "Tor oder Schranke"),
        NavLanguage.AR to listOf("طريق أو معبر برسوم", "طريق غير ممهد أو خشن",
            "معبر عبّارة", "بوابة أو حاجز"),
        NavLanguage.ES to listOf("Vía o paso con peaje", "Carretera irregular o sin pavimentar",
            "Cruce en ferry", "Puerta o barrera"),
        NavLanguage.JA to listOf("有料道路または料金所", "荒れた道または未舗装路",
            "フェリー乗り場", "ゲートまたは遮断機")
    )
    fun text(lang:NavLanguage,type:RouteHazard):String=
        requireNotNull(entries[lang]?.getOrNull(type.ordinal))
    fun complete():Boolean=NavLanguage.entries.all { entries[it]?.size == RouteHazard.entries.size }
}
