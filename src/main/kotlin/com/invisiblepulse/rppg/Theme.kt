package com.invisiblepulse.rppg

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object VitalPulseTheme {
    val Primary          = Color(0xFF0047AB)
    val OnPrimary        = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFDAE2FF)
    val Secondary        = Color(0xFFBE123C)
    val Tertiary         = Color(0xFF0D9488)
    val TertiaryContainer = Color(0xFFF0FDFA)
    val Error            = Color(0xFF9F403D)
    val ErrorContainer   = Color(0xFFFFE4E6)
    val Amber            = Color(0xFFF59E0B)
    val AmberContainer   = Color(0xFFFEF3C7)
    val Background       = Color(0xFFF8FAFC)
    val Surface          = Color(0xFFFFFFFF)
    val SurfaceContainer = Color(0xFFF1F5F9)
    val SurfaceContainerLow = Color(0xFFF8FAFC)
    val OnSurface        = Color(0xFF0F172A)
    val OnSurfaceVariant = Color(0xFF64748B)
    val Outline          = Color(0xFFE2E8F0)
    val OutlineVariant   = Color(0xFFCBD5E1)

    fun statusColor(status: String) = when (status) {
        "LOW"      -> Tertiary
        "NORMAL"   -> Primary
        "ELEVATED" -> Amber
        "HIGH"     -> Secondary
        else       -> OnSurfaceVariant
    }

    fun statusContainerColor(status: String) = when (status) {
        "LOW"      -> TertiaryContainer
        "NORMAL"   -> PrimaryContainer
        "ELEVATED" -> AmberContainer
        "HIGH"     -> ErrorContainer
        else       -> SurfaceContainer
    }

    val Typography = Typography(
        displayLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Black,
            fontSize = 72.sp,
            letterSpacing = (-2).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 36.sp,
            letterSpacing = (-1).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            letterSpacing = (-0.5).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            letterSpacing = (-0.25).sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 22.sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp
        )
    )
}
