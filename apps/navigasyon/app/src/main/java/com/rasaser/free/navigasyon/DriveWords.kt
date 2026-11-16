package com.rasaser.free.navigasyon

enum class DriveInfo { TARGET_SELECTED, NEEDS_GPS, NEEDS_ROUTING_DATA, WORKING, ROUTE_FOUND, NO_ROUTE, LONG_PRESS_HINT, REROUTING, ARRIVED }
object DriveWords {
    private val translations: Map<NavLanguage,List<String>> = mapOf(
        NavLanguage.TR to listOf("Hedef seçildi", "GPS konumu bekleniyor",
            "Çevrimdışı rota verisi henüz yüklenmemiş", "Rota hesaplanıyor",
            "Rota hesaplandı", "Bu hedef için rota bulunamadı", "Hedef seçmek için haritaya uzun basın", "Rota yeniden hesaplanıyor", "Hedefe ulaştınız"),
        NavLanguage.EN to listOf("Destination selected","Waiting for GPS",
            "Offline routing data is not installed","Calculating route",
            "Route calculated","No road route found","Long-press the map to choose a destination", "Recalculating route", "Destination reached"),
        NavLanguage.DE to listOf("Ziel gewählt","Warte auf GPS",
            "Offline-Routendaten nicht installiert","Route wird berechnet",
            "Route berechnet","Keine Route gefunden","Karte lange drücken, um ein Ziel zu wählen", "Route wird neu berechnet", "Ziel erreicht"),
        NavLanguage.AR to listOf("تم اختيار الوجهة","انتظار GPS",
            "لم تُثبّت بيانات الطرق دون اتصال","جارٍ حساب المسار",
            "تم حساب المسار","لم يتم العثور على مسار","اضغط مطولاً على الخريطة لاختيار وجهة", "إعادة حساب المسار", "وصلت إلى وجهتك"),
        NavLanguage.ES to listOf("Destino elegido","Esperando GPS",
            "Datos de ruta sin conexión no instalados","Calculando ruta",
            "Ruta calculada","No se encontró ruta","Mantén pulsado el mapa para elegir destino", "Recalculando ruta", "Destino alcanzado"),
        NavLanguage.JA to listOf("目的地を選択","GPSを待っています",
            "オフライン経路データが未保存です","経路を計算しています",
            "経路を計算しました","経路が見つかりません","地図を長押しして目的地を選択", "経路を再計算しています", "目的地に到着しました")
    )
    fun get(lang:NavLanguage,kind:DriveInfo):String =
        requireNotNull(translations[lang]?.getOrNull(kind.ordinal))
    fun complete()=NavLanguage.entries.all{translations[it]?.size==DriveInfo.entries.size}
}
