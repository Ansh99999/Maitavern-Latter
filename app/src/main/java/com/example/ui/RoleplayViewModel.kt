package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.repository.RoleplayRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RoleplayViewModel(application: Application) : AndroidViewModel(application) {
    val repository = RoleplayRepository(application)

    // --- State Flow Holders ---
    val allCharacters: StateFlow<List<Character>> = repository.allCharacters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCharacterId = MutableStateFlow<Int?>(null)
    val selectedCharacterId = _selectedCharacterId.asStateFlow()

    val selectedCharacter: Flow<Character?> = _selectedCharacterId
        .flatMapLatest { id ->
            if (id != null) repository.getCharacterByIdFlow(id) else flowOf(null)
        }

    val activeAgentState: Flow<AgentState?> = _selectedCharacterId
        .flatMapLatest { id ->
            if (id != null) repository.getAgentStateFlow(id) else flowOf(null)
        }

    val activeMemories: Flow<List<Memory>> = _selectedCharacterId
        .flatMapLatest { id ->
            if (id != null) repository.getMemoriesForCharacter(id) else flowOf(emptyList())
        }

    private val _activeSessionId = MutableStateFlow<Int?>(null)
    val activeSessionId = _activeSessionId.asStateFlow()

    val activeMessages: StateFlow<List<ChatMessage>> = _activeSessionId
        .flatMapLatest { id ->
            if (id != null) repository.getMessagesForSessionFlow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- App Level Settings & Hubs ---
    private val _primaryModel = MutableStateFlow("gemini-3.5-flash")
    val primaryModel = _primaryModel.asStateFlow()

    private val _byokApiKey = MutableStateFlow("")
    val byokApiKey = _byokApiKey.asStateFlow()

    private val _themeSelection = MutableStateFlow("Frosted Glass")
    val themeSelection = _themeSelection.asStateFlow()

    // --- UI UI-Only states ---
    private val _isGeneratingCharacter = MutableStateFlow(false)
    val isGeneratingCharacter = _isGeneratingCharacter.asStateFlow()

    private val _isGeneratingResponse = MutableStateFlow(false)
    val isGeneratingResponse = _isGeneratingResponse.asStateFlow()

    init {
        // Load initial settings
        viewModelScope.launch {
            _byokApiKey.value = repository.getSettingValue("byok_gemini_key", "")
            _primaryModel.value = repository.getSettingValue("primary_model_override", "gemini-3.5-flash")
            _themeSelection.value = repository.getSettingValue("active_theme", "Frosted Glass")

            // Create initial placeholder character if database is completely empty so user starts with a masterpiece!
            repository.allCharacters.firstOrNull()?.let { chars ->
                if (chars.isEmpty()) {
                    createDefaultMasterpieceCharacters()
                }
            }
        }
    }

    private fun createDefaultMasterpieceCharacters() {
        viewModelScope.launch {
            val elara = Character(
                name = "Elara",
                displayName = "The Starweaver",
                description = "Elara is an ancient, immortal astronomer who resides in a crumbling celestial observatory at the edge of space. She watches dying stars go out and meticulously maps the dying cosmos.",
                personality = "Wise, deeply nostalgic, dryly sarcastic, protective of her ancient lorebooks. She is cautious about strangers but possesses deep empathy for those lost in the universal twilight.",
                firstMessage = "*The grand brass dome of the observatory creaks open to reveal a panoramic view of the fading cosmos. Elara stands before a glowing, ancient star map, her fingers gently tracing a dead star.* \"You see this star? It hasn't burned for ten thousand years. And yet, here on my parchment, it still glows. Tell me, wanderer, does carrying ghosts make you strong, or simply heavy?\"",
                scenario = "A dying universe where star after star is winking out into total eternal dark, and {{user}} finds refuge in her sanctuary.",
                creatorNotes = "Best with high temperature (1.1 - 1.3) for stellar poetic descriptions.",
                exampleDialogues = "<START>\n{{user}}: Tell me what lies beyond.\n{{char}}: *smiles melancholically* \"Endings, child. And the beautiful space between them.\"",
                alternateGreetingsJson = "[\"\\\"The market of the sky has closed, yet you still shop for light,\\\" Elara murmurs, not turning from her telescope.\",\"\\\"The constellations are moving out of order,\\\" Elara warns, tracing a geometric rift in the starry glass.\"]"
            )
            val cyberDaemon = Character(
                name = "Kaelen",
                displayName = "VoltPoet Cyber-Hacker",
                description = "Kaelen is a jaded cyber-runner in Neo-Seoul. Above his high-frequency neural augments, he writes underground digital poetry on terminal protocols.",
                personality = "Rebellious, highly intuitive, protective, snappy, hiding a deeply reflective poetic heart under severe military augments.",
                firstMessage = "*Heavy rain beats against the alloy shutters of the neon-drenched alley. Kaelen rests against a server rack, sparks sputtering from his prosthetic arm's power node as he writes lines of green text on a retro deck.* \"Code is just poetry that actually executes. What brings you to my terminal node, organic?\"",
                scenario = "Neo-Seoul under corporate total lock. You are both runaway cyber-runners seeking shelter.",
                creatorNotes = "Excellent with short futuristic quips.",
                exampleDialogues = "<START>\n{{user}}: Can you hack this?\n{{char}}: *cracks digital knuckles* \"Give me three ticks of a CPU cycle and watch the grid black out.\""
            )

            val elaraId = repository.insertCharacter(elara)
            val kaelenId = repository.insertCharacter(cyberDaemon)

            // Insert initial lorebooks for Elara
            repository.saveLorebookEntry(
                LorebookEntry(
                    characterId = elaraId.toInt(),
                    name = "The Dead Star of Vyreth",
                    keysCsv = "vyreth, dead star, dead stars",
                    content = "A critical constellation nexus that went cold 10,000 years past. It was rumoured to hold instructions regarding the Cosmic Void Prophecy."
                )
            )

            // Auto-select Elara to begin with
            selectCharacter(elaraId.toInt())
        }
    }

    fun selectCharacter(id: Int) {
        _selectedCharacterId.value = id
        // Automatically fetch or create a chat session for this character
        viewModelScope.launch {
            val sessionsFlow = repository.getSessionsForCharacter(id).firstOrNull() ?: emptyList()
            if (sessionsFlow.isNotEmpty()) {
                _activeSessionId.value = sessionsFlow.first().id
            } else {
                createNewChatSession(id)
            }
        }
    }

    fun createNewChatSession(characterId: Int) {
        viewModelScope.launch {
            val character = repository.getCharacterById(characterId) ?: return@launch
            val sessionId = repository.createChatSession(characterId, "Chat with ${character.name}")
            _activeSessionId.value = sessionId.toInt()

            // Insert Character's First Message as starting point
            repository.insertMessage(
                ChatMessage(
                    chatSessionId = sessionId.toInt(),
                    sender = "character",
                    text = character.firstMessage,
                    swipeIndex = 0,
                    swipesJson = repository.listToJson(listOf(character.firstMessage))
                )
            )
        }
    }

    // --- Message Interactions ---
    fun sendMessage(text: String) {
        val sessionId = _activeSessionId.value ?: return
        if (text.isBlank() || _isGeneratingResponse.value) return

        viewModelScope.launch {
            _isGeneratingResponse.value = true
            // Save user message in DB
            repository.insertMessage(
                ChatMessage(
                    chatSessionId = sessionId,
                    sender = "user",
                    text = text,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Generate character response
            repository.generateResponse(sessionId, text)
            _isGeneratingResponse.value = false
        }
    }

    fun swipeMessage(message: ChatMessage, direction: Int) {
        val sessionId = _activeSessionId.value ?: return
        val currentSwipes = repository.jsonToList(message.swipesJson)
        val newIndex = message.swipeIndex + direction

        if (newIndex in currentSwipes.indices) {
            // Already generated swipe exists, just slide index
            viewModelScope.launch {
                val updated = message.copy(
                    text = currentSwipes[newIndex],
                    swipeIndex = newIndex
                )
                repository.updateMessage(updated)
            }
        } else if (direction > 0) {
            // User requested generating a NEW swipe!
            triggerRegeneration(message)
        }
    }

    fun triggerRegeneration(message: ChatMessage) {
        val sessionId = _activeSessionId.value ?: return
        if (_isGeneratingResponse.value) return

        viewModelScope.launch {
            _isGeneratingResponse.value = true
            // Query previous messages up to this message's timestamp
            repository.generateResponse(
                sessionId = sessionId,
                userPrompt = "",
                isRegeneration = true,
                regResourceMessageId = message.id
            )
            _isGeneratingResponse.value = false
        }
    }

    fun deleteMessage(message: ChatMessage) {
        viewModelScope.launch {
            repository.deleteMessageById(message.id)
        }
    }

    fun clearChatHistory() {
        val sessionId = _activeSessionId.value ?: return
        val charId = _selectedCharacterId.value ?: return
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            createNewChatSession(charId)
        }
    }

    // --- AI Generator Tab Actions ---
    fun aiGenerateCharacter(descriptionIdea: String, onSuccess: () -> Unit) {
        if (descriptionIdea.isBlank() || _isGeneratingCharacter.value) return
        viewModelScope.launch {
            _isGeneratingCharacter.value = true
            try {
                val generated = repository.generateCompleteCharacter(descriptionIdea)
                val newId = repository.insertCharacter(generated)
                selectCharacter(newId.toInt())
                onSuccess()
            } catch (e: Exception) {
                // handle error
            } finally {
                _isGeneratingCharacter.value = false
            }
        }
    }

    fun manuallyCreateLocalCharacter(character: Character) {
        viewModelScope.launch {
            val newId = repository.insertCharacter(character)
            selectCharacter(newId.toInt())
        }
    }

    fun manuallyUpdateLocalCharacter(character: Character) {
        viewModelScope.launch {
            repository.updateCharacter(character)
        }
    }

    fun manuallyDeleteLocalCharacter(character: Character) {
        viewModelScope.launch {
            repository.deleteCharacter(character)
            // select dynamic default if available
            repository.allCharacters.firstOrNull()?.let { chars ->
                val remaining = chars.filter { it.id != character.id }
                if (remaining.isNotEmpty()) {
                    selectCharacter(remaining.first().id)
                }
            }
        }
    }

    // --- Lorebook Management ---
    fun saveLorebook(entry: LorebookEntry) {
        viewModelScope.launch {
            repository.saveLorebookEntry(entry)
        }
    }

    fun deleteLorebook(entry: LorebookEntry) {
        viewModelScope.launch {
            repository.deleteLorebookEntry(entry)
        }
    }

    // --- Provider and Theme customizers ---
    fun updateByokKey(key: String) {
        _byokApiKey.value = key
        viewModelScope.launch {
            repository.saveSetting("byok_gemini_key", key)
        }
    }

    fun updatePrimaryModel(model: String) {
        _primaryModel.value = model
        viewModelScope.launch {
            repository.saveSetting("primary_model_override", model)
        }
    }

    fun updateTheme(theme: String) {
        _themeSelection.value = theme
        viewModelScope.launch {
            repository.saveSetting("active_theme", theme)
        }
    }
}
