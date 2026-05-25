package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.LorebookEntry
import androidx.compose.ui.text.TextStyle
import com.example.ui.theme.ThemePresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LorebookScreen(viewModel: RoleplayViewModel) {
    val themeName by viewModel.themeSelection.collectAsState()
    val theme = ThemePresets.getTheme(themeName)

    val character by viewModel.selectedCharacter.collectAsState(initial = null)
    val entries by viewModel.repository.allLorebookEntries.collectAsState(initial = emptyList())

    var showEditor by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<LorebookEntry?>(null) }

    // State parameters
    var name by remember { mutableStateOf("") }
    var keysCsv by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    // Filter to current character if selected
    val activeCharacterId = character?.id

    val displayedEntries = if (activeCharacterId == null) {
        entries
    } else {
        entries.filter { it.characterId == null || it.characterId == activeCharacterId }
    }

    LaunchedEffect(editingEntry) {
        if (editingEntry != null) {
            name = editingEntry!!.name
            keysCsv = editingEntry!!.keysCsv
            content = editingEntry!!.content
        } else {
            name = ""
            keysCsv = ""
            content = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (themeName == "Frosted Glass") Color.Transparent else theme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
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
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tavern Lorebook",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = theme.accent,
                                fontSize = 28.sp,
                                fontFamily = FontFamily.Serif
                            )
                        )

                        IconButton(
                            onClick = {
                                editingEntry = null
                                showEditor = true
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(theme.accent)
                                .testTag("add_lorebook_entry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Entry",
                                tint = theme.background
                            )
                        }
                    }

                    Text(
                        text = "World Lore, locations, and rules injected dynamically when keywords trigger in dialogue context.",
                        style = MaterialTheme.typography.bodySmall.copy(color = theme.textSecondary),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (character != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.accent.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Showing custom rules matched for active Companion: ${character!!.name}",
                                fontSize = 11.sp,
                                color = theme.accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Entries List
            if (displayedEntries.isEmpty()) {
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
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = "No pages",
                            modifier = Modifier.size(64.dp),
                            tint = theme.textSecondary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "The Lorebook is currently blank.",
                            color = theme.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Add trigger words (e.g. 'sword', 'nebula') and bio descriptions. Whenever characters mention those words, those rules inject instantly into context.",
                            style = MaterialTheme.typography.bodySmall.copy(color = theme.textSecondary.copy(alpha = 0.7f)),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                ) {
                    items(displayedEntries) { entry ->
                        LorebookCard(
                            entry = entry,
                            themeColors = theme,
                            onEdit = {
                                editingEntry = entry
                                showEditor = true
                            },
                            onDelete = {
                                viewModel.deleteLorebook(entry)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        // --- Lorebook Entry Editor Dialog ---
        if (showEditor) {
            AlertDialog(
                onDismissRequest = { showEditor = false },
                containerColor = theme.cardBackground,
                title = {
                    Text(
                        if (editingEntry == null) "Create Lorebook Block" else "Edit Lorebook Block",
                        style = TextStyle(color = theme.text, fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name of Subject (e.g., Mythril Forge)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lorebook_title_field")
                                .padding(vertical = 4.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = theme.text, focusedBorderColor = theme.accent)
                        )

                        OutlinedTextField(
                            value = keysCsv,
                            onValueChange = { keysCsv = it },
                            label = { Text("Trigger Keys (comma csv: e.g. forge, mythril)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lorebook_keys_field")
                                .padding(vertical = 4.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = theme.text, focusedBorderColor = theme.accent)
                        )

                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = { Text("Fact Summary context to inject") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .testTag("lorebook_content_field")
                                .padding(vertical = 4.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = theme.text, focusedBorderColor = theme.accent)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val newOrUpdated = LorebookEntry(
                                id = editingEntry?.id ?: 0,
                                characterId = activeCharacterId,
                                name = name,
                                keysCsv = keysCsv,
                                content = content
                            )
                            viewModel.saveLorebook(newOrUpdated)
                            showEditor = false
                        },
                        enabled = name.isNotBlank() && keysCsv.isNotBlank(),
                        modifier = Modifier.testTag("lorebook_save_confirm")
                    ) {
                        Text("Deploy Rule", color = theme.accent)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditor = false }) {
                        Text("Cancel", color = theme.textSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun LorebookCard(
    entry: LorebookEntry,
    themeColors: com.example.ui.theme.ActiveThemeColors,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isGlass = themeColors.name == "Frosted Glass"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(
                1.dp,
                if (isGlass) Color(0x11FFFFFF) else themeColors.textSecondary.copy(alpha = 0.15f),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isGlass) Color(0x0CFFFFFF) else themeColors.cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = themeColors.text
                )
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Block", tint = themeColors.textSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Block", tint = themeColors.textSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Keys
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "Trigger keys",
                    tint = themeColors.accent,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Keys: ${entry.keysCsv.split(",").joinToString(", ") { it.trim() }}",
                    fontSize = 11.sp,
                    color = themeColors.accent,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = entry.content,
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                color = themeColors.textSecondary,
                lineHeight = 16.sp
            )
        }
    }
}
