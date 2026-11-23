package com.rasaser.free.icerikureticisi

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.Composable

data class LanguageChoice(val code: String, val flag: String, val title: String)

object AppLanguage {
    val choices = listOf(
        LanguageChoice("tr", "🇹🇷", "Türkçe"),
        LanguageChoice("en", "🇬🇧", "English"),
        LanguageChoice("de", "🇩🇪", "Deutsch"),
        LanguageChoice("es", "🇪🇸", "Español"),
        LanguageChoice("ja", "🇯🇵", "日本語"),
        LanguageChoice("ar", "🇸🇦", "العربية")
    )
    val codes = choices.map { it.code }.toSet()
    fun normalize(value: String): String = value.takeIf { it in codes } ?: "tr"
    fun get(context: Context): String =
        normalize(context.getSharedPreferences("creator_settings", Context.MODE_PRIVATE)
            .getString("language", "tr") ?: "tr")
    fun save(context: Context, code: String) {
        context.getSharedPreferences("creator_settings", Context.MODE_PRIVATE)
            .edit().putString("language", normalize(code)).apply()
    }
}

val LocalCreatorLanguage = compositionLocalOf { "tr" }

object Translations {
    private val texts: Map<String, Map<String, String>> = mapOf(
        "İçerik Üreticisi" to mapOf("en" to "Content Creator", "de" to "Content Creator", "es" to "Creador de contenido", "ja" to "コンテンツ制作者", "ar" to "صانع المحتوى"),
        "İçerik yönetim alanlarım" to mapOf("en" to "My content spaces", "de" to "Meine Inhaltsbereiche", "es" to "Mis espacios de contenido", "ja" to "コンテンツ管理", "ar" to "مساحات المحتوى"),
        "＋ İçerik ekle" to mapOf("en" to "＋ Add content", "de" to "＋ Inhalt hinzufügen", "es" to "＋ Añadir contenido", "ja" to "＋ コンテンツ追加", "ar" to "＋ إضافة محتوى"),
        "İlk içerik sayfanı oluştur" to mapOf("en" to "Create your first content space", "de" to "Ersten Inhaltsbereich erstellen", "es" to "Crea tu primer espacio", "ja" to "最初のコンテンツを作成", "ar" to "أنشئ أول مساحة محتوى"),
        "＋ düğmesine bas, sayfana isim ver ve sosyal medya hesaplarını ekle." to mapOf("en" to "Tap ＋, name your space and add social accounts.", "de" to "Tippe auf ＋, benenne den Bereich und füge Konten hinzu.", "es" to "Pulsa ＋, nombra el espacio y añade cuentas.", "ja" to "＋で名前を付けてSNSアカウントを追加。", "ar" to "اضغط ＋ وسمّ المساحة وأضف حساباتك."),
        "İçerik sayfasını aç" to mapOf("en" to "Open content space", "de" to "Inhaltsbereich öffnen", "es" to "Abrir espacio", "ja" to "コンテンツを開く", "ar" to "فتح المحتوى"),
        "Gizlilik ve lisans bilgileri" to mapOf("en" to "Privacy and licenses", "de" to "Datenschutz und Lizenzen", "es" to "Privacidad y licencias", "ja" to "プライバシーとライセンス", "ar" to "الخصوصية والتراخيص"),
        "‹ Geri" to mapOf("en" to "‹ Back", "de" to "‹ Zurück", "es" to "‹ Volver", "ja" to "‹ 戻る", "ar" to "‹ رجوع"),
        "Sil" to mapOf("en" to "Delete", "de" to "Löschen", "es" to "Eliminar", "ja" to "削除", "ar" to "حذف"),
        "Sosyal medya platformları" to mapOf("en" to "Social platforms", "de" to "Soziale Netzwerke", "es" to "Redes sociales", "ja" to "SNSプラットフォーム", "ar" to "منصات التواصل"),
        "＋ Sosyal medya hesabı ekle" to mapOf("en" to "＋ Add social account", "de" to "＋ Social-Media-Konto hinzufügen", "es" to "＋ Añadir cuenta social", "ja" to "＋ SNSアカウント追加", "ar" to "＋ إضافة حساب تواصل"),
        "Bağlantılarım" to mapOf("en" to "My accounts", "de" to "Meine Konten", "es" to "Mis cuentas", "ja" to "アカウント一覧", "ar" to "حساباتي"),
        "Tümünü göster" to mapOf("en" to "Show all", "de" to "Alle anzeigen", "es" to "Mostrar todo", "ja" to "すべて表示", "ar" to "عرض الكل"),
        "Bu bölümde henüz sosyal medya hesabı yok." to mapOf("en" to "No social accounts here yet.", "de" to "Hier gibt es noch keine Konten.", "es" to "Todavía no hay cuentas.", "ja" to "アカウントはまだありません。", "ar" to "لا توجد حسابات هنا بعد."),
        "Aç ›" to mapOf("en" to "Open ›", "de" to "Öffnen ›", "es" to "Abrir ›", "ja" to "開く ›", "ar" to "فتح ›"),
        "Kaldır" to mapOf("en" to "Remove", "de" to "Entfernen", "es" to "Quitar", "ja" to "削除", "ar" to "إزالة"),
        "Yeni içerik üreticisi" to mapOf("en" to "New content space", "de" to "Neuer Inhaltsbereich", "es" to "Nuevo espacio de contenido", "ja" to "新しいコンテンツ", "ar" to "مساحة محتوى جديدة"),
        "İçerik ismini yazın. İstediğiniz kadar ayrı sayfa oluşturabilirsiniz." to mapOf("en" to "Name your content space. Create as many as you like.", "de" to "Gib einen Namen ein. Unbegrenzt viele Bereiche möglich.", "es" to "Pon un nombre. Puedes crear tantos espacios como quieras.", "ja" to "名前を入力。数に制限はありません。", "ar" to "أدخل اسم المحتوى. يمكنك إنشاء عدد غير محدود."),
        "İçerik adı" to mapOf("en" to "Content name", "de" to "Inhaltsname", "es" to "Nombre de contenido", "ja" to "コンテンツ名", "ar" to "اسم المحتوى"),
        "Oluştur" to mapOf("en" to "Create", "de" to "Erstellen", "es" to "Crear", "ja" to "作成", "ar" to "إنشاء"),
        "Vazgeç" to mapOf("en" to "Cancel", "de" to "Abbrechen", "es" to "Cancelar", "ja" to "キャンセル", "ar" to "إلغاء"),
        "Sosyal medya hesabı ekle" to mapOf("en" to "Add social account", "de" to "Social-Media-Konto hinzufügen", "es" to "Añadir cuenta social", "ja" to "SNSアカウント追加", "ar" to "إضافة حساب تواصل"),
        "Platform" to mapOf("en" to "Platform", "de" to "Plattform", "es" to "Plataforma", "ja" to "プラットフォーム", "ar" to "المنصة"),
        "Diğer" to mapOf("en" to "Other", "de" to "Andere", "es" to "Otros", "ja" to "その他", "ar" to "أخرى"),
        "Sosyal medya adı" to mapOf("en" to "Platform name", "de" to "Plattformname", "es" to "Nombre de red", "ja" to "SNS名", "ar" to "اسم المنصة"),
        "Android paket adı (isteğe bağlı)" to mapOf("en" to "Android package (optional)", "de" to "Android-Paket (optional)", "es" to "Paquete Android (opcional)", "ja" to "Androidパッケージ名（任意）", "ar" to "حزمة أندرويد (اختياري)"),
        "＋ Logo seç (galeriden)" to mapOf("en" to "＋ Choose logo", "de" to "＋ Logo wählen", "es" to "＋ Elegir logotipo", "ja" to "＋ ロゴを選択", "ar" to "＋ اختيار شعار"),
        "✓ Logo seçildi" to mapOf("en" to "✓ Logo selected", "de" to "✓ Logo ausgewählt", "es" to "✓ Logo seleccionado", "ja" to "✓ ロゴ選択済", "ar" to "✓ تم اختيار الشعار"),
        "Hesap / profil ismi" to mapOf("en" to "Account / profile name", "de" to "Konto- / Profilname", "es" to "Nombre de la cuenta", "ja" to "アカウント名", "ar" to "اسم الحساب"),
        "Profil bağlantısı (https://...)" to mapOf("en" to "Profile link (https://...)", "de" to "Profillink (https://...)", "es" to "Enlace de perfil (https://...)", "ja" to "プロフィールURL (https://...)", "ar" to "رابط الملف الشخصي (https://...)"),
        "Hesabı ekle" to mapOf("en" to "Add account", "de" to "Konto hinzufügen", "es" to "Añadir cuenta", "ja" to "アカウント追加", "ar" to "إضافة الحساب"),
        "İçerik sayfasını sil" to mapOf("en" to "Delete content space", "de" to "Inhaltsbereich löschen", "es" to "Eliminar espacio", "ja" to "コンテンツを削除", "ar" to "حذف مساحة المحتوى"),
        "Hesabı kaldır" to mapOf("en" to "Remove account", "de" to "Konto entfernen", "es" to "Quitar cuenta", "ja" to "アカウント削除", "ar" to "إزالة الحساب"),
        "Gizlilik ve lisanslar" to mapOf("en" to "Privacy and licenses", "de" to "Datenschutz und Lizenzen", "es" to "Privacidad y licencias", "ja" to "プライバシーとライセンス", "ar" to "الخصوصية والتراخيص"),
        "Kapat" to mapOf("en" to "Close", "de" to "Schließen", "es" to "Cerrar", "ja" to "閉じる", "ar" to "إغلاق"),
        "Bilgi" to mapOf("en" to "Info", "de" to "Info", "es" to "Información", "ja" to "情報", "ar" to "معلومات"),
        "Tamam" to mapOf("en" to "OK", "de" to "OK", "es" to "Aceptar", "ja" to "OK", "ar" to "حسناً"),
        "Paylaşım oluştur" to mapOf("en" to "Create a post", "de" to "Beitrag erstellen", "es" to "Crear publicación", "ja" to "投稿を作成", "ar" to "إنشاء منشور"),
        "▧ Resim yükle" to mapOf("en" to "▧ Choose photo", "de" to "▧ Foto wählen", "es" to "▧ Elegir foto", "ja" to "▧ 写真選択", "ar" to "▧ اختيار صورة"),
        "▶ Video yükle" to mapOf("en" to "▶ Choose video", "de" to "▶ Video wählen", "es" to "▶ Elegir vídeo", "ja" to "▶ 動画選択", "ar" to "▶ اختيار فيديو"),
        "✓ Video seçildi" to mapOf("en" to "✓ Video selected", "de" to "✓ Video ausgewählt", "es" to "✓ Vídeo seleccionado", "ja" to "✓ 動画選択済", "ar" to "✓ تم اختيار الفيديو"),
        "✓ Resim seçildi" to mapOf("en" to "✓ Photo selected", "de" to "✓ Foto ausgewählt", "es" to "✓ Foto seleccionada", "ja" to "✓ 写真選択済", "ar" to "✓ تم اختيار الصورة"),
        "Açıklama ekle" to mapOf("en" to "Add caption", "de" to "Beschreibung hinzufügen", "es" to "Añadir descripción", "ja" to "説明を追加", "ar" to "إضافة وصف"),
        "Paylaşım metni ve etiketler..." to mapOf("en" to "Text and hashtags...", "de" to "Text und Hashtags...", "es" to "Texto y etiquetas...", "ja" to "本文とハッシュタグ...", "ar" to "النص والوسوم..."),
        "Hepsini seç" to mapOf("en" to "Select all", "de" to "Alle auswählen", "es" to "Seleccionar todo", "ja" to "すべて選択", "ar" to "تحديد الكل"),
        "➤ Gönder" to mapOf("en" to "➤ Send", "de" to "➤ Senden", "es" to "➤ Enviar", "ja" to "➤ 送信", "ar" to "➤ إرسال"),
        "◷ Zaman" to mapOf("en" to "◷ Schedule", "de" to "◷ Planen", "es" to "◷ Programar", "ja" to "◷ 予約", "ar" to "◷ جدولة"),
        "Zamanlanmış taslaklar" to mapOf("en" to "Scheduled drafts", "de" to "Geplante Entwürfe", "es" to "Borradores programados", "ja" to "予約済み下書き", "ar" to "المسودات المجدولة"),
        "Hatırlatma zamanı geldi" to mapOf("en" to "Reminder due", "de" to "Erinnerung fällig", "es" to "Recordatorio listo", "ja" to "通知時間です", "ar" to "حان موعد التذكير"),
        "Hatırlatma bekleniyor" to mapOf("en" to "Reminder pending", "de" to "Erinnerung ausstehend", "es" to "Recordatorio pendiente", "ja" to "通知待ち", "ar" to "بانتظار التذكير"),
        "Hazırla" to mapOf("en" to "Prepare", "de" to "Vorbereiten", "es" to "Preparar", "ja" to "準備", "ar" to "تحضير"),
        "Sıradaki hesaba aktar" to mapOf("en" to "Send to next account", "de" to "Nächstes Konto", "es" to "Siguiente cuenta", "ja" to "次のアカウントへ", "ar" to "الحساب التالي"),
        "Planlanan paylaşımlar" to mapOf("en" to "Scheduled posts", "de" to "Geplante Beiträge", "es" to "Publicaciones programadas", "ja" to "予約投稿", "ar" to "المنشورات المجدولة"),
        "Premium" to mapOf("en" to "Premium", "de" to "Premium", "es" to "Premium", "ja" to "プレミアム", "ar" to "مميز"),
        "Promosyon Kodu Gir" to mapOf("en" to "Enter promo code", "de" to "Promocode eingeben", "es" to "Introducir código promocional", "ja" to "プロモコードを入力", "ar" to "أدخل رمز الترويج"),
        "Google Play'de aç" to mapOf("en" to "Open in Google Play", "de" to "In Google Play öffnen", "es" to "Abrir en Google Play", "ja" to "Google Playで開く", "ar" to "فتح في Google Play"),
        "Doğrula" to mapOf("en" to "Verify", "de" to "Prüfen", "es" to "Verificar", "ja" to "確認", "ar" to "تحقق"),
        "Premium'u etkinleştir" to mapOf("en" to "Activate Premium", "de" to "Premium aktivieren", "es" to "Activar Premium", "ja" to "プレミアムを有効化", "ar" to "تفعيل المميز"),
        "Gönderildi olarak işaretle" to mapOf("en" to "Mark as posted", "de" to "Als veröffentlicht markieren", "es" to "Marcar como publicado", "ja" to "投稿済みとして記録", "ar" to "تحديد كمنشور"),
        "Gönderilmedi" to mapOf("en" to "Not posted", "de" to "Nicht veröffentlicht", "es" to "No publicado", "ja" to "未投稿", "ar" to "لم ينشر"),
        "Google Play satın alma henüz etkin değil. Geçerli promosyon koduyla erişim açılabilir." to mapOf("en" to "Google Play purchases are not enabled yet. You can activate access with a valid promo code.", "de" to "Google Play-Käufe sind noch nicht aktiviert. Mit einem gültigen Promocode ist die Aktivierung möglich.", "es" to "Las compras de Google Play aún no están activadas. Puede activar el acceso con un código válido.", "ja" to "Google Play購入はまだ利用できません。有効なプロモコードでアクセスを有効にできます。", "ar" to "الشراء عبر Google Play غير متاح بعد. يمكنك التفعيل باستخدام رمز ترويجي صالح."),
        "Resmî siteyi aç" to mapOf("en" to "Open official website", "de" to "Offizielle Website öffnen", "es" to "Abrir sitio oficial", "ja" to "公式サイトを開く", "ar" to "فتح الموقع الرسمي"),
        "Resmî profili aç" to mapOf("en" to "Open official profile", "de" to "Offizielles Profil öffnen", "es" to "Abrir perfil oficial", "ja" to "公式プロフィールを開く", "ar" to "فتح الملف الرسمي"),
        "API yetkisi yok" to mapOf("en" to "API access not authorized", "de" to "Kein API-Zugriff", "es" to "Sin permiso de API", "ja" to "API権限なし", "ar" to "لا توجد صلاحية API"),
        "Manuel profil bağlantısı" to mapOf("en" to "Manual profile link", "de" to "Manueller Profillink", "es" to "Enlace de perfil manual", "ja" to "手動プロフィールリンク", "ar" to "رابط ملف يدوي"),
        "Aynı platformdan sınırsız hesap ekleyebilirsiniz." to mapOf("en" to "You can save unlimited accounts from the same platform.", "de" to "Sie können beliebig viele Konten derselben Plattform speichern.", "es" to "Puede guardar cuentas ilimitadas de la misma plataforma.", "ja" to "同じサービスのアカウントをいくつでも登録できます。", "ar" to "يمكنك حفظ عدد غير محدود من الحسابات لنفس المنصة."),
        "Resmî siteden profil adresinizi kopyalayın. Profil bağlantısı tek başına otomatik yayınlama veya bildirim izni vermez." to mapOf("en" to "Copy your profile URL from the official site. A profile link alone does not grant automatic publishing or notification access.", "de" to "Kopieren Sie die Profiladresse von der offiziellen Seite. Ein Link erlaubt weder automatisches Veröffentlichen noch Benachrichtigungszugriff.", "es" to "Copie el enlace de su perfil desde el sitio oficial. Un enlace no autoriza publicaciones ni notificaciones automáticas.", "ja" to "公式サイトからプロフィールURLをコピーしてください。URLだけでは自動投稿や通知へのアクセスは許可されません。", "ar" to "انسخ رابط ملفك من الموقع الرسمي. الرابط وحده لا يمنح صلاحية النشر أو الإشعارات التلقائية."),
        "Abone olun" to mapOf("en" to "Subscribe", "de" to "Abonnieren", "es" to "Suscribirse", "ja" to "登録する", "ar" to "اشترك"),
        "Premium etkin" to mapOf("en" to "Premium active", "de" to "Premium aktiv", "es" to "Premium activo", "ja" to "プレミアム有効", "ar" to "المميز مفعل"),

        "Ücretsiz deneme" to mapOf("en" to "Free trial", "de" to "Kostenlos testen", "es" to "Prueba gratis", "ja" to "無料お試し", "ar" to "تجربة مجانية"),
        "gün kaldı" to mapOf("en" to "days left", "de" to "Tage verbleiben", "es" to "días restantes", "ja" to "日間残り", "ar" to "أيام متبقية"),
        "Deneme süresi doldu" to mapOf("en" to "Trial expired", "de" to "Testzeit abgelaufen", "es" to "Prueba finalizada", "ja" to "試用期間終了", "ar" to "انتهت الفترة التجريبية"),
        "15 günlük ücretsiz denemeniz tamamlandı. İçerikleriniz cihazınızda saklanmaya devam eder." to mapOf("en" to "Your 15-day free trial has ended. Your content remains on your device.", "de" to "Ihre 15-tägige Testphase ist beendet. Ihre Inhalte bleiben auf Ihrem Gerät.", "es" to "Su prueba gratuita de 15 días ha finalizado. Su contenido permanece en su dispositivo.", "ja" to "15日間の無料お試し期間が終了しました。データは端末に保存されたままです。", "ar" to "انتهت تجربتك المجانية لمدة 15 يوما. ستظل بياناتك محفوظة على جهازك."),
        "Promosyon kodu ile etkinleştir" to mapOf("en" to "Activate with promo code", "de" to "Mit Promocode aktivieren", "es" to "Activar con código promocional", "ja" to "プロモコードで有効化", "ar" to "التفعيل برمز ترويجي"),
        "Önce PIN oluşturun; ardından telefonunuzun parmak izi özelliğini etkinleştirin." to mapOf("en" to "Create a PIN first, then enable the phone's fingerprint unlock.", "de" to "Erstellen Sie zuerst eine PIN und aktivieren Sie dann den Fingerabdruck.", "es" to "Primero cree un PIN y después active la huella digital del teléfono.", "ja" to "先にPINを設定し、その後端末の指紋認証を有効にしてください。", "ar" to "أنشئ رمز PIN أولا ثم فعّل بصمة الهاتف."),
        "15 günlük ücretsiz deneme. Ortak promosyon kodu Cloudflare sunucusunda doğrulanır." to mapOf("en" to "15-day free trial. Shared promo codes are verified on the Cloudflare server.", "de" to "15 Tage kostenlos testen. Gemeinsame Promocodes werden auf dem Cloudflare-Server geprüft.", "es" to "Prueba gratuita de 15 días. Los códigos promocionales se verifican en Cloudflare.", "ja" to "15日間の無料お試し。共通プロモコードはCloudflareサーバーで検証されます。", "ar" to "تجربة مجانية لمدة 15 يوما. يتم التحقق من رمز الترويج على خادم Cloudflare."),
        "Promosyon kodu" to mapOf("en" to "Promo code", "de" to "Promocode", "es" to "Código promocional", "ja" to "プロモコード", "ar" to "الرمز الترويجي"),
        "Dil" to mapOf("en" to "Language", "de" to "Sprache", "es" to "Idioma", "ja" to "言語", "ar" to "اللغة"),
        "Tema" to mapOf("en" to "Theme", "de" to "Design", "es" to "Tema", "ja" to "テーマ", "ar" to "السمة"),
        "Açık" to mapOf("en" to "Light", "de" to "Hell", "es" to "Claro", "ja" to "ライト", "ar" to "فاتح"),
        "Koyu" to mapOf("en" to "Dark", "de" to "Dunkel", "es" to "Oscuro", "ja" to "ダーク", "ar" to "داكن"),
        "Uygulama kilidi" to mapOf("en" to "App lock", "de" to "App-Sperre", "es" to "Bloqueo de aplicación", "ja" to "アプリロック", "ar" to "قفل التطبيق"),
        "Şifre oluştur" to mapOf("en" to "Create PIN", "de" to "PIN erstellen", "es" to "Crear PIN", "ja" to "PIN設定", "ar" to "إنشاء رمز PIN"),
        "Şifreyi değiştir" to mapOf("en" to "Change PIN", "de" to "PIN ändern", "es" to "Cambiar PIN", "ja" to "PIN変更", "ar" to "تغيير رمز PIN"),
        "Parmak izi" to mapOf("en" to "Fingerprint", "de" to "Fingerabdruck", "es" to "Huella dactilar", "ja" to "指紋認証", "ar" to "بصمة الإصبع"),
        "Parmak iziyle aç" to mapOf("en" to "Unlock with fingerprint", "de" to "Mit Fingerabdruck öffnen", "es" to "Abrir con huella", "ja" to "指紋で解除", "ar" to "الفتح بالبصمة"),
        "PIN gir" to mapOf("en" to "Enter PIN", "de" to "PIN eingeben", "es" to "Introducir PIN", "ja" to "PINを入力", "ar" to "أدخل PIN"),
        "Yeni PIN" to mapOf("en" to "New PIN", "de" to "Neue PIN", "es" to "Nuevo PIN", "ja" to "新しいPIN", "ar" to "PIN جديد"),
        "PIN tekrarı" to mapOf("en" to "Confirm PIN", "de" to "PIN bestätigen", "es" to "Confirmar PIN", "ja" to "PINを確認", "ar" to "تأكيد PIN"),
        "Kaydet" to mapOf("en" to "Save", "de" to "Speichern", "es" to "Guardar", "ja" to "保存", "ar" to "حفظ"),
        "Kilidi aç" to mapOf("en" to "Unlock", "de" to "Entsperren", "es" to "Desbloquear", "ja" to "ロック解除", "ar" to "فتح القفل"),
        "Kilidi kapat" to mapOf("en" to "Disable lock", "de" to "Sperre ausschalten", "es" to "Desactivar bloqueo", "ja" to "ロックを解除", "ar" to "إلغاء القفل"),
        "Profil resmi" to mapOf("en" to "Profile image", "de" to "Profilbild", "es" to "Imagen de perfil", "ja" to "プロフィール画像", "ar" to "صورة الملف"),
        "Fotoğraf veya logo seç" to mapOf("en" to "Choose photo or logo", "de" to "Foto oder Logo auswählen", "es" to "Elegir foto o logo", "ja" to "写真・ロゴを選択", "ar" to "اختيار صورة أو شعار"),
        "Fotoğraf seçildi" to mapOf("en" to "Photo selected", "de" to "Foto ausgewählt", "es" to "Foto seleccionada", "ja" to "画像選択済", "ar" to "تم اختيار الصورة"),
        "Profil görselini değiştir" to mapOf("en" to "Change profile image", "de" to "Profilbild ändern", "es" to "Cambiar imagen de perfil", "ja" to "プロフィール画像変更", "ar" to "تغيير صورة الملف"),
        "İçerik adı çevirileri" to mapOf("en" to "Profile name translations", "de" to "Profilname Übersetzungen", "es" to "Traducciones del nombre", "ja" to "プロフィール名の翻訳", "ar" to "ترجمات اسم الملف"),
        "RaSaSeR APPS & TECHNOLOGY" to mapOf("en" to "RaSaSeR APPS & TECHNOLOGY", "de" to "RaSaSeR APPS & TECHNOLOGY", "es" to "RaSaSeR APPS & TECHNOLOGY", "ja" to "RaSaSeR APPS & TECHNOLOGY", "ar" to "RaSaSeR APPS & TECHNOLOGY"),
        "Bildirim izni" to mapOf("en" to "Notification permission", "de" to "Benachrichtigungsrecht", "es" to "Permiso de notificación", "ja" to "通知権限", "ar" to "إذن الإشعارات"),
        "İzin iste" to mapOf("en" to "Request permission", "de" to "Berechtigung anfragen", "es" to "Solicitar permiso", "ja" to "許可を求める", "ar" to "طلب الإذن"),
        "Şimdi değil" to mapOf("en" to "Not now", "de" to "Nicht jetzt", "es" to "Ahora no", "ja" to "今はしない", "ar" to "ليس الآن"),
        "Zamanlanmış paylaşım hatırlatmaları" to mapOf("en" to "Scheduled post reminders", "de" to "Erinnerungen für Beiträge", "es" to "Recordatorios programados", "ja" to "予約投稿の通知", "ar" to "تذكيرات النشر المجدول"),
        "Değişiklikler kaydedildi." to mapOf("en" to "Changes saved.", "de" to "Änderungen gespeichert.", "es" to "Cambios guardados.", "ja" to "変更を保存しました。", "ar" to "تم حفظ التغييرات."),
        "Play Store'da henüz yayınlanmadı" to mapOf("en" to "Not yet published on Play Store", "de" to "Noch nicht im Play Store veröffentlicht", "es" to "Aún no está en Play Store", "ja" to "Play ストア未公開", "ar" to "لم يُنشر في متجر Play بعد")
    )
    fun text(lang: String, key: String): String =
        if (lang == "tr") key else texts[key]?.get(lang) ?: key
}

@Composable fun tr(key: String): String =
    Translations.text(LocalCreatorLanguage.current, key)
