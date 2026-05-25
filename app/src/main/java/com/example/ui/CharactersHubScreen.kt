package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Character
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.theme.ActiveThemeColors
import com.example.ui.theme.ThemePresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharactersHubScreen(
    viewModel: RoleplayViewModel,
    onNavigateToChat: () -> Unit
) {
    val themeName by viewModel.themeSelection.collectAsState()
    val theme = ThemePresets.getTheme(themeName)

    val characters by viewModel.allCharacters.collectAsState()
    val selectedCharId by viewModel.selectedCharacterId.collectAsState()
    val isGeneratingChar by viewModel.isGeneratingCharacter.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAiBuilder by remember { mutableStateOf(false) }
    var showManualBuilder by remember { mutableStateOf(false) }

    // Manual creation state fields
    var newName by remember { mutableStateOf("") }
    var newDisplayName by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newPers by remember { mutableStateOf("") }
    var newGreeting by remember { mutableStateOf("") }
    var newScenario by remember { mutableStateOf("") }
    var newNotes by remember { mutableStateOf("") }

    // AI Idea generation state
    var characterIdea by remember { mutableStateOf("") }

    val filteredCharacters = characters.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.displayName.contains(searchQuery, ignoreCase = true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (themeName == "Frosted Glass") Color.Transparent else theme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Section
            Box(
                modifier = if (themeName == "Frosted Glass") {
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0x05FFFFFF))
                        .border(
                            width = 1.dp,
                            color = Color(0x0FFFFFFF),
                            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                        )
                } else {
                    Modifier
                        .fillMaxWidth()
                        .background(theme.cardBackground)
                }
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MaiTavern",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = theme.accent,
                                fontSize = 32.sp,
                                fontFamily = FontFamily.Serif
                            )
                        )
                        IconButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(theme.accent)
                                .testTag("create_character_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Character",
                                tint = theme.background
                            )
                        }
                    }
                    Text(
                        text = "Your Mobile-First Autonomous Agent Universe",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = theme.textSecondary,
                            fontWeight = FontWeight.Light,
                            fontSize = 13.sp
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stunning Custom Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search system memories or character cards...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("search_bar"),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = theme.textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.accent,
                            unfocusedBorderColor = if (themeName == "Frosted Glass") Color(0x19FFFFFF) else theme.textSecondary.copy(alpha = 0.3f),
                            focusedContainerColor = if (themeName == "Frosted Glass") Color(0x0FFFFFFF) else theme.background,
                            unfocusedContainerColor = if (themeName == "Frosted Glass") Color(0x08FFFFFF) else theme.background,
                            focusedTextColor = theme.text,
                            unfocusedTextColor = theme.text
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Characters List Area
            if (filteredCharacters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonOutline,
                            contentDescription = "No results",
                            modifier = Modifier.size(72.dp),
                            tint = theme.textSecondary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No characters discovered in this sector.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = theme.textSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            "Click the (+) button at top-right to create or AI generate a new card instantly.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = theme.textSecondary.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    items(filteredCharacters) { character ->
                        val isSelected = character.id == selectedCharId
                        CharacterRowCard(
                            character = character,
                            isSelected = isSelected,
                            themeColors = theme,
                            onClick = {
                                viewModel.selectCharacter(character.id)
                                onNavigateToChat()
                            },
                            onDelete = {
                                viewModel.manuallyDeleteLocalCharacter(character)
                            }
                        )
                    }
                }
            }
        }

        // --- Custom Create Choice Bottom Sheet/Dialog ---
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                containerColor = theme.cardBackground,
                title = {
                    Text(
                        "Bring Character to Life",
                        style = TextStyle(color = theme.text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Deploy an autonomous agent inside your tavern universe. Choose builder interface:",
                            color = theme.textSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                showCreateDialog = false
                                showAiBuilder = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("select_ai_builder"),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = theme.background)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Assist Concept Generator", color = theme.background)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                showCreateDialog = false
                                showManualBuilder = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("select_manual_builder"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.text)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Manual")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Full Manual Character Spec")
                        }
                    }
                },
                confirmButton = {}
            )
        }

        // --- AI Assist Builder Dialog ---
        if (showAiBuilder) {
            AlertDialog(
                onDismissRequest = { if (!isGeneratingChar) showAiBuilder = false },
                containerColor = theme.cardBackground,
                title = {
                    Text(
                        "Describe Character Concept",
                        style = TextStyle(color = theme.text, fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Type any short visual concept, job, alignment, or personality traits. AI will generate immersive bio, lore constraints, scenario setup, greetings and tuning variables.",
                            color = theme.textSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = characterIdea,
                            onValueChange = { characterIdea = it },
                            placeholder = { Text("e.g. A grumpy dark elf blacksmith who works with molten mythril and speaks with a heavy Scottish accent.") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("ai_concept_input"),
                            enabled = !isGeneratingChar,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = theme.accent,
                                unfocusedBorderColor = theme.textSecondary,
                                focusedTextColor = theme.text,
                                unfocusedTextColor = theme.text
                            )
                        )
                        if (isGeneratingChar) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = theme.accent, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Engaging synthesis node...", color = theme.accent, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.aiGenerateCharacter(characterIdea) {
                                characterIdea = ""
                                showAiBuilder = false
                                onNavigateToChat()
                            }
                        },
                        enabled = characterIdea.isNotBlank() && !isGeneratingChar,
                        modifier = Modifier.testTag("ai_generate_confirm")
                    ) {
                        Text("Synthesize ✨", color = theme.accent)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAiBuilder = false },
                        enabled = !isGeneratingChar
                    ) {
                        Text("Cancel", color = theme.textSecondary)
                    }
                }
            )
        }

        // --- Manual Specification Dialog ---
        if (showManualBuilder) {
            AlertDialog(
                onDismissRequest = { showManualBuilder = false },
                containerColor = theme.cardBackground,
                title = {
                    Text(
                        "New Character Spec",
                        style = TextStyle(color = theme.text, fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("Alias Name (e.g. Elara)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = theme.accent, focusedTextColor = theme.text)
                            )
                            OutlinedTextField(
                                value = newDisplayName,
                                onValueChange = { newDisplayName = it },
                                label = { Text("Display Title (e.g. The Starweaver)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = theme.accent, focusedTextColor = theme.text)
                            )
                            OutlinedTextField(
                                value = newDesc,
                                onValueChange = { newDesc = it },
                                label = { Text("Story Description / Background") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .padding(vertical = 4.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = theme.accent, focusedTextColor = theme.text)
                            )
                            OutlinedTextField(
                                value = newPers,
                                onValueChange = { newPers = it },
                                label = { Text("Personality summary / Traits") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = theme.accent, focusedTextColor = theme.text)
                            )
                            OutlinedTextField(
                                value = newGreeting,
                                onValueChange = { newGreeting = it },
                                label = { Text("Starter Greeting Message") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .padding(vertical = 4.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = theme.accent, focusedTextColor = theme.text)
                            )
                            OutlinedTextField(
                                value = newScenario,
                                onValueChange = { newScenario = it },
                                label = { Text("Active Scenario context") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = theme.accent, focusedTextColor = theme.text)
                            )
                            OutlinedTextField(
                                value = newNotes,
                                onValueChange = { newNotes = it },
                                label = { Text("Advanced parameters / Creator Notes") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = theme.accent, focusedTextColor = theme.text)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val local = Character(
                                name = newName.ifBlank { "Stranger" },
                                displayName = newDisplayName.ifBlank { newName },
                                description = newDesc,
                                personality = newPers,
                                firstMessage = newGreeting.ifBlank { "Hello there." },
                                scenario = newScenario,
                                exampleDialogues = "<START>\n{{user}}: Tell me what lies beyond.\n{{char}}: *smiles melancholically* \"Endings, child.\"",
                                creatorNotes = newNotes
                            )
                            viewModel.manuallyCreateLocalCharacter(local)
                            showManualBuilder = false
                            onNavigateToChat()
                        },
                        enabled = newName.isNotBlank()
                    ) {
                        Text("Deploy 🚀", color = theme.accent)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualBuilder = false }) {
                        Text("Cancel", color = theme.textSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun CharacterRowCard(
    character: Character,
    isSelected: Boolean,
    themeColors: ActiveThemeColors,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isGlass = themeColors.name == "Frosted Glass"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) {
                    themeColors.accent
                } else if (isGlass) {
                    Color(0x11FFFFFF) // Ultra subtle premium glass border details
                } else {
                    themeColors.textSecondary.copy(alpha = 0.15f)
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGlass) Color(0x0CFFFFFF) else themeColors.cardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Virtual Custom Silhouette avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(themeColors.accent.copy(alpha = 0.8f), themeColors.accent.copy(alpha = 0.3f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = character.name.take(1).uppercase(),
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.text
                    )
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = character.name,
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.text
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(themeColors.accent.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "ACTIVE",
                                style = TextStyle(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = themeColors.accent
                                )
                            )
                        }
                    }
                }

                Text(
                    text = character.displayName,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColors.accent.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.padding(top = 1.dp)
                )

                Text(
                    text = character.description,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Light,
                        color = themeColors.textSecondary
                    ),
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp) // Touch targets
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete character",
                    tint = themeColors.textSecondary.copy(alpha = 0.6f)
                )
            }
        }
    }
}
