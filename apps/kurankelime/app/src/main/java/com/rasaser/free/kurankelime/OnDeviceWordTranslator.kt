package com.rasaser.free.kurankelime

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import android.widget.TextView
import android.view.Gravity
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
    private val readerUi by lazy { ReaderUiText.load(activity) }
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
        var status = "Çevriliyor…"

        val dialog = AlertDialog.Builder(activity)
            .setMessage("Çevriliyor…")
            .setPositiveButton(readerUi.copy, null)
            .setNegativeButton(readerUi.closeDialog, null)
            .create()
        activeDialog = dialog
        dialog.setOnDismissListener {
            if (activeDialog === dialog) activeDialog = null
        }
        dialog.show()
        // Only the translated word and two actions are visible. No OCR/ML notes.
        dialog.findViewById<TextView>(android.R.id.message)?.apply {
            textSize = 23f
            gravity = Gravity.CENTER
            setPadding(px(24), px(30), px(24), px(22))
        }
        // Keep the result compact and inside the SAME reading activity.
        dialog.window?.setLayout(
            minOf(activity.resources.displayMetrics.widthPixels - px(32), px(355)),
            -2
        )

        // All existing data-source precedence remains unchanged; hide provenance,
        // warnings, source text and model labels from the translation popup.
        fun translatedWord(): String? = reviewed?.meaning?.takeIf { it.isNotBlank() }
            ?: printed?.text?.takeIf { it.isNotBlank() }
            ?: localHint?.text?.takeIf { it.isNotBlank() }
            ?: machineResult?.takeIf { it.isNotBlank() }
            ?: if (source == target) selectedText else null

        fun render() {
            if (!visible(dialog)) return
            dialog.setMessage(translatedWord() ?: status)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled =
                translatedWord() != null
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
                        .setNegativeButton(readerUi.cancel, null)
                        .setPositiveButton(readerUi.downloadTranslate) { _, _ ->
                            performGoogle(sourceTag, targetTag)
                        }.show()
                }
            }
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val copied = translatedWord() ?: return@setOnClickListener
            (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("Kur’an kelimesi", copied))
            Toast.makeText(activity, readerUi.copied, Toast.LENGTH_SHORT).show()
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
                status = "Çevriliyor…"
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
