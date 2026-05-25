package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.data.database.AgentState
import com.example.data.database.Character
import com.example.data.database.ChatMessage
import com.example.data.database.LorebookEntry
import com.example.ui.theme.ThemePresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaiTavernApp(viewModel: RoleplayViewModel = viewModel()) {
    val themeName by viewModel.themeSelection.collectAsState()
    val theme = ThemePresets.getTheme(themeName)

    val navController = rememberNavController()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = theme.background,
        contentColor = theme.text
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (themeName == "Frosted Glass") {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f)
                    val radius = size.height * 0.7f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x226366F1), // Elegant Indigo Radial glow from HTML (rgba(99,102,241,0.1) approx)
                                Color.Transparent
                            ),
                            center = centerOffset,
                            radius = radius
                        ),
                        center = centerOffset,
                        radius = radius
                    )
                }
            }

            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    NavigationBar(
                        containerColor = if (themeName == "Frosted Glass") Color(0xE70F1115) else theme.cardBackground,
                        contentColor = theme.textSecondary,
                        modifier = if (themeName == "Frosted Glass") {
                            Modifier.border(
                                width = 1.dp,
                                color = Color(0x0FFFFFFF), // White border with very tiny alpha
                                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                            )
                        } else {
                            Modifier
                        }
                    ) {
                        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

                        NavigationBarItem(
                            selected = currentRoute == "characters",
                            onClick = {
                                navController.navigate("characters") {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Characters") },
                            label = { Text("Characters") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = theme.accent,
                                selectedTextColor = theme.accent,
                                indicatorColor = if (themeName == "Frosted Glass") Color(0x33FFFFFF) else theme.background,
                                unselectedIconColor = theme.textSecondary,
                                unselectedTextColor = theme.textSecondary
                            )
                        )
                        NavigationBarItem(
                            selected = currentRoute == "chat",
                            onClick = {
                                navController.navigate("chat") {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Active Chat") },
                            label = { Text("Chat") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = theme.accent,
                                selectedTextColor = theme.accent,
                                indicatorColor = if (themeName == "Frosted Glass") Color(0x33FFFFFF) else theme.background,
                                unselectedIconColor = theme.textSecondary,
                                unselectedTextColor = theme.textSecondary
                            )
                        )
                        NavigationBarItem(
                            selected = currentRoute == "lorebook",
                            onClick = {
                                navController.navigate("lorebook") {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.MenuBook, contentDescription = "Lorebook") },
                            label = { Text("Lorebook") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = theme.accent,
                                selectedTextColor = theme.accent,
                                indicatorColor = if (themeName == "Frosted Glass") Color(0x33FFFFFF) else theme.background,
                                unselectedIconColor = theme.textSecondary,
                                unselectedTextColor = theme.textSecondary
                            )
                        )
                        NavigationBarItem(
                            selected = currentRoute == "settings",
                            onClick = {
                                navController.navigate("settings") {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.Tune, contentDescription = "Settings") },
                            label = { Text("Provider Hub") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = theme.accent,
                                selectedTextColor = theme.accent,
                                indicatorColor = if (themeName == "Frosted Glass") Color(0x33FFFFFF) else theme.background,
                                unselectedIconColor = theme.textSecondary,
                                unselectedTextColor = theme.textSecondary
                            )
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "characters",
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("characters") {
                        CharactersHubScreen(viewModel, onNavigateToChat = {
                            navController.navigate("chat")
                        })
                    }
                    composable("chat") {
                        ChatRoleplayScreen(viewModel)
                    }
                    composable("lorebook") {
                        LorebookScreen(viewModel)
                    }
                    composable("settings") {
                        ProviderHubScreen(viewModel)
                    }
                }
            }
        }
    }
}
