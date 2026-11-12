package com.rasaser.free.kurankelime

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.concurrent.Executors

/**
 * In-reader, privacy-preserving translation.
 * Selected Quran text is NEVER uploaded to a paid Cloud Translation endpoint.
 * The ML Kit language-model download is the only network-dependent phase.
 *
 * Reviewed scripture meanings may be shown ONLY when the optional, independently
 * reviewed exact-token glossary contains a matching source, page, language and ID.
 */
internal class OnDeviceWordTranslator(
    private val activity: Activity,
    private val selectableLayer: SelectableTextLayer
) {
    private val glossary = ReviewedQuranGlossary(activity.applicationContext)
    private val lookupWorker = Executors.newSingleThreadExecutor()
    private val codes = setOf("tr", "en", "de", "es", "zh", "ar")
    private val names = mapOf(
        "tr" to "Türkçe", "en" to "English", "de" to "Deutsch",
        "es" to "Español", "zh" to "中文", "ar" to "العربية"
    )
    private var activeDialog: AlertDialog? = null

    fun translate(
        selection: BundledWordIndex.Selection,
        readerPage: Int,
        firstOcrId: Int,
        lastOcrId: Int
    ) {
        val source = selection.sourceLanguage
        val target = activity.getSharedPreferences("discovery_home", Context.MODE_PRIVATE)
            .getString("lang", "tr")?.takeIf { it in codes } ?: "tr"
        val selectedText = selection.text.trim()
        if (selectedText.isBlank()) return

        activeDialog?.dismiss()
        var reviewed: ReviewedQuranGlossary.Entry? = null
        var printed: SelectableTextLayer.PrintedTurkishGloss? = null
        var machineResult: String? = null
        var status = "PDF’deki basılı Türkçe karşılık kontrol ediliyor…"

        val dialog = AlertDialog.Builder(activity)
            .setTitle("Kelime çevirisi · ${names[target] ?: target}")
            .setMessage("")
            .setPositiveButton("Google çeviri", null)
            .setNeutralButton("Kopyala", null)
            .setNegativeButton("Kapat", null)
            .create()
        activeDialog = dialog
        dialog.setOnDismissListener {
            if (activeDialog === dialog) activeDialog = null
        }
        dialog.show()
        // Keep the result compact and inside the SAME reading activity.
        dialog.window?.setLayout(
            minOf(activity.resources.displayMetrics.widthPixels - px(32), px(355)),
            -2
        )

        fun render() {
            if (!visible(dialog)) return
            dialog.setMessage(buildString {
                append("Seçilen: ").append(selectedText).append("\n\n")
                if (reviewed != null) {
                    append("Doğrulanmış Kur’an kelime anlamı\n")
                    append(reviewed!!.meaning)
                    append("\nKaynak: ").append(reviewed!!.reference)
                } else if (printed != null) {
                    append("PDF’de kelimenin altında basılı Türkçe\n")
                    append(printed!!.text)
                    append("\n\nÇevrimdışı, konumdan eşleştirilmiş OCR metnidir; ")
                    append("dinî açıdan doğrulanmış kelime meali değildir.")
                } else {
                    append("PDF’de bu kelimenin altında güvenilir bir Türkçe OCR ")
                    append("eşleştirmesi bulunamadı.")
                }
                if (machineResult != null) {
                    append("\n\nGoogle ML Kit — otomatik çeviri\n")
                    append(machineResult)
                    append("\nOtomatik çeviri Kur’an meali veya tefsir değildir.")
                } else if (status.isNotBlank()) {
                    append("\n\n").append(status)
                }
                append("\n\nSeçili Arapça metin OCR kaynaklı olabilir; ")
                append("asıl sayfadaki yazıyı kontrol edin.")
            })
        }

        fun performGoogle(sourceTag: String, targetTag: String) {
            if (!visible(dialog)) return
            status = "Google dil modeli kontrol ediliyor…"
            render()
            val client = Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(sourceTag)
                    .setTargetLanguage(targetTag)
                    .build()
            )
            val pairKey = "$source-$target"
            val prefs = activity.getSharedPreferences(
                "quran_translation_models", Context.MODE_PRIVATE
            )
            client.downloadModelIfNeeded(
                DownloadConditions.Builder().requireWifi().build()
            ).addOnSuccessListener {
                prefs.edit().putBoolean(pairKey, true).apply()
                client.translate(selectedText)
                    .addOnSuccessListener { result ->
                        machineResult = result
                        status = ""
                        render()
                    }.addOnFailureListener {
                        status = "Otomatik çeviri alınamadı. Yeniden deneyebilirsiniz."
                        render()
                    }.addOnCompleteListener { client.close() }
            }.addOnFailureListener {
                client.close()
                status = "Dil modeli cihazda yok veya indirilemedi. " +
                    "İlk indirme için Wi-Fi açın; sonra çeviri çevrimdışı yapılabilir."
                render()
            }
        }

        fun requestGoogle() {
            if (!visible(dialog)) return
            if (source == target) {
                status = "Metin zaten seçilen dilde; otomatik çeviri gerekli değil."
                render()
                return
            }
            val sourceTag = TranslateLanguage.fromLanguageTag(source)
            val targetTag = TranslateLanguage.fromLanguageTag(target)
            if (sourceTag == null || targetTag == null) {
                status = "Bu dil için cihaz içi çeviri desteklenmiyor."
                render()
                return
            }
            val key = "$source-$target"
            val downloadedBefore = activity.getSharedPreferences(
                "quran_translation_models", Context.MODE_PRIVATE
            ).getBoolean(key, false)
            if (!downloadedBefore) {
                AlertDialog.Builder(activity)
                    .setTitle("İlk kullanım: ücretsiz dil modeli")
                    .setMessage(
                        "${names[target] ?: target} için Google ML Kit dil modeli " +
                            "ilk kullanımda Wi-Fi ile indirilecek. Sonraki çeviriler " +
                            "model cihazda durduğu sürece internet gerektirmez. " +
                            "Seçilen Kur’an metni Google Cloud Translation'a gönderilmez."
                    )
                    .setNegativeButton("Vazgeç", null)
                    .setPositiveButton("İndir ve çevir") { _, _ ->
                        performGoogle(sourceTag, targetTag)
                    }.show()
            } else performGoogle(sourceTag, targetTag)
        }

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            val copied = reviewed?.meaning ?: printed?.text ?: machineResult ?: selectedText
            (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("Kur’an kelimesi", copied))
            Toast.makeText(activity, "Metin kopyalandı", Toast.LENGTH_SHORT).show()
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            requestGoogle()
        }
        render()

        lookupWorker.execute {
            // Multiple OCR words cannot be inferred to have a verified single-word
            // gloss. Public builds have no approved glossary and safely fall back.
            val isSingleWord = firstOcrId > 0 && firstOcrId == lastOcrId
            val result = if (isSingleWord) {
                glossary.find(readerPage, firstOcrId, source, target, selectedText)
            } else null
            val fromPdf = if (result == null && isSingleWord &&
                source == "ar" && target == "tr"
            ) {
                selectableLayer.printedTurkishBelow(readerPage, firstOcrId, selectedText)
            } else null
            activity.runOnUiThread {
                if (!visible(dialog)) return@runOnUiThread
                reviewed = result
                printed = fromPdf
                status = if (result != null || fromPdf != null)
                    "Google’ın otomatik çevirisi istenirse ayrıca açılabilir."
                else "Google çeviri yalnız düğmeye basıldığında kullanılır."
                render()
                // Never silently start machine translation if a local PDF
                // gloss is missing: the offline source stays primary.
            }
        }
    }

    private fun visible(dialog: AlertDialog): Boolean =
        !activity.isFinishing && !activity.isDestroyed && dialog.isShowing

    private fun px(dp: Int): Int = (dp * activity.resources.displayMetrics.density).toInt()

    fun close() {
        activeDialog?.dismiss()
        activeDialog = null
        lookupWorker.shutdownNow()
        glossary.close()
    }
}
