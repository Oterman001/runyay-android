package com.oterman.rundemo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    // 品牌色
    primary = RunBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EFFF),
    onPrimaryContainer = Color(0xFF001E2E),
    secondary = RunOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEDD0),
    onSecondaryContainer = Color(0xFF2E1500),
    tertiary = StatusSuccess,
    onTertiary = Color.White,
    tertiaryContainer = StatusSuccessContainer,
    onTertiaryContainer = Color(0xFF002111),
    error = StatusDestructive,
    onError = Color.White,
    errorContainer = StatusDestructiveContainer,
    onErrorContainer = Color(0xFF410002),
    // 背景与表面
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF8E8E93),
    // 边框与分割线
    outline = Color(0xFFC7C7CC),
    outlineVariant = Color(0xFFE5E5EA),
    // 反色（Snackbar、Toast 等）
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = RunBlue,
    // 容器扩展
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F2F7),
    surfaceContainerHigh = Color(0xFFE5E5EA),
    surfaceContainerHighest = Color(0xFFD1D1D6),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE5E5EA),
)

private val DarkColorScheme = darkColorScheme(
    // 品牌色
    primary = RunBlue,
    onPrimary = Color(0xFF003350),
    primaryContainer = Color(0xFF004A72),
    onPrimaryContainer = Color(0xFFD6EFFF),
    secondary = RunOrange,
    onSecondary = Color(0xFF2E1500),
    secondaryContainer = Color(0xFF3D2000),
    onSecondaryContainer = Color(0xFFFFDDB5),
    tertiary = StatusSuccess,
    onTertiary = Color(0xFF002111),
    tertiaryContainer = StatusSuccessContainerDark,
    onTertiaryContainer = Color(0xFFB8F0C8),
    error = StatusDestructive,
    onError = Color(0xFF690005),
    errorContainer = StatusDestructiveContainerDark,
    onErrorContainer = Color(0xFFFFDAD6),
    // 背景与表面
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF8E8E93),
    // 边框与分割线
    outline = Color(0xFF48484A),
    outlineVariant = Color(0xFF3A3A3C),
    // 反色（Snackbar、Toast 等）
    inverseSurface = Color(0xFFFFFFFF),
    inverseOnSurface = Color(0xFF1C1C1E),
    inversePrimary = RunBlue,
    // 容器扩展
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerLow = Color(0xFF000000),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF3A3A3C),
    surfaceBright = Color(0xFF2C2C2E),
    surfaceDim = Color(0xFF000000),
)

object RunTheme {
    val colorScheme: RunColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalRunColorScheme.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalIsDarkTheme.current
}

@Composable
fun ComopseDemoHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    val runColorScheme = if (darkTheme) DarkRunColorScheme else LightRunColorScheme

    // 禁用系统字体缩放，fontScale 固定为 1f
    val currentDensity = LocalDensity.current
    val fixedDensity = Density(density = currentDensity.density, fontScale = 1f)

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalRunColorScheme provides runColorScheme,
        LocalDensity provides fixedDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
