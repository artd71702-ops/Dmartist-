package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageType {
    TEXT,
    IMAGE,
    WEBSITE,
    FILE_ANALYSIS
}

enum class MessageSender {
    USER,
    AI
}

enum class MessageStatus {
    SENDING,
    SUCCESS,
    ERROR
}

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val sender: MessageSender,
    val content: String,
    val type: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val attachmentPath: String? = null,
    val attachmentName: String? = null,
    val attachmentMime: String? = null,
    val websiteHtml: String? = null,
    val imageUrl: String? = null,
    val status: MessageStatus = MessageStatus.SUCCESS,
    val parentMessageId: Long? = null,
    val translatedText: String? = null,
    val translatedLanguage: String? = null,
    val reaction: String? = null
)
