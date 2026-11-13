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
    private val codes = setOf("tr", "en", "de", "es", "zh", "ar", "ja")
    private val names = mapOf(
        "tr" to "Türkçe", "en" to "English", "de" to "Deutsch",
        "es" to "Español", "zh" to "中文", "ar" to "العربية", "ja" to "日本語"
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
        val localHint = OfflineWordMeaning.find(selectedText, source, target)
        var reviewed: ReviewedQuranGlossary.Entry? = null
        var printed: SelectableTextLayer.PrintedTurkishGloss? = null
        var machineResult: String? = null
        var status = "Doğrulanmış kelime anlamı kontrol ediliyor…"

        val dialog = AlertDialog.Builder(activity)
            .setTitle("Kelime çevirisi · ${names[source] ?: source} → ${names[target] ?: target}")
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
                append("Seçilen (").append(names[source] ?: source).append("): ")
                    .append(selectedText).append("\n\n")
                if (reviewed != null) {
                    append("Doğrulanmış Kur’an kelime anlamı\n")
                    append(reviewed!!.meaning)
                    append("\nKaynak: ").append(reviewed!!.reference).append("\n\n")
                } else if (printed != null) {
                    append("PDF’de Arapça kelimenin altında yazan Türkçe (OCR)\\n")
                    append(printed!!.text).append("\\n\\n")
                    append("Bu karşılık aynı sayfadan çevrimdışı algılandı; ")
                    append("OCR eşleşmesinin doğruluğu denetlenmelidir.\\n\\n")
                } else if (localHint != null) {
                    append("Çevrimdışı dilbilgisel karşılık (olası)\n")
                    append(localHint.text).append("\n\n")
                    append("Bu Türkçe kelimenin karşılığı cümleye göre değişebilir. ")
                    append("Bu, doğrulanmış Kur’an kelime meali değildir.\n\n")
                } else if (source == target) {
                    append("Seçili metin zaten hedef dilde.\n\n")
                } else {
                    append("Bu seçim için doğrulanmış Kur’an kelime anlamı bulunmuyor.\n\n")
                }
                if (machineResult != null) {
                    append("Google ML Kit — bağlamdan bağımsız otomatik öneri\n")
                    append(machineResult).append("\n\n")
                    append("Tek kelime çevirisi yanılabilir; Kur’an meali veya tefsir değildir.")
                } else {
                    append(status)
                }
                append("\n\nNot: Seçili metin OCR ile algılanmış olabilir; ")
                append("harfleri asıl sayfa üzerinden kontrol edin.")
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
                DownloadConditions.Builder().build()
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
            // Check actual ML Kit models: one-tap pack downloads must work offline
            // even when the old per-language-pair SharedPreferences was not set.
            OfflineLanguagePacks.pairReady(source, target) { alreadyInstalled ->
                if (alreadyInstalled) {
                    performGoogle(sourceTag, targetTag)
                } else if (!OfflineLanguagePacks.hasWifiInternet(activity)) {
                    status = "İlk model indirme için internete bağlı Wi-Fi gerekiyor. " +
                        "Ana sayfadaki Çevrimdışı Dil Paketlerini İndir bölümünü açın."
                    render()
                } else {
                    AlertDialog.Builder(activity)
                        .setTitle("Dil paketi gerekli")
                        .setMessage(
                            "${names[target] ?: target} için Google ML Kit modeli ilk kez " +
                                "Wi-Fi üzerinden indirilecek. Bir kez indirildiğinde " +
                                "metin cihazda çevrilir ve internet gerekmez."
                        )
                        .setNegativeButton("Vazgeç", null)
                        .setPositiveButton("İndir ve çevir") { _, _ ->
                            performGoogle(sourceTag, targetTag)
                        }.show()
                }
            }
        }

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            val copied = reviewed?.meaning ?: printed?.text ?: localHint?.text ?: machineResult ?: selectedText
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
            val isOneWord = firstOcrId > 0 && firstOcrId == lastOcrId
            val result = if (isOneWord) {
                glossary.find(readerPage, firstOcrId, source, target, selectedText)
            } else null
            val fromPrintedPdf = if (result == null && isOneWord &&
                source == "ar" && target == "tr"
            ) {
                selectableLayer.printedTurkishBelow(readerPage, firstOcrId, selectedText)
            } else null
            activity.runOnUiThread {
                if (!visible(dialog)) return@runOnUiThread
                reviewed = result
                printed = fromPrintedPdf
                status = when {
                    result != null -> "Otomatik çeviriyle karşılaştırmak için Google çeviriye dokunun."
                    fromPrintedPdf != null -> "Basılı Türkçe OCR karşılığı bulundu; " +
                        "Google otomatik çevirisi ayrıca açılabilir."
                    localHint != null -> "Bu kelimenin birden çok olası anlamı var. " +
                        "Google ML Kit sonucunu ayrıca karşılaştırabilirsiniz."
                    source == target -> ""
                    else -> "Cihaz içi ML Kit çevirisi kontrol ediliyor…"
                }
                render()
                // Never replace an available verified gloss OR an unambiguous display
                // of grammatical alternatives with a one-word ML Kit guess.
                if (result == null && fromPrintedPdf == null && localHint == null &&
                    source != target) requestGoogle()
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
