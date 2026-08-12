package com.prof18.feedflow.android.readermode

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun ReaderModeToolbar(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    isDetailFullscreen: Boolean = false,
    onToggleDetailFullscreen: (() -> Unit)? = null,
) {
    val strings = LocalFeedFlowStrings.current
    val isEnteringFullscreen = onToggleDetailFullscreen != null && !isDetailFullscreen
    FilledIconButton(
        onClick = onToggleDetailFullscreen ?: navigateBack,
        modifier = modifier
            .statusBarsPadding()
            .padding(start = 12.dp, top = 8.dp)
            .testTag(ReaderModeE2eIds.BACK_BUTTON),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(
            imageVector = if (isEnteringFullscreen) {
                Icons.Default.Fullscreen
            } else {
                Icons.AutoMirrored.Filled.ArrowBack
            },
            contentDescription = when {
                onToggleDetailFullscreen == null -> strings.readerModeBackButton
                isEnteringFullscreen -> strings.readerModeEnterFullscreen
                else -> strings.readerModeExitFullscreen
            },
        )
    }
}
