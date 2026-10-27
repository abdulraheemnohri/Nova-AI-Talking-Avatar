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
    private var activeReadableText: String = ""
    private var activeSpeechRate: Float = 1f
    private var activeUtteranceId: String? = null

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onVisemeTimeline(activeVisemes)
                        onStarted()
                    }
                    override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                        // Some Android TTS engines report character ranges as audio playback advances.
                        // Use these callbacks to re-anchor a short viseme segment near the spoken range.
                        // Providers that omit range callbacks retain the full-text heuristic fallback.
                        if (utteranceId != activeUtteranceId) return
                        val from = start.coerceIn(0, activeReadableText.length)
                        val until = end.coerceIn(from, activeReadableText.length)
                        if (until > from) {
                            onVisemeTimeline(
                                SpeechVisemeTimeline.estimateFromText(
                                    activeReadableText.substring(from, until),
                                    activeSpeechRate
                                )
                            )
                        }
                    }
                    override fun onDone(utteranceId: String?) {
                        if (utteranceId != activeUtteranceId) return
                        onVisemeTimeline(emptyList())
                        activeVisemes = emptyList()
                        activeReadableText = ""
                        activeUtteranceId = null
                        onFinished(null)
                    }
                    @Deprecated("Deprecated by Android; retained for older provider callbacks")
                    override fun onError(utteranceId: String?) {
                        if (utteranceId != activeUtteranceId) return
                        onVisemeTimeline(emptyList())
                        activeVisemes = emptyList()
                        activeReadableText = ""
                        activeUtteranceId = null
                        onFinished("Speech synthesis failed. Check the installed voice and language data.")
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        if (utteranceId != activeUtteranceId) return
                        onVisemeTimeline(emptyList())
                        activeVisemes = emptyList()
                        activeReadableText = ""
                        activeUtteranceId = null
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
        // Range callbacks can align short heuristic segments to TTS playback when the engine supports them.
        // They still do not provide phoneme-level timing; keep a whole-text fallback for other engines.
        activeReadableText = readable
        activeSpeechRate = settings.speechRate.coerceIn(0.5f, 2f)
        activeVisemes = SpeechVisemeTimeline.estimateFromText(readable, activeSpeechRate)
        val utteranceId = UUID.randomUUID().toString()
        activeUtteranceId = utteranceId
        tts.speak(readable, TextToSpeech.QUEUE_FLUSH, Bundle(), utteranceId)
    }

    fun stop() {
        pending = null
        activeVisemes = emptyList()
        activeReadableText = ""
        activeUtteranceId = null
        onVisemeTimeline(emptyList())
        runCatching { engine?.stop() }
        onFinished(null)
    }

    fun shutdown() {
        pending = null
        activeVisemes = emptyList()
        activeReadableText = ""
        activeUtteranceId = null
        onVisemeTimeline(emptyList())
        runCatching { engine?.stop() }
        runCatching { engine?.shutdown() }
        engine = null
        ready = false
    }
}
