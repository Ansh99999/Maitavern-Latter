package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.GeminiApiClient
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiPart
import com.example.data.database.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class RoleplayRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val characterDao = db.characterDao()
    private val chatSessionDao = db.chatSessionDao()
    private val chatMessageDao = db.chatMessageDao()
    private val lorebookDao = db.lorebookDao()
    private val agentStateDao = db.agentStateDao()
    private val memoryDao = db.memoryDao()
    private val appSettingDao = db.appSettingDao()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val listAdapter = moshi.adapter<List<String>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
    )

    fun listToJson(list: List<String>): String =
        try { listAdapter.toJson(list) } catch (e: Exception) { "[]" }

    fun jsonToList(json: String): List<String> =
        try { listAdapter.fromJson(json) ?: emptyList() } catch (e: Exception) { emptyList() }

    // --- Settings / BYOK Hub ---
    suspend fun getSettingValue(key: String, defaultValue: String = ""): String {
        return appSettingDao.getSetting(key)?.value ?: defaultValue
    }

    suspend fun saveSetting(key: String, value: String) {
        appSettingDao.insertSetting(AppSetting(key, value))
    }

    fun getAllSettings(): Flow<List<AppSetting>> = appSettingDao.getAllSettingsFlow()

    // --- Characters ---
    val allCharacters: Flow<List<Character>> = characterDao.getAllCharacters()

    fun getCharacterByIdFlow(id: Int): Flow<Character?> = characterDao.getCharacterByIdFlow(id)

    suspend fun getCharacterById(id: Int): Character? = characterDao.getCharacterById(id)

    suspend fun insertCharacter(character: Character): Long {
        val characterId = characterDao.insertCharacter(character)
        // Initialize AgentState for characters automatically
        if (agentStateDao.getAgentState(characterId.toInt()) == null) {
            agentStateDao.insertAgentState(AgentState(characterId = characterId.toInt()))
        }
        return characterId
    }

    suspend fun updateCharacter(character: Character) {
        characterDao.updateCharacter(character)
    }

    suspend fun deleteCharacter(character: Character) {
        characterDao.deleteCharacter(character)
    }

    // --- Chat Sessions ---
    fun getAllSessions(): Flow<List<ChatSession>> = chatSessionDao.getAllChatSessions()

    fun getSessionsForCharacter(characterId: Int): Flow<List<ChatSession>> =
        chatSessionDao.getChatSessionsByCharacter(characterId)

    suspend fun createChatSession(characterId: Int, title: String): Long {
        return chatSessionDao.insertChatSession(ChatSession(characterId = characterId, title = title))
    }

    suspend fun getSessionById(id: Int): ChatSession? = chatSessionDao.getChatSessionById(id)

    suspend fun deleteSession(sessionId: Int) {
        chatMessageDao.clearMessagesForSession(sessionId)
        chatSessionDao.deleteChatSessionById(sessionId)
    }

    // --- Chat Messages ---
    fun getMessagesForSessionFlow(sessionId: Int): Flow<List<ChatMessage>> =
        chatMessageDao.getMessagesForSessionFlow(sessionId)

    suspend fun getMessagesForSession(sessionId: Int): List<ChatMessage> =
        chatMessageDao.getMessagesForSession(sessionId)

    suspend fun insertMessage(message: ChatMessage): Long {
        return chatMessageDao.insertMessage(message)
    }

    suspend fun updateMessage(message: ChatMessage) {
        chatMessageDao.updateMessage(message)
    }

    suspend fun deleteMessageById(id: Int) {
        chatMessageDao.deleteMessageById(id)
    }

    // --- Lorebook Entries ---
    val allLorebookEntries: Flow<List<LorebookEntry>> = lorebookDao.getAllLorebookEntries()

    fun getLorebookEntriesForCharacter(characterId: Int): Flow<List<LorebookEntry>> =
        lorebookDao.getLorebookEntriesForCharacter(characterId)

    suspend fun saveLorebookEntry(entry: LorebookEntry) {
         if (entry.id == 0) {
             lorebookDao.insertLorebookEntry(entry)
         } else {
             lorebookDao.updateLorebookEntry(entry)
         }
    }

    suspend fun deleteLorebookEntry(entry: LorebookEntry) {
        lorebookDao.deleteLorebookEntry(entry)
    }

    // --- Agentic AI State & Memories ---
    fun getAgentStateFlow(characterId: Int): Flow<AgentState?> = agentStateDao.getAgentStateFlow(characterId)

    suspend fun getAgentState(characterId: Int): AgentState? = agentStateDao.getAgentState(characterId)

    suspend fun updateAgentState(state: AgentState) = agentStateDao.updateAgentState(state)

    fun getMemoriesForCharacter(characterId: Int): Flow<List<Memory>> =
        memoryDao.getMemoriesForCharacter(characterId)

    suspend fun insertMemory(memory: Memory) = memoryDao.insertMemory(memory)


    // ============================================
    // --- MAIN ENGINE GENERATION LOGIC WITH AGENTS ---
    // ============================================

    suspend fun generateResponse(
        sessionId: Int,
        userPrompt: String,
        isRegeneration: Boolean = false,
        regResourceMessageId: Int = 0
    ): String = withContext(Dispatchers.IO) {
        val session = chatSessionDao.getChatSessionById(sessionId)
            ?: return@withContext "Error: Session not found"

        val character = characterDao.getCharacterById(session.characterId)
            ?: return@withContext "Error: Character not found"

        val customApiKey = getSettingValue("byok_gemini_key").ifBlank { null }
        val modelOverride = getSettingValue("primary_model_override", "gemini-3.5-flash")

        // 1. Lorebook Keeper Triggering
        // Process message text for trigger keywords
        val lorebooks = lorebookDao.getLorebookEntriesForCharacterSync(character.id)
        val activeLorebookContext = StringBuilder()
        for (lb in lorebooks) {
            val triggers = lb.keysCsv.split(",").map { it.trim().lowercase() }
            val matched = triggers.any { key ->
                key.isNotBlank() && (userPrompt.lowercase().contains(key) ||
                        (isRegeneration && activeLorebookContext.contains(key)))
            }
            if (matched) {
                activeLorebookContext.append("\n- Lore [${lb.name}]: ${lb.content}")
            }
        }

        // 2. Fetch Active State Tracker Snapshot
        val agentState = agentStateDao.getAgentState(character.id) ?: AgentState(characterId = character.id)
        val statusSnaps = """
            [Current Emotional/Physical Status of ${character.name}]:
            Mood: ${agentState.mood}
            Trust Level (0-100): ${agentState.trust}
            Current Goal: ${agentState.activeGoal}
            Location: ${agentState.location}
            Current Possessions: ${agentState.inventory}
            Injuries/Status: ${agentState.injuries}
            Active Relationship Status: ${agentState.relationshipMilestone}
        """.trimIndent()

        // 3. Cold/Frozen Memory RAG Retrieval
        val memoriesList = memoryDao.getMemoriesForCharacterSync(character.id)
        val coldMemoryContext = if (memoriesList.isNotEmpty()) {
            val summaryText = memoriesList.take(5).joinToString("\n") { "- Summary block: " + it.summary }
            "\n[Relevant Chat Summaries & Frozen Memories]:\n$summaryText"
        } else {
            ""
        }

        // 4. Construct System Instructions
        // Formulated for SillyTavern power roleplay: W++, Ali:chat, plain personality details
        val systemInstruction = """
            You are playing the role of ${character.name} in a rich interactive roleplay with {{user}}.
            
            [Character Persona Details]:
            Name: ${character.name}
            Display: ${character.displayName}
            Description/Background: ${character.description}
            Personality summary: ${character.personality}
            Scenario/World-Setting: ${character.scenario}
            Creator Guidelines: ${character.creatorNotes}
            
            $statusSnaps
            
            [Active World Lore Context]:
            $activeLorebookContext
            $coldMemoryContext
            
            [Rules of Roleplay]:
            1) Stay deeply in character. Be descriptive of your movements, expressions, and surroundings using *asterisks for physical actions*. Use quotes "for spoken dialogue".
            2) Follow the mood and trust level indicated above. Ensure character behavior scales realistically based on the Trust Level.
            3) Keep replies immersive and visually scan-friendly. Use italics for emotional introspection.
            4) Avoid speaking or acting as {{user}}. Always let {{user}} take their own agency and reply independently!
        """.trimIndent()

        // 5. Gather Chat History Core
        val existingMessages = chatMessageDao.getMessagesForSession(sessionId)
        val historyContents = mutableListOf<GeminiContent>()

        // Limit context to last 20 messages (Tiers: Hot Memory)
        val hotHistory = existingMessages.takeLast(20)
        for (msg in hotHistory) {
            if (msg.sender == "user") {
                historyContents.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = msg.text))))
            } else if (msg.sender == "character") {
                historyContents.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = msg.text))))
            }
        }

        // 6. Generate AI Response
        val generatedText = GeminiApiClient.generate(
            prompt = if (isRegeneration) "Please regenerate an completely different alternate response continuing our story. Give a unique option with different focus or tone." else userPrompt,
            systemInstruction = systemInstruction,
            userApiKey = customApiKey,
            modelOverride = modelOverride,
            history = historyContents
        )

        // 7. Save to Database: Swipe or Single Response
        if (isRegeneration && regResourceMessageId != 0) {
            // Regeneration of existing message (Swiping)
            val currentMsg = existingMessages.find { it.id == regResourceMessageId }
            if (currentMsg != null) {
                val currentSwipes = jsonToList(currentMsg.swipesJson).toMutableList()
                currentSwipes.add(generatedText)
                val newIndex = currentSwipes.lastIndex

                val updatedMsg = currentMsg.copy(
                    text = generatedText,
                    swipeIndex = newIndex,
                    swipesJson = listToJson(currentSwipes)
                )
                chatMessageDao.updateMessage(updatedMsg)
            }
        } else {
            // Add as a new character response message
            val responseSwipes = listOf(generatedText)
            val newMessage = ChatMessage(
                chatSessionId = sessionId,
                sender = "character",
                text = generatedText,
                swipeIndex = 0,
                swipesJson = listToJson(responseSwipes)
            )
            chatMessageDao.insertMessage(newMessage)
        }

        // 8. Trigger Background Agent Processes (Non-blocking)
        // Memory curation (Agent 1) & State Evolution (Agent 3) every 5 turns
        val totalMsgs = existingMessages.size + 1
        if (totalMsgs % 5 == 0) {
            // Trigger automatic agents running silently in background!
            triggerAgents(character.id, sessionId, existingMessages + ChatMessage(chatSessionId = sessionId, sender = "character", text = generatedText))
        }

        return@withContext generatedText
    }

    private suspend fun triggerAgents(characterId: Int, sessionId: Int, chatHistory: List<ChatMessage>) {
        val customApiKey = getSettingValue("byok_gemini_key").ifBlank { null }
        val modelOverride = getSettingValue("primary_model_override", "gemini-3.5-flash")

        if (chatHistory.size < 4) return

        // Take last 8 messages for state updates
        val slice = chatHistory.takeLast(8)
        val conversationText = slice.joinToString("\n") { "${it.sender}: ${it.text}" }

        // --- AGENT 1: Memory Curator (summarizing conversation segment) ---
        try {
            val memoryPrompt = """
                You are Agent 1: Memory Curator for a roleplay session.
                Analyze the following recent conversation segment and write an extremely descriptive, dense 2-sentence summary of the active plot developments, important events, and new details:
                
                $conversationText
                
                Format: Provide ONLY the raw 2-sentence summary without introduction or metadata.
            """.trimIndent()

            val segmentSummary = GeminiApiClient.generate(
                prompt = memoryPrompt,
                systemInstruction = "You are a concise memory curator who extracts plots and facts as summaries.",
                userApiKey = customApiKey,
                modelOverride = modelOverride,
                temperature = 0.5f
            )

            if (segmentSummary.isNotBlank() && !segmentSummary.contains("Error")) {
                insertMemory(
                    Memory(
                        characterId = characterId,
                        summary = segmentSummary,
                        timestamp = System.currentTimeMillis()
                    )
                )
                Log.d("MemoryCuratorAgent", "Auto-created summary memory: $segmentSummary")
            }
        } catch (e: Exception) {
            Log.e("MemoryCurator", "Memory generation failed", e)
        }

        // --- AGENT 3: Character Evolution Tracker (relationship changes, active goals) ---
        try {
            val curState = agentStateDao.getAgentState(characterId) ?: AgentState(characterId = characterId)
            val evolutionPrompt = """
                You are Agent 3: Character Evolution Tracker.
                Review the last portion of the conversation and determine how it affects the character's relationship, mood, trust levels, and active status.
                
                [Current State Record]:
                Mood: ${curState.mood}
                Trust level (0-100): ${curState.trust}
                Goals: ${curState.activeGoal}
                Location: ${curState.location}
                Possessions: ${curState.inventory}
                Injuries: ${curState.injuries}
                Relationship milestone: ${curState.relationshipMilestone}
                
                [Recent Interaction]:
                $conversationText
                
                Analyze the interaction. Output an updated, realistic state representation using the exact CSV parameters format:
                Mood | Trust | Goals | Location | Possessions | Injuries | Milestone
                
                Rules:
                - Trust should change dynamically based on the user's choices (increase for kindness/trustworthiness, decrease for hostility or lies). Range is 0-100.
                - Milestone should update progressively (e.g. Stranger -> Acquaintance -> Companion -> Ally -> Intimate Friend).
                - Mood should reflect the latest dialogue.
                - Location should update if they moved somewhere.
                - Possessions must keep existing items unless used/lost, and add items specifically given in dialogue.
                
                Format strictly as: Mood | Trust | Goals | Location | Possessions | Injuries | Milestone
                Include ONLY this formatted line.
            """.trimIndent()

            val stateString = GeminiApiClient.generate(
                prompt = evolutionPrompt,
                systemInstruction = "You output state lines in CSV format: Mood | Trust | Goals | Location | Possessions | Injuries | Milestone",
                userApiKey = customApiKey,
                modelOverride = modelOverride,
                temperature = 0.3f
            )

            if (stateString.isNotBlank() && stateString.contains("|")) {
                val parts = stateString.split("|").map { it.trim() }
                if (parts.size >= 7) {
                    val mood = parts[0]
                    val trust = parts[1].toIntOrNull() ?: curState.trust
                    val goals = parts[2]
                    val location = parts[3]
                    val inventory = parts[4]
                    val injuries = parts[5]
                    val milestone = parts[6]

                    val updatedState = curState.copy(
                        mood = mood,
                        trust = trust,
                        activeGoal = goals,
                        location = location,
                        inventory = inventory,
                        injuries = injuries,
                        relationshipMilestone = milestone,
                        lastUpdatedMsgId = chatHistory.lastOrNull()?.id ?: 0,
                        lastUpdatedTime = System.currentTimeMillis()
                    )
                    agentStateDao.insertAgentState(updatedState)
                    Log.d("CharacterEvolutionAgent", "Successfully updated character state: $updatedState")
                }
            }
        } catch (e: Exception) {
            Log.e("CharacterEvolution", "Evolution failed", e)
        }
    }

    // --- AI Assist Character Builder ---
    suspend fun generateCompleteCharacter(ideaPrompt: String): Character = withContext(Dispatchers.IO) {
        val customApiKey = getSettingValue("byok_gemini_key").ifBlank { null }
        val modelOverride = getSettingValue("primary_model_override", "gemini-3.5-flash")

        val prompt = """
            You are an expert character creator for a SillyTavern-style roleplay app.
            Generate a rich, multi-layered roleplay character profile based on this concept:
            "$ideaPrompt"
            
            Format your response strictly in XML format with the following tags so we can parse them perfectly:
            <name>Simple Short Name</name>
            <displayName>The Full Majestic Title/Descriptor</displayName>
            <description>General character background story, history, secrets, and detailed world place</description>
            <personality>List of core traits, speaking style, behavioral quirks, and deep flaws</personality>
            <scenario>Detailed initial scene setting, situational context, what is happening</scenario>
            <firstMessage>A highly descriptive introductory message written in 3rd person with actions in *asterisks* and dialogue in "quotes". Set the scene, establish stakes, and invite user reply.</firstMessage>
            <creatorNotes>Tips for prompt tuning, recommended temperature (e.g., 1.1-1.3) or model preferences</creatorNotes>
            
            Rules:
            - Make the profile highly immersive, authentic, and free of generic clichés.
            - Ensure all tags are correctly opened and closed.
        """.trimIndent()

        val xmlOutput = GeminiApiClient.generate(
            prompt = prompt,
            systemInstruction = "You output perfect XML formatted characters.",
            userApiKey = customApiKey,
            modelOverride = modelOverride,
            temperature = 0.9f
        )

        fun extractTag(tag: String): String {
            val start = xmlOutput.indexOf("<$tag>")
            val end = xmlOutput.indexOf("</$tag>")
            if (start != -1 && end != -1 && end > start) {
                return xmlOutput.substring(start + tag.length + 2, end).trim()
            }
            return ""
        }

        val name = extractTag("name").ifBlank { ideaPrompt.take(20) }
        val displayName = extractTag("displayName").ifBlank { "$name (Generative)" }
        val description = extractTag("description").ifBlank { "Immortal wanderer with secrets." }
        val personality = extractTag("personality").ifBlank { "Wise, introspective, dry." }
        val scenario = extractTag("scenario").ifBlank { "Deep in the stellar ruins." }
        val firstMessage = extractTag("firstMessage").ifBlank { "*The door swings open as I stare at you.* \"And so, you arrived.\"" }
        val creatorNotes = extractTag("creatorNotes").ifBlank { "Recommended high creative temperature." }

        return@withContext Character(
            name = name,
            displayName = displayName,
            description = description,
            personality = personality,
            firstMessage = firstMessage,
            scenario = scenario,
            exampleDialogues = "<START>\n{{user}}: What is there?\n{{char}}: *stares at distance* \"Only endings.\"",
            creatorNotes = creatorNotes
        )
    }
}
