package com.rasaser.free.rasaserwidgets

import android.content.Context

/** RaSaSeR owner-only login copy; all six VARYOK languages. */
object OwnerLoginText {
    private val labels = mapOf(
        "tr" to mapOf(
            "inactive" to "Güvenli yönetici girişine şu anda bağlanılamadı. Lütfen daha sonra tekrar deneyin.",
            "title" to "RaSaSeR Sahip Doğrulaması",
            "failed" to "Google doğrulaması tamamlanamadı. Bağlantınızı kontrol ederek tekrar deneyin.",
            "pending" to "Google onayından sonra bu uygulamaya dönün. Onay henüz tamamlanmadı.",
            "check" to "Onayı Kontrol Et",
            "later" to "Daha Sonra",
            "active" to "Premium • Süresiz",
            "quota" to "Sınırsız widget oluşturma hakkınız aktif.",
            "privacy" to "Widget ayarları ve seçilen fotoğraflar yalnızca telefonunuzda tutulur. Promosyon kodu etkinleştirilirken kod, uygulama kimliği ve rastgele oluşturulmuş kurulum kimliği güvenli doğrulama servisine gönderilir. Kod açık olarak saklanmaz; reklam veya takip yapılmaz.",
        ),
        "en" to mapOf(
            "inactive" to "Secure owner sign-in is unavailable right now. Please try again later.",
            "title" to "RaSaSeR Owner Verification",
            "failed" to "Google verification could not be completed. Check your connection and try again.",
            "pending" to "Return to this app after Google approval. Approval is still pending.",
            "check" to "Check Approval",
            "later" to "Later",
            "active" to "Premium • Lifetime",
            "quota" to "Unlimited widget creation is active.",
            "privacy" to "Widget settings and selected photos stay on your device. When activating a promo code, the code, app identifier and a randomly generated installation identifier are sent to our secure verification service. The code is not stored in plain text; there are no ads or tracking.",
        ),
        "de" to mapOf(
            "inactive" to "Die sichere Inhaberanmeldung ist derzeit nicht erreichbar. Bitte versuchen Sie es später erneut.",
            "title" to "RaSaSeR-Inhaberverifizierung",
            "failed" to "Die Google-Verifizierung konnte nicht abgeschlossen werden. Verbindung prüfen und erneut versuchen.",
            "pending" to "Kehren Sie nach der Google-Bestätigung zur App zurück. Die Bestätigung steht noch aus.",
            "check" to "Bestätigung prüfen",
            "later" to "Später",
            "active" to "Premium • Dauerhaft",
            "quota" to "Unbegrenzte Widget-Erstellung ist aktiv.",
            "privacy" to "Widget-Einstellungen und ausgewählte Fotos bleiben auf Ihrem Gerät. Beim Einlösen eines Promocodes werden Code, App-Kennung und eine zufällig erzeugte Installationskennung an unseren sicheren Prüfdienst gesendet. Der Code wird nicht im Klartext gespeichert; keine Werbung oder Nachverfolgung.",
        ),
        "es" to mapOf(
            "inactive" to "No se puede acceder al inicio de sesión seguro del propietario. Inténtalo más tarde.",
            "title" to "Verificación del propietario RaSaSeR",
            "failed" to "No se pudo completar la verificación de Google. Comprueba la conexión e inténtalo de nuevo.",
            "pending" to "Vuelve a esta aplicación después de la aprobación de Google. La aprobación está pendiente.",
            "check" to "Comprobar aprobación",
            "later" to "Más tarde",
            "active" to "Premium • De por vida",
            "quota" to "Creación ilimitada de widgets activada.",
            "privacy" to "Los ajustes y fotos de widgets permanecen en tu dispositivo. Para activar un código promocional se envían el código, el identificador de la aplicación y un identificador aleatorio de instalación a nuestro servicio seguro. El código no se guarda sin cifrar; no hay anuncios ni seguimiento.",
        ),
        "ja" to mapOf(
            "inactive" to "安全な所有者ログインには現在接続できません。後でもう一度お試しください。",
            "title" to "RaSaSeR所有者認証",
            "failed" to "Google認証が完了しませんでした。接続を確認して再試行してください。",
            "pending" to "Google承認後、このアプリに戻ってください。承認はまだ保留中です。",
            "check" to "承認を確認",
            "later" to "あとで",
            "active" to "Premium・無期限",
            "quota" to "ウィジェットを無制限に作成できます。",
            "privacy" to "ウィジェット設定と選択した写真は端末内に保存されます。プロモーションコードの有効化時には、コード、アプリ識別子、ランダムなインストール識別子を安全な認証サービスに送信します。コードの平文保存、広告、追跡はありません。",
        ),
        "ar" to mapOf(
            "inactive" to "تسجيل دخول المالك الآمن غير متاح حاليًا. يرجى المحاولة لاحقًا.",
            "title" to "التحقق من مالك RaSaSeR",
            "failed" to "تعذّر إكمال التحقق من Google. تحقق من الاتصال وحاول مجددًا.",
            "pending" to "ارجع إلى التطبيق بعد موافقة Google. الموافقة لا تزال معلّقة.",
            "check" to "التحقق من الموافقة",
            "later" to "لاحقًا",
            "active" to "Premium • مدى الحياة",
            "quota" to "إنشاء أدوات بلا حدود أصبح مفعّلًا.",
            "privacy" to "تظل إعدادات الأدوات والصور المختارة على جهازك. عند تفعيل الرمز الترويجي يُرسل الرمز ومعرّف التطبيق ومعرّف تثبيت عشوائي إلى خدمة التحقق الآمنة. لا يُحفظ الرمز كنص واضح، ولا توجد إعلانات أو تتبّع."
        )
    )

    fun t(context: Context, key: String): String =
        labels[VaryokText.language(context)]?.get(key)
            ?: labels["tr"]?.get(key)
            ?: key
}
