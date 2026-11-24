package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val lastPreview: String,
    val engineUsed: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId")]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val conversationId: Long,
    val isUser: Boolean,
    val text: String,
    val imageUri: String? = null,
    val engineTag: String = "Meena",
    val emotionTag: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "memory_items")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val factText: String,
    val category: String = "Context",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "generated_images")
data class GeneratedImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val prompt: String,
    val style: String,
    val quality: String,
    val aspectRatio: String,
    val filePath: String? = null,
    val noticeText: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quick_actions")
data class QuickActionEntity(
    @PrimaryKey val actionId: String,
    val enabled: Boolean = true,
    val pinned: Boolean = false,
    val orderIndex: Int = 0
)
