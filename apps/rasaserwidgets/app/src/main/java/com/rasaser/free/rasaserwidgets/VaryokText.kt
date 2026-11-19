package com.rasaser.free.rasaserwidgets

import android.content.Context

/** Offline translations and one-shot editor selection, independent of the widget engine. */
object VaryokText {
    val codes = listOf("tr", "en", "de", "es", "ja", "ar")
    val names = listOf("🇹🇷 Türkçe", "🇬🇧 English", "🇩🇪 Deutsch",
        "🇪🇸 Español", "🇯🇵 日本語", "🇸🇦 العربية")
    private val additions = mapOf(
        "tr" to mapOf(
            "apps_all" to "TELEFONUMDAKİ UYGULAMALAR",
            "apps_search" to "Uygulama ara...",
            "apps_hint" to "Yeni yüklenen uygulamalar otomatik görünür. Yeni fotoğrafları galeri seçicisinde bulabilirsiniz.",
            "apps_empty" to "Listelenecek uygulama yok."
        ),
        "en" to mapOf(
            "apps_all" to "APPS ON MY PHONE",
            "apps_search" to "Search apps...",
            "apps_hint" to "Newly installed apps appear automatically. New photos are available in the gallery picker.",
            "apps_empty" to "No apps to show."
        ),
        "de" to mapOf(
            "apps_all" to "APPS AUF MEINEM GERÄT",
            "apps_search" to "Apps suchen...",
            "apps_hint" to "Neue Apps erscheinen automatisch. Neue Fotos sind in der Galerieauswahl verfügbar.",
            "apps_empty" to "Keine Apps gefunden."
        ),
        "es" to mapOf(
            "apps_all" to "APLICACIONES DEL TELÉFONO",
            "apps_search" to "Buscar aplicaciones...",
            "apps_hint" to "Las nuevas apps aparecen automáticamente. Las fotos nuevas están en el selector de galería.",
            "apps_empty" to "No hay aplicaciones."
        ),
        "ja" to mapOf(
            "apps_all" to "端末内のアプリ",
            "apps_search" to "アプリを検索...",
            "apps_hint" to "新しいアプリは自動で表示されます。新しい写真はギャラリーから選べます。",
            "apps_empty" to "アプリがありません。"
        ),
        "ar" to mapOf(
            "apps_all" to "التطبيقات على هاتفي",
            "apps_search" to "البحث عن التطبيقات...",
            "apps_hint" to "تظهر التطبيقات المثبتة حديثًا تلقائيًا. تتوفر الصور الجديدة عبر معرض الصور.",
            "apps_empty" to "لا توجد تطبيقات للعرض."
        )
    )
    private val keys = listOf("create", "lang", "settings", "choose", "photo", "transparent", "app", "web", "templates", "dial", "note", "wifi", "bt", "system", "torch", "quota", "premium", "premium_desc", "cancel", "ok", "manage", "empty", "edit", "privacy", "privacy_desc", "about", "pin_fail", "pin_help", "template_name", "simple", "site", "premium_note")
    private val values = mapOf(
        "tr" to listOf("Widget Oluştur", "Dil", "Ayarlar", "Widget türünü seç", "Fotoğraflı Widget", "Şeffaf Kısayol", "Uygulama Kısayolu", "Web Kısayolu", "Hazır Şablonlar", "Telefon Arama", "Kişisel Not", "Wi-Fi Ayarları", "Bluetooth Ayarları", "Sistem Ayarları", "El Feneri", "Kalan farklı uygulama hakkı: %d/3", "3 farklı uygulama hakkın doldu.", "Aynı uygulama için yeniden widget oluşturabilirsin. Dördüncü farklı uygulama Premium gerektirir. Google Play ödeme henüz etkin değil.", "İptal", "Tamam", "Widget'larım", "Henüz oluşturulmuş widget yok.", "Düzenle", "Gizlilik", "Kişisel verileriniz sunucularımıza gönderilmez. Üyelik, reklam takibi veya analitik bulunmaz. Widget ayarları ve seçtiğiniz görseller yalnızca cihazınızda saklanır.", "Hakkında", "Ana ekran widget isteğini kabul etmedi.", "Ana ekranda boş alana uzun bas → Widget'lar → VARYOK → Ekle.", "Şablon seç", "Sade", "www.okiminsozu.com", "3 farklı uygulama ücretsiz • Sonrası Premium"),
        "en" to listOf("Create Widget", "Language", "Settings", "Choose widget type", "Photo Widget", "Transparent Shortcut", "App Shortcut", "Web Shortcut", "Ready Templates", "Phone Dialer", "Personal Note", "Wi-Fi Settings", "Bluetooth Settings", "System Settings", "Flashlight", "Free app slots left: %d/3", "You have used 3 different app slots.", "Widgets for the same apps remain free. A fourth different app requires Premium. Google Play purchases are not active yet.", "Cancel", "OK", "My Widgets", "No widgets created yet.", "Edit", "Privacy", "No personal data is sent to our servers. No account, advertising tracking or analytics. Only widget settings and selected images are stored locally on your device.", "About", "Home screen rejected the widget request.", "Long press an empty space on your home screen → Widgets → VARYOK → Add.", "Choose template", "Minimal", "www.okiminsozu.com", "3 different apps free • More with Premium"),
        "de" to listOf("Widget erstellen", "Sprache", "Einstellungen", "Widget-Typ auswählen", "Foto-Widget", "Transparente Verknüpfung", "App-Verknüpfung", "Web-Verknüpfung", "Vorlagen", "Telefonwahl", "Persönliche Notiz", "WLAN-Einstellungen", "Bluetooth-Einstellungen", "Systemeinstellungen", "Taschenlampe", "Freie App-Plätze übrig: %d/3", "Die drei verschiedenen App-Plätze sind belegt.", "Weitere Widgets derselben Apps sind gratis. Eine vierte andere App benötigt Premium. Google-Play-Käufe sind noch nicht aktiv.", "Abbrechen", "OK", "Meine Widgets", "Noch keine Widgets erstellt.", "Bearbeiten", "Datenschutz", "Es werden keine persönlichen Daten an unsere Server gesendet. Kein Konto, Werbetracking oder Analyse. Nur Widget-Einstellungen und ausgewählte Bilder bleiben lokal auf Ihrem Gerät.", "Über die App", "Widget-Anfrage wurde abgelehnt.", "Auf dem Startbildschirm lange drücken → Widgets → VARYOK → Hinzufügen.", "Vorlage auswählen", "Einfach", "www.okiminsozu.com", "3 verschiedene Apps gratis • Mehr mit Premium"),
        "es" to listOf("Crear widget", "Idioma", "Ajustes", "Elige tipo de widget", "Widget con foto", "Acceso transparente", "Acceso a aplicación", "Acceso web", "Plantillas", "Marcar teléfono", "Nota personal", "Ajustes Wi-Fi", "Ajustes Bluetooth", "Ajustes del sistema", "Linterna", "Apps diferentes restantes: %d/3", "Ya utilizaste las 3 aplicaciones distintas gratuitas.", "Puedes crear más widgets para esas aplicaciones. Una cuarta app diferente requiere Premium. Las compras de Google Play aún no están disponibles.", "Cancelar", "Aceptar", "Mis widgets", "Todavía no hay widgets.", "Editar", "Privacidad", "No se envían datos personales a nuestros servidores. Sin cuenta, seguimiento publicitario ni análisis. Solo los ajustes de los widgets y las fotos elegidas se guardan en tu dispositivo.", "Acerca de", "Se rechazó la solicitud de widget.", "Mantén pulsada la pantalla de inicio → Widgets → VARYOK → Añadir.", "Elegir plantilla", "Sencillo", "www.okiminsozu.com", "3 apps diferentes gratis • Más con Premium"),
        "ja" to listOf("ウィジェットを作成", "言語", "設定", "ウィジェットの種類", "写真ウィジェット", "透明ショートカット", "アプリのショートカット", "ウェブのショートカット", "テンプレート", "電話画面", "個人メモ", "Wi-Fi設定", "Bluetooth設定", "端末設定", "ライト", "異なるアプリの無料枠: %d/3", "3種類のアプリの無料枠を使い切りました。", "同じアプリのウィジェットは引き続き無料です。4種類目のアプリにはプレミアムが必要です。Google Play購入はまだ利用できません。", "キャンセル", "確認", "マイウィジェット", "ウィジェットはまだありません。", "編集", "プライバシー", "個人データは当社サーバーに送信されません。アカウント登録、広告追跡、分析はありません。ウィジェット設定と選択した画像のみ端末内に保存されます。", "アプリ情報", "追加が拒否されました。", "ホーム画面を長押し → ウィジェット → VARYOK → 追加。", "テンプレートを選択", "シンプル", "www.okiminsozu.com", "異なるアプリ3種類まで無料 • 追加はプレミアム"),
        "ar" to listOf("إنشاء ويدجت", "اللغة", "الإعدادات", "اختر نوع الويدجت", "ويدجت بالصور", "اختصار شفاف", "اختصار تطبيق", "اختصار موقع", "قوالب جاهزة", "الاتصال", "ملاحظة شخصية", "إعدادات Wi-Fi", "إعدادات البلوتوث", "إعدادات النظام", "المصباح", "التطبيقات المختلفة المتبقية: %d/3", "استخدمت ثلاث تطبيقات مختلفة مجانًا.", "يمكنك إنشاء ويدجت إضافية للتطبيقات نفسها مجانًا. التطبيق الرابع المختلف يحتاج إلى Premium. الشراء عبر Google Play غير متاح حاليًا.", "إلغاء", "حسنًا", "ويدجت الخاصة بي", "لم تنشئ أي ويدجت بعد.", "تعديل", "الخصوصية", "لا تُرسل بياناتك الشخصية إلى خوادمنا. لا حسابات ولا تتبع إعلاني أو تحليلات. تُحفظ إعدادات الويدجت والصور المختارة محليًا على جهازك فقط.", "حول التطبيق", "تم رفض إضافة الويدجت.", "اضغط مطولًا على الشاشة الرئيسية ← الويدجت ← VARYOK ← إضافة.", "اختر قالبًا", "بسيط", "www.okiminsozu.com", "3 تطبيقات مختلفة مجانًا • المزيد مع Premium")
    )
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("varyok_ui", Context.MODE_PRIVATE)
    fun language(ctx: Context): String = prefs(ctx).getString("lang", "tr").let {
        if (it in codes) it!! else "tr"
    }
    fun setLanguage(ctx: Context, code: String) {
        if (code in codes) prefs(ctx).edit().putString("lang", code).apply()
    }
    fun t(ctx: Context, key: String): String {
        additions[language(ctx)]?.get(key)?.let { return it }
        val index = keys.indexOf(key)
        return if (index >= 0) values[language(ctx)]?.get(index) ?: values["tr"]!![index] else key
    }
    data class Preset(val kind: String, val opacity: Int, val target: String, val caption: String)
    fun rememberPreset(ctx: Context, kind: String, opacity: Int = 100,
                       target: String = "", caption: String = "") {
        prefs(ctx).edit()
            .putString("pending_kind", kind)
            .putInt("pending_opacity", opacity.coerceIn(0, 100))
            .putString("pending_target", target)
            .putString("pending_caption", caption)
            .putLong("pending_created_at", android.os.SystemClock.elapsedRealtime())
            .apply()
    }
    fun clearPreset(ctx: Context) {
        prefs(ctx).edit().remove("pending_kind").remove("pending_opacity")
            .remove("pending_target").remove("pending_caption")
            .remove("pending_created_at").apply()
    }
    fun takePreset(ctx: Context): Preset? {
        val p = prefs(ctx)
        val kind = p.getString("pending_kind", null) ?: return null
        val age = android.os.SystemClock.elapsedRealtime() - p.getLong("pending_created_at", 0L)
        val result = if (age >= 0L && age <= 600_000L) {
            Preset(kind, p.getInt("pending_opacity", 100).coerceIn(0, 100),
                p.getString("pending_target", "") ?: "", p.getString("pending_caption", "") ?: "")
        } else null
        clearPreset(ctx)
        return result
    }
}
