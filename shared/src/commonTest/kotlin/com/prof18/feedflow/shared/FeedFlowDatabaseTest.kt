package com.prof18.feedflow.shared

import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FlowPace
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.RateLimit
import com.prof18.feedflow.core.model.SourcePresentation
import com.prof18.feedflow.core.model.VoiceStatus
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import kotlinx.coroutines.test.runTest
import org.koin.core.component.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

class FeedFlowDatabaseTest : KoinTestBase() {
    private val databaseHelper by inject<DatabaseHelper>()
    private val sqlDriver by inject<SqlDriver>()

    @Test
    fun `insertCategories stores categories in database`() = runTest {
        val categories = listOf(
            FeedSourceCategory(id = "1", title = "Category 1"),
            FeedSourceCategory(id = "2", title = "Category 2"),
        )

        databaseHelper.insertCategories(categories)

        val savedCategories = databaseHelper.getFeedSourceCategories()
        assertEquals(2, savedCategories.size)
        assertEquals("Category 1", savedCategories.find { it.id == "1" }?.title)
        assertEquals("Category 2", savedCategories.find { it.id == "2" }?.title)
    }

    @Test
    fun `categories default to alphabetical order when positions match`() = runTest {
        databaseHelper.insertCategories(
            listOf(
                FeedSourceCategory(id = "beta", title = "Beta"),
                FeedSourceCategory(id = "alpha", title = "Alpha"),
            ),
        )

        val savedCategories = databaseHelper.getFeedSourceCategories()
        assertEquals(listOf("Alpha", "Beta"), savedCategories.map { it.title })
    }

    @Test
    fun `updateCategoryPositions persists category order`() = runTest {
        databaseHelper.insertCategories(
            listOf(
                FeedSourceCategory(id = "alpha", title = "Alpha"),
                FeedSourceCategory(id = "beta", title = "Beta"),
                FeedSourceCategory(id = "gamma", title = "Gamma"),
            ),
        )

        databaseHelper.updateCategoryPositions(listOf("gamma" to 0, "alpha" to 1, "beta" to 2))

        val savedCategories = databaseHelper.getFeedSourceCategories()
        assertEquals(listOf("Gamma", "Alpha", "Beta"), savedCategories.map { it.title })
        assertEquals(listOf(0, 1, 2), savedCategories.map { it.position })
    }

    @Test
    fun `updateFeedSourcePositions persists feed source order`() = runTest {
        databaseHelper.insertFeedSource(
            listOf(
                createParsedFeedSource(id = "alpha", title = "Alpha"),
                createParsedFeedSource(id = "beta", title = "Beta"),
                createParsedFeedSource(id = "gamma", title = "Gamma"),
            ),
        )

        databaseHelper.updateFeedSourcePositions(listOf("gamma", "alpha", "beta"))

        val savedFeedSources = databaseHelper.getFeedSources()
        assertEquals(listOf("Gamma", "Alpha", "Beta"), savedFeedSources.map { it.title })
        assertEquals(listOf(0, 1, 2), savedFeedSources.map { it.position })
    }

    @Test
    fun `insertFeedSource preserves existing feed source position`() = runTest {
        databaseHelper.insertFeedSource(
            listOf(
                createParsedFeedSource(id = "alpha", title = "Alpha"),
                createParsedFeedSource(id = "beta", title = "Beta"),
            ),
        )
        databaseHelper.updateFeedSourcePositions(listOf("beta", "alpha"))

        databaseHelper.insertFeedSource(listOf(createParsedFeedSource(id = "beta", title = "Beta Updated")))

        val savedFeedSources = databaseHelper.getFeedSources()
        assertEquals(listOf("Beta Updated", "Alpha"), savedFeedSources.map { it.title })
        assertEquals(0, savedFeedSources.first { it.id == "beta" }.position)
    }

    @Test
    fun `insertFeedItems stores feed item content`() = runTest {
        val feedItem = FeedItemGenerator.feedItem(
            id = "content-item",
            content = "<article>Stored feed content</article>",
        )
        databaseHelper.insertFeedSource(
            listOf(
                createParsedFeedSource(
                    id = feedItem.feedSource.id,
                    title = feedItem.feedSource.title,
                ),
            ),
        )

        databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)

        assertEquals("<article>Stored feed content</article>", databaseHelper.getFeedItemContent("content-item"))
    }

    @Test
    fun `let go is local and prevents a released item from returning on refresh`() = runTest {
        val feedItem = FeedItemGenerator.feedItem(
            id = "released-item",
            content = "Released content",
        )
        databaseHelper.insertFeedSource(
            listOf(createParsedFeedSource(feedItem.feedSource.id, feedItem.feedSource.title)),
        )
        databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)

        databaseHelper.letGoFeedItem(FeedItemId(feedItem.id))
        databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)

        assertEquals(null, databaseHelper.getFeedItemContent(feedItem.id))
    }

    @Test
    fun `undo let go restores the original article`() = runTest {
        val feedItem = FeedItemGenerator.feedItem(
            id = "restored-item",
            content = "Restored content",
        )
        databaseHelper.insertFeedSource(
            listOf(createParsedFeedSource(feedItem.feedSource.id, feedItem.feedSource.title)),
        )
        databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)
        databaseHelper.letGoFeedItem(FeedItemId(feedItem.id))

        databaseHelper.restoreLetGoFeedItem(feedItem)

        assertEquals("Restored content", databaseHelper.getFeedItemContent(feedItem.id))
    }

    @Test
    fun `flow includes the exact expiry boundary and excludes older articles`() = runTest {
        val now = 2_000_000_000_000L
        val source = createParsedFeedSource(id = "boundary-source", title = "Boundary")
        databaseHelper.insertFeedSource(listOf(source))
        databaseHelper.insertFeedItems(
            listOf(
                FeedItemGenerator.feedItem(
                    id = "at-boundary",
                    feedSource = source.toFeedSource(),
                    pubDateMillis = now - 24.hours.inWholeMilliseconds,
                ),
                FeedItemGenerator.feedItem(
                    id = "past-boundary",
                    feedSource = source.toFeedSource(),
                    pubDateMillis = now - 24.hours.inWholeMilliseconds - 1,
                ),
            ),
            lastSyncTimestamp = 0,
        )
        val result = databaseHelper.getFlowItems(now = now)

        assertEquals(listOf("at-boundary"), result.map { it.url_hash })
    }

    @Test
    fun `undated articles expire from when they were first seen`() = runTest {
        val now = 2_000_000_000_000L
        val boundarySource = createParsedFeedSource(id = "undated-boundary-source", title = "Boundary")
        val expiredSource = createParsedFeedSource(id = "undated-expired-source", title = "Expired")
        databaseHelper.insertFeedSource(listOf(boundarySource, expiredSource))
        databaseHelper.insertFeedItems(
            listOf(
                FeedItemGenerator.feedItem(
                    id = "undated-at-boundary",
                    feedSource = boundarySource.toFeedSource(),
                    pubDateMillis = null,
                ),
            ),
            lastSyncTimestamp = now - 24.hours.inWholeMilliseconds,
        )
        databaseHelper.insertFeedItems(
            listOf(
                FeedItemGenerator.feedItem(
                    id = "undated-past-boundary",
                    feedSource = expiredSource.toFeedSource(),
                    pubDateMillis = null,
                ),
            ),
            lastSyncTimestamp = now - 24.hours.inWholeMilliseconds - 1,
        )

        val result = databaseHelper.getFlowItems(now = now)

        assertEquals(listOf("undated-at-boundary"), result.map { it.url_hash })
    }

    @Test
    fun `saved articles bypass expiry while ordinary articles disappear`() = runTest {
        val now = 2_000_000_000_000L
        val source = createParsedFeedSource(id = "saved-source", title = "Saved")
        val expiredDate = now - 7 * 24.hours.inWholeMilliseconds
        databaseHelper.insertFeedSource(listOf(source))
        databaseHelper.insertFeedItems(
            listOf(
                FeedItemGenerator.feedItem(
                    id = "saved-expired",
                    feedSource = source.toFeedSource(),
                    pubDateMillis = expiredDate,
                    isBookmarked = true,
                ),
                FeedItemGenerator.feedItem(
                    id = "ordinary-expired",
                    feedSource = source.toFeedSource(),
                    pubDateMillis = expiredDate,
                ),
            ),
            lastSyncTimestamp = 0,
        )
        databaseHelper.updateBookmarkStatus(FeedItemId("saved-expired"), isBookmarked = true)

        val result = databaseHelper.getFlowItems(now = now)

        assertEquals(listOf("saved-expired"), result.map { it.url_hash })
    }

    @Test
    fun `muted sources disappear from Flow but remain available in Saved`() = runTest {
        val now = 2_000_000_000_000L
        val source = createParsedFeedSource(id = "muted-source", title = "Muted")
        databaseHelper.insertFeedSource(listOf(source))
        databaseHelper.insertFeedItems(
            listOf(
                FeedItemGenerator.feedItem(
                    id = "muted-saved",
                    feedSource = source.toFeedSource(),
                    pubDateMillis = now,
                    isBookmarked = true,
                ),
            ),
            lastSyncTimestamp = 0,
        )
        databaseHelper.updateBookmarkStatus(FeedItemId("muted-saved"), isBookmarked = true)
        databaseHelper.updateFeedSourceCalmSettings(
            feedSourceId = source.id,
            flowPace = null,
            mutedUntilMillis = now + 1.hours.inWholeMilliseconds,
            voiceStatus = VoiceStatus.AUTOMATIC,
            sourcePresentation = SourcePresentation.STANDARD,
            rateLimit = RateLimit.NONE,
        )

        assertEquals(emptyList(), databaseHelper.getFlowItems(now).map { it.url_hash })
        assertEquals(
            listOf("muted-saved"),
            databaseHelper.getFeedItems(
                feedFilter = FeedFilter.Saved,
                pageSize = 20,
                showReadItems = true,
                sortOrder = FeedOrder.NEWEST_FIRST,
                currentTimeMillis = now,
            ).map { it.url_hash },
        )
    }

    @Test
    fun `each source pace applies in Flow and its Stream`() = runTest {
        val now = 2_000_000_000_000L
        val category = FeedSourceCategory(id = "mixed-stream", title = "Mixed stream")
        val flashSource = createParsedFeedSource(
            id = "flash-source",
            title = "Flash source",
            category = category,
        )
        val slowSource = createParsedFeedSource(
            id = "slow-source",
            title = "Slow source",
            category = category,
        )
        databaseHelper.insertCategories(listOf(category))
        databaseHelper.insertFeedSource(listOf(flashSource, slowSource))
        databaseHelper.updateFeedSourceCalmSettings(
            feedSourceId = flashSource.id,
            flowPace = FlowPace.FLASH,
            mutedUntilMillis = null,
            voiceStatus = VoiceStatus.AUTOMATIC,
            sourcePresentation = SourcePresentation.STANDARD,
            rateLimit = RateLimit.NONE,
        )
        databaseHelper.updateFeedSourceCalmSettings(
            feedSourceId = slowSource.id,
            flowPace = FlowPace.SLOW,
            mutedUntilMillis = null,
            voiceStatus = VoiceStatus.AUTOMATIC,
            sourcePresentation = SourcePresentation.STANDARD,
            rateLimit = RateLimit.NONE,
        )
        databaseHelper.insertFeedItems(
            listOf(
                FeedItemGenerator.feedItem(
                    id = "fresh-flash",
                    feedSource = flashSource.toFeedSource(),
                    pubDateMillis = now - 2.hours.inWholeMilliseconds,
                ),
                FeedItemGenerator.feedItem(
                    id = "expired-flash",
                    feedSource = flashSource.toFeedSource(),
                    pubDateMillis = now - 4.hours.inWholeMilliseconds,
                ),
                FeedItemGenerator.feedItem(
                    id = "fresh-slow",
                    feedSource = slowSource.toFeedSource(),
                    pubDateMillis = now - 48.hours.inWholeMilliseconds,
                ),
                FeedItemGenerator.feedItem(
                    id = "expired-slow",
                    feedSource = slowSource.toFeedSource(),
                    pubDateMillis = now - 73.hours.inWholeMilliseconds,
                ),
            ),
            lastSyncTimestamp = 0,
        )

        val flowResult = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Flow,
            pageSize = 20,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
            currentTimeMillis = now,
        )
        val streamResult = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Stream(category),
            pageSize = 20,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
            currentTimeMillis = now,
        )

        val expected = listOf("fresh-flash", "fresh-slow")
        assertEquals(expected, flowResult.map { it.url_hash })
        assertEquals(expected, streamResult.map { it.url_hash })
    }

    @Test
    fun `rate limiting keeps only the newest configured number of articles per window`() = runTest {
        val now = 2_000_030_400_000L
        val source = createParsedFeedSource(id = "limited-source", title = "Limited")
        databaseHelper.insertFeedSource(listOf(source))
        databaseHelper.updateFeedSourceCalmSettings(
            feedSourceId = source.id,
            flowPace = FlowPace.TIMELESS,
            mutedUntilMillis = null,
            voiceStatus = VoiceStatus.AUTOMATIC,
            sourcePresentation = SourcePresentation.STANDARD,
            rateLimit = RateLimit.THREE_PER_DAY,
        )
        databaseHelper.insertFeedItems(
            (1..4).map { index ->
                FeedItemGenerator.feedItem(
                    id = "limited-$index",
                    feedSource = source.toFeedSource(),
                    pubDateMillis = now - index.hours.inWholeMilliseconds,
                )
            },
            lastSyncTimestamp = 0,
        )

        val result = databaseHelper.getFlowItems(now)

        assertEquals(listOf("limited-1", "limited-2", "limited-3"), result.map { it.url_hash })
    }

    @Test
    fun `release tombstones survive for thirty days and are pruned afterward`() = runTest {
        val source = createParsedFeedSource(id = "released-source", title = "Released")
        val recentItem = FeedItemGenerator.feedItem(id = "recent-release", feedSource = source.toFeedSource())
        val oldItem = FeedItemGenerator.feedItem(id = "old-release", feedSource = source.toFeedSource())
        databaseHelper.insertFeedSource(listOf(source))
        databaseHelper.insertFeedItems(listOf(recentItem, oldItem), lastSyncTimestamp = 0)
        databaseHelper.letGoFeedItem(FeedItemId(recentItem.id))
        databaseHelper.letGoFeedItem(FeedItemId(oldItem.id))
        sqlDriver.execute(
            identifier = null,
            sql = "UPDATE released_feed_items SET released_at = ? WHERE url_hash = ?",
            parameters = 2,
        ) {
            bindLong(0, 0L)
            bindString(1, oldItem.id)
        }

        databaseHelper.pruneReleasedFeedItems()
        databaseHelper.insertFeedItems(listOf(recentItem, oldItem), lastSyncTimestamp = 0)

        assertEquals(null, databaseHelper.getFeedItemContent(recentItem.id))
        assertEquals(oldItem.content, databaseHelper.getFeedItemContent(oldItem.id))
    }

    @Test
    fun `insertFeedSourcePreference stores the article open mode override`() = runTest {
        databaseHelper.insertFeedSource(
            listOf(createParsedFeedSource(id = "alpha", title = "Alpha")),
        )

        databaseHelper.insertFeedSourcePreference(
            feedSourceId = "alpha",
            articleOpenMode = ArticleOpenMode.FEED_CONTENT,
            isHidden = false,
            isPinned = false,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        )

        assertEquals(ArticleOpenMode.FEED_CONTENT, databaseHelper.getFeedSource("alpha")?.articleOpenMode)
    }

    @Test
    fun `getFeedItemUrlInfo returns the feed article open mode override`() = runTest {
        val feedItem = FeedItemGenerator.feedItem(id = "cs-item")
        databaseHelper.insertFeedSource(
            listOf(createParsedFeedSource(id = feedItem.feedSource.id, title = feedItem.feedSource.title)),
        )
        databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = feedItem.feedSource.id,
            articleOpenMode = ArticleOpenMode.FEED_CONTENT,
            isHidden = false,
            isPinned = false,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        )

        assertEquals(
            ArticleOpenMode.FEED_CONTENT,
            databaseHelper.getFeedItemUrlInfo("cs-item")?.articleOpenMode,
        )
    }

    @Test
    fun `prefetch queries exclude url-less feed content items`() = runTest {
        val webItem = FeedItemGenerator.feedItem(id = "prefetch-web").copy(url = "https://example.com/article")
        val feedOnlyItem = FeedItemGenerator.feedItem(id = "prefetch-feed-only").copy(url = "")
        databaseHelper.insertFeedSource(
            listOf(createParsedFeedSource(id = webItem.feedSource.id, title = webItem.feedSource.title)),
        )
        databaseHelper.insertFeedItems(listOf(webItem, feedOnlyItem), lastSyncTimestamp = 0)

        assertEquals(listOf("prefetch-web"), databaseHelper.getUnfetchedItems().map { it.feedItemId })
        assertEquals(listOf("prefetch-web"), databaseHelper.getFirstUnfetchedItemsBatch(10).map { it.feedItemId })
    }

    @Test
    fun `insertCategories preserves existing category position`() = runTest {
        databaseHelper.insertCategories(
            listOf(
                FeedSourceCategory(id = "alpha", title = "Alpha"),
                FeedSourceCategory(id = "beta", title = "Beta"),
            ),
        )
        databaseHelper.updateCategoryPositions(listOf("beta" to 0, "alpha" to 1))

        databaseHelper.insertCategories(listOf(FeedSourceCategory(id = "beta", title = "Beta Updated")))

        val savedCategories = databaseHelper.getFeedSourceCategories()
        assertEquals(listOf("Beta Updated", "Alpha"), savedCategories.map { it.title })
        assertEquals(0, savedCategories.first { it.id == "beta" }.position)
    }

    @Test
    fun `getPinnedFeedSourceIds returns pinned ids sorted by position`() = runTest {
        databaseHelper.insertFeedSource(
            listOf(
                createParsedFeedSource(id = "alpha", title = "Alpha"),
                createParsedFeedSource(id = "beta", title = "Beta"),
                createParsedFeedSource(id = "gamma", title = "Gamma"),
            ),
        )
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = "alpha",
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHidden = false,
            isPinned = true,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        )
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = "beta",
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHidden = false,
            isPinned = true,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        )
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = "gamma",
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHidden = false,
            isPinned = false,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        )
        databaseHelper.updatePinnedPositions(listOf("beta", "alpha"))

        val pinnedIds = databaseHelper.getPinnedFeedSourceIds()

        assertEquals(listOf("beta", "alpha"), pinnedIds)
    }

    @Test
    fun `insertFeedSourcePreference round-trips the hide images flag`() = runTest {
        databaseHelper.insertFeedSource(
            listOf(createParsedFeedSource(id = "alpha", title = "Alpha")),
        )

        databaseHelper.insertFeedSourcePreference(
            feedSourceId = "alpha",
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHidden = false,
            isPinned = false,
            isNotificationEnabled = false,
            isHideImagesEnabled = true,
        )

        assertEquals(true, databaseHelper.getFeedSource("alpha")?.isHideImagesEnabled)

        databaseHelper.insertFeedSourcePreference(
            feedSourceId = "alpha",
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHidden = false,
            isPinned = false,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        )

        assertEquals(false, databaseHelper.getFeedSource("alpha")?.isHideImagesEnabled)
    }

    @Test
    fun `insertFeedSourcePreference preserves pinned position`() = runTest {
        databaseHelper.insertFeedSource(
            listOf(
                createParsedFeedSource(id = "alpha", title = "Alpha"),
                createParsedFeedSource(id = "beta", title = "Beta"),
            ),
        )
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = "alpha",
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHidden = false,
            isPinned = true,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        )
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = "beta",
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHidden = false,
            isPinned = true,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        )
        databaseHelper.updatePinnedPositions(listOf("beta", "alpha"))

        databaseHelper.insertFeedSourcePreference(
            feedSourceId = "alpha",
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHidden = true,
            isPinned = true,
            isNotificationEnabled = true,
            isHideImagesEnabled = false,
        )

        val updatedSource = databaseHelper.getFeedSource("alpha")
        assertEquals(1, updatedSource?.pinnedPosition)
        assertEquals(true, updatedSource?.isHiddenFromTimeline)
        assertEquals(true, updatedSource?.isNotificationEnabled)
    }

    private fun createParsedFeedSource(
        id: String,
        title: String,
        category: FeedSourceCategory? = null,
    ) = ParsedFeedSource(
        id = id,
        url = "https://example.com/$id.xml",
        title = title,
        category = category,
        logoUrl = null,
        websiteUrl = null,
    )

    private suspend fun DatabaseHelper.getFlowItems(now: Long) = getFeedItems(
        feedFilter = FeedFilter.Flow,
        pageSize = 20,
        showReadItems = true,
        sortOrder = FeedOrder.NEWEST_FIRST,
        currentTimeMillis = now,
    )

    private fun ParsedFeedSource.toFeedSource() = com.prof18.feedflow.core.model.FeedSource(
        id = id,
        url = url,
        title = title,
        category = category,
        lastSyncTimestamp = null,
        logoUrl = logoUrl,
        websiteUrl = websiteUrl,
        fetchFailed = false,
        articleOpenMode = ArticleOpenMode.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
        isHideImagesEnabled = false,
    )
}
