package com.rasaser.free.trendnabiz

object NewsLocalization {
    fun description(item: NewsItem, lang: UiLang): String {
        // Only data with known, machine-readable semantics may be locally translated.
        // For arbitrary future news text, do not pretend an unverified translation exists.
        if (item.source != "USGS") return item.description
        val place = item.title.substringAfter(" · ", "")
        return when (lang) {
            UiLang.TR -> "$place bölgesinde ${item.magnitude} büyüklüğünde deprem kaydedildi."
            UiLang.EN -> "Earthquake of magnitude ${item.magnitude} recorded near $place."
            UiLang.DE -> "Erdbeben der Stärke ${item.magnitude} bei $place registriert."
            UiLang.ES -> "Sismo de magnitud ${item.magnitude} registrado cerca de $place."
            UiLang.ZH -> "$place 附近记录到 ${item.magnitude} 级地震。"
        }
    }
}
