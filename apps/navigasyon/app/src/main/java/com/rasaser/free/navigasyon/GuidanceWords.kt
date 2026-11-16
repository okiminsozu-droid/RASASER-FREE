package com.rasaser.free.navigasyon

enum class GuidanceLabel { NEXT_TURN, ARRIVED, REROUTING, SPEED_CAUTION, SPEED_LIMIT, LIMIT_UNKNOWN, CLEAR_TARGET }

object GuidanceWords {
    private val values: Map<NavLanguage, List<String>> = mapOf(
        NavLanguage.TR to listOf("Sonraki manevra", "Hedefe ulaştınız", "Rota yeniden hesaplanıyor",
            "Dikkat, hız sınırını aştınız", "Hız sınırı", "Hız sınırı bilinmiyor", "Hedefi temizle"),
        NavLanguage.EN to listOf("Next maneuver", "You have arrived", "Recalculating route",
            "Caution, you are exceeding the speed limit", "Speed limit", "Speed limit unknown", "Clear destination"),
        NavLanguage.DE to listOf("Nächstes Manöver", "Ziel erreicht", "Route wird neu berechnet",
            "Achtung, Sie überschreiten das Tempolimit", "Tempolimit", "Tempolimit unbekannt", "Ziel löschen"),
        NavLanguage.AR to listOf("المناورة التالية", "وصلت إلى وجهتك", "إعادة حساب المسار",
            "تنبيه، تجاوزت حد السرعة", "حد السرعة", "حد السرعة غير معروف", "مسح الوجهة"),
        NavLanguage.ES to listOf("Próxima maniobra", "Ha llegado a su destino", "Recalculando ruta",
            "Atención, supera el límite de velocidad", "Límite de velocidad", "Límite desconocido", "Borrar destino"),
        NavLanguage.JA to listOf("次の案内", "目的地に到着しました", "経路を再計算しています",
            "速度制限を超えています", "制限速度", "制限速度不明", "目的地を消去")
    )
    fun text(language:NavLanguage,label:GuidanceLabel):String=
        requireNotNull(values[language]?.getOrNull(label.ordinal))
    fun complete():Boolean=NavLanguage.entries.all {values[it]?.size==GuidanceLabel.entries.size}
}
