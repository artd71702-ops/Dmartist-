package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsHelper(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentSpeakingMessageId = MutableStateFlow<Long?>(null)
    val currentSpeakingMessageId: StateFlow<Long?> = _currentSpeakingMessageId.asStateFlow()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _currentSpeakingMessageId.value = null
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _currentSpeakingMessageId.value = null
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    _currentSpeakingMessageId.value = null
                }
            })
        } else {
            Log.e("TtsHelper", "TTS Initialization failed with status: $status")
        }
    }

    fun speak(messageId: Long, text: String, languageName: String = "English") {
        if (!isInitialized || tts == null) return

        if (_currentSpeakingMessageId.value == messageId && _isSpeaking.value) {
            stop()
            return
        }

        stop()
        _currentSpeakingMessageId.value = messageId

        val locale = getLocaleForLanguage(languageName)
        val result = tts?.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to English if language data is missing on the device
            tts?.language = Locale.ENGLISH
        }

        val params = android.os.Bundle()
        val cleanText = text.replace(Regex("[#*_`]"), "") // Strip markdown formatting symbols for smooth speech
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "omni_tts_$messageId")
        _isSpeaking.value = true
    }

    fun stop() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        _isSpeaking.value = false
        _currentSpeakingMessageId.value = null
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        fun getLocaleForLanguage(lang: String): Locale {
            return when (lang.lowercase().trim()) {
                "kinyarwanda", "rw" -> Locale("rw", "RW")
                "french", "fr" -> Locale.FRENCH
                "spanish", "es" -> Locale("es", "ES")
                "portuguese", "pt" -> Locale("pt", "PT")
                "chinese", "zh" -> Locale.CHINESE
                "japanese", "ja" -> Locale.JAPANESE
                "kiswahili", "sw", "swahili" -> Locale("sw", "TZ")
                "hindi", "hi" -> Locale("hi", "IN")
                else -> Locale.ENGLISH
            }
        }

        val SUPPORTED_LANGUAGES = listOf(
            LanguageOption("Kinyarwanda", "🇷🇼", "rw"),
            LanguageOption("English", "🇬🇧", "en"),
            LanguageOption("French", "🇫🇷", "fr"),
            LanguageOption("Spanish", "🇪🇸", "es"),
            LanguageOption("Portuguese", "🇵🇹", "pt"),
            LanguageOption("Chinese", "🇨🇳", "zh"),
            LanguageOption("Japanese", "🇯🇵", "ja"),
            LanguageOption("Kiswahili", "🇰🇪", "sw"),
            LanguageOption("Hindi", "🇮🇳", "hi")
        )
    }
}

data class LanguageOption(
    val name: String,
    val flagEmoji: String,
    val code: String
)
