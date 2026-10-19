package android_app.ui.theme

import androidx.compose.ui.graphics.Color

// --- Golden Hour Theme Colors ---

val SunsetCoral = Color(0xFFFF6B4A) // Primary Action (Check-in, selected tab, buttons)
val MangoGold = Color(0xFFFFC24B)   // Rewards, badges, achievements
val LagoonTeal = Color(0xFF14B8A6)  // Map, status info
val DeepSea = Color(0xFF0E5E63)     // Secondary buttons, sub-titles
val Lavender = Color(0xFFB79CFF)    // Tags, photo spot labels
val LavenderSoft = Color(0xFFEFE8FF)
val SandCream = Color(0xFFFFF8F0)   // Light theme background & surface
val NightInk = Color(0xFF1E2A38)    // Light theme text (onBackground)
val DarkNight = Color(0xFF0F1B2D)   // Dark theme background
val GoldSoft = Color(0xFFFFF1D6)    // Soft background for gold tags
val SunsetCoralDark = Color(0xFFE65130) // Darker coral for text/icons on light bg

// --- Material 3 Color Schemes Mapping ---

// Light Theme Mapping
val md_theme_light_primary = SunsetCoral
val md_theme_light_onPrimary = Color.White
val md_theme_light_primaryContainer = Color(0xFFFFDBCF) // Muted Coral
val md_theme_light_onPrimaryContainer = Color(0xFF3B0900)

val md_theme_light_secondary = LagoonTeal
val md_theme_light_onSecondary = Color.White
val md_theme_light_secondaryContainer = Color(0xFFCCFBF1)
val md_theme_light_onSecondaryContainer = Color(0xFF042F2E)

val md_theme_light_tertiary = MangoGold
val md_theme_light_onTertiary = Color(0xFF422B00)
val md_theme_light_tertiaryContainer = Color(0xFFFFECCC)
val md_theme_light_onTertiaryContainer = Color(0xFF261900)

val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_onError = Color.White
val md_theme_light_background = SandCream
val md_theme_light_onBackground = NightInk
val md_theme_light_surface = SandCream
val md_theme_light_onSurface = NightInk
val md_theme_light_surfaceVariant = Color(0xFFE7E0D6)
val md_theme_light_onSurfaceVariant = DeepSea
val md_theme_light_outline = Color(0xFF85736D)

// Dark Theme Mapping
val md_theme_dark_primary = SunsetCoral // Keep coral for contrast pop
val md_theme_dark_onPrimary = Color.White
val md_theme_dark_primaryContainer = Color(0xFF7A2A17)
val md_theme_dark_onPrimaryContainer = Color(0xFFFFDBCF)

val md_theme_dark_secondary = LagoonTeal
val md_theme_dark_onSecondary = Color.White
val md_theme_dark_secondaryContainer = Color(0xFF0F766E)
val md_theme_dark_onSecondaryContainer = Color(0xFFCCFBF1)

val md_theme_dark_tertiary = MangoGold
val md_theme_dark_onTertiary = Color(0xFF422B00)
val md_theme_dark_tertiaryContainer = Color(0xFF7A5900)
val md_theme_dark_onTertiaryContainer = Color(0xFFFFECCC)

val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_background = DarkNight
val md_theme_dark_onBackground = Color(0xFFE2E2E6)
val md_theme_dark_surface = DarkNight
val md_theme_dark_onSurface = Color(0xFFE2E2E6)
val md_theme_dark_surfaceVariant = Color(0xFF53433F)
val md_theme_dark_onSurfaceVariant = Color(0xFFD8C2BC)
val md_theme_dark_outline = Color(0xFFA08C87)

// Premium Gradient compatibility (if still used)
val PremiumGradientStart = SunsetCoral
val PremiumGradientEnd = MangoGold
