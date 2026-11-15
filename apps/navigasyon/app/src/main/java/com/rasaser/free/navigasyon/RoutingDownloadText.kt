package com.rasaser.free.navigasyon

enum class RoutingDownloadStatus { BUTTON, EXPLANATION, WIFI_ONLY, DOWNLOADING, VERIFYING, READY, ERROR }

object RoutingDownloadText {
    private val lang: Map<NavLanguage, List<String>> = mapOf(
        NavLanguage.TR to listOf("Türkiye rota verisini indir (1,15 GB)",
          "Hedef seçerek yol tarifi için ayrıca çevrimdışı rota paketi gerekir. İndirme yalnız Wi-Fi ile başlatılır.",
          "Rota verisini indirmek için Wi-Fi bağlantısı gerekiyor.",
          "Türkiye rota verisi indiriliyor...", "Rota verisi doğrulanıyor...",
          "Türkiye çevrimdışı rota verisi hazır.", "Rota paketi indirilemedi veya doğrulanamadı."),
        NavLanguage.EN to listOf("Download Türkiye routing data (1.15 GB)",
          "Offline turn-by-turn routing requires a separate road graph. Large download uses Wi-Fi only.",
          "Connect to Wi-Fi to download routing data.", "Downloading Türkiye routing data...",
          "Verifying road data...", "Türkiye offline routing data is ready.",
          "Routing package download or validation failed."),
        NavLanguage.DE to listOf("Türkei-Routendaten laden (1,15 GB)",
          "Für Offline-Wegbeschreibungen ist ein separates Straßennetz nötig. Download nur per WLAN.",
          "Für Routendaten ist WLAN erforderlich.", "Türkei-Routendaten werden geladen...",
          "Straßendaten werden geprüft...", "Türkei-Offline-Routendaten sind bereit.",
          "Routendaten konnten nicht geladen oder geprüft werden."),
        NavLanguage.AR to listOf("تنزيل بيانات الطرق لتركيا (1.15 غيغابايت)",
          "تتطلب الملاحة دون اتصال ملف طرق منفصلاً. التنزيل عبر Wi-Fi فقط.",
          "اتصل بشبكة Wi-Fi لتنزيل بيانات الطرق.", "جارٍ تنزيل بيانات الطرق...",
          "جارٍ التحقق من بيانات الطرق...", "بيانات الطرق دون اتصال جاهزة.",
          "فشل تنزيل بيانات الطرق أو التحقق منها."),
        NavLanguage.ES to listOf("Descargar rutas de Turquía (1,15 GB)",
          "Para navegar sin conexión se necesita un archivo adicional de rutas. Solo por Wi-Fi.",
          "Conéctate a Wi-Fi para descargar las rutas.", "Descargando datos de rutas...",
          "Verificando datos de rutas...", "Rutas sin conexión listas.",
          "No se pudieron descargar o verificar los datos."),
        NavLanguage.JA to listOf("トルコの経路データを取得 (1.15 GB)",
          "オフライン経路案内には別の道路データが必要です。Wi-Fiのみで取得します。",
          "経路データの取得にはWi-Fiが必要です。", "経路データを取得しています...",
          "経路データを検証しています...", "トルコのオフライン経路データが準備できました。",
          "経路データの取得または検証に失敗しました。")
    )
    fun get(language:NavLanguage,status:RoutingDownloadStatus):String=
        requireNotNull(lang[language]?.getOrNull(status.ordinal))
    fun complete():Boolean=NavLanguage.entries.all{lang[it]?.size==RoutingDownloadStatus.entries.size}
}
