package com.prof18.feedflow.android.focus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun FocusScreen(
    homeViewModel: HomeViewModel,
    navigateBack: () -> Unit,
    onOpenArticle: (FeedItemUrlInfo) -> Unit,
) {
    val flowItems by homeViewModel.feedState.collectAsStateWithLifecycle()
    val pinnedItems by homeViewModel.pinnedFeedState.collectAsStateWithLifecycle()
    val items = (pinnedItems + flowItems).distinctBy { it.id }
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(items.size) {
        currentIndex = currentIndex.coerceIn(0, (items.lastIndex).coerceAtLeast(0))
    }
    val strings = LocalFeedFlowStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.focusTitle) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(Spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(strings.focusEmpty, style = MaterialTheme.typography.headlineSmall)
            }
        } else {
            val item = items[currentIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(Spacing.regular),
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable { onOpenArticle(item.toUrlInfo()) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.large),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = item.feedSource.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = item.title.orEmpty(),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(top = Spacing.regular),
                        )
                        item.author?.let { author ->
                            Text(
                                text = author,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = Spacing.small),
                            )
                        }
                        item.subtitle?.let { subtitle ->
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 8,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = Spacing.large),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.regular),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        enabled = currentIndex > 0,
                        onClick = { currentIndex -= 1 },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = strings.previousArticle,
                        )
                    }
                    FilledTonalButton(
                        onClick = { homeViewModel.letGo(FeedItemId(item.id)) },
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                        Text(strings.letGo, modifier = Modifier.padding(start = Spacing.xsmall))
                    }
                    FilledTonalButton(
                        enabled = !item.isBookmarked,
                        onClick = { homeViewModel.updateBookmarkStatus(FeedItemId(item.id), true) },
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                        Text(strings.actionSave, modifier = Modifier.padding(start = Spacing.xsmall))
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        enabled = currentIndex < items.lastIndex,
                        onClick = { currentIndex += 1 },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = strings.nextArticle,
                        )
                    }
                }
            }
        }
    }
}

private fun FeedItem.toUrlInfo() = FeedItemUrlInfo(
    id = id,
    url = url,
    title = title,
    isBookmarked = isBookmarked,
    articleOpenMode = feedSource.articleOpenMode,
    commentsUrl = commentsUrl,
    imageUrl = imageUrl,
    feedSourceTitle = feedSource.title,
    feedSourceBaseUrl = feedSource.websiteUrlFallback(),
)
