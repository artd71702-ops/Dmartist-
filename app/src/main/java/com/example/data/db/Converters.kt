package com.example.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromMessageType(type: MessageType): String = type.name

    @TypeConverter
    fun toMessageType(value: String): MessageType = runCatching { MessageType.valueOf(value) }.getOrDefault(MessageType.TEXT)

    @TypeConverter
    fun fromMessageSender(sender: MessageSender): String = sender.name

    @TypeConverter
    fun toMessageSender(value: String): MessageSender = runCatching { MessageSender.valueOf(value) }.getOrDefault(MessageSender.USER)

    @TypeConverter
    fun fromMessageStatus(status: MessageStatus): String = status.name

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus = runCatching { MessageStatus.valueOf(value) }.getOrDefault(MessageStatus.SUCCESS)
}
