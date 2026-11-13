package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF00390F),
    primaryContainer = FarmGreenDark,
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFFFFB74D),
    onSecondary = Color(0xFF4E2600),
    secondaryContainer = Color(0xFF703800),
    onSecondaryContainer = Color(0xFFFFDCC1),
    tertiary = Color(0xFFFF8A65),
    background = FarmSurfaceDark,
    surface = FarmCardBgDark,
    onBackground = Color(0xFFE2E3DF),
    onSurface = Color(0xFFE2E3DF),
    outline = Color(0xFF3F4940)
)

private val LightColorScheme = lightColorScheme(
    primary = FarmGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = FarmGreenLight,
    onPrimaryContainer = FarmGreenDark,
    secondary = EggGold,
    onSecondary = Color.White,
    secondaryContainer = EggGoldLight,
    onSecondaryContainer = EggGoldDark,
    tertiary = EarthTerracotta,
    onTertiary = Color.White,
    background = FarmSurface,
    surface = FarmCardBg,
    onBackground = NeutralTextPrimary,
    onSurface = NeutralTextPrimary,
    outline = NeutralBorder
)

@Composable
fun PoultryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted farm palette for brand identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
