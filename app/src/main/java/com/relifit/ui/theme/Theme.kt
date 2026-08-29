package com.relifit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 主题扩展色（Demo 自定义色，Material3 之外的颜色）
 */
data class ExtraColors(
    val success: Color,   // 增长/完成绿
    val amber: Color,     // 时长强调
    val heroGradient: List<Color> // Hero 卡片渐变
)

val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(Color.Transparent, Color.Transparent, emptyList())
}

/** 深色方案（Demo 深色主色 #A8CCEB / 背景 #0F1215） */
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceContainer,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkSurfaceContainerHigh
)

/** 浅色方案（Demo 浅色主色 #33668F / 背景 #F2F5F8） */
private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceContainer,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightSurfaceContainerHigh
)

/**
 * 应用主题入口：根据 darkTheme 参数（由 DataStore 主题设置驱动）选择配色
 * @param darkTheme true=深色 false=浅色
 */
@Composable
fun ReliFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extra = if (darkTheme) ExtraColors(
        success = DarkSuccess,
        amber = DarkAmber,
        heroGradient = listOf(Color(0xFF2A4660), Color(0xFF1E2B38), Color(0xFF18212B))
    ) else ExtraColors(
        success = LightSuccess,
        amber = LightAmber,
        heroGradient = listOf(Color(0xFFE9F2FB), Color(0xFFD9E8F6), Color(0xFFCCDFF0))
    )

    CompositionLocalProvider(LocalExtraColors provides extra) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
