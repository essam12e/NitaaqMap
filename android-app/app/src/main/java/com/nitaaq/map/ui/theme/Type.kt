package com.nitaaq.map.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.nitaaq.map.R

/** Tajawal: the website's typeface, bundled so Arabic is correct on the first frame and offline. */
val Tajawal = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_medium, FontWeight.Medium),
    Font(R.font.tajawal_bold, FontWeight.Bold),
    Font(R.font.tajawal_extrabold, FontWeight.ExtraBold),
)

// Arabic glyphs sit tall; generous line heights keep diacritics and descenders from clipping.
private val Trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = Tajawal,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
    lineHeightStyle = Trim,
)

val NitaaqTypography = Typography(
    displayLarge = style(48, 56, FontWeight.ExtraBold),
    displayMedium = style(40, 48, FontWeight.ExtraBold),
    displaySmall = style(34, 42, FontWeight.ExtraBold),
    headlineLarge = style(30, 40, FontWeight.ExtraBold),
    headlineMedium = style(26, 36, FontWeight.ExtraBold),
    headlineSmall = style(22, 32, FontWeight.Bold),
    titleLarge = style(20, 30, FontWeight.Bold),
    titleMedium = style(17, 26, FontWeight.Bold),
    titleSmall = style(15, 22, FontWeight.Bold),
    bodyLarge = style(17, 28, FontWeight.Normal),
    bodyMedium = style(15, 25, FontWeight.Normal),
    bodySmall = style(13, 21, FontWeight.Normal),
    labelLarge = style(16, 22, FontWeight.Bold),
    labelMedium = style(13, 18, FontWeight.Medium),
    labelSmall = style(12, 16, FontWeight.Medium, 0.2),
)

/** Countdown digits: tabular so "4:59 → 4:58" never jiggles. */
val CountdownStyle = TextStyle(
    fontFamily = Tajawal,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 44.sp,
    lineHeight = 48.sp,
    fontFeatureSettings = "tnum",
    lineHeightStyle = Trim,
)
