package com.invisiblepulse.rppg

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object VitalPulseTheme {
    val Primary = Color(0xFF0047AB)
    val OnPrimary = Color(0xFFFFFFFF)
    val Secondary = Color(0xFFE0115F)
    val Background = Color(0xFFF8FAFC)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceContainer = Color(0xFFF1F5F9)
    val OnSurface = Color(0xFF0F172A)
    val OnSurfaceVariant = Color(0xFF64748B)
    val Outline = Color(0xFFE2E8F0)

    val Typography = Typography(
        displayLarge = TextStyle(
            fontFamily = FontFamily.SansSerif, // Will be Manrope if added to assets
            fontWeight = FontWeight.Black,
            fontSize = 72.sp,
            letterSpacing = (-2).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            letterSpacing = (-0.5).sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
    )
}
