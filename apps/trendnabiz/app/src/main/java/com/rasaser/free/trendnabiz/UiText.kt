package com.rasaser.free.trendnabiz

enum class UiLang(val code: String, val display: String, val tag: String) {
    TR("tr", "🇹🇷 Türkçe", "tr-TR"),
    EN("en", "🇬🇧 English", "en-US"),
    DE("de", "🇩🇪 Deutsch", "de-DE"),
    ES("es", "🇪🇸 Español", "es-ES"),
    ZH("zh-CN", "🇨🇳 中文", "zh-CN");
    companion object {
        fun parse(raw: String?): UiLang = entries.firstOrNull { it.code == raw } ?: TR
    }
}

object UiText {
    private val words = mapOf(
        "feed" to listOf("Canlı Bildirim Merkezi", "Live alerts", "Live-Meldungen", "Alertas en vivo", "实时通知中心"),
        "settings" to listOf("Ayarlar", "Settings", "Einstellungen", "Ajustes", "设置"),
        "premium" to listOf("Premium Özellikler", "Premium features", "Premium-Funktionen", "Funciones Premium", "高级功能"),
        "slogan" to listOf("Sana onların haberleri geldi mi?", "Did you receive their news?", "Hast du ihre Nachrichten erhalten?", "¿Te han llegado sus noticias?", "你收到他们的消息了吗？"),
        "language" to listOf("Dil seç", "Choose language", "Sprache wählen", "Elegir idioma", "选择语言"),
        "refresh" to listOf("Haberleri yenile", "Refresh events", "Aktualisieren", "Actualizar sucesos", "刷新动态"),
        "alerts" to listOf("Bildirim izni", "Notification permission", "Benachrichtigungen", "Permiso de avisos", "通知权限"),
        "test" to listOf("Test bildirimi gönder", "Send test notification", "Testbenachrichtigung", "Enviar aviso de prueba", "发送测试通知"),
        "translate" to listOf("Çeviri", "Translate", "Übersetzen", "Traducir", "翻译"),
        "listen" to listOf("Dinle", "Listen", "Anhören", "Escuchar", "收听"),
        "share" to listOf("Paylaş", "Share", "Teilen", "Compartir", "分享"),
        "source" to listOf("Kaynak", "Source", "Quelle", "Fuente", "来源"),
        "locked" to listOf("Premium ile açılır", "Requires Premium", "Benötigt Premium", "Requiere Premium", "需要高级会员"),
        "purchase" to listOf("Satın alma henüz etkin değil", "Purchase not enabled yet", "Kauf noch nicht verfügbar", "Compra aún no disponible", "购买暂未开放"),
        "premiumDetail" to listOf(
            "Çeviri, sesli dinleme, paylaşım ve widget Premium kapsamındadır. Aktivasyon doğrulama sistemi yayına alınmadan kilit açılamaz.",
            "Translate, spoken playback, sharing and widgets require Premium. Features remain locked until purchase verification is integrated.",
            "Übersetzen, Vorlesen, Teilen und Widgets erfordern Premium. Freischaltung folgt nach Kaufprüfung.",
            "La traducción, la lectura en voz alta, compartir y los widgets requieren Premium. La activación estará disponible tras verificar compras.",
            "翻译、语音朗读、分享和小组件需要高级会员。购买验证接入后才能解锁。"
        ),
        "live" to listOf("Resmî kaynaklardan son veriler", "Latest official-source data", "Aktuelle offizielle Quellen", "Últimos datos oficiales", "官方最新数据"),
        "newsServer" to listOf("Bağımsız haber sunucusu", "Independent news server", "Unabhängiger Nachrichtenserver", "Servidor independiente de noticias", "独立新闻服务器"),
        "newsServerInfo" to listOf("Kendi HTTPS sunucu adresini girin. Boş bırakılırsa doğrudan USGS deprem verisi kullanılır.", "Enter your own HTTPS server URL. Leave blank for direct USGS earthquake feed.", "Eigene HTTPS-Serveradresse eingeben. Leer = direkter USGS-Erdbebenfeed.", "Indica tu servidor HTTPS. En blanco se usa directamente USGS.", "输入自有 HTTPS 服务器地址。留空使用 USGS 地震数据。"),
        "saveServer" to listOf("Sunucu adresini kaydet", "Save server address", "Serveradresse speichern", "Guardar servidor", "保存服务器"),
        "serverInvalid" to listOf("Geçerli bir HTTPS sunucu adresi girin.", "Enter a valid HTTPS server URL.", "Eine gültige HTTPS-Adresse eingeben.", "Introduce una URL HTTPS válida.", "请输入有效的 HTTPS 服务器地址。"),
        "serverConfigured" to listOf("RaSaSeR bağımsız haber sunucusundan veriler gösteriliyor.", "Showing events from your independent news server.", "Nachrichten vom unabhängigen Server.", "Mostrando noticias desde el servidor independiente.", "正在显示独立服务器的动态。"),
        "onlyUsGs" to listOf("İlk sürümde USGS deprem akışı bağlıdır; dünya haberleri sunucusu henüz bağlı değil.",
            "USGS earthquake feed is connected; the global news server is not yet connected.",
            "USGS-Erdbebenfeed ist verbunden; Weltnachrichtenserver noch nicht.",
            "Feed sísmico USGS conectado; servidor mundial aún no conectado.",
            "已接入 USGS 地震数据；全球新闻服务器尚未连接。"),
        "offline" to listOf("İnternet yoksa kaydedilen haberler görüntülenir.", "Saved events appear offline.",
            "Gespeicherte Meldungen sind offline sichtbar.", "Se muestran los sucesos guardados sin conexión.", "离线时显示已保存的动态。"),
        "noEvents" to listOf("Henüz olay kaydı yok. Yenilemeyi deneyin.", "No events yet. Try refresh.",
            "Noch keine Meldungen. Bitte aktualisieren.", "Sin sucesos. Actualiza.", "暂无动态，请刷新。"),
        "back" to listOf("Geri", "Back", "Zurück", "Volver", "返回"),
        "channel" to listOf("Kritik bildirimler", "Critical alerts", "Kritische Warnungen", "Alertas críticas", "紧急通知"),
        "testStory" to listOf("TRENDNABIZ test bildirimi", "TRENDNABIZ test notification", "TRENDNABIZ Testmeldung", "Notificación de prueba TRENDNABIZ", "TRENDNABIZ 测试通知"),
        "testDetail" to listOf("Bu gerçek bir haber değildir. Bildirim, kilit ekranı ve işlem düğmelerini test etmek içindir.",
            "This is not news. It tests notification display and action buttons.",
            "Keine echte Nachricht. Test für Anzeige und Aktionen.",
            "No es una noticia real. Sirve para probar las notificaciones.",
            "这不是真实新闻，仅用于测试通知。"),
        "notificationsOff" to listOf("Bildirim izni kapalı. Açmak için dokunun.", "Notifications are off. Tap to enable.",
            "Benachrichtigungen aus. Zum Aktivieren tippen.", "Avisos desactivados. Toca para activar.", "通知已关闭，点击开启。"),
        "notifyNormal" to listOf("Tüm yeni olaylar", "All new events", "Alle Ereignisse", "Todos los sucesos", "所有新动态"),
        "notifyCritical" to listOf("Yalnızca kritik olaylar", "Critical only", "Nur kritische Ereignisse", "Solo críticos", "仅紧急动态"),
        "website" to listOf("Web sitemiz", "Our website", "Unsere Website", "Nuestro sitio", "我们的网站"),
        "testSent" to listOf("Test bildirimi hazırlandı.", "Test notification prepared.", "Testmeldung erstellt.", "Aviso de prueba creado.", "测试通知已准备好。"),
        "feature" to listOf("Bildirim ağırlıklı haber deneyimi", "Notification-first news", "Nachrichten zuerst als Meldung", "Noticias por notificaciones", "以通知为核心的新闻体验"),
        "debugPremium" to listOf("Premium test modu (yalnız DEBUG)", "Premium test mode (DEBUG only)", "Premium-Testmodus (nur DEBUG)", "Modo de prueba Premium (solo DEBUG)", "高级功能测试模式（仅调试）"),
        "debugNotice" to listOf("Yalnızca test APK'sında. Yayında doğrulanmış satın alma zorunlu.", "Only in debug builds. Release requires verified purchase.", "Nur Testversion; Veröffentlichung erfordert verifizierten Kauf.", "Solo en pruebas. La versión final exige una compra verificada.", "仅限调试版；正式版需要验证购买。"),
        "billingPending" to listOf("Google Play satın alma doğrulaması henüz bağlanmadı.", "Google Play purchase verification is not connected yet.", "Google Play-Kaufprüfung noch nicht verbunden.", "La verificación de compras de Google Play aún no está conectada.", "尚未接入 Google Play 购买验证。"),
        "widgetLocked" to listOf("Premium widget • TRENDNABIZ'i açın", "Premium widget • open TRENDNABIZ", "Premium-Widget • TRENDNABIZ öffnen", "Widget Premium • abre TRENDNABIZ", "高级小组件 • 打开 TRENDNABIZ"),
        "widgetEmpty" to listOf("Son olay burada görünecek", "Latest event will appear here", "Neueste Meldung erscheint hier", "El último suceso aparecerá aquí", "最新动态将在此显示"),
        "background" to listOf("Arka planda canlı izleme (TEST)", "Background live monitoring (TEST)", "Live-Überwachung im Hintergrund (TEST)", "Monitorización en segundo plano (PRUEBA)", "后台实时监听（测试）"),
        "backgroundHint" to listOf("Bildirim alanında sürekli izleme uyarısı görünür. Pil tasarrufu bağlantıyı kesebilir. Test sürümüne özeldir.", "Persistent monitoring notification shown. Battery policies may disconnect it. Debug build only.", "Permanente Benachrichtigung. Energiesparen kann die Verbindung trennen. Nur Testversion.", "Notificación persistente. El ahorro de batería puede desconectar. Solo prueba.", "显示持续监听通知。省电模式可能断开。仅测试版。"),
        "connectionError" to listOf("Cloudflare bağlantı hatası:", "Cloudflare connection error:", "Cloudflare-Verbindungsfehler:", "Error de conexión a Cloudflare:", "Cloudflare 连接错误："),
        "liveConnected" to listOf("CANLI • Cloudflare bağlı. Arşiv tutulmaz.", "LIVE • Cloudflare connected. No history.", "LIVE • Cloudflare verbunden. Kein Archiv.", "EN VIVO • Cloudflare conectado. Sin archivo.", "实时 • Cloudflare 已连接。无历史归档。"),
        "liveDisconnected" to listOf("Bağlantı kuruluyor. Arka planda bağlantı kapanabilir.", "Connecting. Background connection may close.", "Verbindungsaufbau. Im Hintergrund kann sie enden.", "Conectando. La conexión puede cerrarse en segundo plano.", "正在连接。后台连接可能中断。"),
        "liveEmpty" to listOf("Yeni olay bekleniyor; geçmiş haberler gösterilmez.", "Waiting for new events; past news is not shown.", "Warten auf neue Ereignisse. Kein Verlauf.", "Esperando nuevas noticias. Sin historial.", "等待新动态。不显示旧新闻。"),
        "reconnect" to listOf("Canlı bağlantıyı yenile", "Reconnect live", "Live-Verbindung erneuern", "Reconectar en vivo", "重新连接实时推送"),
        "about" to listOf("Lisans ve kaynaklar", "Licenses and sources", "Lizenzen und Quellen", "Licencias y fuentes", "许可与来源")
    )
    fun get(lang: UiLang, key: String): String = words[key]?.get(lang.ordinal) ?: key
}
