package app.rasoi.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import app.rasoi.core.designsystem.tokens.DarkColors
import app.rasoi.core.designsystem.tokens.LightColors

private val LocalAppColors = staticCompositionLocalOf { LightColors }
private val LocalAppTypography = staticCompositionLocalOf { AppTypography() }
private val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
private val LocalAppShapes = staticCompositionLocalOf { AppShapes() }
private val LocalAppMotion = staticCompositionLocalOf { AppMotion() }

/** Root theme: wraps Material 3 and exposes Rasoi tokens through [AppTheme.colors], [AppTheme.type] and friends. */
@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    val typography = AppTypography()
    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalAppSpacing provides AppSpacing(),
        LocalAppShapes provides AppShapes(),
        LocalAppMotion provides AppMotion(),
    ) {
        MaterialTheme(colorScheme = colors.toMaterial(), typography = typography.toMaterial(), content = content)
    }
}

object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current

    val type: AppTypography
        @Composable @ReadOnlyComposable
        get() = LocalAppTypography.current

    val spacing: AppSpacing
        @Composable @ReadOnlyComposable
        get() = LocalAppSpacing.current

    val shapes: AppShapes
        @Composable @ReadOnlyComposable
        get() = LocalAppShapes.current

    val motion: AppMotion
        @Composable @ReadOnlyComposable
        get() = LocalAppMotion.current
}

private fun AppColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondaryContainer = primaryContainer,
        onSecondaryContainer = onPrimaryContainer,
        background = surface,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surfaceVariant,
        surfaceContainerHigh = surfaceVariant,
        surfaceContainerHighest = surfaceVariant,
        outline = outline,
        outlineVariant = surfaceVariant,
        error = danger,
        onError = onDanger,
        errorContainer = dangerContainer,
        onErrorContainer = onDangerContainer,
    )
}

private fun AppTypography.toMaterial(): Typography = Typography(
    displaySmall = display,
    titleLarge = title,
    titleMedium = title,
    bodyLarge = body,
    bodyMedium = body,
    labelLarge = label,
    labelMedium = label,
    bodySmall = caption,
    labelSmall = caption,
)
