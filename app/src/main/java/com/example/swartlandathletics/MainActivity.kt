package com.example.swartlandathletics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swartlandathletics.ui.screens.*
import com.example.swartlandathletics.ui.theme.LssGold
import com.example.swartlandathletics.ui.theme.LssNavyDark
import com.example.swartlandathletics.ui.theme.LssNavyHeader
import com.example.swartlandathletics.ui.theme.LssTextMuted
import com.example.swartlandathletics.ui.theme.SwartlandTheme

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Tuis", Icons.Default.Home)
    object SetupImport : Screen("setup_import", "Inskrywings", Icons.Default.Assignment)
    object DrawSheets : Screen("draw_sheets", "Lotings", Icons.Default.Description)
    object Officiating : Screen("officiating", "Beoordeling", Icons.Default.Timer)
    object Records : Screen("records", "Rekords", Icons.Default.EmojiEvents)
    object LiveFeed : Screen("live_feed", "Live", Icons.Default.Leaderboard)
    object Medals : Screen("medals", "Medaljes", Icons.Default.MilitaryTech)
    object Reports : Screen("reports", "Verslae", Icons.Default.Assessment)
    object Program : Screen("program", "Program", Icons.Default.CalendarMonth)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SwartlandTheme {
                MainAppContent()
            }
        }
    }
}

@Composable
fun MainAppContent() {
    var currentScreen by remember { mutableStateOf("home") }
    var officiatingEvent by remember { mutableStateOf<String?>(null) }
    var officiatingGender by remember { mutableStateOf<String?>(null) }
    var officiatingAge by remember { mutableStateOf<String?>(null) }

    // Navigation back stack handling
    BackHandler(enabled = currentScreen != "home") {
        currentScreen = "home"
    }

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.DrawSheets,
        Screen.Officiating,
        Screen.LiveFeed,
        Screen.Medals
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(LssNavyDark),
        bottomBar = {
            NavigationBar(
                containerColor = LssNavyHeader,
                contentColor = LssGold,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = currentScreen == item.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = item.route },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (isSelected) LssGold else LssTextMuted
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                color = if (isSelected) LssGold else LssTextMuted,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = LssNavyDark
                        ),
                        modifier = Modifier.testTag("nav_bar_${item.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                "home" -> HomeScreen(
                    onNavigateTo = { route -> currentScreen = route }
                )
                "setup_import" -> SetupImportScreen(
                    onNavigateBack = { currentScreen = "home" }
                )
                "draw_sheets" -> DrawSheetsScreen(
                    onNavigateBack = { currentScreen = "home" },
                    onNavigateToOfficiating = { ev, g, a ->
                        officiatingEvent = ev
                        officiatingGender = g
                        officiatingAge = a
                        currentScreen = "officiating"
                    }
                )
                "officiating" -> OfficiatingScreen(
                    initialEvent = officiatingEvent,
                    initialGender = officiatingGender,
                    initialAge = officiatingAge,
                    onNavigateBack = { currentScreen = "home" }
                )
                "records" -> RecordsScreen(
                    onNavigateBack = { currentScreen = "home" }
                )
                "live_feed" -> LiveFeedScreen(
                    onNavigateBack = { currentScreen = "home" }
                )
                "medals" -> MedalsScreen(
                    onNavigateBack = { currentScreen = "home" }
                )
                "reports" -> ReportsScreen(
                    onNavigateBack = { currentScreen = "home" }
                )
                "program" -> ProgramScreen(
                    onNavigateBack = { currentScreen = "home" }
                )
            }
        }
    }
}
