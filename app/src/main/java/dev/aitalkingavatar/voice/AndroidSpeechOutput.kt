package dev.aitalkingavatar.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dev.aitalkingavatar.avatar.SpeechVisemeTimeline
import dev.aitalkingavatar.avatar.VisemeFrame
import dev.aitalkingavatar.data.AvatarPreferences
import java.util.Locale
import java.util.UUID

class AndroidSpeechOutput(
    context: Context,
    private val onStarted: () -> Unit,
    private val onVisemeTimeline: (List<VisemeFrame>) -> Unit,
    private val onFinished: (String?) -> Unit
) {
    private var ready = false
    private var pending: Pair<String, AvatarPreferences>? = null
    private var engine: TextToSpeech? = null
    private var activeVisemes: List<VisemeFrame> = emptyList()

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onVisemeTimeline(activeVisemes)
                        onStarted()
                    }
                    override fun onDone(utteranceId: String?) {
                        onVisemeTimeline(emptyList())
                        activeVisemes = emptyList()
                        onFinished(null)
                    }
                    @Deprecated("Deprecated by Android; retained for older provider callbacks")
                    override fun onError(utteranceId: String?) {
                        onVisemeTimeline(emptyList())
                        activeVisemes = emptyList()
                        onFinished("Speech synthesis failed. Check the installed voice and language data.")
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        onVisemeTimeline(emptyList())
                        activeVisemes = emptyList()
                        onFinished("Speech synthesis failed (code $errorCode). Check the installed voice and language data.")
                    }
                })
                pending?.let { (text, settings) -> speak(text, settings) }
                pending = null
            } else onFinished("Text-to-speech could not initialize. Check that an Android voice engine is installed.")
        }
    }

    fun speak(text: String, settings: AvatarPreferences) {
        if (!ready) {
            pending = text to settings
            return
        }
        val tts = engine ?: return
        val locale = Locale.forLanguageTag(settings.voiceLocale.ifBlank { "en-US" })
        val languageStatus = tts.setLanguage(locale)
        if (languageStatus == TextToSpeech.LANG_MISSING_DATA || languageStatus == TextToSpeech.LANG_NOT_SUPPORTED) {
            onFinished("The installed text-to-speech engine does not have data for ${locale.toLanguageTag()}. Install the language voice or choose another locale.")
            return
        }
        settings.voiceName.takeIf { it.isNotBlank() }?.let { requested ->
            tts.voices?.firstOrNull { it.name == requested }?.let { tts.voice = it }
        }
        tts.setSpeechRate(settings.speechRate.coerceIn(0.5f, 2f))
        tts.setPitch(settings.pitch.coerceIn(0.5f, 2f))
        val readable = text.replace(Regex("```[\\s\\S]*?```"), " Code is shown on screen. ").take(3500)
        // Heuristic text cues are a visual fallback only; Android TTS does not expose phoneme alignment.
        activeVisemes = SpeechVisemeTimeline.estimateFromText(readable, settings.speechRate)
        tts.speak(readable, TextToSpeech.QUEUE_FLUSH, Bundle(), UUID.randomUUID().toString())
    }

    fun stop() {
        pending = null
        activeVisemes = emptyList()
        onVisemeTimeline(emptyList())
        runCatching { engine?.stop() }
        onFinished(null)
    }

    fun shutdown() {
        pending = null
        activeVisemes = emptyList()
        onVisemeTimeline(emptyList())
        runCatching { engine?.stop() }
        runCatching { engine?.shutdown() }
        engine = null
        ready = false
    }
}
