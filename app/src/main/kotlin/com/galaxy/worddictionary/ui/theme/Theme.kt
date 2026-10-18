package com.galaxy.worddictionary.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Shapes
import androidx.wear.compose.material3.Typography
import androidx.wear.compose.material3.dynamicColorScheme

/**
 * 品牌色兜底 ColorScheme（Wear M3 动态配色不可用/不支持时使用）。
 *
 * 配色取自 HTML 交互原型（背单词 MVP）：深色表盘 + 薰衣草紫主色，
 * 认识/不认识沿用原型的薄荷绿与珊瑚红。
 */
internal val DefaultColorScheme = ColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    secondary = Color(0xFFB5EAD7),
    onSecondary = Color(0xFF00382A),
    tertiary = Color(0xFFFFB4AB),
    onTertiary = Color(0xFF690005),
    background = Color(0xFF0E0C11),
    surfaceContainerLow = Color(0xFF0E0C11),
    surfaceContainer = Color(0xFF16141B),
    surfaceContainerHigh = Color(0xFF1E1B24),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFF938F99),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    outline = Color(0xFF938F99)
)

@Composable
fun WordDictionaryTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // Wear M3 动态配色：旧设备/不支持时返回 null → 品牌色兜底
    val scheme = dynamicColorScheme(context) ?: DefaultColorScheme
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}
