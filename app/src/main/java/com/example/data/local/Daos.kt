package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: Long)

    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    fun observeMessages(conversationId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    suspend fun getMessagesOnce(conversationId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun clearMessagesForConversation(conversationId: Long)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_items ORDER BY createdAt DESC")
    fun observeMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memory_items ORDER BY createdAt DESC LIMIT 12")
    suspend fun getTopMemoriesOnce(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Query("DELETE FROM memory_items WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM memory_items")
    suspend fun clearAllMemories()
}

@Dao
interface GeneratedImageDao {
    @Query("SELECT * FROM generated_images ORDER BY createdAt DESC")
    fun observeImages(): Flow<List<GeneratedImageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: GeneratedImageEntity): Long

    @Query("DELETE FROM generated_images WHERE id = :id")
    suspend fun deleteImageById(id: Long)

    @Query("DELETE FROM generated_images")
    suspend fun clearAllImages()
}

@Dao
interface QuickActionDao {
    @Query("SELECT * FROM quick_actions ORDER BY pinned DESC, orderIndex ASC")
    fun observeQuickActions(): Flow<List<QuickActionEntity>>

    @Query("SELECT COUNT(*) FROM quick_actions")
    suspend fun countQuickActions(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<QuickActionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: QuickActionEntity)
}
