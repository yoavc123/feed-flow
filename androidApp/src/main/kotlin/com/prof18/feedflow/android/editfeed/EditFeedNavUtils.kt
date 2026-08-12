package com.prof18.feedflow.android.editfeed

import com.prof18.feedflow.android.EditFeed
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FlowPace
import com.prof18.feedflow.core.model.RateLimit
import com.prof18.feedflow.core.model.SourcePresentation
import com.prof18.feedflow.core.model.VoiceStatus

internal fun EditFeed.toFeedSource(): FeedSource {
    return FeedSource(
        id = id,
        url = url,
        title = title,
        category = if (categoryId != null && categoryTitle != null) {
            FeedSourceCategory(
                id = categoryId,
                title = categoryTitle,
            )
        } else {
            null
        },
        lastSyncTimestamp = lastSyncTimestamp,
        logoUrl = logoUrl,
        websiteUrl = websiteUrl,
        articleOpenMode = ArticleOpenMode.valueOf(articleOpenMode),
        isHiddenFromTimeline = isHidden,
        isPinned = isPinned,
        isNotificationEnabled = isNotificationEnabled,
        isHideImagesEnabled = isHideImagesEnabled,
        flowPace = flowPace?.let(FlowPace::valueOf),
        mutedUntilMillis = mutedUntilMillis,
        voiceStatus = VoiceStatus.valueOf(voiceStatus),
        sourcePresentation = SourcePresentation.valueOf(sourcePresentation),
        rateLimit = RateLimit.valueOf(rateLimit),
        fetchFailed = fetchFailed,
    )
}

internal fun FeedSource.toEditFeed(): EditFeed {
    return EditFeed(
        id = id,
        url = url,
        title = title,
        categoryId = category?.id,
        categoryTitle = category?.title,
        lastSyncTimestamp = lastSyncTimestamp,
        logoUrl = logoUrl,
        websiteUrl = websiteUrl,
        articleOpenMode = articleOpenMode.name,
        isHidden = isHiddenFromTimeline,
        isPinned = isPinned,
        isNotificationEnabled = isNotificationEnabled,
        isHideImagesEnabled = isHideImagesEnabled,
        flowPace = flowPace?.name,
        mutedUntilMillis = mutedUntilMillis,
        voiceStatus = voiceStatus.name,
        sourcePresentation = sourcePresentation.name,
        rateLimit = rateLimit.name,
        fetchFailed = fetchFailed,
    )
}
