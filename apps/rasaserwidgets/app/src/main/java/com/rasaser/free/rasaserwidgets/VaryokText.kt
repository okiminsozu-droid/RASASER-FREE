package com.rasaser.free.rasaserwidgets

import android.content.Context

/** Offline translations and one-shot editor selection, independent of the widget engine. */
object VaryokText {
    val codes = listOf("tr", "en", "de", "es", "ja", "ar")
    val names = listOf("🇹🇷 Türkçe", "🇬🇧 English", "🇩🇪 Deutsch",
        "🇪🇸 Español", "🇯🇵 日本語", "🇸🇦 العربية")
    private val keys = listOf("create", "lang", "settings", "choose", "photo", "transparent", "app", "web", "templates", "dial", "note", "wifi", "bt", "system", "torch", "quota", "premium", "premium_desc", "cancel", "ok", "manage", "empty", "edit", "privacy", "privacy_desc", "about", "pin_fail", "pin_help", "template_name", "simple", "site", "premium_note")
    private val values = mapOf(
        "tr" to listOf("Widget Oluştur", "Dil", "Ayarlar", "Widget türünü seç", "Fotoğraflı Widget", "Şeffaf Kısayol", "Uygulama Kısayolu", "Web Kısayolu", "Hazır Şablonlar", "Telefon Arama", "Kişisel Not", "Wi-Fi Ayarları", "Bluetooth Ayarları", "Sistem Ayarları", "El Feneri", "Kalan ücretsiz hak: %d/3", "3 ücretsiz widget hakkın doldu.", "Daha fazlası için Premium gerekir. Google Play ödeme işlemi henüz etkinleştirilmedi.", "İptal", "Tamam", "Widget'larım", "Henüz oluşturulmuş widget yok.", "Düzenle", "Gizlilik", "Fotoğraflar ve ayarlar cihazında kalır. Reklam veya takip yok.", "Hakkında", "Ana ekran widget isteğini kabul etmedi.", "Ana ekranda boş alana uzun bas → Widget'lar → VARYOK → Ekle.", "Şablon seç", "Sade", "www.okiminsozu.com", "3 widget ücretsiz • Sonrası Premium"),
        "en" to listOf("Create Widget", "Language", "Settings", "Choose widget type", "Photo Widget", "Transparent Shortcut", "App Shortcut", "Web Shortcut", "Ready Templates", "Phone Dialer", "Personal Note", "Wi-Fi Settings", "Bluetooth Settings", "System Settings", "Flashlight", "Free widgets left: %d/3", "Your 3 free widgets are used.", "Premium is required for more widgets. Google Play purchases are not active yet.", "Cancel", "OK", "My Widgets", "No widgets created yet.", "Edit", "Privacy", "Photos and settings stay on your device. No ads or tracking.", "About", "Home screen rejected the widget request.", "Long press an empty space on your home screen → Widgets → VARYOK → Add.", "Choose template", "Minimal", "www.okiminsozu.com", "3 free widgets • More with Premium"),
        "de" to listOf("Widget erstellen", "Sprache", "Einstellungen", "Widget-Typ auswählen", "Foto-Widget", "Transparente Verknüpfung", "App-Verknüpfung", "Web-Verknüpfung", "Vorlagen", "Telefonwahl", "Persönliche Notiz", "WLAN-Einstellungen", "Bluetooth-Einstellungen", "Systemeinstellungen", "Taschenlampe", "Gratis-Widgets übrig: %d/3", "Deine 3 Gratis-Widgets sind aufgebraucht.", "Für weitere Widgets ist Premium erforderlich. Google-Play-Käufe sind noch nicht aktiv.", "Abbrechen", "OK", "Meine Widgets", "Noch keine Widgets erstellt.", "Bearbeiten", "Datenschutz", "Fotos und Einstellungen bleiben auf dem Gerät. Keine Werbung oder Verfolgung.", "Über die App", "Widget-Anfrage wurde abgelehnt.", "Auf dem Startbildschirm lange drücken → Widgets → VARYOK → Hinzufügen.", "Vorlage auswählen", "Einfach", "www.okiminsozu.com", "3 Widgets gratis • Mehr mit Premium"),
        "es" to listOf("Crear widget", "Idioma", "Ajustes", "Elige tipo de widget", "Widget con foto", "Acceso transparente", "Acceso a aplicación", "Acceso web", "Plantillas", "Marcar teléfono", "Nota personal", "Ajustes Wi-Fi", "Ajustes Bluetooth", "Ajustes del sistema", "Linterna", "Widgets gratis restantes: %d/3", "Ya utilizaste los 3 widgets gratis.", "Para más widgets necesitas Premium. Las compras de Google Play aún no están disponibles.", "Cancelar", "Aceptar", "Mis widgets", "Todavía no hay widgets.", "Editar", "Privacidad", "Fotos y ajustes permanecen en tu dispositivo. Sin anuncios ni seguimiento.", "Acerca de", "Se rechazó la solicitud de widget.", "Mantén pulsada la pantalla de inicio → Widgets → VARYOK → Añadir.", "Elegir plantilla", "Sencillo", "www.okiminsozu.com", "3 widgets gratis • Más con Premium"),
        "ja" to listOf("ウィジェットを作成", "言語", "設定", "ウィジェットの種類", "写真ウィジェット", "透明ショートカット", "アプリのショートカット", "ウェブのショートカット", "テンプレート", "電話画面", "個人メモ", "Wi-Fi設定", "Bluetooth設定", "端末設定", "ライト", "無料作成の残り: %d/3", "無料の3個を使い切りました。", "追加作成にはプレミアムが必要です。Google Playの購入はまだ有効ではありません。", "キャンセル", "確認", "マイウィジェット", "ウィジェットはまだありません。", "編集", "プライバシー", "写真と設定は端末に保存されます。広告や追跡はありません。", "アプリ情報", "追加が拒否されました。", "ホーム画面を長押し → ウィジェット → VARYOK → 追加。", "テンプレートを選択", "シンプル", "www.okiminsozu.com", "3個まで無料 • 追加はプレミアム"),
        "ar" to listOf("إنشاء ويدجت", "اللغة", "الإعدادات", "اختر نوع الويدجت", "ويدجت بالصور", "اختصار شفاف", "اختصار تطبيق", "اختصار موقع", "قوالب جاهزة", "الاتصال", "ملاحظة شخصية", "إعدادات Wi-Fi", "إعدادات البلوتوث", "إعدادات النظام", "المصباح", "المتبقي مجانًا: %d/3", "استخدمت الويدجت الثلاثة المجانية.", "يلزم الاشتراك المميز للمزيد. الشراء عبر Google Play غير متاح حاليًا.", "إلغاء", "حسنًا", "ويدجت الخاصة بي", "لم تنشئ أي ويدجت بعد.", "تعديل", "الخصوصية", "تبقى الصور والإعدادات على جهازك. بلا إعلانات أو تتبع.", "حول التطبيق", "تم رفض إضافة الويدجت.", "اضغط مطولًا على الشاشة الرئيسية ← الويدجت ← VARYOK ← إضافة.", "اختر قالبًا", "بسيط", "www.okiminsozu.com", "3 ويدجت مجانًا • المزيد مع المميز")
    )
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("varyok_ui", Context.MODE_PRIVATE)
    fun language(ctx: Context): String = prefs(ctx).getString("lang", "tr").let {
        if (it in codes) it!! else "tr"
    }
    fun setLanguage(ctx: Context, code: String) {
        if (code in codes) prefs(ctx).edit().putString("lang", code).apply()
    }
    fun t(ctx: Context, key: String): String {
        val index = keys.indexOf(key)
        return if (index >= 0) values[language(ctx)]?.get(index) ?: values["tr"]!![index] else key
    }
    fun rememberPreset(ctx: Context, kind: String, opacity: Int = 100) {
        prefs(ctx).edit().putString("pending_kind", kind).putInt("pending_opacity", opacity).apply()
    }
    fun takePreset(ctx: Context): Pair<String, Int>? {
        val kind = prefs(ctx).getString("pending_kind", null) ?: return null
        val opacity = prefs(ctx).getInt("pending_opacity", 100).coerceIn(0, 100)
        prefs(ctx).edit().remove("pending_kind").remove("pending_opacity").apply()
        return kind to opacity
    }
}
