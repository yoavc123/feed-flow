package com.prof18.feedflow.core.model

sealed class FeedFilter {

    data object Flow : FeedFilter()

    data object Saved : FeedFilter()

    data object Voices : FeedFilter()

    data object UncategorizedStream : FeedFilter()

    data class Stream(
        val feedCategory: FeedSourceCategory,
    ) : FeedFilter()

    @Deprecated("Use Flow")
    data object Timeline : FeedFilter()

    @Deprecated("Read state is not a presentation filter")
    data object Read : FeedFilter()

    @Deprecated("Use Saved")
    data object Bookmarks : FeedFilter()

    @Deprecated("Use UncategorizedStream")
    data object Uncategorized : FeedFilter()

    @Deprecated("Use Stream")
    data class Category(
        val feedCategory: FeedSourceCategory,
    ) : FeedFilter()

    data class Source(
        val feedSource: FeedSource,
    ) : FeedFilter()
}
