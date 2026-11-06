package com.example.swartlandathletics.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HomeScreen(
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    EventDashboardScreen(
        onNavigateTo = onNavigateTo,
        modifier = modifier
    )
}
