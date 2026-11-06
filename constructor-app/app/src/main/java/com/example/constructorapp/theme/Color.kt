package com.example.constructorapp.theme

import androidx.compose.ui.graphics.Color

// Text Tokens (Matching webapp light mode: slate-900 / slate-500)
val TextPrimary = Color(0xFF0F172A)          // Slate-900 #0F172A
val TextSecondary = Color(0xFF64748B)        // Slate-500 #64748B
val TextMutedDark = Color(0xFF94A3B8)        // Slate-400 #94A3B8
val SurfaceWhite = Color(0xFFFFFFFF)

// Web Brand Primary (Exact HSL 22 98% 54% Safety Orange from globals.css)
val BrandPrimary = Color(0xFFFA6400)         // #FA6400 (Web Primary Construction Orange)
val BrandPrimaryDark = Color(0xFFE05800)
val BrandPrimaryLight = Color(0xFFFFEDD5)
val BrandPrimaryContainer = Color(0x26FA6400) // 15% opacity primary container

// Modern Web Theme Tokens (Exact bg-secondary #F1F5F9 from layout.tsx and globals.css)
val SlateBackground = Color(0xFFF1F5F9)      // #F1F5F9 (bg-secondary)
val SlateSurface = Color(0xFFFFFFFF)         // Pure white card
val SlateSurfaceSubtle = Color(0xFFE2E8F0)   // Slate-200 for progress track & borders
val SlateBorder = Color(0x1F0F172A)          // Subtle slate border
val SlateDivider = Color(0x140F172A)         // Subtle slate divider

// Exact Glassmorphism Tokens (Matching globals.css .glass-card: rgba(255, 255, 255, 0.70))
val GlassSurface = Color(0xD9FFFFFF)          // 85% white frosted glass
val GlassSurfaceSubtle = Color(0xB3FFFFFF)    // 70% white frosted glass
val GlassCardSurface = Color(0xD9FFFFFF)      // Glass card background
val GlassCardSurfaceElevated = Color(0xF2FFFFFF)

val GlassBorder = Color(0x66FFFFFF)           // rgba(255, 255, 255, 0.40)
val GlassCardBorder = Color(0x80FFFFFF)       // rgba(255, 255, 255, 0.50)
val GlassBorderElevated = Color(0xB3FFFFFF)
val GlassBorderHighlight = Color(0xFFFFFFFF)

val GlassInnerGlowTop = Color(0x80FFFFFF)
val GlassInnerGlowBottom = Color(0x0D000000)
val GlassShadow = Color(0x0F1F2687)           // rgba(31, 38, 135, 0.06) from globals.css

// Dynamic Ambient Drifting Blob Colors (Bright vibrant fluid gradients from webapp)
val BlobIndigo = Color(0xFF6366F1)            // Indigo-500
val BlobPurple = Color(0xFFA855F7)            // Purple-500
val BlobAmber = Color(0xFFF59E0B)             // Amber-500
val BlobOrange = Color(0xFFFA6400)            // Orange-500
val BlobCyan = Color(0xFF06B6D4)              // Cyan-500
val BlobEmerald = Color(0xFF10B981)           // Emerald-500

// Semantic Accents
val EmeraldSuccess = Color(0xFF10B981)
val EmeraldSuccessContainer = Color(0x2610B981)
val SkyInfo = Color(0xFF0EA5E9)
val SkyInfoContainer = Color(0x260EA5E9)
val AmberWarning = Color(0xFFF59E0B)
val AmberWarningContainer = Color(0x26F59E0B)
val RoseDanger = Color(0xFFEF4444)
val RoseDangerContainer = Color(0x26EF4444)
val PurpleAccent = Color(0xFF8B5CF6)
val PurpleAccentContainer = Color(0x268B5CF6)

// Compatibility Aliases
val CoralPrimary = BrandPrimary
val CoralDark = BrandPrimaryDark
val CoralLight = BrandPrimaryLight
val TextDark = Color(0xFF0F172A)
val TextMuted = TextSecondary
val GreenSuccess = EmeraldSuccess
val RedDanger = RoseDanger
val OrangeWarning = AmberWarning
val DarkBackground = SlateBackground
val DarkSurface = SlateSurface
val DarkOnSurface = TextPrimary
val DarkOnBackground = TextPrimary
