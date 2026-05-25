package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY createdAt DESC")
    fun getAllCharacters(): Flow<List<Character>>

    @Query("SELECT * FROM characters WHERE id = :id LIMIT 1")
    fun getCharacterByIdFlow(id: Int): Flow<Character?>

    @Query("SELECT * FROM characters WHERE id = :id LIMIT 1")
    suspend fun getCharacterById(id: Int): Character?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: Character): Long

    @Update
    suspend fun updateCharacter(character: Character)

    @Delete
    suspend fun deleteCharacter(character: Character)
}

@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions ORDER BY createdAt DESC")
    fun getAllChatSessions(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE characterId = :characterId ORDER BY createdAt DESC")
    fun getChatSessionsByCharacter(characterId: Int): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id LIMIT 1")
    suspend fun getChatSessionById(id: Int): ChatSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatSession(session: ChatSession): Long

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteChatSessionById(id: Int)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE chatSessionId = :chatSessionId ORDER BY timestamp ASC")
    fun getMessagesForSessionFlow(chatSessionId: Int): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE chatSessionId = :chatSessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSession(chatSessionId: Int): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Update
    suspend fun updateMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Int)

    @Query("DELETE FROM chat_messages WHERE chatSessionId = :sessionId")
    suspend fun clearMessagesForSession(sessionId: Int)
}

@Dao
interface LorebookDao {
    @Query("SELECT * FROM lorebook_entries ORDER BY name ASC")
    fun getAllLorebookEntries(): Flow<List<LorebookEntry>>

    @Query("SELECT * FROM lorebook_entries WHERE characterId IS NULL OR characterId = :characterId ORDER BY name ASC")
    fun getLorebookEntriesForCharacter(characterId: Int): Flow<List<LorebookEntry>>

    @Query("SELECT * FROM lorebook_entries WHERE characterId IS NULL OR characterId = :characterId")
    suspend fun getLorebookEntriesForCharacterSync(characterId: Int): List<LorebookEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLorebookEntry(entry: LorebookEntry): Long

    @Update
    suspend fun updateLorebookEntry(entry: LorebookEntry)

    @Delete
    suspend fun deleteLorebookEntry(entry: LorebookEntry)
}

@Dao
interface AgentStateDao {
    @Query("SELECT * FROM agent_states WHERE characterId = :characterId LIMIT 1")
    fun getAgentStateFlow(characterId: Int): Flow<AgentState?>

    @Query("SELECT * FROM agent_states WHERE characterId = :characterId LIMIT 1")
    suspend fun getAgentState(characterId: Int): AgentState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgentState(state: AgentState)

    @Update
    suspend fun updateAgentState(state: AgentState)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE characterId = :characterId ORDER BY timestamp DESC")
    fun getMemoriesForCharacter(characterId: Int): Flow<List<Memory>>

    @Query("SELECT * FROM memories WHERE characterId = :characterId ORDER BY timestamp DESC")
    suspend fun getMemoriesForCharacterSync(characterId: Int): List<Memory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: Memory): Long

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Int)
}

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): AppSetting?

    @Query("SELECT * FROM app_settings")
    fun getAllSettingsFlow(): Flow<List<AppSetting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)
}
