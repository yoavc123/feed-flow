package com.prof18.feedflow.shared.ui.style

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

private val DefaultTypography = Typography()

val FeedFlowTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = FontFamily.Serif),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = FontFamily.Serif),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = FontFamily.Serif),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = FontFamily.Serif),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = FontFamily.Serif),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = FontFamily.Serif),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = FontFamily.Serif),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = FontFamily.Serif),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = FontFamily.Serif),
)
