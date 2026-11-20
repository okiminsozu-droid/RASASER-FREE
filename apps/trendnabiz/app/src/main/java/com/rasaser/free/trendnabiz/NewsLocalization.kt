package com.rasaser.free.trendnabiz

/** Source-aware original explanatory sentences; never silently invent or auto-translate reported facts. */
object NewsLocalization {
    fun description(item: NewsItem, lang: UiLang): String {
        if (item.source.contains("TRENDNABIZ") && item.source.contains("TEST")) return UiText.get(lang,"cloudTestHint")
        if (item.source == "USGS" && item.magnitude > 0.0) {
            val place = item.region.ifBlank {
                item.title.substringAfter(": ",item.title.substringAfter(" · ",""))
            }
            return when(lang) {
                UiLang.TR -> "$place yakınlarında ${item.magnitude} büyüklüğünde deprem kaydedildi. Yerel resmî uyarıları takip edin."
                UiLang.EN -> "Earthquake of magnitude ${item.magnitude} near $place. Follow official local safety updates."
                UiLang.DE -> "Erdbeben der Stärke ${item.magnitude} nahe $place. Offizielle örtliche Hinweise beachten."
                UiLang.ES -> "Sismo de magnitud ${item.magnitude} cerca de $place. Consulta los avisos oficiales locales."
                UiLang.ZH -> "$place 附近记录到 ${item.magnitude} 级地震。请关注当地官方安全公告。"
            }
        }
        if(item.source == "NOAA/NWS")return when(lang) {
            UiLang.TR -> "ABD Ulusal Hava Durumu Servisi bu bölge için resmî hava durumu uyarısı yayımladı. Ayrıntılar için kaynağa bakın."
            UiLang.EN -> "US National Weather Service issued an official warning. Open the source for details."
            UiLang.DE -> "Der US-Wetterdienst hat eine amtliche Warnung herausgegeben. Details bei der Quelle."
            UiLang.ES -> "El Servicio Meteorológico de EE. UU. emitió un aviso oficial. Consulta la fuente."
            UiLang.ZH -> "美国国家气象局发布了官方预警。请查阅原始来源。"
        }
        if(item.source=="NASA EONET")return when(lang) {
            UiLang.TR -> "NASA EONET bu doğa olayını kaydetti. Son durum için resmî kaynağı açın."
            UiLang.EN -> "NASA EONET recorded this natural event. Open the source for the latest details."
            UiLang.DE -> "NASA EONET hat dieses Naturereignis erfasst. Aktuelle Details bei der Quelle."
            UiLang.ES -> "NASA EONET registró este fenómeno natural. Consulta la fuente."
            UiLang.ZH -> "NASA EONET 记录了此自然事件。请查看原始来源。"
        }
        if(item.source=="GDACS")return when(lang) {
            UiLang.TR -> "GDACS küresel afet uyarısı yayımladı. Yetkili yerel kurumların talimatlarını takip edin."
            UiLang.EN -> "GDACS issued a disaster notice. Follow local authorities for safety instructions."
            UiLang.DE -> "GDACS meldet eine Katastrophenwarnung. Bitte örtliche Behörden beachten."
            UiLang.ES -> "GDACS emitió una alerta de desastre. Sigue las indicaciones de las autoridades locales."
            UiLang.ZH -> "GDACS 发布了灾害通报。请遵循当地主管部门的指示。"
        }
        if(item.source=="European Commission")return when(lang) {
            UiLang.TR -> "Avrupa Komisyonu yeni bir resmî açıklama yayımladı. Ayrıntılar kaynağında."
            UiLang.EN -> "The European Commission published an official update. Open the source for details."
            UiLang.DE -> "Die Europäische Kommission hat eine offizielle Meldung veröffentlicht."
            UiLang.ES -> "La Comisión Europea publicó una actualización oficial."
            UiLang.ZH -> "欧盟委员会发布了新的官方公告。请查看原始来源。"
        }
        return item.description
    }
}
