package com.example.swartlandathletics.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SwartlandColorScheme = darkColorScheme(
    primary = LssGold,
    onPrimary = LssNavyHeader,
    primaryContainer = LssGoldDark,
    onPrimaryContainer = LssTextWhite,
    secondary = LssGoldLight,
    onSecondary = LssNavyHeader,
    background = LssNavyDark,
    onBackground = LssTextLight,
    surface = LssNavyCard,
    onSurface = LssTextLight,
    surfaceVariant = LssNavyCardLight,
    onSurfaceVariant = LssTextLight,
    error = LssRedError,
    onError = LssTextWhite
)

@Composable
fun SwartlandTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = SwartlandColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = LssNavyHeader.toArgb()
                window.navigationBarColor = LssNavyDark.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
