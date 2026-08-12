package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.shared.ui.home.NextFeedDisplayState
import com.prof18.feedflow.shared.ui.home.NextFeedDisplayState.NextFeedDisplayEnabledState
import com.prof18.feedflow.shared.ui.home.components.list.NavigateNextButton
import com.prof18.feedflow.shared.ui.home.components.list.PullToNextIndicator
import com.prof18.feedflow.shared.ui.home.components.list.PullToNextLayout
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun EmptyFeedView(
    currentFeedFilter: FeedFilter,
    isDrawerVisible: Boolean,
    nextFeedState: NextFeedDisplayState,
    onReloadClick: () -> Unit,
    onBackToTimelineClick: () -> Unit,
    onOpenDrawerClick: () -> Unit,
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier,
    showNextFeedButton: Boolean = false,
) {
    PullToNextLayout(
        modifier = modifier.fillMaxSize(),
        onNavigateNext = onNavigateNext,
        enabled = !showNextFeedButton && nextFeedState is NextFeedDisplayEnabledState,
        indicator = { progress ->
            PullToNextIndicator(
                progress = progress,
                title = (nextFeedState as? NextFeedDisplayEnabledState)?.title,
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val emptyMessage = when (currentFeedFilter) {
                FeedFilter.Saved,
                FeedFilter.Bookmarks,
                -> LocalFeedFlowStrings.current.savedEmptyMessage

                FeedFilter.Voices -> LocalFeedFlowStrings.current.voicesEmptyMessage
                else -> LocalFeedFlowStrings.current.flowEmptyMessage
            }

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.regular),
                text = emptyMessage,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )

            val buttonAction = if (currentFeedFilter == FeedFilter.Saved || currentFeedFilter is FeedFilter.Bookmarks) {
                onBackToTimelineClick
            } else {
                onReloadClick
            }

            val buttonText = when (currentFeedFilter) {
                FeedFilter.Saved, is FeedFilter.Bookmarks -> {
                    LocalFeedFlowStrings.current.emptyScreenBackToFlow
                }
                else -> {
                    LocalFeedFlowStrings.current.refreshFeeds
                }
            }

            Button(
                modifier = Modifier
                    .padding(top = Spacing.regular),
                onClick = buttonAction,
            ) {
                Text(buttonText)
            }

            if (isDrawerVisible) {
                Button(
                    modifier = Modifier
                        .padding(top = Spacing.regular),
                    onClick = onOpenDrawerClick,
                ) {
                    Text(LocalFeedFlowStrings.current.openAnotherFeed)
                }
            }

            if (showNextFeedButton && nextFeedState is NextFeedDisplayEnabledState) {
                NavigateNextButton(
                    title = nextFeedState.title,
                    onClick = onNavigateNext,
                )
            }
        }
    }
}
