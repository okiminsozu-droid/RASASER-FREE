package com.rasaser.free.kurankelime

import android.content.Context

/**
 * Only presentation text for existing native reader controls.
 * Reads the same language chosen on the home screen; it does not modify
 * Quran page assets, word selection, favorites, share implementation or OCR.
 */
internal data class ReaderUiText(
    val fit: String,
    val share: String,
    val hide: String,
    val copy: String,
    val translate: String,
    val closeSelection: String,
    val cancel: String,
    val go: String,
    val page: String,
    val choosePage: String,
    val sharePage: String,
    val shareFailed: String,
    val copied: String,
    val closeDialog: String,
    val downloadTranslate: String
) {
    companion object {
        fun load(context: Context): ReaderUiText {
            val language = context.getSharedPreferences("discovery_home", Context.MODE_PRIVATE)
                .getString("lang", "tr") ?: "tr"
            return forLanguage(language)
        }

        fun forLanguage(language: String): ReaderUiText = when (language) {
            "en" -> ReaderUiText(
                "Fit", "Share", "Hide", "Copy", "Translate", "Close selection",
                "Cancel", "Go", "Page", "Reading page (1–605)",
                "Share page", "Could not share page", "Word copied",
                "Close", "Download and translate"
            )
            "de" -> ReaderUiText(
                "Anpassen", "Teilen", "Ausbl.", "Kopieren", "Übersetzen",
                "Auswahl schließen", "Abbrechen", "Öffnen", "Seite",
                "Leseseite (1–605)", "Seite teilen", "Teilen fehlgeschlagen",
                "Wort kopiert", "Schließen", "Herunterladen und übersetzen"
            )
            "es" -> ReaderUiText(
                "Ajustar", "Compartir", "Ocultar", "Copiar", "Traducir",
                "Cerrar selección", "Cancelar", "Ir", "Página",
                "Página de lectura (1–605)", "Compartir página",
                "No se pudo compartir", "Palabra copiada", "Cerrar", "Descargar y traducir"
            )
            "ar" -> ReaderUiText(
                "ملاءمة", "مشاركة", "إخفاء", "نسخ", "ترجمة", "إغلاق التحديد",
                "إلغاء", "اذهب", "صفحة", "صفحة القراءة (1–605)",
                "مشاركة الصفحة", "تعذرت المشاركة", "تم نسخ الكلمة", "إغلاق", "تنزيل وترجمة"
            )
            "ja" -> ReaderUiText(
                "全体表示", "共有", "非表示", "コピー", "翻訳", "選択を閉じる",
                "キャンセル", "移動", "ページ", "閲覧ページ (1–605)",
                "ページを共有", "共有できません", "単語をコピーしました", "閉じる", "ダウンロードして翻訳"
            )
            "zh" -> ReaderUiText(
                "适应", "分享", "隐藏", "复制", "翻译", "关闭选区",
                "取消", "跳转", "页", "阅读页码 (1–605)",
                "分享页面", "无法分享页面", "已复制单词", "关闭", "下载并翻译"
            )
            else -> ReaderUiText(
                "Sığdır", "Paylaş", "Gizle", "Kopyala", "Çevir", "Seçimi kapat",
                "Vazgeç", "Git", "Sayfa", "Okuma sayfası (1–605)",
                "Sayfayı paylaş", "Paylaşım yapılamadı", "Kelime kopyalandı", "Kapat", "İndir ve çevir"
            )
        }
    }
}
