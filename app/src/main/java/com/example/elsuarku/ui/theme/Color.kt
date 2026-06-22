package com.example.elsuarku.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// ElSuarKu Brand Colors — Deep Blue + Emerald Green + Gold
// Premium palette for cloud-based secure e-voting platform
// ============================================================

// ── Primary — Deep Blue (authority, trust, professionalism) ──
val DeepBlue = Color(0xFF1A237E)
val DeepBlueDark = Color(0xFF0D1642)
val DeepBlueDarker = Color(0xFF060D24)
val DeepBlueLight = Color(0xFF3949AB)
val DeepBlueLighter = Color(0xFF5C6BC0)
val DeepBlueSurface = Color(0xFFE8EAF6)
val DeepBlueSurfaceVivid = Color(0xFFC5CAE9)

// ── Secondary — Emerald Green (growth, success, transparency) ──
val EmeraldGreen = Color(0xFF2E7D32)
val EmeraldGreenDark = Color(0xFF1B5E20)
val EmeraldGreenDarker = Color(0xFF0F3D12)
val EmeraldGreenLight = Color(0xFF4CAF50)
val EmeraldGreenLighter = Color(0xFF81C784)
val EmeraldGreenSurface = Color(0xFFE8F5E9)
val EmeraldGreenSurfaceVivid = Color(0xFFC8E6C9)

// ── Tertiary / Accent — Gold (prestige, value, celebration) ──
val Gold = Color(0xFFFFC107)
val GoldDark = Color(0xFFFF8F00)
val GoldDarker = Color(0xFFE65100)
val GoldLight = Color(0xFFFFECB3)
val GoldLighter = Color(0xFFFFF3CD)
val GoldSurface = Color(0xFFFFF8E1)
val GoldVivid = Color(0xFFFFD54F)

// ── Surface & Background ──
val SoftWhite = Color(0xFFFAFAFA)
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF0D0D0D)
val SurfaceDarkElevated = Color(0xFF1A1A1A)
val SurfaceDarkCard = Color(0xFF242424)
val SurfaceDarkInput = Color(0xFF2A2A2A)

// ── On-Colors ──
val OnDeepBlue = Color(0xFFFFFFFF)
val OnEmeraldGreen = Color(0xFFFFFFFF)
val OnGold = Color(0xFF1A1A1A)
val OnSurface = Color(0xFF1C1B1F)
val OnSurfaceDark = Color(0xFFE6E1E5)
val OnSurfaceDarkDim = Color(0xFF9E9E9E)

// ── Glassmorphism ──
val GlassSurface = Color(0xCCFFFFFF)
val GlassSurfaceAlt = Color(0xE6FFFFFF)
val GlassSurfaceDark = Color(0xCC1E1E1E)
val GlassSurfaceDarkAlt = Color(0xE61E1E1E)
val GlassBorder = Color(0x33FFFFFF)
val GlassBorderLight = Color(0x1A000000)
val GlassBorderDark = Color(0x33FFFFFF)
val GlassShimmer = Color(0x80FFFFFF)

// ── Status Colors ──
val StatusSuccess = Color(0xFF4CAF50)
val StatusSuccessSurface = Color(0xFFE8F5E9)
val StatusWarning = Color(0xFFFF9800)
val StatusWarningSurface = Color(0xFFFFF3E0)
val StatusError = Color(0xFFF44336)
val StatusErrorSurface = Color(0xFFFFEBEE)
val StatusInfo = Color(0xFF2196F3)
val StatusInfoSurface = Color(0xFFE3F2FD)

// ── Semantic Gradients ──
// These are used as gradient stop lists; applied via Brush.verticalGradient / horizontalGradient
val GradientDeepBlue = listOf(DeepBlueDark, DeepBlue, DeepBlueLight)
val GradientEmerald = listOf(EmeraldGreenDark, EmeraldGreen, EmeraldGreenLight)
val GradientGold = listOf(GoldDark, Gold, GoldLight)
val GradientDarkHero = listOf(DeepBlueDarker, DeepBlueDark, DeepBlue)
val GradientSuccessLight = listOf(EmeraldGreen, EmeraldGreenLight, EmeraldGreenLighter)

// ── Chart / Data Vis ──
val ChartBlue = Color(0xFF42A5F5)
val ChartGreen = Color(0xFF66BB6A)
val ChartOrange = Color(0xFFFFA726)
val ChartRed = Color(0xFFEF5350)
val ChartPurple = Color(0xFFAB47BC)
val ChartTeal = Color(0xFF26A69A)
