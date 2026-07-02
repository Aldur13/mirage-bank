package com.mirage.bank.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MiragePurple = Color(0xFF5B4FE9)
private val MiragePurpleDark = Color(0xFF8B7FFF)

private val LightColors = lightColorScheme(primary = MiragePurple)
private val DarkColors = darkColorScheme(primary = MiragePurpleDark)

/**
 * [useDarkTheme] should be driven by MeResponse.theme ("dark"|"light") once the
 * user is logged in; falls back to the system setting pre-login.
 */
@Composable
fun MirageBankTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
