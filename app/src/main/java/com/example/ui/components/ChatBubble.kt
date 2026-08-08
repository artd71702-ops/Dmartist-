package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.db.MessageEntity
import com.example.data.db.MessageSender
import com.example.data.db.MessageType
import com.example.util.TtsHelper

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChatBubble(
    message: MessageEntity,
    onOpenFullscreenWebsite: (String) -> Unit,
    onRefineWebsitePrompt: (String) -> Unit,
    onEditUserMessage: ((MessageEntity) -> Unit)? = null,
    onRegenerateMessage: ((MessageEntity) -> Unit)? = null,
    onSpeakMessage: ((Long, String, String) -> Unit)? = null,
    onTranslateMessage: ((MessageEntity, String) -> Unit)? = null,
    onToggleReaction: ((MessageEntity, String) -> Unit)? = null,
    isSpeakingThisMessage: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == MessageSender.USER
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    var showTranslateMenu by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf(false) }
    val availableReactions = listOf("👍", "❤️", "👎", "😂", "💡", "🔥", "🎉")

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Avatar",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            // Reaction Bar Popup
            if (showReactionPicker) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 6.dp,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableReactions.forEach { emoji ->
                            Surface(
                                shape = CircleShape,
                                color = if (message.reaction == emoji) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier.clickable {
                                    onToggleReaction?.invoke(message, emoji)
                                    showReactionPicker = false
                                }
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            showReactionPicker = !showReactionPicker
                        }
                    )
                }
            ) {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    ),
                    color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    tonalElevation = 2.dp,
                    shadowElevation = 1.dp
                ) {
                Column(modifier = Modifier.padding(12.dp)) {

                    // Render Attachment Badge if present
                    if (message.attachmentName != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Attachment",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = message.attachmentName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Render Message Text
                    if (message.content.isNotBlank()) {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 20.sp
                            )
                        )
                    }

                    // Render Generated Image
                    if (message.imageUrl != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = message.imageUrl,
                                    contentDescription = "Generated AI Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    // Render Interactive Website Card
                    if (message.websiteHtml != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AndroidView(
                                    factory = { context ->
                                        WebView(context).apply {
                                            webViewClient = WebViewClient()
                                            settings.javaScriptEnabled = true
                                            settings.useWideViewPort = true
                                            settings.loadWithOverviewMode = true
                                            loadDataWithBaseURL(null, message.websiteHtml, "text/html", "UTF-8", null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                Surface(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { onOpenFullscreenWebsite(message.websiteHtml) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fullscreen,
                                            contentDescription = "Fullscreen",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onOpenFullscreenWebsite(message.websiteHtml) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Launch,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Live Preview", style = MaterialTheme.typography.labelSmall)
                            }

                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(message.websiteHtml))
                                    copied = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy HTML",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Iterative refinement suggestions
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SuggestionChip(
                                onClick = { onRefineWebsitePrompt("Make the header darker and add a contact section") },
                                label = { Text("Dark Header", style = MaterialTheme.typography.labelSmall) }
                            )
                            SuggestionChip(
                                onClick = { onRefineWebsitePrompt("Add interactive contact form with smooth validation") },
                                label = { Text("+ Contact Form", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Translated Text Sub-Card
                if (!isUser && message.translatedText != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Translate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Translated to ${message.translatedLanguage}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (onSpeakMessage != null) {
                                    IconButton(
                                        onClick = {
                                            onSpeakMessage(
                                                message.id,
                                                message.translatedText,
                                                message.translatedLanguage ?: "English"
                                            )
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSpeakingThisMessage) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                            contentDescription = "Speak translation",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = message.translatedText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

                // Render reaction badge floating at bottom corner
                if (message.reaction != null) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .align(if (isUser) Alignment.BottomStart else Alignment.BottomEnd)
                            .offset(y = 8.dp, x = if (isUser) (-4).dp else 4.dp)
                            .clickable {
                                onToggleReaction?.invoke(message, message.reaction)
                            }
                    ) {
                        Text(
                            text = message.reaction,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Action buttons below messages
            Row(
                modifier = Modifier
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
            ) {
                if (isUser && onEditUserMessage != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEditUserMessage(message) }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit and branch conversation",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Edit & Fork",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Copy to Clipboard Button
                        IconButton(
                            onClick = {
                                val copyText = message.translatedText ?: message.websiteHtml ?: message.content
                                clipboardManager.setText(AnnotatedString(copyText))
                                copied = true
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy to Clipboard",
                                tint = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (copied) {
                            Text(
                                text = "Copied!",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // TTS Speak Button
                        if (onSpeakMessage != null) {
                            IconButton(
                                onClick = {
                                    val speakText = message.translatedText ?: message.content
                                    val lang = message.translatedLanguage ?: "English"
                                    onSpeakMessage(message.id, speakText, lang)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpeakingThisMessage) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Read aloud",
                                    tint = if (isSpeakingThisMessage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Translate Dropdown Button
                        if (onTranslateMessage != null) {
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showTranslateMenu = true }
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Translate,
                                        contentDescription = "Translate message",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Translate",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                DropdownMenu(
                                    expanded = showTranslateMenu,
                                    onDismissRequest = { showTranslateMenu = false }
                                ) {
                                    Text(
                                        text = "Select Language",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                    HorizontalDivider()

                                    TtsHelper.SUPPORTED_LANGUAGES.forEach { lang ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(lang.flagEmoji, fontSize = 16.sp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(lang.name, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            },
                                            onClick = {
                                                showTranslateMenu = false
                                                onTranslateMessage(message, lang.name)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Regenerate Button for AI messages
                        if (onRegenerateMessage != null) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onRegenerateMessage(message) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = "Regenerate AI Response",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Regenerate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
