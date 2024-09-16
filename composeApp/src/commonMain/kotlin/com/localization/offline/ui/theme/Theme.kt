package com.localization.offline.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDirection

private val AppColorScheme = lightColorScheme(
    background = Color(0,213, 0, 40),
    primary = Color(0,213, 0, 120),
    onPrimary = Color.Black
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    ProvideTextStyle(LocalTextStyle.current.copy(textDirection = TextDirection.Content)) {
        MaterialTheme(
            colorScheme = AppColorScheme,
            content = content,
        )
    }
}