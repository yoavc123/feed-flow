package com.prof18.feedflow.shared.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.ui.style.DarkColorScheme
import com.prof18.feedflow.shared.ui.style.FeedFlowShapes
import com.prof18.feedflow.shared.ui.style.FeedFlowTypography
import com.prof18.feedflow.shared.ui.style.HearthColorScheme
import com.prof18.feedflow.shared.ui.style.LightColorScheme
import com.prof18.feedflow.shared.ui.style.OledColorScheme
import com.prof18.feedflow.shared.ui.style.PaperColorScheme
import com.prof18.feedflow.shared.ui.style.SlateColorScheme
import com.prof18.feedflow.shared.ui.style.SolarizedColorScheme
import com.prof18.feedflow.shared.ui.style.TerminalColorScheme
import com.prof18.feedflow.shared.ui.style.TideColorScheme
import com.prof18.feedflow.shared.ui.style.TwilightColorScheme

@Composable
fun FeedFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        themeMode == ThemeMode.PAPER -> PaperColorScheme
        themeMode == ThemeMode.TIDE -> TideColorScheme
        themeMode == ThemeMode.TWILIGHT -> TwilightColorScheme
        themeMode == ThemeMode.HEARTH -> HearthColorScheme
        themeMode == ThemeMode.OLED -> OledColorScheme
        themeMode == ThemeMode.SLATE -> SlateColorScheme
        themeMode == ThemeMode.TERMINAL -> TerminalColorScheme
        themeMode == ThemeMode.SOLARIZED -> SolarizedColorScheme
        themeMode == ThemeMode.LIGHT -> LightColorScheme
        themeMode == ThemeMode.DARK -> DarkColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        shapes = FeedFlowShapes,
        typography = FeedFlowTypography,
        content = content,
    )
}

@Composable
actual fun FeedFlowThemePreview(
    content: @Composable () -> Unit,
) {
    FeedFlowTheme(
        content = content,
    )
}
