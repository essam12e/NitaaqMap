package com.nitaaq.map.ui.theme

import androidx.compose.ui.graphics.Color

// Brand, taken from the website (app/globals.css): blue #2563EB → green #22C55E, teal #2DD4BF, navy #0B1A2B.
internal object Brand {
    val Blue = Color(0xFF2563EB)
    val BlueDeep = Color(0xFF1D4ED8)
    val BlueSoft = Color(0xFF93B4FF)
    val Green = Color(0xFF22C55E)
    val GreenDeep = Color(0xFF15803D)
    val Teal = Color(0xFF2DD4BF)
    val Navy = Color(0xFF0B1A2B)
    val NavyCard = Color(0xFF112235)
    val NavyRaised = Color(0xFF17304A)
    val NavyHigh = Color(0xFF1E3A57)
    val Ink = Color(0xFF0F2238)
}

// Light: clean, airy paper with navy ink. Accent (blue) is reserved for actions and focus.
internal object LightTokens {
    val background = Color(0xFFF5F8FC)
    val surface = Color(0xFFFFFFFF)
    val surfaceContainerLow = Color(0xFFF9FBFE)
    val surfaceContainer = Color(0xFFEEF3F9)
    val surfaceContainerHigh = Color(0xFFE5ECF5)
    val onSurface = Brand.Ink
    val onSurfaceVariant = Color(0xFF4A5F75)
    val outline = Color(0xFFC9D6E4)
    val outlineVariant = Color(0xFFDDE6EF)
    val success = Color(0xFF15803D)
    val successContainer = Color(0xFFDCFCE7)
    val warning = Color(0xFFB45309)
    val warningContainer = Color(0xFFFEF3C7)
    val error = Color(0xFFC62828)
    val errorContainer = Color(0xFFFDECEC)
}

// Dark: the website's navy, with distinct tonal steps so raised cards read as raised.
internal object DarkTokens {
    val background = Brand.Navy
    val surface = Brand.NavyCard
    val surfaceContainerLow = Color(0xFF0F2031)
    val surfaceContainer = Brand.NavyRaised
    val surfaceContainerHigh = Brand.NavyHigh
    val onSurface = Color(0xFFEAF1F8)
    val onSurfaceVariant = Color(0xFF9DB0C4)
    val outline = Color(0xFF2B4560)
    val outlineVariant = Color(0xFF203750)
    val success = Color(0xFF4ADE80)
    val successContainer = Color(0xFF12351F)
    val warning = Color(0xFFFBBF24)
    val warningContainer = Color(0xFF3A2C0A)
    val error = Color(0xFFFF8A80)
    val errorContainer = Color(0xFF3B1517)
}
