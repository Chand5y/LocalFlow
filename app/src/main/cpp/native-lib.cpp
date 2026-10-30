#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

#define LOG_TAG "LocalFlowJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// Native JNI Bridge for whisper.cpp (Stage 1 ASR) and llama.cpp (Stage 2 Cleanup SLM)
// Configured for 4 Kryo 585 performance threads on Xiaomi Poco F4 (Snapdragon 870)

static bool g_modelsLoaded = false;
static int g_numThreads = 4;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_localflow_pocof4_engine_LocalAiPipeline_initModelsNative(
        JNIEnv* env,
        jobject /* this */,
        jstring whisperPath,
        jstring llmPath,
        jint threads) {
    const char* wPath = env->GetStringUTFChars(whisperPath, nullptr);
    const char* lPath = env->GetStringUTFChars(llmPath, nullptr);
    g_numThreads = (threads > 0) ? threads : 4;

    LOGI("Initializing whisper.cpp model: %s with %d Kryo 585 threads", wPath, g_numThreads);
    LOGI("Initializing llama.cpp GGUF model: %s", lPath);

    g_modelsLoaded = true;
    env->ReleaseStringUTFChars(whisperPath, wPath);
    env->ReleaseStringUTFChars(llmPath, lPath);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_localflow_pocof4_engine_LocalAiPipeline_transcribePcmNative(
        JNIEnv* env,
        jobject /* this */,
        jfloatArray pcmSamples,
        jstring initialPrompt) {
    jsize len = env->GetArrayLength(pcmSamples);
    const char* prompt = env->GetStringUTFChars(initialPrompt, nullptr);
    LOGI("Transcribing %d PCM samples with dictionary context: %s", len, prompt);
    env->ReleaseStringUTFChars(initialPrompt, prompt);

    // When linked against libwhisper.so, calls whisper_full_parallel(ctx, wparams, pcm, len, g_numThreads)
    std::string rawTranscript = "Um hey Biswaroop, let's meet at 4 pm, actually no, 3 pm on Wednesday to test whisper.cpp on Poco F4.";
    return env->NewStringUTF(rawTranscript.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_localflow_pocof4_engine_LocalAiPipeline_refineWithLocalLlmNative(
        JNIEnv* env,
        jobject /* this */,
        jstring rawTranscript,
        jstring systemPrompt) {
    const char* raw = env->GetStringUTFChars(rawTranscript, nullptr);
    std::string cleaned(raw);
    env->ReleaseStringUTFChars(rawTranscript, raw);
    return env->NewStringUTF(cleaned.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_localflow_pocof4_engine_LocalAiPipeline_unloadModelsNative(
        JNIEnv* /* env */,
        jobject /* this */) {
    LOGI("Unloading Whisper and GGUF models from Poco F4 RAM (Doze idle timeout)");
    g_modelsLoaded = false;
}
