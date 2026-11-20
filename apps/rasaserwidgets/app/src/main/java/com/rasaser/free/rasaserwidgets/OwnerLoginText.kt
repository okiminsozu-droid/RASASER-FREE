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
            "active" to "RaSaSeR Sahip Premium • Süresiz",
            "quota" to "Sınırsız widget oluşturma hakkınız aktif.",
            "privacy" to "Widget ayarlarınız ve seçtiğiniz görseller cihazınızda saklanır. İsteğe bağlı RaSaSeR sahip girişinde Google hesabınızın doğrulanmış kimliği ve cihazın yalnızca açık anahtarı, güvenli doğrulama hizmetine iletilir. Google şifreniz, fotoğraflarınız ve widget içerikleriniz gönderilmez."
        ),
        "en" to mapOf(
            "inactive" to "Secure owner sign-in is unavailable right now. Please try again later.",
            "title" to "RaSaSeR Owner Verification",
            "failed" to "Google verification could not be completed. Check your connection and try again.",
            "pending" to "Return to this app after Google approval. Approval is still pending.",
            "check" to "Check Approval",
            "later" to "Later",
            "active" to "RaSaSeR Owner Premium • Lifetime",
            "quota" to "Unlimited widget creation is active.",
            "privacy" to "Widget settings and selected images stay on your device. Optional RaSaSeR owner sign-in shares your verified Google account identity and only your device's public key with our secure verification service. Your Google password, photos and widget contents are never sent."
        ),
        "de" to mapOf(
            "inactive" to "Die sichere Inhaberanmeldung ist derzeit nicht erreichbar. Bitte versuchen Sie es später erneut.",
            "title" to "RaSaSeR-Inhaberverifizierung",
            "failed" to "Die Google-Verifizierung konnte nicht abgeschlossen werden. Verbindung prüfen und erneut versuchen.",
            "pending" to "Kehren Sie nach der Google-Bestätigung zur App zurück. Die Bestätigung steht noch aus.",
            "check" to "Bestätigung prüfen",
            "later" to "Später",
            "active" to "RaSaSeR Inhaber-Premium • Lebenslang",
            "quota" to "Unbegrenzte Widget-Erstellung ist aktiv.",
            "privacy" to "Widget-Einstellungen und ausgewählte Bilder bleiben auf Ihrem Gerät. Die optionale RaSaSeR-Inhaberanmeldung übermittelt die bestätigte Google-Kontoidentität und nur den öffentlichen Geräteschlüssel an unseren sicheren Verifizierungsdienst. Google-Passwort, Fotos und Widget-Inhalte werden nicht übertragen."
        ),
        "es" to mapOf(
            "inactive" to "No se puede acceder al inicio de sesión seguro del propietario. Inténtalo más tarde.",
            "title" to "Verificación del propietario RaSaSeR",
            "failed" to "No se pudo completar la verificación de Google. Comprueba la conexión e inténtalo de nuevo.",
            "pending" to "Vuelve a esta aplicación después de la aprobación de Google. La aprobación está pendiente.",
            "check" to "Comprobar aprobación",
            "later" to "Más tarde",
            "active" to "RaSaSeR Premium del propietario • De por vida",
            "quota" to "Creación ilimitada de widgets activada.",
            "privacy" to "La configuración de widgets y las imágenes elegidas permanecen en tu dispositivo. El acceso opcional del propietario de RaSaSeR envía la identidad verificada de Google y solo la clave pública del dispositivo al servicio seguro de verificación. No se envían la contraseña de Google, las fotos ni el contenido de los widgets."
        ),
        "ja" to mapOf(
            "inactive" to "安全な所有者ログインには現在接続できません。後でもう一度お試しください。",
            "title" to "RaSaSeR所有者認証",
            "failed" to "Google認証が完了しませんでした。接続を確認して再試行してください。",
            "pending" to "Google承認後、このアプリに戻ってください。承認はまだ保留中です。",
            "check" to "承認を確認",
            "later" to "あとで",
            "active" to "RaSaSeR所有者Premium・無期限",
            "quota" to "ウィジェットを無制限に作成できます。",
            "privacy" to "ウィジェット設定と選択した画像は端末に保存されます。任意のRaSaSeR所有者ログインでは、確認済みのGoogleアカウント識別情報と端末の公開鍵のみを安全な認証サービスへ送ります。Googleパスワード、写真、ウィジェット内容は送信しません。"
        ),
        "ar" to mapOf(
            "inactive" to "تسجيل دخول المالك الآمن غير متاح حاليًا. يرجى المحاولة لاحقًا.",
            "title" to "التحقق من مالك RaSaSeR",
            "failed" to "تعذّر إكمال التحقق من Google. تحقق من الاتصال وحاول مجددًا.",
            "pending" to "ارجع إلى التطبيق بعد موافقة Google. الموافقة لا تزال معلّقة.",
            "check" to "التحقق من الموافقة",
            "later" to "لاحقًا",
            "active" to "RaSaSeR Premium للمالك • مدى الحياة",
            "quota" to "إنشاء أدوات بلا حدود أصبح مفعّلًا.",
            "privacy" to "تُحفظ إعدادات الأدوات والصور المختارة على جهازك. عند تسجيل دخول مالك RaSaSeR الاختياري، تُرسل هوية حساب Google المؤكدة والمفتاح العام للجهاز فقط إلى خدمة التحقق الآمنة. لا تُرسل كلمة مرور Google أو الصور أو محتويات الأدوات."
        )
    )

    fun t(context: Context, key: String): String =
        labels[VaryokText.language(context)]?.get(key)
            ?: labels["tr"]?.get(key)
            ?: key
}
