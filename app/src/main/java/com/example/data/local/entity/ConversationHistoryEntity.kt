package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Database entity for storing conversation history records,
 * including user input text, AI response, and timestamp.
 */
@Entity(tableName = "conversation_history")
data class ConversationHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long = 0L,
    val userInputText: String,
    val aiResponse: String,
    val timestamp: Long = System.currentTimeMillis()
)
