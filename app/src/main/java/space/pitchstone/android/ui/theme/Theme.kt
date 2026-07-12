package space.pitchstone.android.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PitchstoneDarkColorScheme = darkColorScheme(
    primary = PitchstoneColors.Accent,
    onPrimary = PitchstoneColors.Background,
    primaryContainer = Color(0xFF1A3D2E),
    onPrimaryContainer = PitchstoneColors.Accent,
    secondary = PitchstoneColors.Warn,
    onSecondary = PitchstoneColors.Background,
    secondaryContainer = Color(0xFF3D3218),
    onSecondaryContainer = PitchstoneColors.Warn,
    background = PitchstoneColors.Background,
    onBackground = PitchstoneColors.OnBackground,
    surface = PitchstoneColors.Surface,
    onSurface = PitchstoneColors.OnSurface,
    surfaceVariant = PitchstoneColors.SurfaceVariant,
    onSurfaceVariant = PitchstoneColors.OnSurfaceVariant,
    outline = PitchstoneColors.Outline,
    error = PitchstoneColors.Danger,
    onError = PitchstoneColors.Background,
    errorContainer = Color(0xFF3D1B16),
    onErrorContainer = PitchstoneColors.Danger,
    inverseSurface = PitchstoneColors.OnBackground,
    inverseOnSurface = PitchstoneColors.Background,
    inversePrimary = Color(0xFF1A3D2E),
    scrim = Color(0xCC000000),
)

@Composable
fun PitchstoneTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val backgroundArgb = PitchstoneColors.Background.toArgb()
            window.statusBarColor = backgroundArgb
            window.navigationBarColor = backgroundArgb
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = PitchstoneDarkColorScheme,
        typography = Typography,
        content = content
    )
}
