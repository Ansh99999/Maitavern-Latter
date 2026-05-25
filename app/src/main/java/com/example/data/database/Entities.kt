package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "characters")
data class Character(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val displayName: String,
    val description: String,
    val personality: String,
    val firstMessage: String,
    val alternateGreetingsJson: String = "[]", // List<String>
    val scenario: String,
    val exampleDialogues: String,
    val creatorNotes: String,
    val avatarPath: String? = null, // image uri or base64 or local filepath
    val customThemeId: String? = null,
    val customModelOverride: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val characterId: Int,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatSessionId: Int,
    val sender: String, // "user", "character", "system"
    val text: String, // Currently visible swipe text, or the main message
    val timestamp: Long = System.currentTimeMillis(),
    val swipeIndex: Int = 0,
    val swipesJson: String = "[]", // JSON array of List<String> for alternative regenerations
    val tokenCount: Int = 0,
    val cost: Double = 0.0
) : Serializable

@Entity(tableName = "lorebook_entries")
data class LorebookEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val characterId: Int? = null, // null means global lorebook entry
    val name: String,
    val keysCsv: String, // Trigger keywords, e.g. "dead star, vyreth, constellation"
    val content: String,
    val approved: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "agent_states")
data class AgentState(
    @PrimaryKey val characterId: Int, // 1-to-1 with character
    val mood: String = "Neutral",
    val trust: Int = 50, // 0 to 100
    val activeGoal: String = "Discovering relationships",
    val location: String = "Default Space",
    val inventory: String = "", // items
    val injuries: String = "None",
    val relationshipMilestone: String = "Stranger",
    val lastUpdatedMsgId: Int = 0,
    val lastUpdatedTime: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val characterId: Int,
    val summary: String,
    val keyFacts: String = "", // extra details bulleted
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
) : Serializable
