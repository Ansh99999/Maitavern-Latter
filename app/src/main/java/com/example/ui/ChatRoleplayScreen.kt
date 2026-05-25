package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.AgentState
import com.example.data.database.Character
import com.example.data.database.ChatMessage
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.ui.theme.ActiveThemeColors
import com.example.ui.theme.ThemePresets
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoleplayScreen(viewModel: RoleplayViewModel) {
    val themeName by viewModel.themeSelection.collectAsState()
    val theme = ThemePresets.getTheme(themeName)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val character by viewModel.selectedCharacter.collectAsState(initial = null)
    val agentState by viewModel.activeAgentState.collectAsState(initial = null)
    val messages by viewModel.activeMessages.collectAsState()
    val isGeneratingRes by viewModel.isGeneratingResponse.collectAsState()

    var inputMessageText by remember { mutableStateOf("") }
    var showStateSnapshot by remember { mutableStateOf(false) }
    var authorsNoteText by remember { mutableStateOf("") }
    var showAuthorsNoteDialog by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()

    // Smooth scroll to latest messages when list size updates
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    if (character == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Empty Selector",
                    modifier = Modifier.size(64.dp),
                    tint = theme.textSecondary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Select a character card on the first tab to begin roleplaying.", color = theme.textSecondary)
            }
        }
        return
    }

    val activeChar = character!!

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (themeName == "Frosted Glass") Color.Transparent else theme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Immersive Header
            Box(
                modifier = if (themeName == "Frosted Glass") {
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0x05FFFFFF))
                        .border(
                            width = 1.dp,
                            color = Color(0x0FFFFFFF),
                            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                        )
                } else {
                    Modifier
                        .fillMaxWidth()
                        .background(theme.cardBackground)
                }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small circular avatar
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(theme.accent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeChar.name.take(1).uppercase(),
                            color = theme.accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeChar.name,
                            style = TextStyle(
                                color = theme.text,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.Green)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Agent online | Memory Curator active",
                                style = TextStyle(
                                    color = theme.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Light
                                )
                            )
                        }
                    }

                    // Interactive Agentic Evolution State Snapshot Badge!
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.accent.copy(alpha = 0.15f))
                            .border(1.dp, theme.accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { showStateSnapshot = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("state_badge")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Mood",
                                tint = theme.accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${agentState?.mood ?: "Calm"} • ${agentState?.trust ?: 50}%",
                                style = TextStyle(
                                    color = theme.accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Authors Note Indicator Toggle
                    IconButton(
                        onClick = { showAuthorsNoteDialog = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.StickyNote2,
                            contentDescription = "Author's Note",
                            tint = if (authorsNoteText.isNotBlank()) theme.accent else theme.textSecondary
                        )
                    }
                }
            }

            // Message Flow area
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                itemsIndexed(messages) { index, msg ->
                    MessageBubbleRow(
                        message = msg,
                        themeColors = theme,
                        onSwipeLeft = { viewModel.swipeMessage(msg, -1) },
                        onSwipeRight = { viewModel.swipeMessage(msg, 1) },
                        onDelete = { viewModel.deleteMessage(msg) },
                        onRegenerate = { viewModel.triggerRegeneration(msg) },
                        onSpeak = {
                            // Synthesizer Speak Playback Demo Mock
                            Toast.makeText(context, "[TTS Synthesis Play]: ${msg.text.take(40)}...", Toast.LENGTH_SHORT).show()
                        },
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(msg.text))
                            Toast.makeText(context, "Copied response to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                if (isGeneratingRes) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(theme.cardBackground)
                                    .padding(vertical = 12.dp, horizontal = 16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = theme.accent,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Generating alternate reality...",
                                        style = TextStyle(
                                            color = theme.textSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(10.dp)) }
            }

            // quick action button row (Continue, Suggest/Impersonating, clear history)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // AI Appends to its last statement
                        if (!isGeneratingRes) {
                            scope.launch {
                                viewModel.sendMessage("Please continue your last statement smoothly.")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("continue_action_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Continue", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Continue", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        // Impersonate Mode: AI writes as prompt suggestion for User
                        if (!isGeneratingRes) {
                            scope.launch {
                                Toast.makeText(context, "Analysing state for suggestions...", Toast.LENGTH_SHORT).show()
                                inputMessageText = "*I wait a long moment, then softly murmur my reply...*"
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("suggest_action_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.text),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Suggest", modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Suggest Action", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { viewModel.clearChatHistory() },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("clear_action_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.textSecondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = "Clean", modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restart Story", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Primary Bottom Input layout
            Box(
                modifier = if (themeName == "Frosted Glass") {
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xE70F1115))
                        .border(
                            width = 1.dp,
                            color = Color(0x11FFFFFF),
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        )
                } else {
                    Modifier
                        .fillMaxWidth()
                        .background(theme.cardBackground)
                }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { Toast.makeText(context, "Voice simulation engaged...", Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Record", tint = theme.textSecondary)
                    }

                    IconButton(
                        onClick = { Toast.makeText(context, "Character PNG background card attachment selector", Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = theme.textSecondary)
                    }

                    OutlinedTextField(
                        value = inputMessageText,
                        onValueChange = { inputMessageText = it },
                        placeholder = { Text("Write your reply...") },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 120.dp)
                            .testTag("message_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.accent,
                            unfocusedBorderColor = if (themeName == "Frosted Glass") Color(0x19FFFFFF) else theme.textSecondary.copy(alpha = 0.2f),
                            focusedContainerColor = if (themeName == "Frosted Glass") Color(0x0FFFFFFF) else theme.background,
                            unfocusedContainerColor = if (themeName == "Frosted Glass") Color(0x08FFFFFF) else theme.background,
                            focusedTextColor = theme.text,
                            unfocusedTextColor = theme.text
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputMessageText.isNotBlank()) {
                                viewModel.sendMessage(inputMessageText)
                                inputMessageText = ""
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(theme.accent)
                            .testTag("send_button"),
                        enabled = inputMessageText.isNotBlank() && !isGeneratingRes
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Send", tint = theme.background)
                    }
                }
            }
        }

        // --- Custom floating Author's Note Injection Card ---
        if (showAuthorsNoteDialog) {
            AlertDialog(
                onDismissRequest = { showAuthorsNoteDialog = false },
                containerColor = theme.cardBackground,
                title = {
                    Text(
                        "Configure Author's Note Injection",
                        style = TextStyle(color = theme.text, fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Inject a floating context sentence at depths of memory. Incredibly powerful for steering plot themes or specific character behaviors temporarily.",
                            color = theme.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = authorsNoteText,
                            onValueChange = { authorsNoteText = it },
                            placeholder = { Text("e.g., [Theme: Make Elara's speaking style extremely mysterious containing cryptic cosmic riddles]") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .testTag("authors_note_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = theme.accent,
                                focusedTextColor = theme.text
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAuthorsNoteDialog = false }) {
                        Text("Save Injections", color = theme.accent)
                    }
                }
            )
        }

        // --- Interactive State Snapshot Dialog (Evolution Tracker) ---
        if (showStateSnapshot) {
            AlertDialog(
                onDismissRequest = { showStateSnapshot = false },
                containerColor = theme.cardBackground,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Evolution", tint = theme.accent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${activeChar.name} Evolution Tracker",
                            style = TextStyle(color = theme.text, fontWeight = FontWeight.Bold)
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Three background autonomous AI agents are constantly tracks physical mutations, moods, trust progress, inventory states, and relationship milestones as conversational segments build up.",
                            color = theme.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )

                        // Mood block
                        StateValueRow(label = "😊 Active Mood", value = agentState?.mood ?: "Calm / Melancholic", themeColors = theme)
                        // Trust Bar
                        Text(
                            "Trust Meter: ${agentState?.trust ?: 50}%",
                            color = theme.text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        LinearProgressIndicator(
                            progress = { (agentState?.trust ?: 50) / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = theme.accent,
                            trackColor = theme.textSecondary.copy(alpha = 0.2f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        StateValueRow(label = "📍 Spatial Location", value = agentState?.location ?: "Sanctuary", themeColors = theme)
                        StateValueRow(label = "📦 Inventory / Possessions", value = (agentState?.inventory?.ifBlank { "Celestial Map, Void Compass" } ?: "No items"), themeColors = theme)
                        StateValueRow(label = "⚡ Active Goal & Motivation", value = agentState?.activeGoal ?: "Trace dying cosmic signs", themeColors = theme)
                        StateValueRow(label = "🩹 Injuries / Status", value = agentState?.injuries ?: "Excellent health", themeColors = theme)
                        StateValueRow(label = "🏆 Relationship Milestone", value = agentState?.relationshipMilestone ?: "Wanderer Sync Status Established", themeColors = theme)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showStateSnapshot = false }) {
                        Text("Close Snapshot", color = theme.accent)
                    }
                }
            )
        }
    }
}

@Composable
fun StateValueRow(label: String, value: String, themeColors: ActiveThemeColors) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(themeColors.background)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(value, color = themeColors.text, fontSize = 12.sp, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
fun MessageBubbleRow(
    message: ChatMessage,
    themeColors: ActiveThemeColors,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onDelete: () -> Unit,
    onRegenerate: () -> Unit,
    onSpeak: () -> Unit,
    onCopy: () -> Unit
) {
    val isUser = message.sender == "user"

    // Parse swipes
    val swipes = remember(message.swipesJson) {
        try {
            val list = message.swipesJson
            // Since we can't directly call repository inside composable, we extract swipes array simply:
            // Custom parser for swipe array json string
            if (list.startsWith("[")) {
                list.removeSurrounding("[", "]")
                    .split("\",\"")
                    .map { it.trim('"').replace("\\\"", "\"") }
                    .filter { it.isNotBlank() }
            } else {
                listOf(message.text)
            }
        } catch (e: Exception) {
            listOf(message.text)
        }
    }

    val swipeNum = swipes.size
    val activeIdx = message.swipeIndex

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            val isGlass = themeColors.name == "Frosted Glass"
            Box(
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isUser) {
                            themeColors.userBubble
                        } else {
                            if (isGlass) Color(0x0EFFFFFF) else themeColors.aiBubble
                        }
                    )
                    .then(
                        if (isGlass && !isUser) {
                            Modifier.border(
                                width = 1.dp,
                                color = Color(0x11FFFFFF),
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 4.dp,
                                    bottomEnd = 16.dp
                                )
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                Column {
                    // Message Header (Sender name)
                    Text(
                        text = if (isUser) "You" else "Agent",
                        style = TextStyle(
                            color = themeColors.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Render beautifully with Markdown support simulation (Italic block highlights for physical actions)
                    RenderRoleplayText(
                        rawText = message.text,
                        themeColors = themeColors
                    )
                }
            }
        }

        // Swipes and Message Toolbar under bubble (Only for character / AI responses to prevent crowding!)
        if (!isUser) {
            Row(
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Swipe Indexer: e.g. "◀ 2/4 ▶"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    IconButton(
                        onClick = onSwipeLeft,
                        modifier = Modifier.size(24.dp),
                        enabled = activeIdx > 0
                    ) {
                        Icon(Icons.Default.ArrowBackIos, contentDescription = "Prev", modifier = Modifier.size(12.dp), tint = themeColors.textSecondary)
                    }
                    Text(
                        text = "${activeIdx + 1}/${java.lang.Math.max(swipeNum, 1)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themeColors.accent,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(
                        onClick = onSwipeRight,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next", modifier = Modifier.size(12.dp), tint = themeColors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Toolbar Actions row (Speaker/TTS, Copy, Regenerate, Delete)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speak response",
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onSpeak() },
                        tint = themeColors.textSecondary
                    )

                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy text",
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { onCopy() },
                        tint = themeColors.textSecondary
                    )

                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate swipe option",
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onRegenerate() },
                        tint = themeColors.textSecondary
                    )

                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete block",
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onDelete() },
                        tint = themeColors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun RenderRoleplayText(rawText: String, themeColors: ActiveThemeColors) {
    // Custom regex simulation: styling text between *asterisks* in rich italics
    val segments = remember(rawText) {
        val parts = mutableListOf<Pair<String, Boolean>>() // text, isItalic
        val regex = "\\*([^*]+)\\*".toRegex()
        var lastIdx = 0
        regex.findAll(rawText).forEach { match ->
            if (match.range.first > lastIdx) {
                parts.add(rawText.substring(lastIdx, match.range.first) to false)
            }
            parts.add(match.groupValues[1] to true)
            lastIdx = match.range.last + 1
        }
        if (lastIdx < rawText.length) {
            parts.add(rawText.substring(lastIdx) to false)
        }
        if (parts.isEmpty()) {
            parts.add(rawText to false)
        }
        parts
    }

    Text(
        text = buildAnnotatedString {
            segments.forEach { (str, isItalic) ->
                if (isItalic) {
                    withStyle(
                        style = androidx.compose.ui.text.SpanStyle(
                            fontStyle = FontStyle.Italic,
                            color = themeColors.textSecondary // softer visual density for actions
                        )
                    ) {
                        append("*$str*")
                    }
                } else {
                    append(str)
                }
            }
        },
        style = TextStyle(
            color = themeColors.text,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal
        )
    )
}
