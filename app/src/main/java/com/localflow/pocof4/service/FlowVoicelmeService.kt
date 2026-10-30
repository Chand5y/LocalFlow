package com.localflow.pocof4.service

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import com.localflow.pocof4.engine.LocalAiPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Companion Voice Keyboard (InputMethodService) for apps where the user prefers
 * switching keyboards instead of using the floating Accessibility overlay.
 */
class FlowVoiceImeService : InputMethodService() {

    private val imeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isRecording = false
    private var whisperMode = false

    override fun onCreateInputView(): View {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 24, 32, 24)
        }

        val micBtn = Button(this).apply {
            text = "🎙️ Hold or Tap to Speak (Offline)"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val whisperBtn = Button(this).apply {
            text = "🤫 Whisper: OFF"
            setOnClickListener {
                whisperMode = !whisperMode
                text = if (whisperMode) "🤫 Whisper: ON (2.5x)" else "🤫 Whisper: OFF"
            }
        }

        micBtn.setOnClickListener {
            val pipeline = LocalAiPipeline.getInstance(applicationContext)
            if (!isRecording) {
                isRecording = true
                micBtn.text = "🔴 Listening... Tap to Insert"
                pipeline.startRecording(whisperModeBoost = whisperMode)
            } else {
                isRecording = false
                micBtn.text = "⏳ Refining with Local AI..."
                imeScope.launch {
                    val packageName = currentInputEditorInfo?.packageName ?: "com.whatsapp"
                    val polished = pipeline.stopAndProcessAudio(packageName, null)
                    currentInputConnection?.commitText("$polished ", 1)
                    micBtn.text = "🎙️ Hold or Tap to Speak (Offline)"
                }
            }
        }

        layout.addView(micBtn)
        layout.addView(whisperBtn)
        return layout
    }
}
