package com.prof18.feedflow.android.settings.appearance

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.android.settings.SettingsE2eIds
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.shared.ui.settings.ConfirmationSettingItem
import com.prof18.feedflow.shared.ui.settings.SettingDropdownOption
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun AppearanceScreenContent(
    navigateBack: () -> Unit,
    themeMode: ThemeMode,
    isReduceMotionEnabled: Boolean,
    areCalmInsightsEnabled: Boolean,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onReduceMotionToggled: (Boolean) -> Unit,
    onCalmInsightsToggled: (Boolean) -> Unit,
    onClearReadingHistory: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsAppearance) },
                navigationIcon = {
                    IconButton(
                        modifier = Modifier.testTag(SettingsE2eIds.BACK_BUTTON),
                        onClick = navigateBack,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val layoutDir = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .padding(start = paddingValues.calculateLeftPadding(layoutDir))
                .padding(end = paddingValues.calculateRightPadding(layoutDir)),
        ) {
            item {
                CompactSettingDropdownRow(
                    modifier = Modifier.testTag(SettingsE2eIds.APPEARANCE_THEME),
                    title = strings.settingsTheme,
                    currentValue = themeMode,
                    options = persistentListOf(
                        SettingDropdownOption(ThemeMode.SYSTEM, strings.settingsThemeMaterialYou),
                        SettingDropdownOption(ThemeMode.PAPER, strings.settingsThemePaper),
                        SettingDropdownOption(ThemeMode.TIDE, strings.settingsThemeTide),
                        SettingDropdownOption(ThemeMode.TWILIGHT, strings.settingsThemeTwilight),
                        SettingDropdownOption(ThemeMode.HEARTH, strings.settingsThemeHearth),
                        SettingDropdownOption(ThemeMode.OLED, strings.settingsThemeOled),
                        SettingDropdownOption(ThemeMode.SLATE, strings.settingsThemeSlate),
                        SettingDropdownOption(ThemeMode.TERMINAL, strings.settingsThemeTerminal),
                        SettingDropdownOption(ThemeMode.SOLARIZED, strings.settingsThemeSolarized),
                    ),
                    onOptionSelected = onThemeModeSelected,
                )
            }

            item {
                SettingSwitchItem(
                    title = strings.settingsCalmInsights,
                    isChecked = areCalmInsightsEnabled,
                    onCheckedChange = onCalmInsightsToggled,
                )
            }

            item {
                ConfirmationSettingItem(
                    title = strings.settingsClearReadingHistory,
                    dialogTitle = strings.settingsClearReadingHistory,
                    dialogMessage = strings.settingsClearReadingHistoryConfirmation,
                    onConfirm = onClearReadingHistory,
                )
            }

            item {
                SettingSwitchItem(
                    modifier = Modifier.testTag(SettingsE2eIds.APPEARANCE_REDUCE_MOTION),
                    title = strings.settingsReduceMotion,
                    isChecked = isReduceMotionEnabled,
                    onCheckedChange = onReduceMotionToggled,
                )
            }

            item {
                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
            }
        }
    }
}

@Preview
@Composable
private fun AppearanceScreenContentPreview() {
    FeedFlowTheme {
        AppearanceScreenContent(
            navigateBack = {},
            themeMode = ThemeMode.SYSTEM,
            isReduceMotionEnabled = false,
            areCalmInsightsEnabled = true,
            onThemeModeSelected = {},
            onReduceMotionToggled = {},
            onCalmInsightsToggled = {},
            onClearReadingHistory = {},
        )
    }
}
