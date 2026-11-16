package com.rasaser.free.navigasyon

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * On-device recognition is always preferred. The optional Android system
 * recognizer may contact its provider, and is used ONLY after explicit consent.
 * No commercial or paid speech API is embedded in this app.
 */
class VoiceController(
    private val context: Context,
    private val onMessage: (TextKey, String?) -> Unit,
    private val onRecognized: (String) -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var speech: SpeechRecognizer? = null
    private var lang = NavLanguage.TR
    private var speechReady = false
    private var enabled = true

    fun init() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) setLanguage(lang)
        }
    }
    fun setLanguage(language: NavLanguage) {
        lang = language
        val engine = tts ?: return
        val wanted = Locale.forLanguageTag(lang.tag)
        // Never select a cloud-dependent TTS voice in an offline-first app.
        val localVoice = runCatching {
            engine.voices?.filter { it.locale.language == wanted.language &&
                !it.isNetworkConnectionRequired }?.firstOrNull()
        }.getOrNull()
        speechReady = localVoice != null && runCatching {
            engine.setVoice(localVoice) == TextToSpeech.SUCCESS
        }.getOrDefault(false)
    }
    fun setEnabled(state: Boolean) {
        enabled = state
        if (!state) tts?.stop()
    }
    fun speak(message: String) {
        if (enabled && speechReady) tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "nav_speech")
        else if (enabled) onMessage(TextKey.VOICE_UNAVAILABLE, null)
    }
    fun canRecognizeOffline():Boolean =
        Build.VERSION.SDK_INT >= 31 &&
        SpeechRecognizer.isOnDeviceRecognitionAvailable(context) &&
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED

    fun canRecognizeSystem():Boolean=
        SpeechRecognizer.isRecognitionAvailable(context) &&
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED

    fun listenOffline() = listen(allowSystemProvider=false)
    /** Uses the device speech provider only after an affirmative user action. */
    fun listenSystemWithConsent() = listen(allowSystemProvider=true)

    private fun listen(allowSystemProvider:Boolean) {
        if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            onMessage(TextKey.MICROPHONE_PERMISSION,null)
            return
        }
        val onDevice=Build.VERSION.SDK_INT>=31 &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        if(!onDevice && !(allowSystemProvider && SpeechRecognizer.isRecognitionAvailable(context))){
            onMessage(TextKey.VOICE_UNAVAILABLE,null)
            return
        }
        speech?.destroy()
        speech = try {
            if(onDevice)SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            else SpeechRecognizer.createSpeechRecognizer(context)
        }catch (_:Exception){
            onMessage(TextKey.VOICE_UNAVAILABLE,null)
            return
        }
        speech?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { onMessage(TextKey.RECORDING, null) }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                // A missing on-device language model is not a navigation failure.
                // Android may prompt the user before downloading that voice model.
                if(onDevice && Build.VERSION.SDK_INT>=33 &&
                    (error==SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ||
                     error==SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED)) {
                    runCatching {
                        speech?.triggerModelDownload(recognitionIntent())
                    }
                }
                onMessage(TextKey.VOICE_ERROR,null)
            }
            override fun onResults(results: Bundle?) {
                val first = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                onMessage(if (first.isNullOrBlank()) TextKey.VOICE_ERROR else TextKey.RECOGNIZED, first)
                if(!first.isNullOrBlank())onRecognized(first)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speech?.startListening(recognitionIntent())
    }

    private fun recognitionIntent()=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE,lang.tag)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3)
    }

    fun close() {
        speech?.destroy()
        speech = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
