package dev.aitalkingavatar.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SpeechRecognitionController(
    private val context: Context,
    private val onPartial: (String) -> Unit,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onListening: (Boolean) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    private var destroyed = false

    fun start(localeTag: String = "en-US") {
        if (destroyed) return
        stop(cancel = true)
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("No speech recognition provider is installed. You can still type your message.")
            return
        }
        try {
            val speech = if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = speech
            speech.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { onListening(true) }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() { onListening(false) }
                override fun onError(error: Int) {
                    onListening(false)
                    onError(messageFor(error))
                }
                override fun onResults(results: Bundle?) {
                    onListening(false)
                    val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    if (words.isBlank()) onError("No speech was recognized. Try again or type your message.") else onResult(words)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onPartial)
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.forLanguageTag(localeTag).toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak once, then review the transcription")
            }
            speech.startListening(intent)
        } catch (_: SecurityException) {
            onListening(false)
            onError("Microphone permission is required. Enable it in Android Settings to use voice input.")
        } catch (_: UnsupportedOperationException) {
            onListening(false)
            onError("On-device recognition is unavailable. Install or enable a speech provider, or type instead.")
        } catch (_: Exception) {
            onListening(false)
            onError("Speech recognition could not start. Check the installed provider and try again.")
        }
    }

    fun stop(cancel: Boolean = false) {
        onListening(false)
        runCatching { if (cancel) recognizer?.cancel() else recognizer?.stopListening() }
        if (cancel) {
            runCatching { recognizer?.destroy() }
            recognizer = null
        }
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        stop(cancel = true)
    }

    private fun messageFor(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone audio could not be captured. Check microphone access and try again."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is missing. Type a message or enable permission in Android Settings."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "The speech provider needs a network connection for this language. Offline recognition depends on provider and downloaded language data."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "This speech provider does not have the selected language available."
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech was recognized. Try again or type your message."
        else -> "Speech recognition stopped unexpectedly (code $code). Try again or type instead."
    }
}
