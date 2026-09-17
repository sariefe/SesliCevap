package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ConversationHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) interface for operations on [ConversationHistoryEntity],
 * providing query, insert, update, and delete access for stored conversation history.
 */
@Dao
interface ConversationHistoryDao {

    @Query("SELECT * FROM conversation_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ConversationHistoryEntity>>

    @Query("SELECT * FROM conversation_history WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getHistoryByConversationId(conversationId: Long): Flow<List<ConversationHistoryEntity>>

    @Query("SELECT * FROM conversation_history WHERE id = :id LIMIT 1")
    fun getHistoryById(id: Long): Flow<ConversationHistoryEntity?>

    @Query("SELECT * FROM conversation_history WHERE id = :id LIMIT 1")
    suspend fun getHistoryByIdOnce(id: Long): ConversationHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(historyItem: ConversationHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(historyItems: List<ConversationHistoryEntity>): List<Long>

    @Update
    suspend fun updateHistory(historyItem: ConversationHistoryEntity)

    @Delete
    suspend fun deleteHistory(historyItem: ConversationHistoryEntity)

    @Query("DELETE FROM conversation_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM conversation_history WHERE conversationId = :conversationId")
    suspend fun deleteHistoryForConversation(conversationId: Long)

    @Query("DELETE FROM conversation_history")
    suspend fun clearAllHistory()

    @Query("SELECT COUNT(*) FROM conversation_history")
    fun getHistoryCount(): Flow<Int>

    @Query("SELECT * FROM conversation_history WHERE userInputText LIKE '%' || :query || '%' OR aiResponse LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistory(query: String): Flow<List<ConversationHistoryEntity>>
}
