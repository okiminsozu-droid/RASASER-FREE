package com.rasaser.free.rasaserwidgets

import android.content.Context

/** RaSaSeR owner-only login copy; all six VARYOK languages. */
object OwnerLoginText {
    private val labels = mapOf(
        "tr" to mapOf(
            "inactive" to "RaSaSeR yönetici doğrulaması henüz etkin değil. Mevcut ücretsiz haklarınız çalışmaya devam eder.",
            "title" to "RaSaSeR Sahip Doğrulaması",
            "failed" to "Google doğrulaması tamamlanamadı. Bağlantınızı kontrol ederek tekrar deneyin.",
            "pending" to "Google onayından sonra bu uygulamaya dönün. Onay henüz tamamlanmadı.",
            "check" to "Onayı Kontrol Et",
            "later" to "Daha Sonra",
            "active" to "RaSaSeR Sahip Premium • Süresiz",
            "quota" to "Sınırsız widget oluşturma hakkınız aktif.",
            "privacy" to "Widget bilgileri ve seçilen görseller yalnızca cihazda tutulur. RaSaSeR sahibinin isteğe bağlı Google doğrulamasında doğrulanmış Google hesap kimliği ve cihazın yalnızca açık anahtarı güvenli Cloudflare sunucusuna iletilir. Google şifreniz, fotoğraflarınız ve widget içerikleriniz gönderilmez."
        ),
        "en" to mapOf(
            "inactive" to "RaSaSeR owner verification is not active yet. Your free widgets remain available.",
            "title" to "RaSaSeR Owner Verification",
            "failed" to "Google verification could not be completed. Check your connection and try again.",
            "pending" to "Return to this app after Google approval. Approval is still pending.",
            "check" to "Check Approval",
            "later" to "Later",
            "active" to "RaSaSeR Owner Premium • Lifetime",
            "quota" to "Unlimited widget creation is active.",
            "privacy" to "Widget settings and selected images stay on your device. Optional RaSaSeR owner Google verification sends verified Google account identity and only the device public key to a secure Cloudflare server. Your Google password, photos and widget contents are not sent."
        ),
        "de" to mapOf(
            "inactive" to "Die RaSaSeR-Inhaberverifizierung ist noch nicht aktiv. Kostenlose Widgets bleiben verfügbar.",
            "title" to "RaSaSeR-Inhaberverifizierung",
            "failed" to "Die Google-Verifizierung konnte nicht abgeschlossen werden. Verbindung prüfen und erneut versuchen.",
            "pending" to "Kehren Sie nach der Google-Bestätigung zur App zurück. Die Bestätigung steht noch aus.",
            "check" to "Bestätigung prüfen",
            "later" to "Später",
            "active" to "RaSaSeR Inhaber-Premium • Lebenslang",
            "quota" to "Unbegrenzte Widget-Erstellung ist aktiv.",
            "privacy" to "Widget-Einstellungen und Bilder bleiben auf dem Gerät. Bei der optionalen Google-Inhaberverifizierung werden die bestätigte Google-Kontoidentität und nur der öffentliche Geräteschlüssel an einen sicheren Cloudflare-Server übermittelt. Passwort, Bilder und Widget-Inhalte werden nicht übertragen."
        ),
        "es" to mapOf(
            "inactive" to "La verificación del propietario de RaSaSeR aún no está activa. Tus widgets gratuitos siguen disponibles.",
            "title" to "Verificación del propietario RaSaSeR",
            "failed" to "No se pudo completar la verificación de Google. Comprueba la conexión e inténtalo de nuevo.",
            "pending" to "Vuelve a esta aplicación después de la aprobación de Google. La aprobación está pendiente.",
            "check" to "Comprobar aprobación",
            "later" to "Más tarde",
            "active" to "RaSaSeR Premium del propietario • De por vida",
            "quota" to "Creación ilimitada de widgets activada.",
            "privacy" to "Los ajustes y fotos de widgets permanecen en el dispositivo. La verificación opcional del propietario envía la identidad verificada de Google y solo la clave pública del dispositivo a un servidor seguro de Cloudflare. No se envían contraseña, fotos ni contenidos de widgets."
        ),
        "ja" to mapOf(
            "inactive" to "RaSaSeR所有者認証はまだ有効ではありません。無料ウィジェットは引き続き使用できます。",
            "title" to "RaSaSeR所有者認証",
            "failed" to "Google認証が完了しませんでした。接続を確認して再試行してください。",
            "pending" to "Google承認後、このアプリに戻ってください。承認はまだ保留中です。",
            "check" to "承認を確認",
            "later" to "あとで",
            "active" to "RaSaSeR所有者Premium・無期限",
            "quota" to "ウィジェットを無制限に作成できます。",
            "privacy" to "ウィジェット設定と画像は端末内に保存されます。任意の所有者Google認証では、確認済みGoogleアカウントのIDと端末の公開鍵のみをCloudflareの安全なサーバーに送信します。Googleパスワード、写真、ウィジェット内容は送信しません。"
        ),
        "ar" to mapOf(
            "inactive" to "التحقق من مالك RaSaSeR غير مفعّل بعد. تظل الأدوات المجانية متاحة.",
            "title" to "التحقق من مالك RaSaSeR",
            "failed" to "تعذّر إكمال التحقق من Google. تحقق من الاتصال وحاول مجددًا.",
            "pending" to "ارجع إلى التطبيق بعد موافقة Google. الموافقة لا تزال معلّقة.",
            "check" to "التحقق من الموافقة",
            "later" to "لاحقًا",
            "active" to "RaSaSeR Premium للمالك • مدى الحياة",
            "quota" to "إنشاء أدوات بلا حدود أصبح مفعّلًا.",
            "privacy" to "تبقى إعدادات الأدوات والصور على جهازك. التحقق الاختياري للمالك عبر Google يرسل هوية حساب Google المؤكدة والمفتاح العام للجهاز فقط إلى خادم Cloudflare آمن. لا تُرسل كلمة المرور أو الصور أو محتويات الأدوات."
        )
    )

    fun t(context: Context, key: String): String =
        labels[VaryokText.language(context)]?.get(key)
            ?: labels["tr"]?.get(key)
            ?: key
}
