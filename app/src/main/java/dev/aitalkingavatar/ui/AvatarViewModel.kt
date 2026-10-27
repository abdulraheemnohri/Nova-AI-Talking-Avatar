package dev.aitalkingavatar.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.aitalkingavatar.ai.AiAdapter
import dev.aitalkingavatar.ai.AiProviderFactory
import dev.aitalkingavatar.ai.ConnectionResult
import dev.aitalkingavatar.ai.OfflineDemoAdapter
import dev.aitalkingavatar.ai.OllamaAdapter
import dev.aitalkingavatar.avatar.AvatarState
import dev.aitalkingavatar.avatar.VisemeFrame
import dev.aitalkingavatar.data.AiProviderType
import dev.aitalkingavatar.data.AvatarPreferences
import dev.aitalkingavatar.data.ChatMessage
import dev.aitalkingavatar.data.Conversation
import dev.aitalkingavatar.data.ConversationStore
import dev.aitalkingavatar.data.PreferencesRepository
import dev.aitalkingavatar.data.importCharacter
import dev.aitalkingavatar.voice.AndroidSpeechOutput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

 data class AvatarUiState(
    val settings: AvatarPreferences = AvatarPreferences(),
    val conversations: List<Conversation> = emptyList(),
    val activeConversationId: String = "",
    val draft: String = "",
    val partialReply: String = "",
    val avatarState: AvatarState = AvatarState.IDLE,
    val busy: Boolean = false,
    val listening: Boolean = false,
    val status: String = "Offline demo ready",
    val error: String? = null,
    val connection: ConnectionResult? = null,
    val characterStatus: String = "Import a licensed rigged GLB character to begin",
    val cameraResetVersion: Int = 0,
    val diagnosticEvents: List<String> = emptyList(),
    val characterReloadVersion: Int = 0,
    val visemeFrames: List<VisemeFrame> = emptyList(),
    val visemeTimelineVersion: Int = 0
) {
    val activeConversation: Conversation? get() = conversations.firstOrNull { it.id == activeConversationId }
    val messages: List<ChatMessage> get() = activeConversation?.messages.orEmpty()
}

class AvatarViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesRepository = PreferencesRepository(application)
    private val conversationStore = ConversationStore(application)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var responseJob: Job? = null
    private var appInForeground = false
    var state by mutableStateOf(AvatarUiState())
        private set

    private val speaker = AndroidSpeechOutput(
        context = application,
        onStarted = { state = state.copy(avatarState = AvatarState.SPEAKING, status = "Speaking") },
        onVisemeTimeline = { frames ->
            state = state.copy(visemeFrames = frames, visemeTimelineVersion = state.visemeTimelineVersion + 1)
        },
        onFinished = { message ->
            state = state.copy(
                avatarState = if (message == null) AvatarState.IDLE else AvatarState.ERROR,
                status = message ?: "Ready",
                error = message ?: state.error,
                visemeFrames = emptyList(),
                visemeTimelineVersion = state.visemeTimelineVersion + 1
            )
        }
    )

    init {
        val settings = preferencesRepository.load()
        var conversations = conversationStore.load()
        if (conversations.isEmpty()) {
            conversations = listOf(newEmptyConversation())
            conversationStore.save(conversations)
        }
        val active = conversations.first()
        val characterExists = settings.characterPath?.let { File(it).isFile } == true
        state = AvatarUiState(
            settings = settings,
            conversations = conversations,
            activeConversationId = active.id,
            status = if (settings.modelName.isBlank()) "Offline demo · no model configured" else "Local model configured",
            characterStatus = if (characterExists) "Character saved · loading 3D asset" else "Import a licensed rigged GLB character to begin"
        )
        recordDiagnostic("App started; local settings and history loaded")
    }

    fun updateDraft(value: String) { state = state.copy(draft = value) }
    fun setListening(value: Boolean) {
        state = state.copy(listening = value, avatarState = if (value) AvatarState.LISTENING else if (!state.busy) AvatarState.IDLE else AvatarState.THINKING,
            status = if (value) "Listening · review the transcript before sending" else if (!state.busy) "Ready" else "Thinking")
    }
    fun setVoiceError(message: String) {
        state = state.copy(error = message, status = message, avatarState = AvatarState.ERROR)
        recordDiagnostic("Speech or voice service reported an error")
    }
    fun clearError() { state = state.copy(error = null, avatarState = if (state.busy) AvatarState.THINKING else AvatarState.IDLE) }

    fun updateSettings(transform: (AvatarPreferences) -> AvatarPreferences) {
        val previous = state.settings
        val updated = transform(previous)
        preferencesRepository.save(updated)
        state = state.copy(settings = updated, connection = null)
        if (updated.endpoint != previous.endpoint || updated.modelName != previous.modelName) recordDiagnostic("Local model settings changed; endpoint values omitted")
    }

    fun setCodingMode(enabled: Boolean) = updateSettings { it.copy(codingMode = enabled) }

    fun newConversation() {
        if (state.busy) cancelResponse()
        val conversation = newEmptyConversation()
        val items = listOf(conversation) + state.conversations
        conversationStore.save(items)
        state = state.copy(conversations = items, activeConversationId = conversation.id, draft = "", partialReply = "", error = null)
    }

    fun openConversation(id: String) {
        if (state.busy) cancelResponse()
        if (state.conversations.any { it.id == id }) state = state.copy(activeConversationId = id, partialReply = "", error = null)
    }

    fun clearHistory() {
        if (state.busy) cancelResponse()
        conversationStore.clear()
        val first = newEmptyConversation()
        conversationStore.save(listOf(first))
        state = state.copy(conversations = listOf(first), activeConversationId = first.id, partialReply = "", error = null)
    }

    fun exportHistory(): String = conversationStore.exportJson()

    fun sendDraft() {
        val text = state.draft.trim()
        if (text.isBlank() || state.busy) return
        sendMessage(text)
    }

    fun retryLast() {
        val conversation = state.activeConversation ?: return
        val lastUserIndex = conversation.messages.indexOfLast { it.role == "user" }
        if (lastUserIndex < 0) return
        val last = conversation.messages[lastUserIndex]
        sendMessage(last.content, retryExistingUser = true)
    }

    private fun sendMessage(text: String, retryExistingUser: Boolean = false) {
        val conversation = state.activeConversation ?: newEmptyConversation().also { new ->
            state = state.copy(conversations = listOf(new) + state.conversations, activeConversationId = new.id)
        }
        val existingIndex = if (retryExistingUser) conversation.messages.indexOfLast { it.role == "user" && it.content == text } else -1
        val prior: List<ChatMessage>
        if (existingIndex >= 0) {
            prior = conversation.messages.take(existingIndex)
            val existingUser = conversation.messages[existingIndex]
            updateConversation(conversation.copy(updatedAt = System.currentTimeMillis(), messages = prior + existingUser))
        } else {
            prior = conversation.messages
            val user = ChatMessage(UUID.randomUUID().toString(), "user", text)
            updateConversation(conversation.copy(title = if (prior.isEmpty()) text.take(54) else conversation.title, updatedAt = user.createdAt, messages = prior + user))
        }
        state = state.copy(draft = "", partialReply = "", busy = true, avatarState = AvatarState.THINKING, status = "Thinking", error = null)
        recordDiagnostic(if (state.settings.modelName.isBlank()) "Chat request started in offline demo mode" else "Chat request started for configured local model")
        responseJob?.cancel()
        responseJob = scope.launch {
            try {
                val activeSettings = state.settings
                val adapter: AiAdapter = AiProviderFactory.createAdapter(activeSettings)
                val answer = adapter.reply(prior, text, activeSettings.codingMode) { accumulated ->
                    state = state.copy(partialReply = accumulated)
                }
                val latest = state.conversations.firstOrNull { it.id == conversation.id } ?: conversation
                val assistant = ChatMessage(UUID.randomUUID().toString(), "assistant", answer)
                updateConversation(latest.copy(updatedAt = assistant.createdAt, messages = latest.messages + assistant))
                state = state.copy(busy = false, partialReply = "", avatarState = if (appInForeground) AvatarState.SPEAKING else AvatarState.IDLE,
                    status = if (appInForeground) "Speaking" else "Response saved · speech is paused while the app is in the background")
                if (appInForeground) speaker.speak(answer, activeSettings)
                recordDiagnostic("Chat response completed and saved locally")
            } catch (cancelled: CancellationException) {
                state = state.copy(busy = false, partialReply = "", avatarState = AvatarState.IDLE, status = "Response cancelled")
                throw cancelled
            } catch (error: Exception) {
                val message = error.message?.take(240) ?: "The AI provider request failed. Check Settings and retry."
                val latest = state.conversations.firstOrNull { it.id == conversation.id } ?: conversation
                val assistant = ChatMessage(UUID.randomUUID().toString(), "assistant", "I couldn’t get a response from the selected AI provider. $message\n\nRetry the request or check your provider settings. Offline demo mode works when no model/key is required.")
                updateConversation(latest.copy(updatedAt = assistant.createdAt, messages = latest.messages + assistant))
                state = state.copy(busy = false, partialReply = "", avatarState = AvatarState.ERROR, status = "AI Provider unavailable", error = message)
                recordDiagnostic("AI provider request failed")
            }
        }
    }

    fun cancelResponse() {
        responseJob?.cancel()
        responseJob = null
        state = state.copy(busy = false, partialReply = "", avatarState = AvatarState.IDLE, status = "Response cancelled")
    }

    fun stopSpeaking() {
        speaker.stop()
        state = state.copy(avatarState = AvatarState.IDLE, status = "Speech stopped")
    }

    fun setAppForeground(foreground: Boolean) {
        appInForeground = foreground
        if (!foreground && state.busy) cancelResponse()
        if (!foreground && state.avatarState == AvatarState.SPEAKING) {
            speaker.stop()
            state = state.copy(avatarState = AvatarState.IDLE, status = "Speech stopped while app is in the background")
        }
    }

    fun testConnection() {
        testSpecificProviderConnection(state.settings.providerType)
    }

    fun testSpecificProviderConnection(type: AiProviderType) {
        scope.launch {
            state = state.copy(status = "Testing ${type.displayName} connection…", connection = null)
            val tempSettings = state.settings.copy(providerType = type)
            val result = runCatching {
                AiProviderFactory.createAdapter(tempSettings).testConnection()
            }.getOrElse { err -> ConnectionResult(false, err.message ?: "Check ${type.displayName} settings.") }

            state = state.copy(connection = result, status = result.message)
            recordDiagnostic("${type.displayName} test: ${result.message}")
        }
    }

    fun saveConfiguration() {
        preferencesRepository.save(state.settings)
        state = state.copy(status = "Settings configuration saved successfully")
        recordDiagnostic("Configuration saved to local storage")
    }

    fun previewAvatarState(avatarState: AvatarState) {
        state = state.copy(avatarState = avatarState, status = "Previewing ${avatarState.label} animation")
    }

    fun testVoiceSpeech(text: String = "Hello! I am Nova, your 3D AI companion. All systems are fully operational.") {
        stopSpeaking()
        state = state.copy(avatarState = AvatarState.SPEAKING, status = "Speaking voice preview")
        speaker.speak(text, state.settings)
    }



    fun importCharacter(uri: Uri) {
        scope.launch {
            state = state.copy(characterStatus = "Importing character…", error = null)
            try {
                val file = withContext(Dispatchers.IO) { importCharacter(getApplication(), uri) }
                updateSettings { it.copy(characterPath = file.absolutePath) }
                state = state.copy(characterStatus = "Character file imported · loading GLB", status = "Local character ready", characterReloadVersion = state.characterReloadVersion + 1)
                recordDiagnostic("A local character asset was imported")
            } catch (error: Exception) {
                state = state.copy(characterStatus = "No valid character loaded", error = error.message ?: "Could not import the selected GLB.")
                recordDiagnostic("Character import failed")
            }
        }
    }

    fun onCharacterLoadResult(success: Boolean, message: String) {
        state = state.copy(characterStatus = message, error = if (success) null else message, avatarState = if (success) state.avatarState else AvatarState.ERROR)
    }

    fun removeCharacter() {
        state.settings.characterPath?.let { runCatching { File(it).delete() } }
        updateSettings { it.copy(characterPath = null) }
        state = state.copy(characterStatus = "Import a licensed rigged GLB character to begin")
    }

    fun resetSettings() {
        val selectedCharacter = state.settings.characterPath
        preferencesRepository.reset()
        val defaults = AvatarPreferences(characterPath = selectedCharacter)
        preferencesRepository.save(defaults)
        state = state.copy(settings = defaults, connection = null, status = "Offline demo · settings reset")
    }

    fun resetCamera() {
        state = state.copy(cameraResetVersion = state.cameraResetVersion + 1)
        updateSettings { it.copy(cameraZoom = 1f) }
    }

    private fun updateConversation(conversation: Conversation) {
        val items = state.conversations.filterNot { it.id == conversation.id } + conversation
        val sorted = items.sortedByDescending { it.updatedAt }
        conversationStore.save(sorted)
        state = state.copy(conversations = sorted, activeConversationId = if (state.activeConversationId.isBlank()) conversation.id else state.activeConversationId)
    }

    private fun newEmptyConversation() = Conversation(UUID.randomUUID().toString(), "New conversation", System.currentTimeMillis(), emptyList())

    private fun recordDiagnostic(message: String) {
        val line = "${System.currentTimeMillis()} · $message"
        state = state.copy(diagnosticEvents = (listOf(line) + state.diagnosticEvents).take(20))
    }

    override fun onCleared() {
        responseJob?.cancel()
        speaker.shutdown()
        scope.cancel()
        super.onCleared()
    }
}
