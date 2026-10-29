package com.cyz.wxyd.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 主题调色板（spec 第 5 节「界面范式 → 主题」、决策 D7）。
 *
 * 服务端 `user/updateTheme` 只认 7 个值（与服务端契约对齐，H5 跨端不丢样式）：
 * dark / light / eyecare / techblue / orange / pink / system。
 * 默认 `dark`（对齐 H5，紫罗兰 `#a78bfa` 系列为静态兜底）。
 *
 * 每套主题定义 light + dark 两个 [ColorScheme]（`system` 跟随系统在两套间切换）。
 * 色板方向：紫罗兰 `#a78bfa` 系列 + H5 暗色玻璃拟态神似映射
 * （深色背景偏紫蓝、卡片半透明描边走 `surfaceVariant`），求神似非像素级。
 */

/** 服务端认的 7 个主题值（顺序即 H5/服务端约定）。 */
val ALL_THEMES = listOf(
    "dark", "light", "eyecare", "techblue", "orange", "pink", "system",
)

// ---------------------------------------------------------------------------
// 暗色 ColorScheme（7 套）
// ---------------------------------------------------------------------------

private val DARK_SCHEMES: Map<String, ColorScheme> = mapOf(
    // dark：紫罗兰 #a78bfa 默认主色，深紫蓝玻璃拟态背景
    "dark" to darkColorScheme(
        primary = Color(0xFFa78bfa),
        secondary = Color(0xFFc4b5fd),
        tertiary = Color(0xFF818cf8),
        background = Color(0xFF15131F),
        surface = Color(0xFF1E1B2E),
        surfaceVariant = Color(0xFF2A2640),
    ),
    // light 主题在暗色系统下的紫罗兰深底
    "light" to darkColorScheme(
        primary = Color(0xFF8b5cf6),
        secondary = Color(0xFFa78bfa),
        tertiary = Color(0xFF7c3aed),
        background = Color(0xFF15131F),
        surface = Color(0xFF1E1B2E),
        surfaceVariant = Color(0xFF2A2640),
    ),
    // eyecare：护眼柔绿
    "eyecare" to darkColorScheme(
        primary = Color(0xFF8FB996),
        secondary = Color(0xFFA7C4A0),
        tertiary = Color(0xFF6FA88A),
        background = Color(0xFF141A14),
        surface = Color(0xFF1D241D),
        surfaceVariant = Color(0xFF293229),
    ),
    // techblue：科技蓝
    "techblue" to darkColorScheme(
        primary = Color(0xFF5AA9E6),
        secondary = Color(0xFF7DC4F0),
        tertiary = Color(0xFF4F8FD6),
        background = Color(0xFF0F1620),
        surface = Color(0xFF18222E),
        surfaceVariant = Color(0xFF22303F),
    ),
    // orange：暖橙
    "orange" to darkColorScheme(
        primary = Color(0xFFF5A65B),
        secondary = Color(0xFFF8BE85),
        tertiary = Color(0xFFE08A3C),
        background = Color(0xFF1A140D),
        surface = Color(0xFF241A10),
        surfaceVariant = Color(0xFF332616),
    ),
    // pink：粉红
    "pink" to darkColorScheme(
        primary = Color(0xFFF472B6),
        secondary = Color(0xFFF9A2CE),
        tertiary = Color(0xFFDB4E9A),
        background = Color(0xFF1A0F16),
        surface = Color(0xFF241420),
        surfaceVariant = Color(0xFF332033),
    ),
    // system：跟随系统，暗色态复用紫罗兰兜底
    "system" to darkColorScheme(
        primary = Color(0xFFa78bfa),
        secondary = Color(0xFFc4b5fd),
        tertiary = Color(0xFF818cf8),
        background = Color(0xFF15131F),
        surface = Color(0xFF1E1B2E),
        surfaceVariant = Color(0xFF2A2640),
    ),
)

// ---------------------------------------------------------------------------
// 浅色 ColorScheme（7 套）
// ---------------------------------------------------------------------------

private val LIGHT_SCHEMES: Map<String, ColorScheme> = mapOf(
    // dark 主题在浅色系统下的紫罗兰亮底
    "dark" to lightColorScheme(
        primary = Color(0xFF7C3AED),
        secondary = Color(0xFF8B5CF6),
        tertiary = Color(0xFF6D28D9),
        background = Color(0xFFF5F3FF),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFEDE9FE),
    ),
    // light：紫罗兰浅色主界面
    "light" to lightColorScheme(
        primary = Color(0xFF7C3AED),
        secondary = Color(0xFF8B5CF6),
        tertiary = Color(0xFF6D28D9),
        background = Color(0xFFF5F3FF),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFEDE9FE),
    ),
    // eyecare：护眼柔绿浅色
    "eyecare" to lightColorScheme(
        primary = Color(0xFF5E8C61),
        secondary = Color(0xFF7BA37E),
        tertiary = Color(0xFF4A6F4D),
        background = Color(0xFFF2F7EE),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE3EBDD),
    ),
    // techblue：科技蓝浅色
    "techblue" to lightColorScheme(
        primary = Color(0xFF2563EB),
        secondary = Color(0xFF3B82F6),
        tertiary = Color(0xFF1D4ED8),
        background = Color(0xFFF0F6FC),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE1ECF7),
    ),
    // orange：暖橙浅色
    "orange" to lightColorScheme(
        primary = Color(0xFFD97706),
        secondary = Color(0xFFEA8C1A),
        tertiary = Color(0xFFB45309),
        background = Color(0xFFFCF5EC),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF6E8D5),
    ),
    // pink：粉红浅色
    "pink" to lightColorScheme(
        primary = Color(0xFFDB2777),
        secondary = Color(0xFFEC4899),
        tertiary = Color(0xFFBE185D),
        background = Color(0xFFFDF2F7),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF8E3EE),
    ),
    // system：跟随系统，浅色态复用紫罗兰兜底
    "system" to lightColorScheme(
        primary = Color(0xFF7C3AED),
        secondary = Color(0xFF8B5CF6),
        tertiary = Color(0xFF6D28D9),
        background = Color(0xFFF5F3FF),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFEDE9FE),
    ),
)

/**
 * 根据主题名与系统深浅模式解析 [ColorScheme]。
 *
 * 规则：
 * - 未知值（7 值外）coerce 到 `"dark"`（spec：user/theme 返回 7 值外时 coerce 到 dark）；
 * - `"system"` 按 [darkSystem] 在紫罗兰暗/亮两套间切换；
 * - `"dark"` 始终暗色、`"light"` 始终亮色（名字即偏好）；
 * - 其余彩色主题（eyecare/techblue/orange/pink）跟随系统深浅 [darkSystem]。
 */
fun colorSchemeFor(theme: String, darkSystem: Boolean): ColorScheme {
    val resolved = if (theme in ALL_THEMES) theme else "dark"
    val useDark = when (resolved) {
        "light" -> false
        "dark" -> true
        else -> darkSystem
    }
    return (if (useDark) DARK_SCHEMES else LIGHT_SCHEMES).getValue(resolved)
}
