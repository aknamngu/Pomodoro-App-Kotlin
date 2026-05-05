package com.example.pomodoro2.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = MauNhanTym,
    background = NenToi, // Màu đặc 0xFF1A1A2E
    surface = Color(0xFF252538), // Màu Card tối đặc
    onBackground = ChuTrang,
    onSurface = ChuTrang
)

private val LightColorScheme = lightColorScheme(
    primary = MauNhanTym,
    background = NenSang, // Màu đặc 0xFFF8F9FF
    surface = Color.White, // Màu Card sáng đặc
    onBackground = ChuToi,
    onSurface = ChuToi
)

@Composable
fun LuminousFocusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
