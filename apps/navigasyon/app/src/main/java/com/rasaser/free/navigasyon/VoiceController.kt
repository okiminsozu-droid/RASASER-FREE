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
 * No cloud speech service. Recognition is offered only where Android has an
 * explicitly on-device speech recognizer. Text-to-speech also depends on locally
 * installed voice data for the selected language.
 */
class VoiceController(
    private val context: Context,
    private val onMessage: (TextKey, String?) -> Unit
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
    fun listenOffline() {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            onMessage(TextKey.MICROPHONE_PERMISSION, null)
            return
        }
        if (Build.VERSION.SDK_INT < 31 || !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            onMessage(TextKey.VOICE_UNAVAILABLE, null)
            return
        }
        speech?.destroy()
        speech = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        speech?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { onMessage(TextKey.RECORDING, null) }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { onMessage(TextKey.VOICE_ERROR, null) }
            override fun onResults(results: Bundle?) {
                val first = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                onMessage(if (first.isNullOrBlank()) TextKey.VOICE_ERROR else TextKey.RECOGNIZED, first)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speech?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang.tag)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        })
    }
    fun close() {
        speech?.destroy()
        speech = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
