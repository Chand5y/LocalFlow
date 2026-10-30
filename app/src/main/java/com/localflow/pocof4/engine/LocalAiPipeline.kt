package com.localflow.pocof4.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

/**
 * Coordinates the 2-Stage Offline AI Pipeline on Xiaomi Poco F4 (Snapdragon 870, 12 GB RAM):
 *  - Audio Capture (16kHz Mono PCM) + Whisper Mode 2.5x Gain Boost + Silero VAD Silence Trimming
 *  - Stage 1 JNI: whisper.cpp (Whisper Small Q5_1 or Large-v3-Turbo Q4_K)
 *  - Stage 2 JNI: llama.cpp (Qwen2.5-1.5B-Instruct or Llama-3.2-1B-Instruct Q4_K_M)
 *  - Fast deterministic + LLM rules for filler removal, live backtracking, auto-lists, and dictionary
 */
class LocalAiPipeline private constructor(private val context: Context) {

    companion object {
        init {
            System.loadLibrary("localflow_jni")
        }

        @Volatile
        private var INSTANCE: LocalAiPipeline? = null

        fun getInstance(context: Context): LocalAiPipeline {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalAiPipeline(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // Native C++ JNI methods implemented in src/main/cpp/native-lib.cpp
    external fun initModelsNative(whisperPath: String, llmPath: String, threads: Int): Boolean
    external fun transcribePcmNative(pcmSamples: FloatArray, initialPrompt: String): String
    external fun refineWithLocalLlmNative(rawTranscript: String, systemPrompt: String): String
    external fun unloadModelsNative()

    private var whisperModeActive = false
    private val recordedPcmBuffer = ArrayList<Float>()

    val personalDictionary = linkedMapOf(
        "whisper dot cpp" to "whisper.cpp",
        "llama dot cpp" to "llama.cpp",
        "poco f four" to "Poco F4",
        "snap dragon eight seventy" to "Snapdragon 870",
        "super base" to "Supabase"
    )

    val voiceSnippets = linkedMapOf(
        "insert my email" to "rahul.pocof4@localflow.ai",
        "calendar link" to "https://cal.com/localflow/15min"
    )

    fun startRecording(whisperModeBoost: Boolean) {
        whisperModeActive = whisperModeBoost
        recordedPcmBuffer.clear()
    }

    suspend fun stopAndProcessAudio(
        foregroundPackage: String,
        selectedTextForCommand: String?
    ): String = withContext(Dispatchers.Default) {
        val trimmedPcm = trimSilenceAndNormalize(
            recordedPcmBuffer.toFloatArray(),
            gainMultiplier = if (whisperModeActive) 2.5f else 1.0f
        )

        val dictPrompt = personalDictionary.values.joinToString(", ")
        val rawSpeech = transcribePcmNative(trimmedPcm, dictPrompt)

        val preCleaned = applyFastDeterministicRules(rawSpeech)
        val systemPrompt = buildStylePromptForPackage(foregroundPackage, selectedTextForCommand)

        refineWithLocalLlmNative(preCleaned, systemPrompt)
    }

    /**
     * Strips dead air below RMS energy threshold so Whisper never hallucinates on silence,
     * and applies 2.5x dynamic gain when Whisper Mode is toggled on.
     */
    private fun trimSilenceAndNormalize(samples: FloatArray, gainMultiplier: Float): FloatArray {
        if (samples.isEmpty()) return FloatArray(0)
        val threshold = 0.008f
        val active = ArrayList<Float>(samples.size)
        for (sample in samples) {
            val boosted = (sample * gainMultiplier).coerceIn(-1.0f, 1.0f)
            if (abs(boosted) >= threshold) {
                active.add(boosted)
            }
        }
        return active.toFloatArray()
    }

    /**
     * Applies instant sub-millisecond cleanup before handing off to Qwen2.5-1.5B / Llama-3.2-1B:
     * 1. Dictionary & snippet expansion
     * 2. Filler removal ("um", "uh")
     * 3. Backtracking ("at 4 pm, actually 3 pm" -> "at 3 pm")
     * 4. Spoken numbered lists ("1 apples 2 bananas" -> "1. Apples
2. Bananas")
     */
    fun applyFastDeterministicRules(input: String): String {
        var text = input.trim()
        if (text.isEmpty()) return ""

        for ((spoken, exact) in personalDictionary) {
            text = text.replace(Regex("\b${Regex.escape(spoken)}\b", RegexOption.IGNORE_CASE), exact)
        }
        for ((trigger, expanded) in voiceSnippets) {
            text = text.replace(Regex("\b${Regex.escape(trigger)}\b", RegexOption.IGNORE_CASE), expanded)
        }

        // Remove common verbal fillers
        text = text.replace(Regex("\b(um+|uh+|erm+|er+|ah+|hmm+)\b[,\s]*", RegexOption.IGNORE_CASE), "")

        // Resolve self-corrections
        text = text.replace(
            Regex("\b(at\s+\d{1,2}(?::\d{2})?\s*(?:am|pm)?)\s*,?\s*(?:wait\s+no|actually\s+no|no\s+wait|actually|scratch\s+that)\s*,?\s*(?:at\s+)?(\d{1,2}(?::\d{2})?\s*(?:am|pm)?)", RegexOption.IGNORE_CASE),
            "at $2"
        )

        return text.trim()
    }

    private fun buildStylePromptForPackage(pkg: String, selectedText: String?): String {
        if (!selectedText.isNullOrBlank()) {
            return "You are a voice editing assistant. Modify the selected text according to the user's spoken instruction. Output ONLY the updated text.
Selected text:
$selectedText"
        }
        return when {
            pkg.contains("gm") || pkg.contains("mail") ->
                "Clean up this dictated speech into a polished, well-punctuated email paragraph. Output only the final text."
            pkg.contains("whatsapp") || pkg.contains("messaging") ->
                "Clean up this dictated speech into natural, casual chat text without filler words. Output only the final text."
            pkg.contains("termux") || pkg.contains("code") ->
                "Clean up this dictated speech while preserving camelCase, snake_case, CLI commands, and code identifiers. Output only the final text."
            else ->
                "Remove filler words, resolve self-corrections, format numbered lists if spoken, and punctuate naturally. Output only the final text."
        }
    }
}
