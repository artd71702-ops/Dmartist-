package com.example.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

enum class ThemePreset(val displayName: String, val primaryColorHex: Long) {
    DEFAULT("Purple Slate", 0xFF6366F1),
    MIDNIGHT("Deep Midnight", 0xFF3B82F6),
    SEPIA("Warm Sepia", 0xFFD97706),
    EMERALD("Emerald Mint", 0xFF10B981),
    AMOLED("Pure AMOLED", 0xFF8B5CF6),
    SUNSET("Sunset Orange", 0xFFF97316)
}

object ThemeConfig {
    private const val PREFS_NAME = "omni_ai_theme_prefs"
    private const val KEY_MODE = "theme_mode"
    private const val KEY_PRESET = "theme_preset"

    fun getSavedThemeMode(context: Context): ThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeStr = prefs.getString(KEY_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return runCatching { ThemeMode.valueOf(modeStr) }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun saveThemeMode(context: Context, mode: ThemeMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun getSavedThemePreset(context: Context): ThemePreset {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val presetStr = prefs.getString(KEY_PRESET, ThemePreset.DEFAULT.name) ?: ThemePreset.DEFAULT.name
        return runCatching { ThemePreset.valueOf(presetStr) }.getOrDefault(ThemePreset.DEFAULT)
    }

    fun saveThemePreset(context: Context, preset: ThemePreset) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PRESET, preset.name).apply()
    }

    fun getColorScheme(darkTheme: Boolean, preset: ThemePreset): ColorScheme {
        return when (preset) {
            ThemePreset.DEFAULT -> if (darkTheme) DefaultDarkScheme else DefaultLightScheme
            ThemePreset.MIDNIGHT -> if (darkTheme) MidnightDarkScheme else MidnightLightScheme
            ThemePreset.SEPIA -> if (darkTheme) SepiaDarkScheme else SepiaLightScheme
            ThemePreset.EMERALD -> if (darkTheme) EmeraldDarkScheme else EmeraldLightScheme
            ThemePreset.AMOLED -> if (darkTheme) AmoledDarkScheme else DefaultLightScheme
            ThemePreset.SUNSET -> if (darkTheme) SunsetDarkScheme else SunsetLightScheme
        }
    }
}

// Color schemes
private val DefaultLightScheme = lightColorScheme(
    primary = Color(0xFF6366F1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF8B5CF6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF2E1065),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569)
)

private val DefaultDarkScheme = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF2E1065),
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFEDE9FE),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val MidnightLightScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    background = Color(0xFFF0F6FF),
    surface = Color.White,
    surfaceVariant = Color(0xFFE0EDFF),
    onSurfaceVariant = Color(0xFF1E293B)
)

private val MidnightDarkScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF172554),
    primaryContainer = Color(0xFF1E40AF),
    onPrimaryContainer = Color(0xFFDBEAFE),
    background = Color(0xFF0B132B),
    onBackground = Color(0xFFEDF2F7),
    surface = Color(0xFF1C2541),
    onSurface = Color(0xFFEDF2F7),
    surfaceVariant = Color(0xFF2D3748),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val SepiaLightScheme = lightColorScheme(
    primary = Color(0xFFD97706),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    background = Color(0xFFFFFBEB),
    onBackground = Color(0xFF451A03),
    surface = Color(0xFFFEF9C3),
    onSurface = Color(0xFF451A03),
    surfaceVariant = Color(0xFFFDE68A),
    onSurfaceVariant = Color(0xFF78350F)
)

private val SepiaDarkScheme = darkColorScheme(
    primary = Color(0xFFFBBF24),
    onPrimary = Color(0xFF451A03),
    primaryContainer = Color(0xFF92400E),
    onPrimaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF1C1917),
    onBackground = Color(0xFFFEF3C7),
    surface = Color(0xFF292524),
    onSurface = Color(0xFFFEF3C7),
    surfaceVariant = Color(0xFF44403C),
    onSurfaceVariant = Color(0xFFFDE68A)
)

private val EmeraldLightScheme = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    background = Color(0xFFF0FDF4),
    surface = Color.White,
    surfaceVariant = Color(0xFFDCFCE7),
    onSurfaceVariant = Color(0xFF14532D)
)

private val EmeraldDarkScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF064E3B),
    primaryContainer = Color(0xFF047857),
    onPrimaryContainer = Color(0xFFD1FAE5),
    background = Color(0xFF062016),
    onBackground = Color(0xFFECFDF5),
    surface = Color(0xFF0B3323),
    onSurface = Color(0xFFECFDF5),
    surfaceVariant = Color(0xFF134E35),
    onSurfaceVariant = Color(0xFFA7F3D0)
)

private val AmoledDarkScheme = darkColorScheme(
    primary = Color(0xFFA855F7),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF581C87),
    onPrimaryContainer = Color(0xFFF3E8FF),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFE5E7EB)
)

private val SunsetLightScheme = lightColorScheme(
    primary = Color(0xFFEA580C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDD5),
    onPrimaryContainer = Color(0xFF7C2D12),
    background = Color(0xFFFFF7ED),
    surface = Color.White,
    surfaceVariant = Color(0xFFFED7AA),
    onSurfaceVariant = Color(0xFF9A3412)
)

private val SunsetDarkScheme = darkColorScheme(
    primary = Color(0xFFFB923C),
    onPrimary = Color(0xFF7C2D12),
    primaryContainer = Color(0xFFC2410C),
    onPrimaryContainer = Color(0xFFFFEDD5),
    background = Color(0xFF1F110B),
    onBackground = Color(0xFFFFF7ED),
    surface = Color(0xFF311910),
    onSurface = Color(0xFFFFF7ED),
    surfaceVariant = Color(0xFF4A271B),
    onSurfaceVariant = Color(0xFFFED7AA)
)
