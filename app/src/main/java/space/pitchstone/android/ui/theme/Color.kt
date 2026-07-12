package space.pitchstone.android.ui.theme

import androidx.compose.ui.graphics.Color

object PitchstoneColors {
    val Background = Color(0xFF0B0D10)      // bg / screen
    val Surface = Color(0xFF13161B)         // surface / agent bubble
    val SurfaceVariant = Color(0xFF1D232C)  // user bubble
    val InputField = Color(0xFF171B21)      // input / field fill
    val Outline = Color(0xFF2E3340)

    val Accent = Color(0xFF56E0A0)          // accent / ok / CTA
    val Danger = Color(0xFFE06D56)          // over budget
    val Warn = Color(0xFFE0B356)            // warn (>80% budget)
    val Info = Color(0xFF5690E0)

    // Text ramp — E7EAEE → AEB6C2 → 8B93A1 → 5D6570
    val OnBackground = Color(0xFFE7EAEE)    // primary text
    val OnSurface = Color(0xFFE7EAEE)
    val TextSecondary = Color(0xFFAEB6C2)   // secondary — labels, recipients
    val OnSurfaceVariant = Color(0xFF8B93A1) // tertiary — subtitles, status meta
    val TextMuted = Color(0xFF5D6570)       // muted — eyebrows, field labels, metadata
}

// Backward-compat aliases so old screens compile without changes
val SuccessGreen = PitchstoneColors.Accent
val ErrorRed = PitchstoneColors.Danger
val InfoBlue = PitchstoneColors.Info
