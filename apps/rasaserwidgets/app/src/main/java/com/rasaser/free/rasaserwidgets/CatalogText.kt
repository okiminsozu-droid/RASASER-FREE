package com.rasaser.free.rasaserwidgets

import android.content.Context

/** Text for compact app catalog and created-widget list, in all six supported languages. */
object CatalogText {
    private val keys = listOf("title", "search", "customize", "preview", "iconStyle", "default", "custom", "transparency", "create", "close", "photoLoading", "photoError", "deleteTitle", "deleteMessage", "deleteConfirm", "deleteDone")
    private val translations = mapOf(
        "tr" to listOf("Widget Oluştur", "Uygulama ara", "Özelleştir:", "Ön izleme", "Simge biçimi", "Varsayılan", "Özel Fotoğraf", "Görünürlük / Şeffaflık", "Oluştur", "Kapat", "Fotoğraf hazırlanıyor", "Fotoğraf okunamadı", "Widget silinsin mi?", "Bu widget'ın yerel ayarları silinecek. Android ana ekranındaki kutucuğu kaldırmak için üzerine uzun basıp Kaldır'ı seç.", "Sil", "Widget ayarları silindi."),
        "en" to listOf("Add New Widget", "Search apps", "Customize:", "Preview", "Icon style", "Default", "Custom Photo", "Visibility / Transparency", "Create", "Close", "Preparing photo", "Photo could not be opened", "Delete widget?", "Its local settings will be erased. To remove the tile from your Android home screen, long-press it and choose Remove.", "Delete", "Widget settings deleted."),
        "de" to listOf("Neues Widget", "Apps suchen", "Anpassen:", "Vorschau", "Symbolstil", "Standard", "Eigenes Foto", "Sichtbarkeit / Transparenz", "Erstellen", "Schließen", "Foto wird vorbereitet", "Foto konnte nicht geöffnet werden", "Widget löschen?", "Lokale Einstellungen werden gelöscht. Zum Entfernen vom Startbildschirm das Widget lange drücken und Entfernen wählen.", "Löschen", "Widget-Einstellungen gelöscht."),
        "es" to listOf("Añadir widget", "Buscar aplicaciones", "Personalizar:", "Vista previa", "Estilo de icono", "Predeterminado", "Foto propia", "Visibilidad / Transparencia", "Crear", "Cerrar", "Preparando foto", "No se pudo abrir la foto", "¿Eliminar widget?", "Se borrarán los ajustes locales. Para quitarlo de la pantalla de inicio, mantenlo pulsado y selecciona Quitar.", "Eliminar", "Ajustes eliminados."),
        "ja" to listOf("ウィジェットを追加", "アプリを検索", "カスタマイズ:", "プレビュー", "アイコン形式", "標準", "写真を選択", "表示 / 透明度", "作成", "閉じる", "写真を準備中", "写真を開けません", "削除しますか？", "端末内の設定を削除します。ホーム画面の枠は長押しして削除してください。", "削除", "設定を削除しました。"),
        "ar" to listOf("إضافة ويدجت", "بحث عن التطبيقات", "تخصيص:", "معاينة", "نمط الأيقونة", "افتراضي", "صورة مخصصة", "الظهور / الشفافية", "إنشاء", "إغلاق", "تجهيز الصورة", "تعذر فتح الصورة", "حذف الويدجت؟", "سيتم حذف الإعدادات المحلية. لإزالة المربع من الشاشة الرئيسية اضغط عليه مطولًا ثم اختر إزالة.", "حذف", "تم حذف إعدادات الويدجت.")
    )
    fun t(ctx: Context, key: String): String {
        val index = keys.indexOf(key)
        if (index < 0) return key
        return translations[VaryokText.language(ctx)]?.get(index)
            ?: translations["tr"]!![index]
    }
}
