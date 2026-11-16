package com.rasaser.free.navigasyon

enum class DriveLocationWord { MY_LOCATION, GRANT_GPS, GPS_DISABLED, GPS_PENDING, OPEN_GPS_SETTINGS }
object DriveLocationText {
    private val texts:Map<NavLanguage,List<String>> = mapOf(
        NavLanguage.TR to listOf("Konumum",
            "Haritanın konumunu göstermesi için konum izni verin.",
            "Cihazın konum servisi kapalı. Açmak için Konum Ayarları'na gidin.",
            "GPS konumu aranıyor. Açık alanda uydu konumu birkaç dakika sürebilir; harita yine sürüş yakınlığında kalır.",
            "Konum ayarları"),
        NavLanguage.EN to listOf("My location","Allow location to show your position on the map.",
            "Device location is off. Turn it on in settings.",
            "Searching for GPS. A satellite fix may take minutes outdoors; the map remains in driving view.",
            "Location settings"),
        NavLanguage.DE to listOf("Mein Standort","Standortberechtigung für die Kartenposition zulassen.",
            "Gerätestandort ist deaktiviert. Bitte einschalten.",
            "GPS wird gesucht. Ein Satellitensignal kann draußen einige Minuten dauern.",
            "Standorteinstellungen"),
        NavLanguage.AR to listOf("موقعي","اسمح بالوصول إلى الموقع لعرض موقعك على الخريطة.",
            "خدمة الموقع مغلقة. فعّلها من الإعدادات.",
            "جارٍ البحث عن GPS. قد يستغرق التقاط إشارة الأقمار دقائق.",
            "إعدادات الموقع"),
        NavLanguage.ES to listOf("Mi ubicación","Permite acceder a tu ubicación para mostrarla en el mapa.",
            "La ubicación del dispositivo está desactivada. Actívala en ajustes.",
            "Buscando GPS. La señal de satélite puede tardar unos minutos al aire libre.",
            "Ajustes de ubicación"),
        NavLanguage.JA to listOf("現在地","地図上に位置を表示するため位置情報を許可してください。",
            "端末の位置情報が無効です。設定で有効にしてください。",
            "GPSを検索しています。屋外での測位に数分かかる場合があります。",
            "位置情報設定")
    )
    fun get(lang:NavLanguage,key:DriveLocationWord):String =
        requireNotNull(texts[lang]?.getOrNull(key.ordinal))
    fun complete()=NavLanguage.entries.all { texts[it]?.size==DriveLocationWord.entries.size }
}
