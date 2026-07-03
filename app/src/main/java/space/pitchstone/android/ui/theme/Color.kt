package space.pitchstone.android.ui.theme

import androidx.compose.ui.graphics.Color

object PitchstoneColors {
    val Background = Color(0xFF0B0D10)
    val Surface = Color(0xFF14171C)
    val SurfaceVariant = Color(0xFF1E2128)
    val Outline = Color(0xFF2E3340)

    val Accent = Color(0xFF56E0A0)
    val Danger = Color(0xFFE06D56)
    val Warn = Color(0xFFE0B356)
    val Info = Color(0xFF5690E0)

    val OnBackground = Color(0xFFF0F2F5)
    val OnSurface = Color(0xFFF0F2F5)
    val OnSurfaceVariant = Color(0xFF8A909A)
}

// Backward-compat aliases so old screens compile without changes
val SuccessGreen = PitchstoneColors.Accent
val ErrorRed = PitchstoneColors.Danger
val InfoBlue = PitchstoneColors.Info
