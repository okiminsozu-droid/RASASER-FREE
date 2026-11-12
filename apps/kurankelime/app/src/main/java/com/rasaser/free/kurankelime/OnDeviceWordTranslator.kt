package com.rasaser.free.kurankelime

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.google.mlkit.nl.translate.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

/** Optional on-device word translation. Only language model downloads need connectivity.
 * Machine translation is not an authenticated Quran word-by-word commentary.
 */
internal class OnDeviceWordTranslator(private val activity: Activity) {
    private val codes=setOf("tr","en","de","es","zh","ar")
    private val names=mapOf(
        "tr" to "Türkçe","en" to "English","de" to "Deutsch",
        "es" to "Español","zh" to "中文","ar" to "العربية"
    )

    fun translate(selection: BundledWordIndex.Selection) {
        val source=selection.sourceLanguage
        val target=activity.getSharedPreferences("discovery_home",Context.MODE_PRIVATE)
            .getString("lang","tr")?.takeIf { it in codes } ?: "tr"
        val selected=selection.text.trim()
        if(selected.isEmpty())return
        if(source==target) {
            showTranslation(selected,target)
            return
        }
        val sourceTag=TranslateLanguage.fromLanguageTag(source)
        val targetTag=TranslateLanguage.fromLanguageTag(target)
        if(sourceTag==null || targetTag==null) {
            Toast.makeText(activity,"Bu dil için çeviri motoru mevcut değil.",Toast.LENGTH_LONG).show()
            return
        }
        val pairKey="$source-$target"
        val flags=activity.getSharedPreferences("quran_translation_models",Context.MODE_PRIVATE)
        if(!flags.getBoolean(pairKey,false)) {
            AlertDialog.Builder(activity)
                .setTitle("Çevrimdışı çeviri modeli")
                .setMessage(
                    "${names[target]} çevirisi için ilk kullanımda ücretsiz dil modeli "+
                    "Wi-Fi üzerinden indirilebilir (yaklaşık 30 MB/dil). "+
                    "İndirildikten sonra çeviri cihazda yapılır. Devam edilsin mi?"
                )
                .setNegativeButton("Vazgeç",null)
                .setPositiveButton("Devam"){_,_-> runTranslation(sourceTag,targetTag,target,selected,pairKey)}
                .show()
        } else runTranslation(sourceTag,targetTag,target,selected,pairKey)
    }

    private fun runTranslation(source:String,targetTag:String,target:String,text:String,pairKey:String) {
        val translator=Translation.getClient(TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(targetTag)
            .build())
        val prefs=activity.getSharedPreferences("quran_translation_models",Context.MODE_PRIVATE)
        var translated:String?=null
        val dialog=AlertDialog.Builder(activity)
            .setTitle("Çevir · ${names[target] ?: target}")
            .setMessage("Seçilen kelime: $text\n\nÇevrimdışı çeviri modeli kontrol ediliyor…")
            .setNegativeButton("Kapat",null)
            .setPositiveButton("Kopyala"){_,_->
                translated?.let {
                    (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("Çevrilen kelime",it))
                }
            }.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=false
        // Called only after a user selects Translate, never at application startup.
        translator.downloadModelIfNeeded(DownloadConditions.Builder().requireWifi().build())
            .addOnSuccessListener {
                prefs.edit().putBoolean(pairKey,true).apply()
                translator.translate(text)
                    .addOnSuccessListener { result ->
                        translated=result
                        if(!activity.isFinishing&&!activity.isDestroyed&&dialog.isShowing) {
                            dialog.setMessage(
                                "$text\n\n${names[target] ?: target}: $result\n\n"+
                                "Otomatik çeviri; doğrulanmış Kur’an kelime meali değildir."
                            )
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=true
                        }
                    }
                    .addOnFailureListener {
                        if(!activity.isFinishing&&!activity.isDestroyed&&dialog.isShowing)
                            dialog.setMessage("Kelime çevrilemedi. Metni yeniden seçebilirsiniz.")
                    }
                    .addOnCompleteListener { translator.close() }
            }
            .addOnFailureListener {
                translator.close()
                if(!activity.isFinishing&&!activity.isDestroyed&&dialog.isShowing)
                    dialog.setMessage(
                        "Bu dilin modeli henüz cihazda bulunmuyor veya indirilemedi. "+
                        "İlk kurulumda Wi-Fi gerekir; sonraki çeviriler internetsiz yapılır."
                    )
            }
    }

    private fun showTranslation(original:String,target:String) {
        AlertDialog.Builder(activity)
            .setTitle("Seçili kelime · ${names[target] ?: target}")
            .setMessage("$original\n\nAynı dil seçili; çeviri gerekmiyor.")
            .setPositiveButton("Kopyala"){_,_->
                (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("Seçili kelime",original))
            }.setNegativeButton("Kapat",null).show()
    }
}
