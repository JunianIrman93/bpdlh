package id.lapor.lh.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LaporLHLightColors =
    lightColorScheme(
        primary = BluePrimaryLight,
        onPrimary = OnBluePrimaryLight,
        primaryContainer = BluePrimaryContainerLight,
        onPrimaryContainer = OnBluePrimaryContainerLight,
        secondary = BlueSecondaryLight,
        onSecondary = OnBlueSecondaryLight,
        secondaryContainer = BlueSecondaryContainerLight,
        onSecondaryContainer = OnBlueSecondaryContainerLight,
        tertiary = GreenTertiaryLight,
        onTertiary = OnGreenTertiaryLight,
        tertiaryContainer = GreenTertiaryContainerLight,
        onTertiaryContainer = OnGreenTertiaryContainerLight,
        error = ErrorLight,
        onError = OnErrorLight,
        errorContainer = ErrorContainerLight,
        onErrorContainer = OnErrorContainerLight,
        background = BackgroundLight,
        onBackground = OnBackgroundLight,
        surface = SurfaceLight,
        onSurface = OnSurfaceLight,
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = OnSurfaceVariantLight,
        outline = OutlineLight,
        outlineVariant = OutlineVariantLight,
        surfaceDim = SurfaceDimLight,
        surfaceBright = SurfaceBrightLight,
        surfaceContainerLowest = SurfaceContainerLowestLight,
        surfaceContainerLow = SurfaceContainerLowLight,
        surfaceContainer = SurfaceContainerLight,
        surfaceContainerHigh = SurfaceContainerHighLight,
        surfaceContainerHighest = SurfaceContainerHighestLight,
        scrim = ScrimColor,
        inverseSurface = OnBackgroundLight,
        inverseOnSurface = BackgroundLight,
        inversePrimary = BluePrimaryDark,
        surfaceTint = BluePrimaryLight,
    )

private val LaporLHDarkColors =
    darkColorScheme(
        primary = BluePrimaryDark,
        onPrimary = OnBluePrimaryDark,
        primaryContainer = BluePrimaryContainerDark,
        onPrimaryContainer = OnBluePrimaryContainerDark,
        secondary = BlueSecondaryDark,
        onSecondary = OnBlueSecondaryDark,
        secondaryContainer = BlueSecondaryContainerDark,
        onSecondaryContainer = OnBlueSecondaryContainerDark,
        tertiary = GreenTertiaryDark,
        onTertiary = OnGreenTertiaryDark,
        tertiaryContainer = GreenTertiaryContainerDark,
        onTertiaryContainer = OnGreenTertiaryContainerDark,
        error = ErrorDark,
        onError = OnErrorDark,
        errorContainer = ErrorContainerDark,
        onErrorContainer = OnErrorContainerDark,
        background = BackgroundDark,
        onBackground = OnBackgroundDark,
        surface = SurfaceDark,
        onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = OnSurfaceVariantDark,
        outline = OutlineDark,
        outlineVariant = OutlineVariantDark,
        surfaceDim = SurfaceDimDark,
        surfaceBright = SurfaceBrightDark,
        surfaceContainerLowest = SurfaceContainerLowestDark,
        surfaceContainerLow = SurfaceContainerLowDark,
        surfaceContainer = SurfaceContainerDark,
        surfaceContainerHigh = SurfaceContainerHighDark,
        surfaceContainerHighest = SurfaceContainerHighestDark,
        scrim = ScrimColor,
        inverseSurface = OnBackgroundDark,
        inverseOnSurface = BackgroundDark,
        inversePrimary = BluePrimaryLight,
        surfaceTint = BluePrimaryDark,
    )

@Composable
fun LaporLHTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) LaporLHDarkColors else LaporLHLightColors,
        typography = LaporLHTypography,
        shapes = LaporLHShapes,
        content = content,
    )
}
