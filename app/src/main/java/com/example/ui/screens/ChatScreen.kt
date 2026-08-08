package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDashboard: () -> Unit = {}
) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val attachedFile by viewModel.attachedFile.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val recentModels by viewModel.recentModels.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val fullscreenWebsiteHtml by viewModel.fullscreenWebsiteHtml.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val ttsHelper = remember(context) { com.example.util.TtsHelper(context) }
    val currentSpeakingMessageId by ttsHelper.currentSpeakingMessageId.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
        }
    }

    val speechLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                val current = inputText
                val updated = if (current.isBlank()) spokenText else "$current $spokenText"
                viewModel.onInputTextChanged(updated)
            }
        }
    }

    fun launchSpeechToText() {
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Dictate prompt to OmniAI...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Speech recognition unavailable on this device", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    var showAttachmentPicker by remember { mutableStateOf(false) }
    var showPromptTemplatesSheet by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<com.example.data.db.MessageEntity?>(null) }
    var editedTextState by remember { mutableStateOf("") }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val autoScrollEnabled by viewModel.autoScrollEnabled.collectAsState()

    // Scroll to bottom when new messages arrive if autoScrollEnabled is true
    LaunchedEffect(messages.size, isGenerating, autoScrollEnabled) {
        if (autoScrollEnabled && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "OmniAI",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ModelSelectorDropdown(
                            activeModel = activeModel,
                            recentModels = recentModels,
                            onModelSelected = { viewModel.onModelSelected(it) }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = { com.example.util.PdfExporter.shareTextConversation(context, messages) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Conversation Text",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { showClearChatDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Chat",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More options")
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Share Text Transcript")
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    com.example.util.PdfExporter.shareTextConversation(context, messages)
                                },
                                enabled = messages.isNotEmpty()
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Export Conversation as PDF")
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    com.example.util.PdfExporter.generateAndSharePdf(context, messages)
                                },
                                enabled = messages.isNotEmpty()
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Prompt Templates")
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showPromptTemplatesSheet = true
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("New Chat")
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.createNewConversation()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.BarChart,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Analytics Dashboard")
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenDashboard()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Settings")
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenSettings()
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = inputText,
                onInputTextChanged = { viewModel.onInputTextChanged(it) },
                selectedMode = selectedMode,
                onModeSelected = { viewModel.onModeSelected(it) },
                attachedFile = attachedFile,
                onRemoveAttachment = { viewModel.removeAttachedFile() },
                onAttachClick = { showAttachmentPicker = true },
                onOpenTemplatesClick = { showPromptTemplatesSheet = true },
                onSendClick = { viewModel.sendMessage() },
                onMicClick = { launchSpeechToText() },
                isGenerating = isGenerating
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (messages.isEmpty()) {
                // Welcome / Quick Suggestions Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "How can OmniAI assist you?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Conversational Q&A • Image Generation • Web Page Creation • File Analysis",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Suggestion Cards Grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SuggestionCard(
                            title = "💻 Generate a Web Landing Page",
                            subtitle = "\"Build a landing page for a cozy coffee shop with dark mode\"",
                            icon = Icons.Default.Code
                        ) {
                            viewModel.onModeSelected("WEBSITE")
                            viewModel.onInputTextChanged("Build a landing page for a cozy coffee shop with dark mode, interactive menu, and contact form")
                        }

                        SuggestionCard(
                            title = "🎨 Generate Custom Art or Image",
                            subtitle = "\"A futuristic neon cyberpunk city at sunset with glowing lights\"",
                            icon = Icons.Default.Image
                        ) {
                            viewModel.onModeSelected("IMAGE")
                            viewModel.onInputTextChanged("A futuristic neon cyberpunk city at sunset with glowing lights and flying vehicles")
                        }

                        SuggestionCard(
                            title = "📁 Attach & Analyze a Document",
                            subtitle = "Import TXT, CSV, or photos for instant AI summary",
                            icon = Icons.Default.Description
                        ) {
                            showAttachmentPicker = true
                        }
                    }
                }
            } else {
                // Messages List
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatBubble(
                            message = message,
                            onOpenFullscreenWebsite = { html -> viewModel.showFullscreenWebsite(html) },
                            onRefineWebsitePrompt = { prompt ->
                                viewModel.onModeSelected("WEBSITE")
                                viewModel.onInputTextChanged(prompt)
                            },
                            onEditUserMessage = { msgToEdit ->
                                editingMessage = msgToEdit
                                editedTextState = msgToEdit.content
                            },
                            onRegenerateMessage = { aiMsg ->
                                viewModel.regenerateMessage(aiMsg)
                            },
                            onSpeakMessage = { msgId, text, lang ->
                                ttsHelper.speak(msgId, text, lang)
                            },
                            onTranslateMessage = { msg, targetLang ->
                                viewModel.translateMessage(msg, targetLang)
                            },
                            onToggleReaction = { msg, emoji ->
                                viewModel.toggleMessageReaction(msg, emoji)
                            },
                            isSpeakingThisMessage = (currentSpeakingMessageId == message.id)
                        )
                    }

                    if (isGenerating) {
                        item {
                            TypingIndicator()
                        }
                    }
                }
            }
        }
    }

    // Attachment Picker Modal
    if (showAttachmentPicker) {
        AttachmentPickerBottomSheet(
            onDismissRequest = { showAttachmentPicker = false },
            onFileSelected = { fileData ->
                viewModel.onFileAttached(fileData)
            }
        )
    }

    // Prompt Templates Library Bottom Sheet
    if (showPromptTemplatesSheet) {
        PromptTemplatesBottomSheet(
            onDismissRequest = { showPromptTemplatesSheet = false },
            onSelectTemplate = { template ->
                if (template.category == "Image Gen") {
                    viewModel.onModeSelected("IMAGE")
                } else if (template.category == "Websites") {
                    viewModel.onModeSelected("WEBSITE")
                } else if (template.category == "Coding") {
                    viewModel.onModeSelected("CHAT")
                }
                viewModel.onInputTextChanged(template.templateText)
            }
        )
    }

    // Fullscreen Website Live Interactive Preview Dialog
    fullscreenWebsiteHtml?.let { html ->
        WebsitePreviewDialog(
            htmlContent = html,
            onDismiss = { viewModel.dismissFullscreenWebsite() }
        )
    }

    // Edit Message and Fork Branch Dialog
    editingMessage?.let { targetMsg ->
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ForkRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit & Fork Branch",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Modifying this message will create a new conversation branch with updated context and history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editedTextState,
                        onValueChange = { editedTextState = it },
                        label = { Text("Updated Prompt") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val msg = targetMsg
                        val newText = editedTextState
                        editingMessage = null
                        viewModel.editAndBranchConversation(msg, newText)
                    },
                    enabled = editedTextState.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Fork & Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Clear Chat Confirmation Dialog
    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Clear Current Chat?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to clear all messages in this conversation? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearChatDialog = false
                        viewModel.clearCurrentChat()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SuggestionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}
