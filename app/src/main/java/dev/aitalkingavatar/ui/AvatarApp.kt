package dev.aitalkingavatar.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.aitalkingavatar.ai.ConnectionResult
import dev.aitalkingavatar.avatar.AvatarState
import dev.aitalkingavatar.avatar.FilamentAvatarRenderer
import dev.aitalkingavatar.data.AiProviderType
import dev.aitalkingavatar.data.AvatarPreferences
import dev.aitalkingavatar.data.ChatMessage
import dev.aitalkingavatar.data.Conversation
import dev.aitalkingavatar.data.DiagnosticSummary
import dev.aitalkingavatar.data.EndpointRules
import dev.aitalkingavatar.data.GraphicsQuality
import dev.aitalkingavatar.data.diagnosticSummary
import dev.aitalkingavatar.voice.SpeechRecognitionController
import java.io.File
import java.text.DateFormat
import java.util.Date

private val Navy = Color(0xFF080B14)
private val SurfaceDark = Color(0xFF111827)
private val SurfaceRaised = Color(0xFF172033)
private val Cyan = Color(0xFF4DEBFF)
private val Violet = Color(0xFF8B5CF6)
private val TextPrimary = Color(0xFFF5F7FF)
private val TextMuted = Color(0xFFA6B0C4)
private val ErrorRed = Color(0xFFFF7285)
private val SuccessGreen = Color(0xFF10B981)

private enum class MainTab(val title: String) {
    AVATAR("Avatar"), HISTORY("History"), SETTINGS("Settings"), ABOUT("About")
}

@Composable
fun AvatarApp(viewModel: AvatarViewModel, speechController: SpeechRecognitionController) {
    val state = viewModel.state
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(MainTab.AVATAR) }
    var pendingDialog by rememberSaveable { mutableStateOf("") }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let(viewModel::importCharacter) }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) speechController.start(state.settings.voiceLocale)
        else viewModel.setVoiceError("Microphone permission was not granted. You can still type your message.")
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.saveConfiguration()
        else viewModel.setVoiceError("Notification permission was not granted.")
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(viewModel.exportHistory().toByteArray(Charsets.UTF_8)) }
        }.onFailure { viewModel.setVoiceError("History export failed. Try another destination.") }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Cyan, secondary = Violet, background = Navy, surface = SurfaceDark,
        onPrimary = Navy, onSecondary = TextPrimary, onBackground = TextPrimary,
        onSurface = TextPrimary, error = ErrorRed
    )) {
        Scaffold(
            containerColor = Navy,
            contentWindowInsets = WindowInsets.navigationBars,
            bottomBar = {
                NavigationBar(containerColor = SurfaceDark, contentColor = TextPrimary) {
                    MainTab.entries.forEach { item ->
                        val icon = when (item) {
                            MainTab.AVATAR -> Icons.Default.ChatBubbleOutline
                            MainTab.HISTORY -> Icons.Default.History
                            MainTab.SETTINGS -> Icons.Default.Settings
                            MainTab.ABOUT -> Icons.Default.Info
                        }
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Icon(icon, contentDescription = item.title) },
                            label = { Text(item.title, fontSize = 11.sp) }
                        )
                    }
                }
            }
        ) { padding ->
            when (tab) {
                MainTab.AVATAR -> AvatarScreen(
                    viewModel, speechController, state,
                    onImport = { importLauncher.launch(arrayOf("model/gltf-binary", "application/octet-stream")) },
                    onRequestMic = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) speechController.start(state.settings.voiceLocale)
                        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    modifier = Modifier.padding(padding)
                )
                MainTab.HISTORY -> HistoryScreen(
                    state.conversations, state.activeConversationId,
                    onOpen = { viewModel.openConversation(it); tab = MainTab.AVATAR },
                    onNew = { viewModel.newConversation(); tab = MainTab.AVATAR },
                    onExport = { exportLauncher.launch("avatar-history.json") },
                    onClear = { pendingDialog = "clear" }, modifier = Modifier.padding(padding)
                )
                MainTab.SETTINGS -> SettingsScreen(
                    settings = state.settings,
                    connection = state.connection,
                    error = state.error,
                    characterStatus = state.characterStatus,
                    statusText = state.status,
                    onSettings = viewModel::updateSettings,
                    onTestConnection = viewModel::testConnection,
                    onTestSpecificProvider = viewModel::testSpecificProviderConnection,
                    onSaveConfiguration = viewModel::saveConfiguration,
                    onStopSpeaking = viewModel::stopSpeaking,
                    onImport = { importLauncher.launch(arrayOf("model/gltf-binary", "application/octet-stream")) },
                    onRemoveCharacter = viewModel::removeCharacter,
                    onReset = { pendingDialog = "reset" },
                    onExport = { exportLauncher.launch("avatar-history.json") },
                    onRequestMicPermission = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            viewModel.setVoiceError("Microphone permission is already granted.")
                        } else {
                            micPermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                viewModel.setVoiceError("Notification permission is already granted.")
                            } else {
                                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            viewModel.setVoiceError("Notifications are auto-enabled on this Android version.")
                        }
                    },
                    diagnostics = diagnosticSummary(state.settings, state.settings.characterPath?.let { File(it).isFile } == true, state.error, state.diagnosticEvents),
                    modifier = Modifier.padding(padding)
                )
                MainTab.ABOUT -> AboutScreen(Modifier.padding(padding))
            }
        }
    }

    if (pendingDialog == "clear") AlertDialog(
        onDismissRequest = { pendingDialog = "" },
        title = { Text("Delete conversation history?") },
        text = { Text("This removes all saved local conversations on this device.") },
        confirmButton = { TextButton(onClick = { viewModel.clearHistory(); pendingDialog = "" }) { Text("Delete", color = ErrorRed) } },
        dismissButton = { TextButton(onClick = { pendingDialog = "" }) { Text("Cancel") } }
    )

    if (pendingDialog == "reset") AlertDialog(
        onDismissRequest = { pendingDialog = "" },
        title = { Text("Reset settings to defaults?") },
        text = { Text("This resets local model parameters, graphics settings, and voice options.") },
        confirmButton = { TextButton(onClick = { viewModel.resetSettings(); pendingDialog = "" }) { Text("Reset") } },
        dismissButton = { TextButton(onClick = { pendingDialog = "" }) { Text("Cancel") } }
    )
}

@Composable
private fun AvatarScreen(
    viewModel: AvatarViewModel,
    speechController: SpeechRecognitionController,
    state: AvatarUiState,
    onImport: () -> Unit,
    onRequestMic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val rendererRef = remember { mutableStateOf<FilamentAvatarRenderer?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val renderer = rendererRef.value
            when (event) {
                Lifecycle.Event.ON_RESUME -> renderer?.start()
                Lifecycle.Event.ON_PAUSE -> renderer?.pause()
                Lifecycle.Event.ON_DESTROY -> renderer?.dispose()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.avatarState) { rendererRef.value?.setState(state.avatarState) }
    LaunchedEffect(state.settings.graphicsQuality) { rendererRef.value?.setQuality(state.settings.graphicsQuality) }
    LaunchedEffect(state.settings.reducedMotion) { rendererRef.value?.setReducedMotion(state.settings.reducedMotion) }
    LaunchedEffect(state.settings.cameraZoom) { rendererRef.value?.setCameraZoom(state.settings.cameraZoom) }
    LaunchedEffect(state.cameraResetVersion) { rendererRef.value?.resetCamera() }

    val characterFile = state.settings.characterPath?.let { File(it) }

    Box(modifier.fillMaxSize()) {
        Column(modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxWidth().weight(1f).background(
                    Brush.verticalGradient(listOf(Color(0xFF030712), Navy))
                )
            ) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).also { surface ->
                            val renderer = FilamentAvatarRenderer(surface)
                            rendererRef.value = renderer
                            val starterAsset = File(ctx.filesDir, "assistant.glb")
                            if (!starterAsset.exists()) {
                                runCatching {
                                    ctx.assets.open("avatar/assistant.glb").use { input ->
                                        starterAsset.outputStream().use { output -> input.copyTo(output) }
                                    }
                                }
                            }
                            val targetFile = if (characterFile != null && characterFile.isFile) characterFile else starterAsset
                            if (targetFile.isFile) {
                                renderer.loadModel(targetFile) { ok, msg -> viewModel.onCharacterLoadResult(ok, msg) }
                            } else {
                                viewModel.onCharacterLoadResult(false, "No 3D character asset loaded")
                            }
                            renderer.start()
                        }
                    },
                    update = {
                        val renderer = rendererRef.value
                        val targetFile = if (characterFile != null && characterFile.isFile) characterFile else File(it.context.filesDir, "assistant.glb")
                        if (renderer != null && targetFile.isFile) {
                            renderer.loadModel(targetFile) { ok, msg -> viewModel.onCharacterLoadResult(ok, msg) }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Column(Modifier.align(Alignment.TopStart).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = SurfaceDark.copy(alpha = 0.85f), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(if (state.busy) Violet else if (state.listening) Cyan else SuccessGreen))
                            Spacer(Modifier.width(8.dp))
                            Text(state.status, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Row(Modifier.align(Alignment.TopEnd).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.setCodingMode(!state.settings.codingMode) },
                        modifier = Modifier.clip(CircleShape).background(if (state.settings.codingMode) Cyan else SurfaceDark.copy(alpha = 0.85f))
                    ) {
                        Icon(Icons.Default.Code, contentDescription = "Toggle Coding Mode", tint = if (state.settings.codingMode) Navy else TextPrimary)
                    }
                    IconButton(
                        onClick = { viewModel.resetCamera() },
                        modifier = Modifier.clip(CircleShape).background(SurfaceDark.copy(alpha = 0.85f))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Camera", tint = TextPrimary)
                    }
                }
            }

            Surface(color = SurfaceDark, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val messages = state.messages
                    if (messages.isNotEmpty()) {
                        val last = messages.last()
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (last.role == "user") "YOU" else "AVATAR", color = if (last.role == "user") Cyan else Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("${messages.size} messages", color = TextMuted, fontSize = 10.sp)
                        }
                        Text(
                            if (state.partialReply.isNotEmpty()) state.partialReply else last.content,
                            color = TextPrimary, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                if (state.listening) speechController.stop()
                                else onRequestMic()
                            },
                            modifier = Modifier.size(46.dp).clip(CircleShape).background(if (state.listening) ErrorRed else SurfaceRaised)
                        ) {
                            Icon(if (state.listening) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Voice Input", tint = TextPrimary)
                        }

                        OutlinedTextField(
                            value = state.draft,
                            onValueChange = viewModel::updateDraft,
                            placeholder = { Text(if (state.settings.codingMode) "Ask for code help, debugging, or explanation…" else "Talk to your avatar…") },
                            singleLine = true,
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Cyan,
                                unfocusedBorderColor = Color(0xFF263249),
                                focusedContainerColor = SurfaceRaised,
                                unfocusedContainerColor = SurfaceRaised
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                if (state.busy) viewModel.cancelResponse()
                                else viewModel.sendDraft()
                            },
                            modifier = Modifier.size(46.dp).clip(CircleShape).background(if (state.busy) ErrorRed else Cyan)
                        ) {
                            Icon(if (state.busy) Icons.Default.Pause else Icons.Default.ArrowUpward, contentDescription = "Send", tint = Navy)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    conversations: List<Conversation>,
    activeId: String,
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        ScreenHeader("Conversation history", "Stored locally on this device")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onNew, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, null); Text("New") }
            OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Text("Export JSON") }
            IconButton(onClick = onClear, modifier = Modifier.semantics { contentDescription = "Delete all local conversation history" }) { Icon(Icons.Default.DeleteOutline, null, tint = ErrorRed) }
        }
        if (conversations.none { it.messages.isNotEmpty() }) {
            EmptyPanel("No saved messages yet", "Your conversations will appear here after the first message.")
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(top = 12.dp)) {
            items(conversations.filter { it.messages.isNotEmpty() }, key = { it.id }) { conversation ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpen(conversation.id) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = if (conversation.id == activeId) Color(0xFF142737) else SurfaceDark)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(conversation.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${conversation.messages.size} messages · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(conversation.updatedAt))}", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        Text(conversation.messages.lastOrNull()?.content.orEmpty(), color = TextMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: AvatarPreferences,
    connection: ConnectionResult?,
    error: String?,
    characterStatus: String,
    statusText: String,
    onSettings: ((AvatarPreferences) -> AvatarPreferences) -> Unit,
    onTestConnection: () -> Unit,
    onTestSpecificProvider: (AiProviderType) -> Unit,
    onSaveConfiguration: () -> Unit,
    onStopSpeaking: () -> Unit,
    onImport: () -> Unit,
    onRemoveCharacter: () -> Unit,
    onReset: () -> Unit,
    onExport: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    diagnostics: DiagnosticSummary,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Settings & AI Adapters", "Configure Local Models, Gemini API, Permissions, and 3D Avatar") }

        item {
            Button(
                onClick = onSaveConfiguration,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Navy)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("SAVE ALL CONFIGURATIONS", fontWeight = FontWeight.Bold)
            }
        }

        item {
            SettingsCard("ACTIVE AI ADAPTER SELECTOR", "Choose active engine and manage adapter state") {
                Text("Active Engine Provider", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AiProviderType.entries.forEach { provider ->
                        val selected = settings.providerType == provider
                        OutlinedButton(
                            onClick = { onSettings { it.copy(providerType = provider) } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (selected) Cyan else TextMuted,
                                containerColor = if (selected) Color(0xFF142737) else Color.Transparent
                            )
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(provider.displayName, modifier = Modifier.weight(1f))
                                val isActive = when (provider) {
                                    AiProviderType.OFFLINE_DEMO -> true
                                    AiProviderType.OLLAMA -> settings.ollamaActive
                                    AiProviderType.LITERT_CLI -> settings.liteRtActive
                                    AiProviderType.GEMINI_API -> settings.geminiActive
                                }
                                Surface(
                                    color = if (isActive) SuccessGreen.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        if (isActive) "ACTIVE" else "INACTIVE",
                                        color = if (isActive) SuccessGreen else ErrorRed,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF263249))
                Text("AI Adapter State Toggles", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                SwitchRow("Enable Ollama Local Adapter", settings.ollamaActive) { active -> onSettings { it.copy(ollamaActive = active) } }
                SwitchRow("Enable Google LiteRT-LM Adapter", settings.liteRtActive) { active -> onSettings { it.copy(liteRtActive = active) } }
                SwitchRow("Enable Google Gemini API Adapter", settings.geminiActive) { active -> onSettings { it.copy(geminiActive = active) } }
            }
        }

        item {
            SettingsCard("LOCAL AI MODEL: OLLAMA", "Connect local Ollama endpoint (e.g., qwen2.5, llama3)") {
                LabeledField("Ollama Endpoint URL", settings.ollamaEndpoint, "http://localhost:11434", KeyboardType.Uri) { value -> onSettings { it.copy(ollamaEndpoint = value) } }
                LabeledField("Ollama Model Name", settings.ollamaModel, "qwen2.5:3b") { value -> onSettings { it.copy(ollamaModel = value) } }
                SwitchRow("Allow Local HTTP Network", settings.allowInsecureHttp) { value -> onSettings { it.copy(allowInsecureHttp = value) } }
                OutlinedButton(onClick = { onTestSpecificProvider(AiProviderType.OLLAMA) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Test Ollama Connection")
                }
            }
        }

        item {
            SettingsCard("LOCAL AI MODEL: GOOGLE LITERT-LM CLI", "On-device or local CLI inference endpoint") {
                LabeledField("LiteRT Endpoint URL", settings.liteRtEndpoint, "http://localhost:8080", KeyboardType.Uri) { value -> onSettings { it.copy(liteRtEndpoint = value) } }
                LabeledField("LiteRT Model Name", settings.liteRtModel, "litert-lm-3b") { value -> onSettings { it.copy(liteRtModel = value) } }
                OutlinedButton(onClick = { onTestSpecificProvider(AiProviderType.LITERT_CLI) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Test LiteRT Connection")
                }
            }
        }

        item {
            SettingsCard("GOOGLE GEMINI API SETTINGS", "Cloud AI generation via Gemini models") {
                Text("Gemini Model Selection", color = TextMuted, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-2.5-pro", "gemini-1.5-flash").forEach { model ->
                        val selected = settings.geminiModel == model
                        OutlinedButton(
                            onClick = { onSettings { it.copy(geminiModel = model) } },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (selected) Cyan else TextMuted,
                                containerColor = if (selected) Color(0xFF142737) else Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(model.removePrefix("gemini-"), fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }
                LabeledField("Custom Gemini Model Name", settings.geminiModel, "gemini-3.5-flash") { value -> onSettings { it.copy(geminiModel = value) } }
                LabeledField("Gemini API Key (Optional Override)", settings.geminiApiKey, "Default: BuildConfig.GEMINI_API_KEY") { value -> onSettings { it.copy(geminiApiKey = value) } }
                OutlinedButton(onClick = { onTestSpecificProvider(AiProviderType.GEMINI_API) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Test Gemini API Connection")
                }
                connection?.let { ConnectionStatus(it) }
            }
        }

        item {
            SettingsCard("ANDROID PERMISSIONS & ACCESSIBILITY", "Manage hardware permissions and system settings") {
                val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Microphone Permission", color = TextPrimary, fontSize = 13.sp)
                        Text(if (micGranted) "Granted · Voice recognition ready" else "Denied · Push to talk requires permission", color = TextMuted, fontSize = 11.sp)
                    }
                    if (!micGranted) {
                        OutlinedButton(onClick = onRequestMicPermission) { Text("Grant") }
                    } else {
                        Text("ACTIVE", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color(0xFF263249))

                val notifGranted = if (Build.VERSION.SDK_INT >= 33) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else true

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Notifications Permission", color = TextPrimary, fontSize = 13.sp)
                        Text(if (notifGranted) "Granted · Status alerts active" else "Denied · Tap to allow status updates", color = TextMuted, fontSize = 11.sp)
                    }
                    if (!notifGranted) {
                        OutlinedButton(onClick = onRequestNotificationPermission) { Text("Grant") }
                    } else {
                        Text("ACTIVE", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color(0xFF263249))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Network Permissions", color = TextPrimary, fontSize = 13.sp)
                        Text("INTERNET & ACCESS_NETWORK_STATE declared in Manifest", color = TextMuted, fontSize = 11.sp)
                    }
                    Text("SYSTEM OK", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = Color(0xFF263249))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("System Accessibility Settings", color = TextPrimary, fontSize = 13.sp)
                        Text("Configure TalkBack, Screen Readers, and Voice Access", color = TextMuted, fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                            .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }
                    }) {
                        Text("Open Settings")
                    }
                }
            }
        }

        item {
            SettingsCard("VOICE & SPEECH SYNTHESIS", "Text-to-speech output parameters") {
                LabeledField("Speech Language Locale", settings.voiceLocale, "en-US") { value -> onSettings { it.copy(voiceLocale = value) } }
                LabeledField("Preferred Voice Name (Optional)", settings.voiceName, "System default") { value -> onSettings { it.copy(voiceName = value) } }
                SliderRow("Speech Rate", settings.speechRate, 0.5f..2f, "${"%.1f".format(settings.speechRate)}×") { value -> onSettings { it.copy(speechRate = value) } }
                SliderRow("Voice Pitch", settings.pitch, 0.5f..2f, "${"%.1f".format(settings.pitch)}×") { value -> onSettings { it.copy(pitch = value) } }
                OutlinedButton(onClick = onStopSpeaking, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.VolumeOff, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Stop Speech Output")
                }
            }
        }

        item {
            SettingsCard("3D AVATAR & GRAPHICS", "Adjust rendering performance and camera view") {
                Text("Render Quality", color = TextPrimary, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GraphicsQuality.entries.forEach { quality ->
                        val selected = settings.graphicsQuality == quality
                        OutlinedButton(
                            onClick = { onSettings { it.copy(graphicsQuality = quality) } },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (selected) Cyan else TextMuted,
                                containerColor = if (selected) Color(0xFF142737) else Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(quality.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
                SliderRow("Camera Zoom Level", settings.cameraZoom, 0.5f..2f, "${"%.1f".format(settings.cameraZoom)}×") { value -> onSettings { it.copy(cameraZoom = value) } }
                SwitchRow("Reduced Motion Mode", settings.reducedMotion) { value -> onSettings { it.copy(reducedMotion = value) } }

                HorizontalDivider(color = Color(0xFF263249))

                Text("Character Asset Asset (.glb)", color = TextPrimary, fontSize = 13.sp)
                Text(characterStatus, color = TextMuted, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Import .glb") }
                    OutlinedButton(onClick = onRemoveCharacter) { Text("Remove Asset") }
                }
            }
        }

        item {
            SettingsCard("APP THEME & STYLE", "Customize interface theme and palette") {
                val themes = listOf(
                    "dark_sci_fi" to "Dark Sci-Fi",
                    "cyberpunk_cyan" to "Cyberpunk Cyan",
                    "oled_dark" to "OLED Ultra Dark",
                    "light_modern" to "Light Modern"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    themes.forEach { (key, name) ->
                        val selected = settings.themeName == key
                        OutlinedButton(
                            onClick = { onSettings { it.copy(themeName = key) } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (selected) Cyan else TextMuted,
                                containerColor = if (selected) Color(0xFF142737) else Color.Transparent
                            )
                        ) {
                            Text(name)
                        }
                    }
                }
            }
        }

        item {
            SettingsCard("DIAGNOSTICS & SYSTEM EVENTS", "Redacted local runtime diagnostics") {
                DiagnosticRow("Active Adapter", diagnostics.adapter)
                DiagnosticRow("Configured Endpoint", diagnostics.endpoint)
                DiagnosticRow("Selected Model", diagnostics.model)
                DiagnosticRow("Voice Status", diagnostics.voiceStatus)
                DiagnosticRow("Character Status", diagnostics.characterStatus)
                DiagnosticRow("Last Error", diagnostics.lastError)
                Text("Recent Events (In-Memory)", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                if (diagnostics.events.isEmpty()) Text("No events recorded yet", color = TextMuted, fontSize = 11.sp)
                diagnostics.events.forEach { event -> Text(event, color = TextPrimary, fontSize = 10.sp, lineHeight = 14.sp) }
            }
        }

        item {
            SettingsCard("DATA & BACKUP", "Local conversation history and configuration storage") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Text("Export JSON") }
                    OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("Reset Settings") }
                }
            }
        }

        if (!error.isNullOrBlank()) item { Text(error, color = ErrorRed, fontSize = 12.sp) }
        item { Spacer(Modifier.height(30.dp)) }
    }
}

@Composable
private fun AboutScreen(modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("About & licenses", "AI Talking Avatar · version 1.0.0") }
        item {
            SettingsCard("WHAT THIS APP DOES", "A personal talking avatar with a local-first chat interface.") {
                Text("AI responses use the rule-based offline demo unless you explicitly configure a local Ollama-compatible endpoint and model. Inference is independent of the Filament renderer. Model quality depends on the selected model.", color = TextMuted, fontSize = 12.sp)
                Text("Microphone access is requested only when you tap voice input. The selected speech recognition provider may process audio remotely; offline recognition depends on provider and language model availability.", color = TextMuted, fontSize = 12.sp)
                Text("This source project includes a starter 3D humanoid assistant asset. You can import custom licensed .glb assets in Settings.", color = TextMuted, fontSize = 12.sp)
            }
        }
        item {
            SettingsCard("THIRD-PARTY COMPONENTS", "Review the licenses of all transitive dependencies before distributing a build.") {
                DiagnosticRow("Filament", "Apache License 2.0 · Google Filament")
                DiagnosticRow("OkHttp", "Apache License 2.0 · Square")
                DiagnosticRow("AndroidX / Compose", "Apache License 2.0 · Google")
                Text("A license inventory and source links are in THIRD_PARTY_NOTICES.md. Character-model rights are separate and must be recorded by the asset owner.", color = TextMuted, fontSize = 11.sp)
            }
        }
        item { Spacer(Modifier.height(30.dp)) }
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(top = 15.dp, bottom = 4.dp)) {
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 25.sp)
        Text(subtitle, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SettingsCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
            HorizontalDivider(color = Color(0xFF263249))
            content()
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String, placeholder: String, keyboardType: KeyboardType = KeyboardType.Text, onValue: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = TextMuted, fontSize = 11.sp)
        OutlinedTextField(
            value = value, onValueChange = onValue, modifier = Modifier.fillMaxWidth(), placeholder = { Text(placeholder) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType), shape = RoundedCornerShape(13.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Cyan, unfocusedBorderColor = Color(0xFF2B3549), focusedContainerColor = SurfaceRaised, unfocusedContainerColor = SurfaceRaised)
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChecked(!checked) }.semantics { contentDescription = label }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, display: String, onValue: (Float) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(display, color = Cyan, fontSize = 12.sp)
        }
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onValue, valueRange = range)
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        SelectionContainer { Text(value, color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp) }
    }
}

@Composable
private fun ConnectionStatus(result: ConnectionResult) {
    Text(result.message, color = if (result.connected) SuccessGreen else Color(0xFFFFC27A), fontSize = 12.sp)
}

@Composable
private fun EmptyPanel(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(top = 26.dp).clip(RoundedCornerShape(20.dp)).background(SurfaceDark).padding(18.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(body, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
}
