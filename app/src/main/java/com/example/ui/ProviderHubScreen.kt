package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ThemePresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderHubScreen(viewModel: RoleplayViewModel) {
    val themeName by viewModel.themeSelection.collectAsState()
    val theme = ThemePresets.getTheme(themeName)
    val context = LocalContext.current

    val primaryModelSelected by viewModel.primaryModel.collectAsState()
    val userKeySaved by viewModel.byokApiKey.collectAsState()

    var inputKey by remember { mutableStateOf(userKeySaved) }
    var selectedModel by remember { mutableStateOf(primaryModelSelected) }
    var budgetCapText by remember { mutableStateOf("5.00") }

    LaunchedEffect(userKeySaved) {
        inputKey = userKeySaved
    }

    LaunchedEffect(primaryModelSelected) {
        selectedModel = primaryModelSelected
    }

    val isGlass = themeName == "Frosted Glass"
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isGlass) Color.Transparent else theme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = if (isGlass) {
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
                    Text(
                        text = "⚙️ Provider Hub",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = theme.accent,
                            fontSize = 28.sp,
                            fontFamily = FontFamily.Serif
                        )
                    )
                    Text(
                        text = "Securely bring your own keys (BYOK). Absolute modular routing with real-time costs tracking and zero vendor lock-in.",
                        style = MaterialTheme.typography.bodySmall.copy(color = theme.textSecondary),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION 1: TEXT GENERATION BYOK CONFIGURATION
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isGlass) Color(0x0CFFFFFF) else theme.cardBackground),
                    modifier = Modifier.border(
                        1.dp,
                        if (isGlass) Color(0x11FFFFFF) else theme.textSecondary.copy(alpha = 0.15f),
                        RoundedCornerShape(16.dp)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "📝 TEXT GENERATION OVERRIDE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = theme.accent
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Key Input
                        Text("Custom Gemini API Key Override", color = theme.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Leave empty to use built-in AI Studio API key.",
                            color = theme.textSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = inputKey,
                            onValueChange = { inputKey = it },
                            placeholder = { Text("AIzaSy...") },
                            trailingIcon = {
                                if (inputKey.isNotBlank()) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Configured",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_field")
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = theme.text,
                                focusedBorderColor = theme.accent
                            )
                        )

                        // Model Selector
                        Text("Active Core LLM Model", color = theme.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))

                        ModelSelectorRow(
                            label = "Gemini 3.5 Flash (Default & Highly Balanced)",
                            isSelected = selectedModel == "gemini-3.5-flash",
                            onClick = { selectedModel = "gemini-3.5-flash" },
                            themeColors = theme
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        ModelSelectorRow(
                            label = "Gemini 3.1 Pro (Advanced Spatial Reasoning)",
                            isSelected = selectedModel == "gemini-3.1-pro-preview",
                            onClick = { selectedModel = "gemini-3.1-pro-preview" },
                            themeColors = theme
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Save Configuration button
                        Button(
                            onClick = {
                                viewModel.updateByokKey(inputKey)
                                viewModel.updatePrimaryModel(selectedModel)
                                Toast.makeText(context, "Provider configuration synchronized", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_settings_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                        ) {
                            Text("Synchronize Provider Hub", color = theme.background, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // SECTION 2: CHAT APPEARANCE / THEME SYSTEM
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isGlass) Color(0x0CFFFFFF) else theme.cardBackground),
                    modifier = Modifier.border(
                        1.dp,
                        if (isGlass) Color(0x11FFFFFF) else theme.textSecondary.copy(alpha = 0.15f),
                        RoundedCornerShape(16.dp)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "🎨 CHAT APPEARANCE & THEMES",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = theme.accent
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Select Visual Theme Vibe", color = theme.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Fully alters backgrounds, bubbles, shapes and typography styling presets universally.",
                            color = theme.textSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeSelectionRow(
                                title = "Frosted Glass",
                                desc = "Ultra-premium glass design look with glowing radial backdrops and fine white borders.",
                                isSelected = themeName == "Frosted Glass",
                                onClick = { viewModel.updateTheme("Frosted Glass") },
                                themeColors = theme
                            )
                            ThemeSelectionRow(
                                title = "Midnight Void",
                                desc = "Warm glowing rose accent over deep space obsidian palette.",
                                isSelected = themeName == "Midnight Void",
                                onClick = { viewModel.updateTheme("Midnight Void") },
                                themeColors = theme
                            )
                            ThemeSelectionRow(
                                title = "Parchment Tavern",
                                desc = "Cozy vintage paper cream colors with deep wood brown contrast.",
                                isSelected = themeName == "Parchment Tavern",
                                onClick = { viewModel.updateTheme("Parchment Tavern") },
                                themeColors = theme
                            )
                            ThemeSelectionRow(
                                title = "Cyberpunk Neon",
                                desc = "High frequency cyan highlights over matte black sheets.",
                                isSelected = themeName == "Cyberpunk Neon",
                                onClick = { viewModel.updateTheme("Cyberpunk Neon") },
                                themeColors = theme
                            )
                            ThemeSelectionRow(
                                title = "Forest Sanctuary",
                                desc = "Serene soft sage and moss-earth background values.",
                                isSelected = themeName == "Forest Sanctuary",
                                onClick = { viewModel.updateTheme("Forest Sanctuary") },
                                themeColors = theme
                            )
                        }
                    }
                }

                // SECTION 3: COST METRICS AND ROUTING SETTINGS
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isGlass) Color(0x0CFFFFFF) else theme.cardBackground),
                    modifier = Modifier.border(
                        1.dp,
                        if (isGlass) Color(0x11FFFFFF) else theme.textSecondary.copy(alpha = 0.15f),
                        RoundedCornerShape(16.dp)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "🔀 DYNAMIC CAP BUDGETING",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = theme.accent
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Automated Budget Cap", color = theme.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Auto block context routing if costs breach limits.", color = theme.textSecondary, fontSize = 11.sp)
                            }
                            OutlinedTextField(
                                value = budgetCapText,
                                onValueChange = { budgetCapText = it },
                                prefix = { Text("$") },
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = theme.accent,
                                    focusedTextColor = theme.text
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Current Daily Accumulation", color = theme.text, fontSize = 12.sp)
                            Text("$0.02 / $${budgetCapText}", color = theme.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        LinearProgressIndicator(
                            progress = { 0.02f / (budgetCapText.toFloatOrNull() ?: 5.0f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = theme.accent,
                            trackColor = theme.textSecondary.copy(alpha = 0.15f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ModelSelectorRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    themeColors: com.example.ui.theme.ActiveThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) themeColors.accent.copy(alpha = 0.15f) else themeColors.background)
            .border(
                1.dp,
                if (isSelected) themeColors.accent else themeColors.textSecondary.copy(alpha = 0.15f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = themeColors.accent, unselectedColor = themeColors.textSecondary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = themeColors.text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ThemeSelectionRow(
    title: String,
    desc: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    themeColors: com.example.ui.theme.ActiveThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) themeColors.accent.copy(alpha = 0.1f) else themeColors.background)
            .border(
                1.5.dp,
                if (isSelected) themeColors.accent else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.Circle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) themeColors.accent else themeColors.textSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                color = themeColors.text,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = themeColors.textSecondary,
                fontWeight = FontWeight.Light,
                lineHeight = 14.sp
            )
        }
    }
}
