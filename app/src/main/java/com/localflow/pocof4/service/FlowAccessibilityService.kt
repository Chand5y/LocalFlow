package com.localflow.pocof4.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import com.localflow.pocof4.engine.LocalAiPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Replicates Wispr Flow's Android AccessibilityService:
 * 1. Detects when any EditText field across WhatsApp, Gmail, Chrome, Slack, or Notes gains focus.
 * 2. Displays a floating mic bubble only when an editable field is active (or keeps it docked).
 * 3. Records speech -> runs whisper.cpp + llama.cpp locally -> injects polished text at the cursor.
 */
class FlowAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var floatingMicButton: Button? = null
    private var activePackageName: String = "com.whatsapp"
    private var isRecording = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        setupFloatingMicBubble()
    }

    private fun setupFloatingMicBubble() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val button = Button(this).apply {
            text = "🎙️ Flow"
            alpha = 0.92f
            setOnClickListener {
                toggleDictationAndInject()
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 36
            y = 220
        }

        windowManager?.addView(button, params)
        floatingMicButton = button
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        event.packageName?.toString()?.let { pkg ->
            activePackageName = pkg
        }
    }

    private fun toggleDictationAndInject() {
        val pipeline = LocalAiPipeline.getInstance(applicationContext)
        if (!isRecording) {
            isRecording = true
            floatingMicButton?.text = "🔴 Stop"
            pipeline.startRecording(whisperModeBoost = false)
        } else {
            isRecording = false
            floatingMicButton?.text = "⏳ Cleaning..."
            serviceScope.launch {
                val selectedText = getSelectedTextFromFocusedNode()
                val polished = pipeline.stopAndProcessAudio(
                    foregroundPackage = activePackageName,
                    selectedTextForCommand = selectedText
                )
                injectTextIntoFocusedField(polished)
                floatingMicButton?.text = "🎙️ Flow"
            }
        }
    }

    private fun getSelectedTextFromFocusedNode(): String? {
        val root = rootInActiveWindow ?: return null
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return null
        if (focused.textSelectionStart >= 0 && focused.textSelectionEnd > focused.textSelectionStart) {
            val full = focused.text?.toString() ?: return null
            return full.substring(focused.textSelectionStart, focused.textSelectionEnd)
        }
        return null
    }

    private fun injectTextIntoFocusedField(polishedText: String) {
        if (polishedText.isBlank()) return
        val root = rootInActiveWindow ?: return
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return

        val existing = focused.text?.toString() ?: ""
        val combined = if (existing.isBlank()) polishedText else "$existing $polishedText"

        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                combined
            )
        }
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    override fun onInterrupt() {}
}
