package com.prof18.feedflow.shared.domain.feed

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.domain.TimeProvider
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedUpdateStatus
import com.prof18.feedflow.core.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.db.SelectFeeds
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.mappers.toFeedItem
import com.prof18.feedflow.shared.presentation.model.DatabaseError
import com.prof18.feedflow.shared.presentation.model.ErrorState
import com.prof18.feedflow.shared.utils.executeWithRetry
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import com.prof18.feedflow.core.model.DatabaseError as DatabaseErrorCode

internal class FeedStateRepository(
    private val databaseHelper: DatabaseHelper,
    private val logger: Logger,
    private val settingsRepository: SettingsRepository,
    private val feedAppearanceSettingsRepository: FeedAppearanceSettingsRepository,
    private val dateFormatter: DateFormatter,
    private val timeProvider: TimeProvider,
) {
    private val errorMutableState: MutableSharedFlow<ErrorState> = MutableSharedFlow()
    val errorState = errorMutableState.asSharedFlow()

    private val updateMutableState: MutableStateFlow<FeedUpdateStatus> = MutableStateFlow(
        FinishedFeedUpdateStatus,
    )
    val updateState = updateMutableState.asStateFlow()

    private val mutableFeedState: MutableStateFlow<ImmutableList<FeedItem>> = MutableStateFlow(persistentListOf())
    val feedState = mutableFeedState.asStateFlow()

    private val mutablePinnedFeedState: MutableStateFlow<ImmutableList<FeedItem>> =
        MutableStateFlow(persistentListOf())
    val pinnedFeedState = mutablePinnedFeedState.asStateFlow()

    private val mutableFeedListVersion: MutableStateFlow<Long> = MutableStateFlow(0L)
    val feedListVersion = mutableFeedListVersion.asStateFlow()

    private val pendingNewArticlesMutableState = MutableStateFlow(0)
    val pendingNewArticlesState: StateFlow<Int> = pendingNewArticlesMutableState.asStateFlow()

    private val currentFeedFilterMutableState: MutableStateFlow<FeedFilter> = MutableStateFlow(FeedFilter.Flow)
    val currentFeedFilter: StateFlow<FeedFilter> = currentFeedFilterMutableState.asStateFlow()

    private var lastFetchedPubDate: Long? = null
    private var lastFetchedUrlHash: String? = null
    private var hasMorePages: Boolean = true
    private var isLoadingMore: Boolean = false

    suspend fun getFeeds() {
        pendingNewArticlesMutableState.update { 0 }
        try {
            val feedOrder = feedAppearanceSettingsRepository.getFeedOrder()
            val nowMillis = timeProvider.nowMillis()
            val currentFilter = currentFeedFilterMutableState.value

            val feeds = executeWithRetry {
                databaseHelper.getFeedItems(
                    feedFilter = currentFilter,
                    pageSize = FEED_DB_PAGE_SIZE,
                    showReadItems = settingsRepository.getShowReadArticlesTimeline(),
                    sortOrder = feedOrder,
                    currentTimeMillis = nowMillis,
                    pinnedFilter = currentFilter.regularPinnedFilter(),
                )
            }
            val pinnedFeeds = if (currentFilter.hasPinnedSection()) {
                executeWithRetry {
                    databaseHelper.getFeedItems(
                        feedFilter = currentFilter,
                        pageSize = PINNED_DB_PAGE_SIZE,
                        showReadItems = true,
                        sortOrder = feedOrder,
                        currentTimeMillis = nowMillis,
                        pinnedFilter = 1,
                    )
                }
            } else {
                emptyList()
            }
            updateCursor(feeds)
            val settings = feedAppearanceSettingsRepository.getFeedItemMappingSettings()

            if (feeds.isNotEmpty()) {
                updateMutableState.update { FinishedFeedUpdateStatus }
            }
            updateFeedState {
                feeds.map {
                    it.toFeedItem(
                        dateFormatter = dateFormatter,
                        settings = settings,
                        nowMillis = nowMillis,
                    )
                }.toImmutableList()
            }
            mutablePinnedFeedState.update {
                pinnedFeeds.map { row ->
                    row.toFeedItem(
                        dateFormatter = dateFormatter,
                        settings = settings,
                        nowMillis = nowMillis,
                    )
                }.toImmutableList()
            }
        } catch (e: Throwable) {
            logger.d(e) { "Something wrong while getting data from Database" }
            errorMutableState.emit(DatabaseError(DatabaseErrorCode.FeedRetrievalFailed))
        }
    }

    suspend fun refreshPendingNewArticlesCount() {
        try {
            val feedFilter = currentFeedFilterMutableState.value
            val nowMillis = timeProvider.nowMillis()
            val feedListVersion = mutableFeedListVersion.value
            val visibleIds = feedState.value.map { it.id }.toHashSet()
            val feeds = executeWithRetry {
                databaseHelper.getFeedItems(
                    feedFilter = feedFilter,
                    pageSize = FEED_DB_PAGE_SIZE,
                    showReadItems = settingsRepository.getShowReadArticlesTimeline(),
                    sortOrder = FeedOrder.NEWEST_FIRST,
                    currentTimeMillis = nowMillis,
                    pinnedFilter = feedFilter.regularPinnedFilter(),
                )
            }
            if (currentFeedFilterMutableState.value != feedFilter || mutableFeedListVersion.value != feedListVersion) {
                return
            }
            pendingNewArticlesMutableState.update {
                feeds.count { it.url_hash !in visibleIds }
            }
        } catch (_: Throwable) {
            // The pill is best-effort and must not turn a successful refresh into an error.
        }
    }

    suspend fun loadMoreFeeds() {
        if (!hasMorePages || isLoadingMore) {
            return
        }
        isLoadingMore = true
        try {
            val feedOrder = feedAppearanceSettingsRepository.getFeedOrder()
            val nowMillis = timeProvider.nowMillis()
            val currentFilter = currentFeedFilterMutableState.value
            val feeds = executeWithRetry {
                databaseHelper.getFeedItems(
                    feedFilter = currentFilter,
                    pageSize = FEED_DB_PAGE_SIZE,
                    showReadItems = settingsRepository.getShowReadArticlesTimeline(),
                    sortOrder = feedOrder,
                    lastPubDate = lastFetchedPubDate,
                    lastUrlHash = lastFetchedUrlHash,
                    currentTimeMillis = nowMillis,
                    pinnedFilter = currentFilter.regularPinnedFilter(),
                )
            }
            updateCursor(feeds)
            val settings = feedAppearanceSettingsRepository.getFeedItemMappingSettings()
            updateFeedState(incrementListVersion = false) { currentItems ->
                val newList = feeds.map {
                    it.toFeedItem(
                        dateFormatter = dateFormatter,
                        settings = settings,
                        nowMillis = nowMillis,
                    )
                }.toImmutableList()
                (currentItems + newList).toImmutableList()
            }
        } catch (e: Throwable) {
            logger.d(e) { "Something wrong while getting data from Database" }
            errorMutableState.emit(DatabaseError(DatabaseErrorCode.PaginationFailed))
        } finally {
            isLoadingMore = false
        }
    }

    // Only ever called after a successful query: a failed one must leave the cursor pointing at
    // the list the user is still looking at, otherwise the next page restarts from the top and is
    // appended as duplicates. See .ai/PAGINATION.md.
    private fun updateCursor(fetchedRows: List<SelectFeeds>) {
        hasMorePages = fetchedRows.size == FEED_DB_PAGE_SIZE.toInt()
        val lastRow = fetchedRows.lastOrNull()
        lastFetchedPubDate = lastRow?.pub_date
        lastFetchedUrlHash = lastRow?.url_hash
    }

    suspend fun updateFeedFilter(feedFilter: FeedFilter) {
        currentFeedFilterMutableState.update {
            feedFilter
        }
        getFeeds()
    }

    suspend fun updateFeedSourceFilter(feedSourceId: String) {
        val feedSource = databaseHelper.getFeedSource(feedSourceId)
        if (feedSource == null) {
            getFeeds()
            return
        }
        val newFeedFilter = FeedFilter.Source(
            feedSource = feedSource,
        )
        currentFeedFilterMutableState.update {
            newFeedFilter
        }
        getFeeds()
    }

    suspend fun updateCategoryFilter(categoryId: String) {
        val category = databaseHelper.getFeedSourceCategory(categoryId)
        if (category == null) {
            getFeeds()
            return
        }
        val newFeedFilter = FeedFilter.Stream(
            feedCategory = category,
        )
        currentFeedFilterMutableState.update {
            newFeedFilter
        }
        getFeeds()
    }

    fun getUnreadFeedCountFlow(): Flow<Long> =
        currentFeedFilter.flatMapLatest { feedFilter ->
            databaseHelper.getUnreadFeedCountFlow(
                feedFilter = feedFilter,
            )
        }

    fun getUnreadBookmarksCountFlow(): Flow<Long> =
        databaseHelper.getUnreadFeedCountFlow(
            feedFilter = FeedFilter.Bookmarks,
        )

    fun getUnreadTimelineCountFlow(): Flow<Long> =
        databaseHelper.getUnreadFeedCountFlow(
            feedFilter = FeedFilter.Timeline,
        )

    fun updateCurrentFilterName(newName: String) {
        val currentFilter = currentFeedFilter.value
        if (currentFilter is FeedFilter.Source) {
            currentFeedFilterMutableState.update {
                FeedFilter.Source(
                    feedSource = currentFilter.feedSource.copy(
                        title = newName,
                    ),
                )
            }
        }
    }

    fun markAsRead(itemsToUpdates: HashSet<FeedItemId>) {
        val hideReadItems = settingsRepository.getHideReadItems()
        val currentFilter = currentFeedFilter.value
        val shouldRemoveReadItems = hideReadItems && currentFilter != FeedFilter.Read
        updateFeedState(incrementListVersion = shouldRemoveReadItems) { currentItems ->
            currentItems.mapNotNull { feedItem ->
                if (FeedItemId(feedItem.id) in itemsToUpdates) {
                    val updatedItem = feedItem.copy(isRead = true)
                    if (shouldRemoveReadItems) {
                        null
                    } else {
                        updatedItem
                    }
                } else {
                    feedItem
                }
            }.toImmutableList()
        }
    }

    fun getItemsAbove(targetItemId: String): List<FeedItemId> {
        val currentItems = feedState.value
        val targetIndex = currentItems.indexOfFirst { it.id == targetItemId }

        if (targetIndex == -1) return emptyList()

        return currentItems.subList(0, targetIndex + 1)
            .filter { !it.isRead }
            .map { FeedItemId(it.id) }
    }

    fun getItemsBelow(targetItemId: String): List<FeedItemId> {
        val currentItems = feedState.value
        val targetIndex = currentItems.indexOfFirst { it.id == targetItemId }

        if (targetIndex == -1) return emptyList()

        return currentItems.subList(targetIndex, currentItems.size)
            .filter { !it.isRead }
            .map { FeedItemId(it.id) }
    }

    fun getCurrentFeedFilter(): FeedFilter =
        currentFeedFilter.value

    fun updateBookmarkStatus(feedItemId: FeedItemId, isBookmarked: Boolean) {
        updateFeedState { currentItems ->
            currentItems.mapNotNull { feedItem ->
                if (feedItem.id == feedItemId.id) {
                    if (currentFeedFilter.value == FeedFilter.Bookmarks && !isBookmarked) {
                        null
                    } else {
                        feedItem.copy(isBookmarked = isBookmarked)
                    }
                } else {
                    feedItem
                }
            }.toImmutableList()
        }
    }

    fun letGo(feedItemId: FeedItemId) {
        updateFeedState(incrementListVersion = true) { currentItems ->
            currentItems.filterNot { it.id == feedItemId.id }.toImmutableList()
        }
        mutablePinnedFeedState.update { currentItems ->
            currentItems.filterNot { it.id == feedItemId.id }.toImmutableList()
        }
    }

    fun updateReadStatus(feedItemId: FeedItemId, isRead: Boolean) {
        val hideReadItems = settingsRepository.getHideReadItems()
        val currentFilter = currentFeedFilter.value
        updateFeedState { currentItems ->
            currentItems.mapNotNull { feedItem ->
                if (feedItem.id == feedItemId.id) {
                    if (currentFilter == FeedFilter.Read && !isRead) {
                        null
                    } else {
                        val updatedItem = feedItem.copy(isRead = isRead)
                        if (hideReadItems && isRead && currentFilter != FeedFilter.Read) {
                            null
                        } else {
                            updatedItem
                        }
                    }
                } else {
                    feedItem
                }
            }.toImmutableList()
        }
    }

    fun markItemsAboveAsRead(targetItemId: String) {
        val hideReadItems = settingsRepository.getHideReadItems()
        val currentFilter = currentFeedFilter.value
        updateFeedState { currentItems ->
            val targetIndex = currentItems.indexOfFirst { it.id == targetItemId }
            if (targetIndex == -1) {
                currentItems
            } else {
                currentItems.mapIndexedNotNull { index, feedItem ->
                    if (index <= targetIndex && !feedItem.isRead) {
                        val updatedItem = feedItem.copy(isRead = true)
                        if (hideReadItems && currentFilter != FeedFilter.Read) {
                            null
                        } else {
                            updatedItem
                        }
                    } else {
                        feedItem
                    }
                }.toImmutableList()
            }
        }
    }

    fun markItemsBelowAsRead(targetItemId: String) {
        val hideReadItems = settingsRepository.getHideReadItems()
        val currentFilter = currentFeedFilter.value
        updateFeedState { currentItems ->
            val targetIndex = currentItems.indexOfFirst { it.id == targetItemId }
            if (targetIndex == -1) {
                currentItems
            } else {
                currentItems.mapIndexedNotNull { index, feedItem ->
                    if (index >= targetIndex && !feedItem.isRead) {
                        val updatedItem = feedItem.copy(isRead = true)
                        if (hideReadItems && currentFilter != FeedFilter.Read) {
                            null
                        } else {
                            updatedItem
                        }
                    } else {
                        feedItem
                    }
                }.toImmutableList()
            }
        }
    }

    fun emitUpdateStatus(status: FeedUpdateStatus) {
        updateMutableState.update {
            status
        }
    }

    suspend fun emitErrorState(errorState: ErrorState) {
        errorMutableState.emit(errorState)
    }

    suspend fun getNextArticle(currentArticleId: String): FeedItem? {
        val currentList = mutableFeedState.value
        val currentIndex = currentList.indexOfFirst { it.id == currentArticleId }
        if (currentIndex == -1) return null

        val nextIndex = currentIndex + 1
        if (nextIndex < currentList.size) {
            val shouldLoadMore = nextIndex >= currentList.size - PAGINATION_THRESHOLD
            if (shouldLoadMore) {
                loadMoreFeeds()
            }
            return currentList.getOrNull(nextIndex)
        }
        return null
    }

    fun getPreviousArticle(currentArticleId: String): FeedItem? {
        val currentList = mutableFeedState.value
        val currentIndex = currentList.indexOfFirst { it.id == currentArticleId }
        if (currentIndex == -1 || currentIndex == 0) return null
        return currentList.getOrNull(currentIndex - 1)
    }

    fun getArticlePosition(currentArticleId: String): ArticlePosition? {
        val currentList = mutableFeedState.value
        val currentIndex = currentList.indexOfFirst { it.id == currentArticleId }
        if (currentIndex == -1) return null
        return ArticlePosition(
            currentPosition = currentIndex + 1,
            totalArticles = currentList.size,
        )
    }

    private fun updateFeedState(
        incrementListVersion: Boolean = true,
        transform: (ImmutableList<FeedItem>) -> ImmutableList<FeedItem>,
    ) {
        mutableFeedState.update { currentItems ->
            transform(currentItems)
        }
        if (incrementListVersion) {
            mutableFeedListVersion.update { it + 1 }
        }
    }

    data class ArticlePosition(
        val currentPosition: Int,
        val totalArticles: Int,
    )

    companion object {
        internal const val FEED_DB_PAGE_SIZE = 40L
        private const val PINNED_DB_PAGE_SIZE = 200L
        private const val PAGINATION_THRESHOLD = 5
    }
}

private fun FeedFilter.hasPinnedSection(): Boolean =
    this == FeedFilter.Flow || this == FeedFilter.Timeline

private fun FeedFilter.regularPinnedFilter(): Long? =
    if (hasPinnedSection()) 0 else null
