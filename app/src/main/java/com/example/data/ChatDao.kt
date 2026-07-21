package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert
    suspend fun insertSession(session: ChatSession): Long

    @Update
    suspend fun updateSession(session: ChatSession)

    @Delete
    suspend fun deleteSession(session: ChatSession)

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE sessionId = :id LIMIT 1")
    suspend fun getSessionById(id: Long): ChatSession?

    @Insert
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionList(sessionId: Long): List<ChatMessage>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId AND isUser = 1 ORDER BY timestamp ASC")
    fun getUserMessagesForSession(sessionId: Long): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNickname(nickname: AppNickname)

    @Query("SELECT * FROM app_nicknames")
    fun getAllNicknames(): Flow<List<AppNickname>>

    @Query("SELECT * FROM app_nicknames WHERE packageName = :packageName LIMIT 1")
    suspend fun getNicknameForPackage(packageName: String): AppNickname?
}
