package com.prof18.feedflow.shared.ui.home.components.list

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.window.PopupProperties
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedItemUrlTitle
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.ui.home.components.ShareCommentsIcon
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal actual fun FeedItemContextMenu(
    showMenu: Boolean,
    menuPositionInWindow: Offset?,
    feedItem: FeedItem,
    shareMenuLabel: String,
    shareCommentsMenuLabel: String,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onLetGo: ((FeedItemId) -> Unit)?,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    closeMenu: () -> Unit,
    onShareClick: (FeedItemUrlTitle) -> Unit,
    onOpenFeedSettings: (FeedSource) -> Unit,
    onOpenFeedWebsite: (String) -> Unit,
    onMarkAllAboveAsRead: (String) -> Unit,
    onMarkAllBelowAsRead: (String) -> Unit,
) {
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = closeMenu,
        shape = MaterialTheme.shapes.large,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        DropdownMenuItem(
            text = { Text(LocalFeedFlowStrings.current.openFeedSettings) },
            leadingIcon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
            onClick = {
                onOpenFeedSettings(feedItem.feedSource)
                closeMenu()
            },
        )

        val websiteUrl = feedItem.feedSource.websiteUrlFallback()
        if (websiteUrl != null) {
            DropdownMenuItem(
                text = { Text(LocalFeedFlowStrings.current.openFeedWebsiteButton) },
                leadingIcon = { Icon(imageVector = Icons.Default.Public, contentDescription = null) },
                onClick = {
                    onOpenFeedWebsite(websiteUrl)
                    closeMenu()
                },
            )
        }

        HorizontalDivider()

        if (feedItem.commentsUrl != null) {
            ShareCommentsMenuItem(
                feedItem = feedItem,
                shareCommentsMenuLabel = shareCommentsMenuLabel,
                onShareClick = onShareClick,
                closeMenu = closeMenu,
            )

            OpenCommentsMenuItem(
                feedItem = feedItem,
                closeMenu = closeMenu,
                onCommentClick = onCommentClick,
            )

            HorizontalDivider()
        }

        if (feedItem.url.isNotBlank()) {
            ShareMenuItem(
                feedItem = feedItem,
                shareMenuLabel = shareMenuLabel,
                onShareClick = onShareClick,
                closeMenu = closeMenu,
            )
        }

        ChangeBookmarkStatusMenuItem(
            feedItem = feedItem,
            onBookmarkClick = onBookmarkClick,
            closeMenu = closeMenu,
        )

        if (onLetGo != null) {
            LetGoMenuItem(
                feedItem = feedItem,
                onLetGo = onLetGo,
                closeMenu = closeMenu,
            )
        }
    }
}

@Composable
private fun ShareMenuItem(
    feedItem: FeedItem,
    shareMenuLabel: String,
    onShareClick: (FeedItemUrlTitle) -> Unit,
    closeMenu: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = shareMenuLabel,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
            )
        },
        onClick = {
            onShareClick(
                FeedItemUrlTitle(
                    title = feedItem.title,
                    url = feedItem.url,
                ),
            )
            closeMenu()
        },
    )
}

@Composable
private fun ShareCommentsMenuItem(
    feedItem: FeedItem,
    shareCommentsMenuLabel: String,
    onShareClick: (FeedItemUrlTitle) -> Unit,
    closeMenu: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = shareCommentsMenuLabel,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = ShareCommentsIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = {
            onShareClick(
                FeedItemUrlTitle(
                    title = feedItem.title,
                    url = requireNotNull(feedItem.commentsUrl),
                ),
            )
            closeMenu()
        },
    )
}

@Composable
private fun OpenCommentsMenuItem(
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    feedItem: FeedItem,
    closeMenu: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                LocalFeedFlowStrings.current.menuOpenComments,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Forum,
                contentDescription = null,
            )
        },
        onClick = {
            onCommentClick(
                FeedItemUrlInfo(
                    id = feedItem.id,
                    url = requireNotNull(feedItem.commentsUrl),
                    title = feedItem.title,
                    openOnlyOnBrowser = true,
                    isBookmarked = feedItem.isBookmarked,
                    articleOpenMode = ArticleOpenMode.PREFERRED_BROWSER,
                    commentsUrl = feedItem.commentsUrl,
                ),
            )
            closeMenu()
        },
    )
}

@Composable
private fun ChangeBookmarkStatusMenuItem(
    feedItem: FeedItem,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    closeMenu: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = if (feedItem.isBookmarked) {
                    LocalFeedFlowStrings.current.menuRemoveFromBookmark
                } else {
                    LocalFeedFlowStrings.current.menuAddToBookmark
                },
            )
        },
        leadingIcon = {
            if (feedItem.isBookmarked) {
                Icon(
                    imageVector = Icons.Default.BookmarkRemove,
                    contentDescription = null,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = null,
                )
            }
        },
        onClick = {
            onBookmarkClick(
                FeedItemId(feedItem.id),
                !feedItem.isBookmarked,
            )
            closeMenu()
        },
    )
}

@Composable
private fun LetGoMenuItem(
    feedItem: FeedItem,
    onLetGo: (FeedItemId) -> Unit,
    closeMenu: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(LocalFeedFlowStrings.current.letGo)
        },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Clear, contentDescription = null)
        },
        onClick = {
            onLetGo(FeedItemId(feedItem.id))
            closeMenu()
        },
    )
}
