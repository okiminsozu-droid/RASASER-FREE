package com.rasaser.free.navigasyon

/** User-controlled complete country installation; never claims downloads are complete early. */
object TurkeyPackageUi {
    fun button(language:NavLanguage, ready:Boolean):String {
        val languageTag=language.tag.lowercase()
        if(ready) return when(languageTag) {
            "en" -> "Türkiye is ready"
            "de" -> "Türkei ist bereit"
            "es" -> "Turquía está lista"
            "ar" -> "تركيا جاهزة"
            "ja" -> "トルコのデータは準備完了"
            else -> "Türkiye hazır"
        }
        return when(languageTag) {
            "en" -> "Download Türkiye • 1.82 GB"
            "de" -> "Türkei herunterladen • 1,82 GB"
            "es" -> "Descargar Turquía • 1,82 GB"
            "ar" -> "تنزيل خريطة تركيا • 1.82 غيغابايت"
            "ja" -> "トルコをダウンロード • 1.82 GB"
            else -> "Türkiye'yi indir • 1,82 GB"
        }
    }
    fun stage(language:NavLanguage,map:Boolean,route:Boolean,search:Boolean,
              mapDownloading:Boolean,mapPercent:Int,routeDownloading:Boolean,
              routeInstalling:Boolean,searchDownloading:Boolean,searchInstalling:Boolean):String {
        val step=when {
            !map && mapDownloading -> "1/3 • Harita indiriliyor: $mapPercent%"
            !map -> "1/3 • Türkiye haritası hazırlanıyor"
            !route && routeInstalling -> "2/3 • Rota verisi doğrulanıyor"
            !route && routeDownloading -> "2/3 • Rota verisi indiriliyor"
            !route -> "2/3 • Rota verisi hazırlanıyor"
            !search && searchInstalling -> "3/3 • Yer adları doğrulanıyor"
            !search && searchDownloading -> "3/3 • Yer adları indiriliyor"
            !search -> "3/3 • Yer arama hazırlanıyor"
            else -> "Türkiye çevrimdışı paketi hazır"
        }
        // Preserve numbered install state across translations as well as UI restarts.
        return when(language.tag.lowercase()) {
            "en" -> step.replace("Harita indiriliyor","Map downloading")
                .replace("Türkiye haritası hazırlanıyor","Preparing map")
                .replace("Rota verisi indiriliyor","Routing data downloading")
                .replace("Rota verisi doğrulanıyor","Verifying routing data")
                .replace("Rota verisi hazırlanıyor","Preparing routing data")
                .replace("Yer adları indiriliyor","Place index downloading")
                .replace("Yer adları doğrulanıyor","Verifying place index")
                .replace("Yer arama hazırlanıyor","Preparing place search")
                .replace("Türkiye çevrimdışı paketi hazır","Türkiye offline data ready")
            else -> step
        }
    }
}
