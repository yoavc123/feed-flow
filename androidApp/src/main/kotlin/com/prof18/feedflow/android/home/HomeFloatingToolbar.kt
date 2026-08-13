package com.prof18.feedflow.android.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.android.home.drawer.DrawerE2eIds
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.presentation.model.HomeViewMenuState
import com.prof18.feedflow.shared.ui.home.HomeDisplayState
import com.prof18.feedflow.shared.ui.icons.CloseSidebar
import com.prof18.feedflow.shared.ui.icons.CloseSidebarReversed
import com.prof18.feedflow.shared.ui.icons.OpenSidebar
import com.prof18.feedflow.shared.ui.icons.OpenSidebarReversed
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFloatingToolbar(
    displayState: HomeDisplayState,
    showDrawerMenu: Boolean,
    isDrawerOpen: Boolean,
    onDrawerMenuClick: () -> Unit,
    onClearOldArticlesClicked: () -> Unit,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onSearchClick: () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onBackupClick: () -> Unit,
    viewMenuState: HomeViewMenuState,
    onFeedOrderChange: (FeedOrder) -> Unit,
    onFocusClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showViewOptionsSheet by remember { mutableStateOf(false) }
    val viewOptionsSheetState = rememberModalBottomSheetState()
    val strings = LocalFeedFlowStrings.current
    val currentFeedFilter = displayState.currentFeedFilter
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(MaterialTheme.colorScheme.background)
            .padding(top = FloatingToolbarDefaults.ScreenOffset),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onDoubleTap = { onDoubleClick() },
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showDrawerMenu) {
                DrawerIcon(
                    onDrawerMenuClick = onDrawerMenuClick,
                    isDrawerOpen = isDrawerOpen,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                val isMainFlow = currentFeedFilter == FeedFilter.Flow ||
                    currentFeedFilter == FeedFilter.Timeline
                Text(
                    modifier = Modifier.widthIn(max = 220.dp),
                    text = if (isMainFlow) strings.appName else currentFeedFilter.getTitle(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isMainFlow) {
                    val date = remember {
                        LocalDate.now().format(
                            DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.getDefault()),
                        ).uppercase(Locale.getDefault())
                    }
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(
                modifier = Modifier.testTag(HomeToolbarE2eIds.SEARCH_BUTTON),
                onClick = onSearchClick,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = strings.searchButtonContentDescription,
                )
            }

            Box {
                IconButton(
                    modifier = Modifier.testTag(HomeToolbarE2eIds.MORE_MENU_BUTTON),
                    onClick = { showMenu = !showMenu },
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = strings.moreOptionsButtonContentDescription,
                    )
                }

                HomeAppBarDropdownMenu(
                    showMenu = showMenu,
                    feedFilter = currentFeedFilter,
                    closeMenu = { showMenu = false },
                    onClearOldArticlesClicked = onClearOldArticlesClicked,
                    onEditFeedClick = { feedSource ->
                        showMenu = false
                        onEditFeedClick(feedSource)
                    },
                    isSyncUploadRequired = displayState.isSyncUploadRequired,
                    onBackupClick = {
                        showMenu = false
                        onBackupClick()
                    },
                    onViewOptionsClick = { showViewOptionsSheet = true },
                    onFocusClick = onFocusClick,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }

    if (showViewOptionsSheet) {
        HomeViewOptionsBottomSheet(
            state = viewMenuState,
            onFeedOrderChange = onFeedOrderChange,
            onDismiss = { showViewOptionsSheet = false },
            sheetState = viewOptionsSheetState,
        )
    }
}

@Composable
internal fun homeFloatingToolbarContainerColor(): Color {
    return if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
}

@Composable
internal fun homeFloatingToolbarBorder(): BorderStroke? {
    return if (isSystemInDarkTheme()) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    } else {
        null
    }
}

@Composable
private fun FeedFilter.getTitle(): String =
    when (this) {
        is FeedFilter.Category -> this.feedCategory.title
        is FeedFilter.Stream -> this.feedCategory.title
        is FeedFilter.Source -> this.feedSource.title
        FeedFilter.Timeline -> LocalFeedFlowStrings.current.flowTitle
        FeedFilter.Flow -> LocalFeedFlowStrings.current.flowTitle
        FeedFilter.Voices -> LocalFeedFlowStrings.current.voicesTitle
        FeedFilter.Read -> LocalFeedFlowStrings.current.drawerTitleRead
        FeedFilter.Bookmarks -> LocalFeedFlowStrings.current.drawerTitleBookmarks
        FeedFilter.Saved -> LocalFeedFlowStrings.current.drawerTitleBookmarks
        FeedFilter.Uncategorized -> LocalFeedFlowStrings.current.noCategory
        FeedFilter.UncategorizedStream -> LocalFeedFlowStrings.current.noCategory
    }

@Composable
private fun DrawerIcon(onDrawerMenuClick: () -> Unit, isDrawerOpen: Boolean) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    IconButton(
        modifier = Modifier.testTag(DrawerE2eIds.MENU_BUTTON),
        onClick = onDrawerMenuClick,
    ) {
        val icon = when {
            isDrawerOpen && isRtl -> CloseSidebarReversed
            isDrawerOpen -> CloseSidebar
            isRtl -> OpenSidebarReversed
            else -> OpenSidebar
        }
        Icon(
            imageVector = icon,
            contentDescription = LocalFeedFlowStrings.current.drawerMenuButtonContentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
