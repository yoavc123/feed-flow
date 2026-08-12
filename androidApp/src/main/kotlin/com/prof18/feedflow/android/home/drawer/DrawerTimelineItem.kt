package com.prof18.feedflow.android.home.drawer

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.PreviewHelper

@Composable
internal fun DrawerTimelineItem(
    currentFeedFilter: FeedFilter,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    drawerItemVisualStyle: DrawerItemVisualStyle,
) {
    NavigationDrawerItem(
        modifier = Modifier
            .testTag(DrawerE2eIds.TIMELINE)
            .padding(vertical = drawerItemVisualStyle.itemVerticalPadding)
            .height(drawerItemVisualStyle.itemMinHeight),
        selected = currentFeedFilter is FeedFilter.Flow,
        label = {
            Text(
                text = LocalFeedFlowStrings.current.flowTitle,
            )
        },
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Feed,
                contentDescription = null,
            )
        },
        shape = drawerItemVisualStyle.itemShape,
        colors = drawerItemColors(drawerItemVisualStyle),
        onClick = {
            onFeedFilterSelected(FeedFilter.Flow)
        },
    )
}

@Preview
@Composable
private fun DrawerTimelineItemPreview() {
    PreviewHelper {
        DrawerTimelineItem(
            currentFeedFilter = FeedFilter.Flow,
            onFeedFilterSelected = {},
            drawerItemVisualStyle = DefaultDrawerItemVisualStyle,
        )
    }
}
