package dev.aitalkingavatar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dev.aitalkingavatar.ui.AvatarApp
import dev.aitalkingavatar.ui.AvatarViewModel
import dev.aitalkingavatar.voice.SpeechRecognitionController

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<AvatarViewModel>()
    private lateinit var speechController: SpeechRecognitionController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speechController = SpeechRecognitionController(
            context = this,
            onPartial = viewModel::updateDraft,
            onResult = viewModel::updateDraft,
            onError = viewModel::setVoiceError,
            onListening = viewModel::setListening
        )
        setContent { AvatarApp(viewModel, speechController) }
    }

    override fun onPause() {
        speechController.stop(cancel = true)
        viewModel.setAppForeground(false)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.setAppForeground(true)
    }

    override fun onDestroy() {
        speechController.destroy()
        super.onDestroy()
    }
}
