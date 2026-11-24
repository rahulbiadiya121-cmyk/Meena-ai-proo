package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ConversationEntity::class,
        ChatMessageEntity::class,
        MemoryEntity::class,
        GeneratedImageEntity::class,
        QuickActionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MeenaDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun memoryDao(): MemoryDao
    abstract fun generatedImageDao(): GeneratedImageDao
    abstract fun quickActionDao(): QuickActionDao

    companion object {
        @Volatile
        private var INSTANCE: MeenaDatabase? = null

        fun getInstance(context: Context): MeenaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MeenaDatabase::class.java,
                    "meena_ai_pro_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
