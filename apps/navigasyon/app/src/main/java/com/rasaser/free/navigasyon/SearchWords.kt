package com.rasaser.free.navigasyon

enum class SearchNotice {
 TITLE, ENTER_PLACE, SEARCH, RESULTS, NO_MATCH, MISSING, DOWNLOAD, DOWNLOADING,
 VERIFYING, READY, DOWNLOAD_ERROR, NO_OFFLINE_SPEECH, SEARCHING, SELECT_ONE, EXPLANATION
}
object SearchWords {
    private val labels:Map<NavLanguage,List<String>> = mapOf(
        NavLanguage.TR to listOf(
          "Gidilecek yeri ara","Hedef","Ara",
          "Hedef seç","Bu isimde bir yer bulunamadı. Farklı bir ad deneyin.",
          "Türkiye çevrimdışı yer arama verisi henüz yüklenmedi.",
          "Yükle / Tekrar dene","Türkiye yer verisi indiriliyor...",
          "Yer verisi doğrulanıyor...","Türkiye çevrimdışı yer arama hazır.",
          "Yer arama paketi indirilemedi veya doğrulanamadı.",
          "Cihazda çevrimdışı ses tanıma bulunmuyor. Hedefi yazarak arayabilirsiniz.",
          "Yer aranıyor...","Birden fazla yer bulundu. Doğru hedefi seçin.",
          "Türkiye genelindeki gerçek işletme, durak ve yer adları için internet gerektirmeyen arama."),
        NavLanguage.EN to listOf(
          "Find destination","Destination","Search","Choose destination",
          "No matching place found. Try another name.","Offline Türkiye place index is not installed.",
          "Download / Retry","Downloading place data...","Verifying offline index...",
          "Türkiye offline place search is ready.","Place data download or verification failed.",
          "Offline speech recognition is not available on this device. Type a destination.",
          "Searching places...","Multiple matches. Select your destination.",
          "Offline search for real businesses, stops and settlements across Türkiye."),
        NavLanguage.DE to listOf(
          "Ziel suchen","Ziel","Suchen","Ziel wählen",
          "Kein passender Ort gefunden.","Offline-Ortsdaten für die Türkei fehlen.",
          "Laden / Wiederholen","Ortsdaten werden geladen...","Ortsdaten werden geprüft...",
          "Offline-Ortssuche ist bereit.","Ortsdaten konnten nicht geprüft werden.",
          "Offline-Spracherkennung ist nicht verfügbar. Ziel bitte eingeben.",
          "Orte werden gesucht...","Mehrere Treffer. Ziel wählen.",
          "Offline-Suche nach Geschäften, Haltestellen und Orten in der Türkei."),
        NavLanguage.AR to listOf(
          "البحث عن الوجهة","الوجهة","بحث","اختر الوجهة",
          "لم يتم العثور على مكان مطابق.","بيانات البحث عن أماكن تركيا غير مثبتة.",
          "تنزيل / إعادة المحاولة","جارٍ تنزيل بيانات الأماكن...",
          "جارٍ التحقق من بيانات الأماكن...","البحث دون اتصال جاهز.",
          "فشل تنزيل بيانات الأماكن أو التحقق منها.",
          "التعرف على الصوت دون اتصال غير متاح. اكتب وجهتك.",
          "جارٍ البحث...","عُثر على عدة أماكن. اختر وجهتك.",
          "بحث دون اتصال عن الشركات والمحطات والأماكن في تركيا."),
        NavLanguage.ES to listOf(
          "Buscar destino","Destino","Buscar","Elegir destino",
          "No se encontró el lugar.","Falta el índice de lugares sin conexión de Turquía.",
          "Descargar / Reintentar","Descargando lugares...","Verificando lugares...",
          "Búsqueda sin conexión preparada.","Falló la descarga o verificación.",
          "El reconocimiento de voz sin conexión no está disponible. Escribe el destino.",
          "Buscando lugares...","Hay varios resultados. Elige el destino.",
          "Búsqueda sin conexión de negocios, paradas y lugares de Turquía."),
        NavLanguage.JA to listOf(
          "目的地を検索","目的地","検索","目的地を選択",
          "一致する場所がありません。","トルコのオフライン場所データがありません。",
          "取得 / 再試行","場所データを取得中...","データを確認中...",
          "オフライン場所検索の準備ができました。","場所データの確認に失敗しました。",
          "オフライン音声認識が使えません。目的地を入力してください。",
          "場所を検索中...","複数の候補があります。目的地を選択してください。",
          "トルコ全域の施設や停留所などをオフラインで検索。")
    )
    fun text(language:NavLanguage,key:SearchNotice):String=
        requireNotNull(labels[language]?.getOrNull(key.ordinal))
    fun complete()=NavLanguage.entries.all{labels[it]?.size==SearchNotice.entries.size}
}
