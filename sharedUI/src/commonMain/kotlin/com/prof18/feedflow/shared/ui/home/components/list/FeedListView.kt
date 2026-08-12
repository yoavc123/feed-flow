package com.prof18.feedflow.shared.ui.home.components.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.CalmCoachingCard
import com.prof18.feedflow.core.model.CalmCoachingType
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemDisplaySettings
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedItemUrlTitle
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActionType.NONE
import com.prof18.feedflow.core.model.SwipeActionType.OPEN_IN_BROWSER
import com.prof18.feedflow.core.model.SwipeActionType.TOGGLE_BOOKMARK_STATUS
import com.prof18.feedflow.core.model.SwipeActionType.TOGGLE_READ_STATUS
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.core.model.VisibleFeedItem
import com.prof18.feedflow.shared.ui.home.NextFeedDisplayState
import com.prof18.feedflow.shared.ui.home.NextFeedDisplayState.NextFeedDisplayEnabledState
import com.prof18.feedflow.shared.ui.preview.feedItemsForPreview
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.PreviewColumn
import com.prof18.feedflow.shared.ui.utils.PreviewHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed as staggeredItemsIndexed

val FeedListMaxContentWidth = 720.dp

private val GridContentPadding = Spacing.regular
private val GridMinCellWidth = 280.dp
private val CoachingCardMinWidth = 280.dp
private val CoachingCardMaxWidth = 360.dp
private const val MinimumFlowAlpha = 0.55f
private const val FlowAlphaRange = 0.45f
private const val FadeStartFreshness = 0.4f
private const val CoachingCardsListKey = "calm-coaching-cards"

@OptIn(ExperimentalFoundationApi::class)
@Suppress("CyclomaticComplexMethod", "MagicNumber")
@Composable
fun FeedList(
    feedItems: ImmutableList<FeedItem>,
    nextFeedState: NextFeedDisplayState,
    feedFontSize: FeedFontSizes,
    feedLayout: FeedLayout,
    currentFeedFilter: FeedFilter,
    shareMenuLabel: String,
    shareCommentsMenuLabel: String,
    swipeActions: SwipeActions,
    onVisibleFeedItemsChanged: (List<VisibleFeedItem>) -> Unit,
    requestMoreItems: () -> Unit,
    onFeedItemClick: (FeedItemUrlInfo) -> Unit,
    onOpenInBrowser: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onLetGo: (FeedItemId) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    onShareClick: (FeedItemUrlTitle) -> Unit,
    onOpenFeedSettings: (com.prof18.feedflow.core.model.FeedSource) -> Unit,
    onOpenFeedWebsite: (String) -> Unit,
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier,
    pinnedFeedItems: ImmutableList<FeedItem> = persistentListOf(),
    onGridArrangementChanged: (Boolean) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(),
    showNextFeedButton: Boolean = false,
    isGridLayoutEnabled: Boolean = true,
    isGridLayoutAllowed: Boolean = true,
    onMarkAllAboveAsRead: (String) -> Unit = {},
    onMarkAllBelowAsRead: (String) -> Unit = {},
    feedItemDisplaySettings: FeedItemDisplaySettings = FeedItemDisplaySettings(),
    coachingCards: ImmutableList<CalmCoachingCard> = persistentListOf(),
) {
    val allFeedItems = remember(feedItems, pinnedFeedItems) {
        (pinnedFeedItems + feedItems).toImmutableList()
    }
    val pinnedSectionTitle = LocalFeedFlowStrings.current.drawerTitlePinnedFeeds
    val hasCoachingCards = coachingCards.isNotEmpty()
    val leadingListItemCount = if (hasCoachingCards) 1 else 0
    val itemFeedLayout = feedLayout.normalizeForFeedList()
    val feedBackgroundModifier = if (itemFeedLayout.usesCardBackground() && allFeedItems.isNotEmpty()) {
        Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    } else {
        Modifier
    }

    BoxWithConstraints(modifier = modifier.then(feedBackgroundModifier)) {
        val gridContentPadding = contentPadding.withHorizontal(GridContentPadding)
        val isGridArrangement = isGridLayoutEnabled &&
            isGridLayoutAllowed &&
            itemFeedLayout.supportsGridArrangement() &&
            maxWidth >= itemFeedLayout.gridMinContentWidth()
        val latestOnGridArrangementChanged by rememberUpdatedState(onGridArrangementChanged)
        LaunchedEffect(isGridArrangement) {
            latestOnGridArrangementChanged(isGridArrangement)
        }
        var hadCoachingCards by remember { mutableStateOf(hasCoachingCards) }
        LaunchedEffect(hasCoachingCards, isGridArrangement) {
            if (hasCoachingCards && !hadCoachingCards) {
                if (isGridArrangement && gridState.firstVisibleItemIndex <= 1) {
                    gridState.scrollToItem(0)
                } else if (!isGridArrangement && listState.firstVisibleItemIndex <= 1) {
                    listState.scrollToItem(0)
                }
            }
            hadCoachingCards = hasCoachingCards
        }
        val shouldStartPaginate = remember(isGridArrangement) {
            derivedStateOf {
                if (isGridArrangement) {
                    val lastVisibleItemIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                        ?: return@derivedStateOf false
                    val totalItemsCount = gridState.layoutInfo.totalItemsCount
                    lastVisibleItemIndex >= totalItemsCount - 15
                } else {
                    val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                        ?: return@derivedStateOf false
                    val totalItemsCount = listState.layoutInfo.totalItemsCount
                    lastVisibleItemIndex >= totalItemsCount - 15
                }
            }
        }

        PullToNextLayout(
            onNavigateNext = { onNavigateNext() },
            enabled = !showNextFeedButton && nextFeedState is NextFeedDisplayEnabledState,
            indicator = { progress ->
                PullToNextIndicator(
                    progress = progress,
                    title = (nextFeedState as? NextFeedDisplayEnabledState)?.title,
                )
            },
        ) {
            if (isGridArrangement) {
                LazyVerticalStaggeredGrid(
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    columns = StaggeredGridCells.Adaptive(minSize = itemFeedLayout.gridMinCellWidth()),
                    contentPadding = gridContentPadding,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
                    verticalItemSpacing = Spacing.regular,
                ) {
                    if (hasCoachingCards) {
                        item(
                            key = CoachingCardsListKey,
                            span = StaggeredGridItemSpan.FullLine,
                        ) {
                            CoachingCardsRow(
                                cards = coachingCards,
                                onTuneSource = onOpenFeedSettings,
                                horizontalContentPadding = 0.dp,
                            )
                        }
                    }

                    staggeredItemsIndexed(
                        items = allFeedItems,
                        key = { _, item -> feedItemKey(item) },
                    ) { index, item ->
                        Column {
                            if (index == 0 && pinnedFeedItems.isNotEmpty()) {
                                PinnedSectionLabel(pinnedSectionTitle)
                            }
                            FeedItemContainer(
                                modifier = Modifier.alpha(
                                    item.flowAlpha(currentFeedFilter),
                                ),
                                feedLayout = itemFeedLayout,
                                isGridCell = true,
                            ) { itemModifier ->
                                FeedItemView(
                                    modifier = itemModifier,
                                    feedItem = item,
                                    shareMenuLabel = shareMenuLabel,
                                    shareCommentsMenuLabel = shareCommentsMenuLabel,
                                    onFeedItemClick = onFeedItemClick,
                                    onCommentClick = onCommentClick,
                                    onBookmarkClick = onBookmarkClick,
                                    onReadStatusClick = onReadStatusClick,
                                    onLetGo = onLetGo,
                                    feedFontSize = feedFontSize,
                                    onOpenFeedSettings = onOpenFeedSettings,
                                    onOpenFeedWebsite = onOpenFeedWebsite,
                                    onShareClick = onShareClick,
                                    feedLayout = itemFeedLayout,
                                    isGridCell = true,
                                    onMarkAllAboveAsRead = onMarkAllAboveAsRead,
                                    onMarkAllBelowAsRead = onMarkAllBelowAsRead,
                                    feedItemDisplaySettings = feedItemDisplaySettings,
                                )
                            }
                        }
                    }

                    item(span = StaggeredGridItemSpan.FullLine) {
                        FeedFooterButtons(
                            showNextFeedButton = showNextFeedButton,
                            nextFeedState = nextFeedState,
                            onNavigateNext = onNavigateNext,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = contentPadding,
                ) {
                    if (hasCoachingCards) {
                        item(key = CoachingCardsListKey) {
                            CoachingCardsRow(
                                cards = coachingCards,
                                onTuneSource = onOpenFeedSettings,
                            )
                        }
                    }

                    itemsIndexed(
                        items = allFeedItems,
                        key = { _, item -> feedItemKey(item) },
                    ) { index, item ->
                        if (index == 0 && pinnedFeedItems.isNotEmpty()) {
                            PinnedSectionLabel(pinnedSectionTitle)
                        }
                        FeedListItem(
                            item = item,
                            feedFontSize = feedFontSize,
                            feedLayout = itemFeedLayout,
                            shareMenuLabel = shareMenuLabel,
                            shareCommentsMenuLabel = shareCommentsMenuLabel,
                            swipeActions = swipeActions,
                            onFeedItemClick = onFeedItemClick,
                            onOpenInBrowser = onOpenInBrowser,
                            onBookmarkClick = onBookmarkClick,
                            onReadStatusClick = onReadStatusClick,
                            onLetGo = onLetGo,
                            onCommentClick = onCommentClick,
                            onShareClick = onShareClick,
                            onOpenFeedSettings = onOpenFeedSettings,
                            onOpenFeedWebsite = onOpenFeedWebsite,
                            onMarkAllAboveAsRead = onMarkAllAboveAsRead,
                            onMarkAllBelowAsRead = onMarkAllBelowAsRead,
                            feedItemDisplaySettings = feedItemDisplaySettings,
                            itemAlpha = item.flowAlpha(currentFeedFilter),
                        )
                        if (index == allFeedItems.size - 1) {
                            FeedFooterButtons(
                                showNextFeedButton = showNextFeedButton,
                                nextFeedState = nextFeedState,
                                onNavigateNext = onNavigateNext,
                            )
                        }
                    }
                }
            }
        }

        if (isGridArrangement) {
            ObserveVisibleGridFeedItems(
                feedItems = allFeedItems,
                gridState = gridState,
                leadingItemCount = leadingListItemCount,
                onVisibleFeedItemsChanged = onVisibleFeedItemsChanged,
            )
        } else {
            ObserveVisibleFeedItems(
                feedItems = allFeedItems,
                listState = listState,
                leadingItemCount = leadingListItemCount,
                onVisibleFeedItemsChanged = onVisibleFeedItemsChanged,
            )
        }

        val latestRequestMoreItems by rememberUpdatedState(requestMoreItems)
        LaunchedEffect(key1 = shouldStartPaginate.value) {
            if (shouldStartPaginate.value) {
                latestRequestMoreItems()
            }
        }
    }
}

@Composable
private fun FeedListItem(
    item: FeedItem,
    feedFontSize: FeedFontSizes,
    feedLayout: FeedLayout,
    shareMenuLabel: String,
    shareCommentsMenuLabel: String,
    swipeActions: SwipeActions,
    onFeedItemClick: (FeedItemUrlInfo) -> Unit,
    onOpenInBrowser: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onLetGo: (FeedItemId) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    onShareClick: (FeedItemUrlTitle) -> Unit,
    onOpenFeedSettings: (com.prof18.feedflow.core.model.FeedSource) -> Unit,
    onOpenFeedWebsite: (String) -> Unit,
    onMarkAllAboveAsRead: (String) -> Unit,
    onMarkAllBelowAsRead: (String) -> Unit,
    feedItemDisplaySettings: FeedItemDisplaySettings,
    itemAlpha: Float,
) {
    val swipeBackgroundColor = when (feedLayout) {
        FeedLayout.LIST -> MaterialTheme.colorScheme.surfaceContainerHighest
        FeedLayout.CARD,
        FeedLayout.BIG_IMAGE,
        FeedLayout.GRID,
        -> MaterialTheme.colorScheme.surfaceContainer
    }

    val swipeToRight = remember(
        item,
        swipeActions.rightSwipeAction,
        swipeBackgroundColor,
        onOpenInBrowser,
        onBookmarkClick,
        onLetGo,
    ) {
        swipeActions.rightSwipeAction.toSwipeAction(
            feedItem = item,
            swipeBackgroundColor = swipeBackgroundColor,
            onOpenInBrowser = onOpenInBrowser,
            onBookmarkClick = onBookmarkClick,
            onLetGo = onLetGo,
        )
    }
    val swipeToLeft = remember(
        item,
        swipeActions.leftSwipeAction,
        swipeBackgroundColor,
        onOpenInBrowser,
        onBookmarkClick,
        onLetGo,
    ) {
        swipeActions.leftSwipeAction.toSwipeAction(
            feedItem = item,
            swipeBackgroundColor = swipeBackgroundColor,
            onOpenInBrowser = onOpenInBrowser,
            onBookmarkClick = onBookmarkClick,
            onLetGo = onLetGo,
        )
    }
    val startSwipeActions = remember(swipeToRight) {
        swipeToRight?.let { listOf(it) }.orEmpty()
    }
    val endSwipeActions = remember(swipeToLeft) {
        swipeToLeft?.let { listOf(it) }.orEmpty()
    }

    FeedItemContainer(
        modifier = Modifier.alpha(itemAlpha),
        feedLayout = feedLayout,
    ) { itemModifier ->
        if (swipeToRight == null && swipeToLeft == null) {
            FeedItemView(
                modifier = itemModifier,
                feedItem = item,
                shareMenuLabel = shareMenuLabel,
                shareCommentsMenuLabel = shareCommentsMenuLabel,
                onFeedItemClick = onFeedItemClick,
                onCommentClick = onCommentClick,
                onBookmarkClick = onBookmarkClick,
                onReadStatusClick = onReadStatusClick,
                onLetGo = onLetGo,
                feedFontSize = feedFontSize,
                onOpenFeedSettings = onOpenFeedSettings,
                onOpenFeedWebsite = onOpenFeedWebsite,
                onShareClick = onShareClick,
                feedLayout = feedLayout,
                onMarkAllAboveAsRead = onMarkAllAboveAsRead,
                onMarkAllBelowAsRead = onMarkAllBelowAsRead,
                feedItemDisplaySettings = feedItemDisplaySettings,
            )
        } else {
            SwipeableActionsBox(
                modifier = itemModifier,
                backgroundUntilSwipeThreshold = swipeBackgroundColor,
                startActions = startSwipeActions,
                endActions = endSwipeActions,
            ) {
                FeedItemView(
                    feedItem = item,
                    shareMenuLabel = shareMenuLabel,
                    shareCommentsMenuLabel = shareCommentsMenuLabel,
                    feedLayout = feedLayout,
                    onFeedItemClick = onFeedItemClick,
                    onCommentClick = onCommentClick,
                    onBookmarkClick = onBookmarkClick,
                    onReadStatusClick = onReadStatusClick,
                    onLetGo = onLetGo,
                    feedFontSize = feedFontSize,
                    onOpenFeedSettings = onOpenFeedSettings,
                    onOpenFeedWebsite = onOpenFeedWebsite,
                    onShareClick = onShareClick,
                    onMarkAllAboveAsRead = onMarkAllAboveAsRead,
                    onMarkAllBelowAsRead = onMarkAllBelowAsRead,
                    feedItemDisplaySettings = feedItemDisplaySettings,
                )
            }
        }
    }
}

@Composable
private fun CoachingCardsRow(
    cards: ImmutableList<CalmCoachingCard>,
    onTuneSource: (com.prof18.feedflow.core.model.FeedSource) -> Unit,
    horizontalContentPadding: Dp = Spacing.regular,
) {
    if (cards.isEmpty()) return
    val strings = LocalFeedFlowStrings.current
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = horizontalContentPadding,
            vertical = Spacing.small,
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        items(cards, key = CalmCoachingCard::id) { card ->
            Card(
                modifier = Modifier
                    .widthIn(
                        min = CoachingCardMinWidth,
                        max = CoachingCardMaxWidth,
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .padding(
                            start = Spacing.regular,
                            top = Spacing.regular,
                            end = Spacing.small,
                            bottom = Spacing.xsmall,
                        ),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            modifier = Modifier.weight(1f),
                            text = card.message(strings),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    card.feedSource?.let { source ->
                        TextButton(
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag(CalmCoachingE2eIds.TUNE_SOURCE),
                            onClick = { onTuneSource(source) },
                        ) {
                            Text(strings.coachingTuneSource)
                        }
                    }
                }
            }
        }
    }
}

private fun FeedItem.flowAlpha(
    currentFeedFilter: FeedFilter,
): Float = if (
    currentFeedFilter == FeedFilter.Bookmarks ||
    currentFeedFilter == FeedFilter.Saved ||
    isBookmarked ||
    feedSource.isPinned
) {
    1f
} else {
    if (freshness >= FadeStartFreshness) {
        1f
    } else {
        MinimumFlowAlpha + (freshness / FadeStartFreshness) * FlowAlphaRange
    }
}

private fun CalmCoachingCard.message(strings: com.prof18.feedflow.i18n.FeedFlowStrings): String =
    when (type) {
        CalmCoachingType.FLOODING_SOURCE -> strings.coachingFloodingSource(feedSource?.title.orEmpty())
        CalmCoachingType.REPEATED_RELEASES -> strings.coachingRepeatedReleases(feedSource?.title.orEmpty())
        CalmCoachingType.FREQUENTLY_OPENED -> strings.coachingFrequentlyOpened(feedSource?.title.orEmpty())
        CalmCoachingType.SUGGESTED_STREAM -> strings.coachingSuggestedStream
    }

private fun feedItemKey(item: FeedItem): String = "feed-item:${item.id}"

@Preview(name = "Coaching card - phone", widthDp = 360, heightDp = 320)
@Composable
private fun CoachingCardsPhonePreview() {
    CoachingCardsPreview()
}

@Preview(name = "Coaching card - tablet", widthDp = 840, heightDp = 320)
@Composable
private fun CoachingCardsTabletPreview() {
    CoachingCardsPreview()
}

@Composable
private fun CoachingCardsPreview() {
    val source = feedItemsForPreview.first().feedSource.copy(title = "Android Authority")
    PreviewHelper(paddingEnabled = false) {
        CoachingCardsRow(
            cards = persistentListOf(
                CalmCoachingCard(
                    id = "preview-coaching-card",
                    type = CalmCoachingType.FLOODING_SOURCE,
                    feedSource = source,
                    evidenceCount = 24,
                ),
            ),
            onTuneSource = {},
        )
    }
}

@Composable
private fun PinnedSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            horizontal = Spacing.regular,
            vertical = Spacing.small,
        ),
    )
}

@Composable
private fun FeedFooterButtons(
    showNextFeedButton: Boolean,
    nextFeedState: NextFeedDisplayState,
    onNavigateNext: () -> Unit,
) {
    if (showNextFeedButton && nextFeedState is NextFeedDisplayEnabledState) {
        NavigateNextButton(
            title = nextFeedState.title,
            onClick = onNavigateNext,
        )
    }
}

@Composable
private fun ObserveVisibleFeedItems(
    feedItems: ImmutableList<FeedItem>,
    listState: LazyListState,
    leadingItemCount: Int,
    onVisibleFeedItemsChanged: (List<VisibleFeedItem>) -> Unit,
) {
    val latestFeedItems by rememberUpdatedState(feedItems)
    val latestLeadingItemCount by rememberUpdatedState(leadingItemCount)
    val latestOnVisibleFeedItemsChanged by rememberUpdatedState(onVisibleFeedItemsChanged)
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .sortedBy { it.offset }
                .mapNotNull { visibleItem ->
                    val feedItemIndex = visibleItem.index - latestLeadingItemCount
                    val feedItem = latestFeedItems.getOrNull(feedItemIndex) ?: return@mapNotNull null
                    VisibleFeedItem(
                        id = feedItem.id,
                        index = feedItemIndex,
                    )
                }
        }
            .distinctUntilChanged()
            .collect { visibleItems ->
                latestOnVisibleFeedItemsChanged(visibleItems)
            }
    }
}

@Composable
private fun ObserveVisibleGridFeedItems(
    feedItems: ImmutableList<FeedItem>,
    gridState: LazyStaggeredGridState,
    leadingItemCount: Int,
    onVisibleFeedItemsChanged: (List<VisibleFeedItem>) -> Unit,
) {
    val latestFeedItems by rememberUpdatedState(feedItems)
    val latestLeadingItemCount by rememberUpdatedState(leadingItemCount)
    val latestOnVisibleFeedItemsChanged by rememberUpdatedState(onVisibleFeedItemsChanged)
    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo
                .sortedWith(compareBy({ it.offset.y }, { it.offset.x }))
                .mapNotNull { visibleItem ->
                    val feedItemIndex = visibleItem.index - latestLeadingItemCount
                    val feedItem = latestFeedItems.getOrNull(feedItemIndex) ?: return@mapNotNull null
                    VisibleFeedItem(
                        id = feedItem.id,
                        index = feedItemIndex,
                    )
                }
        }
            .distinctUntilChanged()
            .collect { visibleItems ->
                latestOnVisibleFeedItemsChanged(visibleItems)
            }
    }
}

@Composable
internal fun NavigateNextButton(
    title: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth(),
    ) {
        TextButton(
            modifier = Modifier
                .padding(bottom = Spacing.medium)
                .align(Alignment.Center),
            onClick = onClick,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xsmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun FeedItemContainer(
    feedLayout: FeedLayout,
    modifier: Modifier = Modifier,
    isGridCell: Boolean = false,
    content: @Composable (Modifier) -> Unit,
) {
    when (feedLayout.normalizeForFeedList()) {
        FeedLayout.LIST -> content(modifier)
        FeedLayout.CARD -> {
            FeedItemCard(
                modifier = if (isGridCell) {
                    modifier.fillMaxWidth()
                } else {
                    modifier.padding(Spacing.small)
                },
            ) {
                content(Modifier)
            }
        }
        FeedLayout.BIG_IMAGE -> {
            FeedItemCard(
                modifier = if (isGridCell) {
                    modifier.fillMaxWidth()
                } else {
                    modifier.padding(
                        horizontal = Spacing.regular,
                        vertical = Spacing.small,
                    )
                },
            ) {
                content(Modifier)
            }
        }
        FeedLayout.GRID -> content(modifier)
    }
}

private fun FeedLayout.normalizeForFeedList(): FeedLayout =
    if (this == FeedLayout.GRID) FeedLayout.BIG_IMAGE else this

private fun FeedLayout.supportsGridArrangement(): Boolean =
    this == FeedLayout.CARD || this == FeedLayout.BIG_IMAGE

private fun FeedLayout.usesCardBackground(): Boolean =
    this == FeedLayout.CARD || this == FeedLayout.BIG_IMAGE || this == FeedLayout.GRID

private fun FeedLayout.gridMinCellWidth(): Dp = GridMinCellWidth

private fun FeedLayout.gridMinContentWidth(): Dp =
    gridMinCellWidth() * 2 + Spacing.regular

private fun PaddingValues.withHorizontal(padding: Dp): PaddingValues = PaddingValues(
    start = padding,
    top = calculateTopPadding(),
    end = padding,
    bottom = calculateBottomPadding(),
)

@Composable
private fun FeedItemCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        ),
    ) {
        content()
    }
}

private fun SwipeActionType.toSwipeAction(
    feedItem: FeedItem,
    swipeBackgroundColor: Color,
    onOpenInBrowser: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onLetGo: (FeedItemId) -> Unit,
): SwipeAction? {
    return when (this) {
        TOGGLE_READ_STATUS -> SwipeAction(
            icon = {
                Icon(
                    modifier = Modifier.padding(Spacing.regular),
                    imageVector = Icons.Default.Clear,
                    contentDescription = LocalFeedFlowStrings.current.letGo,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            background = swipeBackgroundColor,
            onSwipe = {
                onLetGo(FeedItemId(feedItem.id))
            },
        )

        TOGGLE_BOOKMARK_STATUS -> SwipeAction(
            icon = {
                Icon(
                    modifier = Modifier.padding(Spacing.regular),
                    imageVector = if (feedItem.isBookmarked) {
                        Icons.Default.BookmarkRemove
                    } else {
                        Icons.Default.BookmarkAdd
                    },
                    contentDescription = if (feedItem.isBookmarked) {
                        LocalFeedFlowStrings.current.menuRemoveFromBookmark
                    } else {
                        LocalFeedFlowStrings.current.menuAddToBookmark
                    },
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            background = swipeBackgroundColor,
            onSwipe = {
                onBookmarkClick(
                    FeedItemId(feedItem.id),
                    !feedItem.isBookmarked,
                )
            },
        )

        // URL-less items have nothing to open in a browser, so no swipe action.
        OPEN_IN_BROWSER -> if (feedItem.url.isBlank()) {
            null
        } else {
            SwipeAction(
                icon = {
                    Icon(
                        modifier = Modifier.padding(Spacing.regular),
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = LocalFeedFlowStrings.current.readerModeBrowserButtonContentDescription,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                background = swipeBackgroundColor,
                onSwipe = {
                    onOpenInBrowser(feedItem.toSwipeActionUrlInfo())
                },
            )
        }

        NONE -> null
    }
}

private fun FeedItem.toSwipeActionUrlInfo(): FeedItemUrlInfo =
    FeedItemUrlInfo(
        id = id,
        url = url,
        title = title,
        isBookmarked = isBookmarked,
        articleOpenMode = ArticleOpenMode.PREFERRED_BROWSER,
        commentsUrl = commentsUrl,
        imageUrl = imageUrl,
    )

@Preview
@Composable
internal fun FeedListPreview() {
    PreviewHelper {
        PreviewColumn {
            FeedList(
                feedItems = feedItemsForPreview,
                nextFeedState = NextFeedDisplayState.NextFeedDisplayDisabledState,
                feedFontSize = FeedFontSizes(),
                feedLayout = FeedLayout.LIST,
                currentFeedFilter = FeedFilter.Flow,
                shareMenuLabel = "Share",
                shareCommentsMenuLabel = "Share with comments",
                swipeActions = SwipeActions(
                    leftSwipeAction = TOGGLE_READ_STATUS,
                    rightSwipeAction = TOGGLE_BOOKMARK_STATUS,
                ),
                onVisibleFeedItemsChanged = {},
                requestMoreItems = {},
                onFeedItemClick = {},
                onOpenInBrowser = {},
                onBookmarkClick = { _, _ -> },
                onReadStatusClick = { _, _ -> },
                onLetGo = {},
                onCommentClick = {},
                onShareClick = {},
                onOpenFeedSettings = {},
                onOpenFeedWebsite = {},
                onNavigateNext = {},
            )

            FeedList(
                feedItems = feedItemsForPreview,
                nextFeedState = NextFeedDisplayState.NextFeedDisplayDisabledState,
                feedFontSize = FeedFontSizes(),
                feedLayout = FeedLayout.CARD,
                currentFeedFilter = FeedFilter.Flow,
                shareMenuLabel = "Share",
                shareCommentsMenuLabel = "Share with comments",
                swipeActions = SwipeActions(
                    leftSwipeAction = TOGGLE_READ_STATUS,
                    rightSwipeAction = TOGGLE_BOOKMARK_STATUS,
                ),
                onVisibleFeedItemsChanged = {},
                requestMoreItems = {},
                onFeedItemClick = {},
                onOpenInBrowser = {},
                onBookmarkClick = { _, _ -> },
                onReadStatusClick = { _, _ -> },
                onLetGo = {},
                onCommentClick = {},
                onShareClick = {},
                onOpenFeedSettings = {},
                onOpenFeedWebsite = {},
                onNavigateNext = {},
            )
        }
    }
}
