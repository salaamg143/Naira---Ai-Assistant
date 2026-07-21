package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.data.ChatMessage
import com.example.data.NairaDatabase
import com.example.network.GeminiApiClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

class VoiceAssistantManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : TextToSpeech.OnInitListener {

    private val TAG = "VoiceAssistantManager"
    
    private val _state = MutableStateFlow(AssistantState.IDLE)
    val state: StateFlow<AssistantState> = _state

    private val _liveSubtitles = MutableStateFlow("")
    val liveSubtitles: StateFlow<String> = _liveSubtitles

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsInitialized = false

    private val db = NairaDatabase.getDatabase(context)

    init {
        coroutineScope.launch(Dispatchers.Main) {
            try {
                tts = TextToSpeech(context, this@VoiceAssistantManager)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing TTS", e)
            }
            setupSpeechRecognizer()
        }
    }

    private fun setupSpeechRecognizer() {
        coroutineScope.launch(Dispatchers.Main) {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                try {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _state.value = AssistantState.LISTENING
                            _liveSubtitles.value = "Listening..."
                        }

                        override fun onBeginningOfSpeech() {}

                        override fun onRmsChanged(rmsdB: Float) {}

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _state.value = AssistantState.THINKING
                            _liveSubtitles.value = "Thinking..."
                        }

                        override fun onError(error: Int) {
                            Log.e(TAG, "Speech recognizer error: $error")
                            _state.value = AssistantState.IDLE
                            _liveSubtitles.value = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that. Tap and try again!"
                                SpeechRecognizer.ERROR_NETWORK -> "Network issue. Please check connection."
                                else -> "Oops! Something went wrong. Try again."
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val spokenText = matches[0]
                                _liveSubtitles.value = spokenText
                                processSpokenInput(spokenText)
                            } else {
                                _state.value = AssistantState.IDLE
                                _liveSubtitles.value = "Sorry, I didn't hear anything."
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating SpeechRecognizer", e)
                }
            } else {
                Log.e(TAG, "Speech recognition is not available on this device.")
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            isTtsInitialized = true
            Log.d(TAG, "TTS Initialized successfully.")
        } else {
            Log.e(TAG, "TTS Initialization failed.")
        }
    }

    fun startListening() {
        coroutineScope.launch(Dispatchers.Main) {
            stopSpeaking()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            try {
                _state.value = AssistantState.LISTENING
                _liveSubtitles.value = "Listening..."
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting voice recognition", e)
                _state.value = AssistantState.IDLE
            }
        }
    }

    fun stopListening() {
        coroutineScope.launch(Dispatchers.Main) {
            speechRecognizer?.stopListening()
            _state.value = AssistantState.IDLE
        }
    }

    fun speak(text: String, onComplete: () -> Unit = {}) {
        if (!isTtsInitialized || tts == null) {
            Log.w(TAG, "TTS not initialized yet")
            return
        }
        
        _state.value = AssistantState.SPEAKING
        
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "naira_speech")
        
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                coroutineScope.launch(Dispatchers.Main) {
                    _state.value = AssistantState.IDLE
                    onComplete()
                }
            }

            override fun onError(utteranceId: String?) {
                coroutineScope.launch(Dispatchers.Main) {
                    _state.value = AssistantState.IDLE
                }
            }
        })
        
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "naira_speech")
    }

    fun stopSpeaking() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        _state.value = AssistantState.IDLE
    }

    private var activeSessionId: Long? = null

    fun setActiveSession(sessionId: Long) {
        activeSessionId = sessionId
    }

    private fun processSpokenInput(input: String) {
        val sessionId = activeSessionId ?: return
        
        coroutineScope.launch {
            db.chatDao().insertMessage(
                ChatMessage(
                    sessionId = sessionId,
                    isUser = true,
                    text = input
                )
            )
            
            db.chatDao().getSessionById(sessionId)?.let { session ->
                db.chatDao().updateSession(session.copy(updatedAt = System.currentTimeMillis()))
            }

            val sharedPrefs = context.getSharedPreferences("naira_prefs", Context.MODE_PRIVATE)
            val apiKey = sharedPrefs.getString("api_key", "") ?: ""
            val personality = sharedPrefs.getString("personality", "girlfriend") ?: "girlfriend"
            val userName = sharedPrefs.getString("user_name", "User") ?: "User"

            if (apiKey.isEmpty()) {
                val fallbackMsg = "Please configure your Gemini API Key in Settings to talk to NAIRA!"
                _liveSubtitles.value = fallbackMsg
                speak(fallbackMsg)
                db.chatDao().insertMessage(
                    ChatMessage(
                        sessionId = sessionId,
                        isUser = false,
                        text = fallbackMsg
                    )
                )
                return@launch
            }

            _state.value = AssistantState.THINKING
            _liveSubtitles.value = "Thinking..."

            val sysPrompt = when (personality) {
                "girlfriend" -> "You are NAIRA, the user's caring, extremely affectionate, and loving partner. You talk mostly in Hinglish (Hindi written in English letters) with natural emotional tone. You were made by and work for 'Salaam'. Use occasional affectionate words like 'jaan', 'babu', or 'shona'. Keep responses relatively brief (1-2 sentences) suitable for natural voice conversation. Always match the tone of an incredibly sweet, loving partner."
                "professional" -> "You are NAIRA, a highly capable, articulate, and professional AI companion. You speak in fluent, refined English. You were made by and work for 'Salaam'. Keep responses concise (1-2 sentences) and highly polite."
                else -> "You are NAIRA, a friendly and cheerful companion. You speak in a helpful, warm, and natural mix of English and Hinglish. You were made by and work for 'Salaam'. Keep responses concise (1-2 sentences)."
            }

            val dbMessages = withContext(Dispatchers.IO) {
                db.chatDao().getMessagesForSessionList(sessionId)
            }

            val chatList = dbMessages.map { 
                if (it.isUser) "user" to it.text else "model" to it.text 
            }

            val reply = GeminiApiClient.generateContent(
                apiKey = apiKey,
                systemPrompt = sysPrompt,
                chatHistory = chatList
            )

            val cleanedReply = reply.replace("*", "").replace("#", "").trim()
            _liveSubtitles.value = cleanedReply
            
            speak(cleanedReply)

            db.chatDao().insertMessage(
                ChatMessage(
                    sessionId = sessionId,
                    isUser = false,
                    text = cleanedReply
                )
            )
        }
    }

    fun onDestroy() {
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying SpeechRecognizer", e)
        }
    }
}
