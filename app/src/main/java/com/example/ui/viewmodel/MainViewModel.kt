package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.SettingsDataStore
import com.example.data.db.*
import com.example.data.repository.AiRepository
import com.example.data.repository.AiResponseResult
import com.example.ui.theme.ThemeConfig
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemePreset
import com.example.util.FileAttachmentData
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val conversationDao = db.conversationDao()
    private val messageDao = db.messageDao()
    private val recentModelDao = db.recentModelDao()
    private val repository = AiRepository()
    private val settingsDataStore = SettingsDataStore(application)

    // DataStore Preferences
    val systemPrompt: StateFlow<String> = settingsDataStore.systemPromptFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val isDarkMode: StateFlow<Boolean> = settingsDataStore.isDarkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val useDynamicColor: StateFlow<Boolean> = settingsDataStore.useDynamicColorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val temperature: StateFlow<Float> = settingsDataStore.temperatureFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.7f)

    val maxTokens: StateFlow<Int> = settingsDataStore.maxTokensFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2048)

    val notificationsEnabled: StateFlow<Boolean> = settingsDataStore.notificationsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationSoundEnabled: StateFlow<Boolean> = settingsDataStore.notificationSoundEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationVibrateEnabled: StateFlow<Boolean> = settingsDataStore.notificationVibrateEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoScrollEnabled: StateFlow<Boolean> = settingsDataStore.autoScrollEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val onlineSearchEnabled: StateFlow<Boolean> = settingsDataStore.onlineSearchEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Search Query State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Conversations Flow with search support
    val conversations: StateFlow<List<ConversationEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                conversationDao.getAllConversations()
            } else {
                conversationDao.searchConversations(query.trim())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current Conversation ID State
    private val _currentConversationId = MutableStateFlow<Long?>(null)
    val currentConversationId: StateFlow<Long?> = _currentConversationId.asStateFlow()

    // Messages Flow for current conversation
    val messages: StateFlow<List<MessageEntity>> = _currentConversationId.flatMapLatest { id ->
        if (id != null) {
            messageDao.getMessagesForConversation(id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Messages Flow for Dashboard analytics
    val allMessages: StateFlow<List<MessageEntity>> = messageDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recent Models Flow
    val recentModels: StateFlow<List<RecentModelEntity>> = recentModelDao.getRecentModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultModelList())

    // Active Model State
    private val _activeModel = MutableStateFlow(defaultModelList().first())
    val activeModel: StateFlow<RecentModelEntity> = _activeModel.asStateFlow()

    // Input Bar State
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedMode = MutableStateFlow("AUTO") // "AUTO", "CHAT", "IMAGE", "WEBSITE", "FILE_ANALYSIS"
    val selectedMode: StateFlow<String> = _selectedMode.asStateFlow()

    private val _attachedFile = MutableStateFlow<FileAttachmentData?>(null)
    val attachedFile: StateFlow<FileAttachmentData?> = _attachedFile.asStateFlow()

    // Status State
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Theme States
    private val _themeMode = MutableStateFlow(ThemeConfig.getSavedThemeMode(application))
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _themePreset = MutableStateFlow(ThemeConfig.getSavedThemePreset(application))
    val themePreset: StateFlow<ThemePreset> = _themePreset.asStateFlow()

    // Website Fullscreen Preview Modal State
    private val _fullscreenWebsiteHtml = MutableStateFlow<String?>(null)
    val fullscreenWebsiteHtml: StateFlow<String?> = _fullscreenWebsiteHtml.asStateFlow()

    init {
        // Ensure default models are saved to DB
        viewModelScope.launch {
            defaultModelList().forEach { model ->
                recentModelDao.insertOrUpdateModel(model)
            }
        }
    }

    private fun defaultModelList(): List<RecentModelEntity> {
        return listOf(
            RecentModelEntity(
                modelId = "gemini-3.5-flash",
                displayName = "Gemini 3.5 Flash",
                provider = "Google AI",
                description = "Fast, intelligent multimodal reasoning & conversation"
            ),
            RecentModelEntity(
                modelId = "gemini-3.1-pro-preview",
                displayName = "Gemini 3.1 Pro",
                provider = "Google AI",
                description = "Advanced reasoning, coding, & website generation"
            ),
            RecentModelEntity(
                modelId = "gemini-2.5-flash-image",
                displayName = "Gemini Image Studio",
                provider = "Google AI",
                description = "Specialized visual synthesis & art generation"
            )
        )
    }

    fun saveSystemPrompt(prompt: String) {
        viewModelScope.launch {
            settingsDataStore.saveSystemPrompt(prompt)
        }
    }

    fun setDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveDarkMode(isDark)
        }
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun onModeSelected(mode: String) {
        _selectedMode.value = mode
    }

    fun onFileAttached(attachment: FileAttachmentData?) {
        _attachedFile.value = attachment
        if (attachment != null && _selectedMode.value == "AUTO") {
            _selectedMode.value = "FILE_ANALYSIS"
        }
    }

    fun removeAttachedFile() {
        _attachedFile.value = null
        if (_selectedMode.value == "FILE_ANALYSIS") {
            _selectedMode.value = "AUTO"
        }
    }

    fun onModelSelected(model: RecentModelEntity) {
        _activeModel.value = model
        viewModelScope.launch {
            recentModelDao.insertOrUpdateModel(model.copy(lastUsedTimestamp = System.currentTimeMillis()))
        }
    }

    fun createNewConversation(initialTitle: String = "New Conversation") {
        viewModelScope.launch {
            val id = conversationDao.insertConversation(
                ConversationEntity(
                    title = initialTitle,
                    modelUsed = _activeModel.value.modelId
                )
            )
            _currentConversationId.value = id
        }
    }

    fun selectConversation(id: Long) {
        _currentConversationId.value = id
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            messageDao.deleteMessagesForConversation(id)
            conversationDao.deleteConversationById(id)
            if (_currentConversationId.value == id) {
                _currentConversationId.value = null
            }
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            conversationDao.deleteAllConversations()
            _currentConversationId.value = null
        }
    }

    fun clearCurrentChat() {
        val convId = _currentConversationId.value
        if (convId != null) {
            viewModelScope.launch {
                messageDao.deleteMessagesForConversation(convId)
            }
        } else {
            _inputText.value = ""
            _attachedFile.value = null
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveNotificationsEnabled(enabled)
        }
    }

    fun setNotificationSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveNotificationSoundEnabled(enabled)
        }
    }

    fun setNotificationVibrateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveNotificationVibrateEnabled(enabled)
        }
    }

    fun sendTestNotification() {
        NotificationHelper.sendNotification(
            context = getApplication(),
            title = "Notification Sound Test 🔔",
            message = "This is a test notification playing your configured alert sound.",
            soundEnabled = notificationSoundEnabled.value,
            vibrationEnabled = notificationVibrateEnabled.value
        )
    }

    private fun triggerResponseNotification(contentSnippet: String) {
        if (notificationsEnabled.value) {
            val previewText = if (contentSnippet.length > 90) contentSnippet.take(87) + "..." else contentSnippet
            NotificationHelper.sendNotification(
                context = getApplication(),
                title = "OmniAI Response Ready",
                message = previewText.ifBlank { "OmniAI completed generating your response." },
                soundEnabled = notificationSoundEnabled.value,
                vibrationEnabled = notificationVibrateEnabled.value
            )
        }
    }

    fun sendMessage() {
        val prompt = _inputText.value.trim()
        val file = _attachedFile.value
        if (prompt.isEmpty() && file == null) return

        viewModelScope.launch {
            // Create conversation if none selected
            var convId = _currentConversationId.value
            if (convId == null) {
                val title = if (prompt.isNotEmpty()) {
                    if (prompt.length > 25) prompt.take(22) + "..." else prompt
                } else {
                    file?.name ?: "New Conversation"
                }
                convId = conversationDao.insertConversation(
                    ConversationEntity(
                        title = title,
                        modelUsed = _activeModel.value.modelId
                    )
                )
                _currentConversationId.value = convId
            }

            // Determine effective mode
            val mode = when {
                _selectedMode.value != "AUTO" -> _selectedMode.value
                prompt.contains("image", ignoreCase = true) || prompt.contains("draw", ignoreCase = true) || prompt.contains("generate a picture", ignoreCase = true) -> "IMAGE"
                prompt.contains("website", ignoreCase = true) || prompt.contains("landing page", ignoreCase = true) || prompt.contains("build a page", ignoreCase = true) -> "WEBSITE"
                file != null -> "FILE_ANALYSIS"
                else -> "CHAT"
            }

            // Map message type
            val messageType = when (mode) {
                "IMAGE" -> MessageType.IMAGE
                "WEBSITE" -> MessageType.WEBSITE
                "FILE_ANALYSIS" -> MessageType.FILE_ANALYSIS
                else -> MessageType.TEXT
            }

            // Add user message
            val userMsg = MessageEntity(
                conversationId = convId,
                sender = MessageSender.USER,
                content = prompt,
                type = messageType,
                attachmentName = file?.name,
                attachmentMime = file?.mimeType
            )
            messageDao.insertMessage(userMsg)

            // Clear input fields immediately
            _inputText.value = ""
            _attachedFile.value = null
            _isGenerating.value = true

            // Get conversation history for context
            val history = messageDao.getMessagesListForConversation(convId).map {
                Pair(it.sender.name, it.content)
            }

            // Insert placeholder AI message for streaming updates
            val initialAiMsg = MessageEntity(
                conversationId = convId,
                sender = MessageSender.AI,
                content = "",
                type = messageType,
                status = MessageStatus.SENDING
            )
            val aiMsgId = messageDao.insertMessage(initialAiMsg)

            var accumulatedContent = ""

            try {
                repository.streamChatMessage(
                    prompt = prompt,
                    model = _activeModel.value.modelId,
                    attachment = file,
                    mode = mode,
                    conversationHistory = history,
                    customSystemPrompt = systemPrompt.value,
                    onlineSearchEnabled = onlineSearchEnabled.value
                ).collect { chunkText ->
                    accumulatedContent = chunkText
                    val html = if (mode == "WEBSITE" || chunkText.contains("```html")) {
                        extractHtmlFromResponse(chunkText)
                    } else null

                    val updatedAiMsg = MessageEntity(
                        id = aiMsgId,
                        conversationId = convId,
                        sender = MessageSender.AI,
                        content = accumulatedContent,
                        type = messageType,
                        websiteHtml = html,
                        status = MessageStatus.SENDING
                    )
                    messageDao.updateMessage(updatedAiMsg)
                }

                // Finalize streaming response
                val finalHtml = if (mode == "WEBSITE" || accumulatedContent.contains("```html")) {
                    extractHtmlFromResponse(accumulatedContent)
                } else null

                val finalImageUrl = if (mode == "IMAGE" && accumulatedContent.contains("data:image")) {
                    extractImageUrl(accumulatedContent)
                } else null

                val finalAiMsg = MessageEntity(
                    id = aiMsgId,
                    conversationId = convId,
                    sender = MessageSender.AI,
                    content = if (accumulatedContent.isBlank()) "No response received." else accumulatedContent,
                    type = messageType,
                    websiteHtml = finalHtml,
                    imageUrl = finalImageUrl,
                    status = MessageStatus.SUCCESS
                )
                messageDao.updateMessage(finalAiMsg)
                triggerResponseNotification(accumulatedContent)

            } catch (e: Exception) {
                e.printStackTrace()
                val errorMsg = MessageEntity(
                    id = aiMsgId,
                    conversationId = convId,
                    sender = MessageSender.AI,
                    content = "Error generating response: ${e.message}",
                    type = MessageType.TEXT,
                    status = MessageStatus.ERROR
                )
                messageDao.updateMessage(errorMsg)
            } finally {
                _isGenerating.value = false
                conversationDao.getConversationById(convId)?.let { conv ->
                    conversationDao.updateConversation(conv.copy(updatedAt = System.currentTimeMillis()))
                }
            }
        }
    }

    private fun extractHtmlFromResponse(responseText: String): String? {
        val regex = "```html\\s*([\\s\\S]*?)\\s*```".toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(responseText)
        return match?.groupValues?.get(1)?.trim()
    }

    private fun extractImageUrl(responseText: String): String? {
        val regex = "https?://[^\\s\"]+|data:image/[^\\s\"]+".toRegex()
        val match = regex.find(responseText)
        return match?.value
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun editAndBranchConversation(targetMessage: MessageEntity, newText: String) {
        if (newText.isBlank()) return
        viewModelScope.launch {
            val currentConvId = targetMessage.conversationId
            val parentConv = conversationDao.getConversationById(currentConvId)
            val branchTitle = (parentConv?.title ?: "Chat") + " (Branch)"

            // Create new branched conversation entity
            val newConvId = conversationDao.insertConversation(
                ConversationEntity(
                    title = branchTitle,
                    modelUsed = _activeModel.value.modelId,
                    parentConversationId = currentConvId
                )
            )

            // Get all messages prior to targetMessage in current conversation
            val priorMessages = messageDao.getMessagesBefore(currentConvId, targetMessage.id)

            // Copy prior messages into the new conversation
            priorMessages.forEach { msg ->
                messageDao.insertMessage(
                    msg.copy(
                        id = 0, // auto-generate new ID
                        conversationId = newConvId
                    )
                )
            }

            // Switch active conversation to new branch
            _currentConversationId.value = newConvId

            // Insert new edited user message in the branched conversation
            val editedUserMsg = MessageEntity(
                conversationId = newConvId,
                sender = MessageSender.USER,
                content = newText,
                type = targetMessage.type,
                attachmentPath = targetMessage.attachmentPath,
                attachmentName = targetMessage.attachmentName,
                attachmentMime = targetMessage.attachmentMime,
                parentMessageId = targetMessage.id
            )
            messageDao.insertMessage(editedUserMsg)

            // Trigger AI streaming response for the new branched conversation
            _isGenerating.value = true

            // Build history for repository call from copied prior messages
            val history = priorMessages.map { Pair(it.sender.name, it.content) }

            val mode = _selectedMode.value
            val messageType = when (mode) {
                "IMAGE" -> MessageType.IMAGE
                "WEBSITE" -> MessageType.WEBSITE
                "FILE_ANALYSIS" -> MessageType.FILE_ANALYSIS
                else -> MessageType.TEXT
            }

            val initialAiMsg = MessageEntity(
                conversationId = newConvId,
                sender = MessageSender.AI,
                content = "",
                type = messageType,
                status = MessageStatus.SENDING
            )
            val aiMsgId = messageDao.insertMessage(initialAiMsg)

            var accumulatedContent = ""

            try {
                repository.streamChatMessage(
                    prompt = newText,
                    model = _activeModel.value.modelId,
                    attachment = null,
                    mode = mode,
                    conversationHistory = history,
                    customSystemPrompt = systemPrompt.value,
                    onlineSearchEnabled = onlineSearchEnabled.value
                ).collect { chunkText ->
                    accumulatedContent = chunkText
                    val html = if (mode == "WEBSITE" || chunkText.contains("```html")) {
                        extractHtmlFromResponse(chunkText)
                    } else null

                    val updatedAiMsg = MessageEntity(
                        id = aiMsgId,
                        conversationId = newConvId,
                        sender = MessageSender.AI,
                        content = accumulatedContent,
                        type = messageType,
                        websiteHtml = html,
                        status = MessageStatus.SENDING
                    )
                    messageDao.updateMessage(updatedAiMsg)
                }

                val finalHtml = if (mode == "WEBSITE" || accumulatedContent.contains("```html")) {
                    extractHtmlFromResponse(accumulatedContent)
                } else null

                val finalImageUrl = if (mode == "IMAGE" && accumulatedContent.contains("data:image")) {
                    extractImageUrl(accumulatedContent)
                } else null

                val finalAiMsg = MessageEntity(
                    id = aiMsgId,
                    conversationId = newConvId,
                    sender = MessageSender.AI,
                    content = if (accumulatedContent.isBlank()) "No response received." else accumulatedContent,
                    type = messageType,
                    websiteHtml = finalHtml,
                    imageUrl = finalImageUrl,
                    status = MessageStatus.SUCCESS
                )
                messageDao.updateMessage(finalAiMsg)
                triggerResponseNotification(accumulatedContent)

            } catch (e: Exception) {
                e.printStackTrace()
                val errorMsg = MessageEntity(
                    id = aiMsgId,
                    conversationId = newConvId,
                    sender = MessageSender.AI,
                    content = "Error generating response: ${e.message}",
                    type = MessageType.TEXT,
                    status = MessageStatus.ERROR
                )
                messageDao.updateMessage(errorMsg)
            } finally {
                _isGenerating.value = false
                conversationDao.getConversationById(newConvId)?.let { conv ->
                    conversationDao.updateConversation(conv.copy(updatedAt = System.currentTimeMillis()))
                }
            }
        }
    }

    fun saveTemperature(value: Float) {
        viewModelScope.launch {
            settingsDataStore.saveTemperature(value)
        }
    }

    fun saveMaxTokens(value: Int) {
        viewModelScope.launch {
            settingsDataStore.saveMaxTokens(value)
        }
    }

    fun regenerateMessage(targetAiMessage: MessageEntity) {
        if (_isGenerating.value) return
        val convId = targetAiMessage.conversationId
        viewModelScope.launch {
            val allMsgs = messageDao.getMessagesListForConversation(convId)
            val aiIndex = allMsgs.indexOfFirst { it.id == targetAiMessage.id }
            if (aiIndex < 0) return@launch

            val precedingUserMsg = allMsgs.subList(0, aiIndex).lastOrNull { it.sender == MessageSender.USER }
                ?: return@launch

            // Delete the targeted AI message to regenerate
            messageDao.deleteMessageById(targetAiMessage.id)

            _isGenerating.value = true

            val priorMessages = allMsgs.filter { it.id < precedingUserMsg.id }
            val history = priorMessages.map { Pair(it.sender.name, it.content) }

            val mode = _selectedMode.value
            val messageType = when (mode) {
                "IMAGE" -> MessageType.IMAGE
                "WEBSITE" -> MessageType.WEBSITE
                "FILE_ANALYSIS" -> MessageType.FILE_ANALYSIS
                else -> MessageType.TEXT
            }

            val initialAiMsg = MessageEntity(
                conversationId = convId,
                sender = MessageSender.AI,
                content = "",
                type = messageType,
                status = MessageStatus.SENDING
            )
            val newAiMsgId = messageDao.insertMessage(initialAiMsg)

            var accumulatedContent = ""

            try {
                repository.streamChatMessage(
                    prompt = precedingUserMsg.content,
                    model = _activeModel.value.modelId,
                    attachment = null,
                    mode = mode,
                    conversationHistory = history,
                    customSystemPrompt = systemPrompt.value,
                    onlineSearchEnabled = onlineSearchEnabled.value
                ).collect { chunkText ->
                    accumulatedContent = chunkText
                    val html = if (mode == "WEBSITE" || chunkText.contains("```html")) {
                        extractHtmlFromResponse(chunkText)
                    } else null

                    val updatedAiMsg = MessageEntity(
                        id = newAiMsgId,
                        conversationId = convId,
                        sender = MessageSender.AI,
                        content = accumulatedContent,
                        type = messageType,
                        websiteHtml = html,
                        status = MessageStatus.SENDING
                    )
                    messageDao.updateMessage(updatedAiMsg)
                }

                val finalHtml = if (mode == "WEBSITE" || accumulatedContent.contains("```html")) {
                    extractHtmlFromResponse(accumulatedContent)
                } else null

                val finalImageUrl = if (mode == "IMAGE" && accumulatedContent.contains("data:image")) {
                    extractImageUrl(accumulatedContent)
                } else null

                val finalAiMsg = MessageEntity(
                    id = newAiMsgId,
                    conversationId = convId,
                    sender = MessageSender.AI,
                    content = if (accumulatedContent.isBlank()) "No response received." else accumulatedContent,
                    type = messageType,
                    websiteHtml = finalHtml,
                    imageUrl = finalImageUrl,
                    status = MessageStatus.SUCCESS
                )
                messageDao.updateMessage(finalAiMsg)
                triggerResponseNotification(accumulatedContent)

            } catch (e: Exception) {
                e.printStackTrace()
                val errorMsg = MessageEntity(
                    id = newAiMsgId,
                    conversationId = convId,
                    sender = MessageSender.AI,
                    content = "Error generating response: ${e.message}",
                    type = MessageType.TEXT,
                    status = MessageStatus.ERROR
                )
                messageDao.updateMessage(errorMsg)
            } finally {
                _isGenerating.value = false
                conversationDao.getConversationById(convId)?.let { conv ->
                    conversationDao.updateConversation(conv.copy(updatedAt = System.currentTimeMillis()))
                }
            }
        }
    }

    fun translateMessage(targetMessage: MessageEntity, targetLanguage: String) {
        viewModelScope.launch {
            try {
                val prompt = "Translate the following text into $targetLanguage accurately. Preserve markdown formatting, bullet points, and code blocks. Return ONLY the translated content:\n\n${targetMessage.content}"
                var accumulatedTranslation = ""
                repository.streamChatMessage(
                    prompt = prompt,
                    model = _activeModel.value.modelId,
                    attachment = null,
                    mode = "CHAT",
                    conversationHistory = emptyList(),
                    customSystemPrompt = "You are an expert multi-lingual translator fluent in Kinyarwanda, English, French, Spanish, Portuguese, Chinese, Japanese, Kiswahili, and Hindi.",
                    onlineSearchEnabled = onlineSearchEnabled.value
                ).collect { chunk ->
                    accumulatedTranslation = chunk
                    messageDao.updateMessage(
                        targetMessage.copy(
                            translatedText = accumulatedTranslation,
                            translatedLanguage = targetLanguage
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateThemeMode(context: Context, mode: ThemeMode) {
        _themeMode.value = mode
        ThemeConfig.saveThemeMode(context, mode)
        viewModelScope.launch {
            settingsDataStore.saveThemeMode(mode.name)
            settingsDataStore.saveDarkMode(mode == ThemeMode.DARK)
        }
    }

    fun updateUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveUseDynamicColor(enabled)
        }
    }

    fun updateThemePreset(context: Context, preset: ThemePreset) {
        _themePreset.value = preset
        ThemeConfig.saveThemePreset(context, preset)
        viewModelScope.launch {
            settingsDataStore.saveThemePreset(preset.name)
        }
    }

    fun showFullscreenWebsite(html: String) {
        _fullscreenWebsiteHtml.value = html
    }

    fun dismissFullscreenWebsite() {
        _fullscreenWebsiteHtml.value = null
    }

    fun setAutoScrollEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveAutoScrollEnabled(enabled)
        }
    }

    fun setOnlineSearchEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveOnlineSearchEnabled(enabled)
        }
    }

    fun toggleMessageReaction(message: MessageEntity, reactionEmoji: String) {
        viewModelScope.launch {
            val newReaction = if (message.reaction == reactionEmoji) null else reactionEmoji
            messageDao.updateMessage(message.copy(reaction = newReaction))
        }
    }
}
