package com.rasaser.free.rasaserwidgets

import android.content.Context

/** Fully offline security setup and unlock copy for six VARYOK languages. */
object AppLockText {
    private val keys = listOf("setup", "choose", "pin", "biometric", "enter", "confirm", "create", "unlock", "wrongPin", "pinRule", "notMatch", "notAvailable", "biometricTitle", "biometricSubtitle", "fingerprintReady", "cancel", "deviceCredential", "locked", "enableError", "tryAgain", "failAuth")
    private val texts = mapOf(
        "tr" to listOf("Şifre Oluştur", "Koruma yöntemini seç", "Şifre (PIN) oluştur", "Parmak iziyle koru", "4–12 haneli şifre", "Şifreyi tekrar gir", "Kaydet", "Kilidi aç", "Şifre yanlış", "Şifre 4–12 rakam olmalıdır", "Şifreler eşleşmiyor", "Bu cihazda kayıtlı parmak izi kullanılamıyor. Önce telefon ayarlarından parmak izi kaydet veya PIN kullan.", "Parmak izi doğrulaması", "VARYOK kilidini etkinleştirmek için doğrula", "Parmak izi kilidi etkinleştirildi", "İptal", "Telefon kilidiyle aç", "VARYOK kilitli", "Kilit kurulamadı", "Yeniden dene", "Doğrulama başarısız"),
        "en" to listOf("Create Password", "Choose protection method", "Create PIN", "Use Fingerprint", "4–12 digit PIN", "Repeat PIN", "Save", "Unlock", "Incorrect PIN", "PIN must have 4–12 digits", "PINs do not match", "No enrolled fingerprint is available. Enroll one in your phone settings or use a PIN.", "Fingerprint authentication", "Confirm to enable the VARYOK lock", "Fingerprint lock enabled", "Cancel", "Use device screen lock", "VARYOK locked", "Could not enable lock", "Try again", "Authentication failed"),
        "de" to listOf("Passwort erstellen", "Schutzmethode wählen", "PIN erstellen", "Fingerabdruck nutzen", "4–12-stellige PIN", "PIN wiederholen", "Speichern", "Entsperren", "Falsche PIN", "PIN muss 4–12 Ziffern haben", "PINs stimmen nicht überein", "Kein registrierter Fingerabdruck verfügbar. In den Geräteeinstellungen einrichten oder PIN verwenden.", "Fingerabdruck prüfen", "VARYOK-Sperre bestätigen", "Fingerabdruck-Sperre aktiviert", "Abbrechen", "Gerätesperre verwenden", "VARYOK gesperrt", "Sperre konnte nicht aktiviert werden", "Erneut versuchen", "Authentifizierung fehlgeschlagen"),
        "es" to listOf("Crear contraseña", "Elegir método de protección", "Crear PIN", "Usar huella digital", "PIN de 4 a 12 dígitos", "Repetir PIN", "Guardar", "Desbloquear", "PIN incorrecto", "El PIN debe tener de 4 a 12 dígitos", "Los PIN no coinciden", "No hay huellas registradas. Añade una en los ajustes del teléfono o usa un PIN.", "Autenticación de huella", "Confirma para activar el bloqueo de VARYOK", "Bloqueo por huella activado", "Cancelar", "Usar bloqueo del teléfono", "VARYOK bloqueado", "No se pudo activar", "Reintentar", "Autenticación fallida"),
        "ja" to listOf("パスワードを作成", "保護方法を選択", "PINを作成", "指紋を使う", "4～12桁のPIN", "PINを再入力", "保存", "ロック解除", "PINが違います", "PINは4～12桁の数字です", "PINが一致しません", "登録済みの指紋がありません。端末設定で登録するかPINを使用してください。", "指紋認証", "VARYOKのロックを有効にするため認証", "指紋ロックが有効になりました", "キャンセル", "端末の画面ロックを使う", "VARYOKはロック中", "ロックできませんでした", "再試行", "認証に失敗しました"),
        "ar" to listOf("إنشاء كلمة مرور", "اختر طريقة الحماية", "إنشاء رمز PIN", "استخدام البصمة", "رمز من 4 إلى 12 رقمًا", "تأكيد الرمز", "حفظ", "فتح القفل", "الرمز غير صحيح", "يجب أن يتكون الرمز من 4 إلى 12 رقمًا", "الرمزان غير متطابقين", "لا توجد بصمة مسجلة. سجل بصمة في إعدادات الهاتف أو استخدم رمز PIN.", "المصادقة بالبصمة", "تحقق لتفعيل قفل VARYOK", "تم تفعيل قفل البصمة", "إلغاء", "استخدام قفل الهاتف", "VARYOK مقفل", "تعذر تفعيل القفل", "إعادة المحاولة", "فشلت المصادقة")
    )
    fun t(ctx: Context, key: String): String {
        val index = keys.indexOf(key)
        return if (index < 0) key else texts[VaryokText.language(ctx)]?.get(index)
            ?: texts["tr"]!![index]
    }
}
