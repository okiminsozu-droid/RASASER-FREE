package com.rasaser.free.navigasyon

/** User-controlled complete country installation; never claims downloads are complete early. */
object TurkeyPackageUi {
    fun button(language:NavLanguage, ready:Boolean, resume:Boolean=false):String {
        val languageTag=language.tag.lowercase()
        if(ready) return when(languageTag) {
            "en" -> "Türkiye is ready"
            "de" -> "Türkei ist bereit"
            "es" -> "Turquía está lista"
            "ar" -> "تركيا جاهزة"
            "ja" -> "トルコのデータは準備完了"
            else -> "Türkiye hazır"
        }
        if(resume) return when(languageTag) {
            "en" -> "Resume Türkiye installation"
            "de" -> "Türkei-Installation fortsetzen"
            "es" -> "Reanudar instalación de Turquía"
            "ar" -> "متابعة تثبيت تركيا"
            "ja" -> "トルコのインストールを再開"
            else -> "Kaldığı yerden devam et"
        }
        return when(languageTag) {
            "en" -> "Download Türkiye • 1.82 GB"
            "de" -> "Türkei herunterladen • 1,82 GB"
            "es" -> "Descargar Turquía • 1,82 GB"
            "ar" -> "تنزيل خريطة تركيا • 1.82 غيغابايت"
            "ja" -> "トルコをダウンロード • 1.82 GB"
            else -> "Türkiye haritasını indir • 1,82 GB"
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
    /** No storage provider address appears in these user-facing strings. */
    fun workerStatus(language:NavLanguage,stage:Int,percent:Int,waiting:Boolean):String {
        val tag=language.tag.lowercase()
        val label=when(stage) {
            1 -> when(tag) {
                "en" -> "1/3 • Türkiye map"
                "de" -> "1/3 • Türkei-Karte"
                "es" -> "1/3 • Mapa de Turquía"
                "ar" -> "1/3 • خريطة تركيا"
                "ja" -> "1/3 • トルコ地図"
                else -> "1/3 • Türkiye haritası"
            }
            2 -> when(tag) {
                "en" -> "2/3 • Offline routes"
                "de" -> "2/3 • Offline-Routen"
                "es" -> "2/3 • Rutas sin conexión"
                "ar" -> "2/3 • المسارات دون اتصال"
                "ja" -> "2/3 • オフライン経路"
                else -> "2/3 • Çevrimdışı rotalar"
            }
            3 -> when(tag) {
                "en" -> "3/3 • Offline place search"
                "de" -> "3/3 • Offline-Ortssuche"
                "es" -> "3/3 • Búsqueda sin conexión"
                "ar" -> "3/3 • البحث عن الأماكن"
                "ja" -> "3/3 • オフライン場所検索"
                else -> "3/3 • Çevrimdışı yer arama"
            }
            4 -> return button(language,true)
            5 -> return when(tag) {
                "en" -> "3/3 • Verifying place index • Download complete"
                "de" -> "3/3 • Ortsindex wird geprüft • Download abgeschlossen"
                "es" -> "3/3 • Verificando lugares • Descarga finalizada"
                "ar" -> "3/3 • التحقق من الأماكن • اكتمل التنزيل"
                "ja" -> "3/3 • 場所データを検証中 • ダウンロード完了"
                else -> "3/3 • Yer arama doğrulanıyor • İndirme tamamlandı"
            }
            else -> when(tag) {
                "en" -> "Waiting for Wi-Fi"
                "de" -> "Warten auf WLAN"
                "es" -> "Esperando Wi-Fi"
                "ar" -> "في انتظار شبكة Wi-Fi"
                "ja" -> "Wi-Fi接続待ち"
                else -> "Wi-Fi bağlantısı bekleniyor"
            }
        }
        val resume=if(!waiting) "" else when(tag) {
            "en" -> " • Resumes automatically"
            "de" -> " • Setzt automatisch fort"
            "es" -> " • Reanuda automáticamente"
            "ar" -> " • يُستأنف تلقائياً"
            "ja" -> " • 自動再開"
            else -> " • Otomatik devam eder"
        }
        return "$label • $percent%$resume"
    }

    fun error(language:NavLanguage,code:String):String {
        val storage=code.contains("storage",ignoreCase=true)
        val search=code.startsWith("search_",ignoreCase=true)
        return when(language.tag.lowercase()) {
            "en" -> when {
                storage -> "Not enough free storage. Free up space and tap Resume."
                search -> "Place index verification could not finish. Tap Resume; downloaded maps are kept."
                else -> "Installation paused. Check Wi-Fi, then tap Resume; existing files are kept."
            }
            "de" -> when {
                storage -> "Zu wenig Speicher. Platz schaffen und Fortsetzen wählen."
                search -> "Ortsindex nicht fertig geprüft. Fortsetzen; Karten bleiben erhalten."
                else -> "Installation pausiert. WLAN prüfen und fortsetzen."
            }
            "es" -> when {
                storage -> "Falta espacio libre. Libera espacio y pulsa Reanudar."
                search -> "No se completó la verificación de lugares. Reanuda sin borrar mapas."
                else -> "Instalación pausada. Revisa Wi-Fi y reanuda."
            }
            "ar" -> when {
                storage -> "لا توجد مساحة كافية. حرر مساحة ثم تابع."
                search -> "لم يكتمل فحص الأماكن. تابع دون حذف الخرائط."
                else -> "توقف التثبيت مؤقتًا. تحقق من الشبكة ثم تابع."
            }
            "ja" -> when {
                storage -> "空き容量不足です。容量を確保して再開してください。"
                search -> "場所データの検証が未完了です。既存の地図を残して再開できます。"
                else -> "インストールは一時停止しました。通信を確認して再開してください。"
            }
            else -> when {
                storage -> "Boş depolama alanı yetersiz. Yer açıp 'Kaldığı yerden devam et'e basın."
                search -> "Yer arama doğrulaması tamamlanamadı (hata: ${code.take(42)}). Haritalar korunuyor; kaldığı yerden devam edin."
                else -> "Kurulum durakladı. Wi-Fi'yi kontrol edip kaldığı yerden devam edin."
            }
        }
    }
}
