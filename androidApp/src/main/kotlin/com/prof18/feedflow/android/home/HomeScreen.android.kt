package com.prof18.feedflow.android.home

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.categoryselection.EditCategorySheet
import com.prof18.feedflow.android.openShareSheet
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedOperation
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.isReaderMode
import com.prof18.feedflow.core.model.resolveArticleOpenMode
import com.prof18.feedflow.shared.presentation.ChangeFeedCategoryViewModel
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.model.NextFeedPreviewState
import com.prof18.feedflow.shared.presentation.model.NextFeedPreviewState.NextFeedPreviewDisabledState
import com.prof18.feedflow.shared.presentation.model.NextFeedPreviewState.NextFeedPreviewEnabledState
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import com.prof18.feedflow.shared.ui.home.FeedListActions
import com.prof18.feedflow.shared.ui.home.FeedManagementActions
import com.prof18.feedflow.shared.ui.home.HomeDisplayState
import com.prof18.feedflow.shared.ui.home.NextFeedDisplayState
import com.prof18.feedflow.shared.ui.home.ShareBehavior
import com.prof18.feedflow.shared.ui.home.components.LoadingOperationDialog
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.syncErrorMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun HomeScreen(
    homeViewModel: HomeViewModel,
    navigateToReaderMode: (FeedItemUrlInfo) -> Unit,
    onSettingsButtonClicked: () -> Unit,
    onAddFeedClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAccountsClick: () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onFeedSuggestionsClick: () -> Unit,
    onNavigateToNextFeed: () -> Unit,
    onImportExportClick: () -> Unit = {},
    onFocusClick: () -> Unit = {},
) {
    val browserManager = koinInject<BrowserManager>()
    val changeFeedCategoryViewModel: ChangeFeedCategoryViewModel = koinInject()

    val loadingState by homeViewModel.loadingState.collectAsStateWithLifecycle()
    val feedState by homeViewModel.feedState.collectAsStateWithLifecycle()
    val pinnedFeedState by homeViewModel.pinnedFeedState.collectAsStateWithLifecycle()
    val navDrawerState by homeViewModel.navDrawerState.collectAsStateWithLifecycle()
    val currentFeedFilter by homeViewModel.currentFeedFilter.collectAsStateWithLifecycle()
    val nextFeedPreviewState: NextFeedPreviewState by homeViewModel.nextFeedPreviewState.collectAsStateWithLifecycle()
    val feedFontSizes by homeViewModel.feedFontSizeState.collectAsStateWithLifecycle()
    val swipeActions by homeViewModel.swipeActions.collectAsStateWithLifecycle()
    val feedOperation by homeViewModel.feedOperationState.collectAsStateWithLifecycle()
    val feedLayout by homeViewModel.feedLayout.collectAsStateWithLifecycle()
    val isGridLayoutEnabled by homeViewModel.isGridLayoutEnabled.collectAsStateWithLifecycle()
    val isSyncUploadRequired by homeViewModel.isSyncUploadRequired.collectAsStateWithLifecycle()
    val feedItemDisplaySettings by homeViewModel.feedItemDisplaySettings.collectAsStateWithLifecycle()
    val viewMenuState by homeViewModel.viewMenuState.collectAsStateWithLifecycle()
    val coachingCards by homeViewModel.coachingCards.collectAsStateWithLifecycle()
    val showFlowOnboarding by homeViewModel.showFlowOnboarding.collectAsStateWithLifecycle()

    val categoriesState by changeFeedCategoryViewModel.categoriesState.collectAsStateWithLifecycle()

    var showChangeCategorySheet by rememberSaveable { mutableStateOf(false) }
    var showNoFeedsBottomSheet by rememberSaveable { mutableStateOf(false) }
    val changeCategorySheetState = rememberModalBottomSheetState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hapticFeedback = LocalHapticFeedback.current
    val strings = LocalFeedFlowStrings.current

    if (feedOperation != FeedOperation.None) {
        LoadingOperationDialog(feedOperation)
    }

    if (showFlowOnboarding) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(strings.flowOnboardingTitle) },
            text = { Text(strings.flowOnboardingMessage) },
            confirmButton = {
                TextButton(onClick = homeViewModel::dismissFlowOnboarding) {
                    Text(strings.flowOnboardingAction)
                }
            },
        )
    }

    LaunchedEffect(Unit) {
        changeFeedCategoryViewModel.categoryChangedState.collect {
            showChangeCategorySheet = false
        }
    }

    LaunchedEffect(homeViewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                homeViewModel.reloadFeedState()
                delay(1.minutes)
            }
        }
    }

    LaunchedEffect(Unit) {
        homeViewModel.letGoEvents.collect { feedItemId ->
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            val result = withTimeoutOrNull(6.seconds) {
                snackbarHostState.showSnackbar(
                    message = strings.letGoConfirmation,
                    actionLabel = strings.undo,
                    duration = SnackbarDuration.Indefinite,
                )
            }
            snackbarHostState.currentSnackbarData?.dismiss()
            if (result == SnackbarResult.ActionPerformed) {
                homeViewModel.undoLetGo(feedItemId)
            }
        }
    }

    LaunchedEffect(Unit) {
        homeViewModel.errorState.collect { errorState ->
            when (errorState) {
                is UIErrorState.DatabaseError -> {
                    snackbarHostState.showSnackbar(
                        strings.databaseError(errorState.errorCode.code),
                        duration = SnackbarDuration.Short,
                    )
                }

                is UIErrorState.SyncError -> {
                    snackbarHostState.showSnackbar(
                        strings.syncErrorMessage(errorState.errorCode),
                        duration = SnackbarDuration.Short,
                    )
                }

                is UIErrorState.DeleteFeedSourceError -> {
                    snackbarHostState.showSnackbar(
                        strings.deleteFeedSourceError,
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    val visibleFeedState = feedState

    val homeDisplayState = remember(
        feedState,
        pinnedFeedState,
        visibleFeedState,
        navDrawerState,
        loadingState,
        feedFontSizes,
        currentFeedFilter,
        swipeActions,
        feedLayout,
        isGridLayoutEnabled,
        isSyncUploadRequired,
        nextFeedPreviewState,
        feedItemDisplaySettings,
        coachingCards,
    ) {
        HomeDisplayState(
            feedItems = visibleFeedState,
            pinnedFeedItems = pinnedFeedState,
            navDrawerState = navDrawerState,
            feedUpdateStatus = loadingState,
            feedFontSizes = feedFontSizes,
            currentFeedFilter = currentFeedFilter,
            swipeActions = swipeActions,
            feedLayout = feedLayout,
            isGridLayoutEnabled = isGridLayoutEnabled,
            isSyncUploadRequired = isSyncUploadRequired,
            nextFeedDisplayState = nextFeedPreviewState.asDisplayState(),
            feedItemDisplaySettings = feedItemDisplaySettings,
            coachingCards = coachingCards,
        )
    }

    val feedListActions = remember(
        homeViewModel,
        navigateToReaderMode,
        browserManager,
        context,
    ) {
        FeedListActions(
            onClearOldArticlesClicked = { homeViewModel.deleteOldFeedItems() },
            onDeleteDatabaseClick = { homeViewModel.deleteAllFeeds() },
            refreshData = { homeViewModel.getNewFeeds(forceRefresh = true) },
            requestNewData = { homeViewModel.requestNewFeedsPage() },
            onBackToTimelineClick = { homeViewModel.onFeedFilterSelected(FeedFilter.Flow) },
            onVisibleFeedItemsChanged = homeViewModel::onVisibleFeedItemsChanged,
            markAsRead = { feedItemId -> homeViewModel.markAsRead(feedItemId.id) },
            openUrl = { urlInfo ->
                openUrl(
                    urlInfo = urlInfo,
                    navigateToReaderMode = navigateToReaderMode,
                    browserManager = browserManager,
                    context = context,
                )
            },
            openInBrowser = { urlInfo -> browserManager.openUrlWithFavoriteBrowser(urlInfo.url, context) },
            updateBookmarkStatus = { feedItemId, isBookmarked ->
                homeViewModel.updateBookmarkStatus(feedItemId, isBookmarked)
            },
            updateReadStatus = { feedItemId, isRead -> homeViewModel.updateReadStatus(feedItemId, isRead) },
            letGo = homeViewModel::letGo,
            markAllAboveAsRead = { feedItemId -> homeViewModel.markAllAboveAsRead(feedItemId) },
            markAllBelowAsRead = { feedItemId -> homeViewModel.markAllBelowAsRead(feedItemId) },
        )
    }

    val feedManagementActions = remember(
        homeViewModel,
        changeFeedCategoryViewModel,
        onAddFeedClick,
        onEditFeedClick,
        browserManager,
        context,
    ) {
        FeedManagementActions(
            onAddFeedClick = onAddFeedClick,
            onFeedFilterSelected = { feedFilter -> homeViewModel.onFeedFilterSelected(feedFilter) },
            onEditFeedClick = onEditFeedClick,
            onDeleteFeedSourceClick = { feedSource -> homeViewModel.deleteFeedSource(feedSource) },
            onPinFeedClick = { feedSource -> homeViewModel.toggleFeedPin(feedSource) },
            onEditCategoryClick = { categoryId, newName -> homeViewModel.updateCategoryName(categoryId, newName) },
            validateCategoryName = homeViewModel::validateCategoryName,
            onDeleteCategoryClick = { categoryId -> homeViewModel.deleteCategory(categoryId) },
            onChangeFeedCategoryClick = { feedSource ->
                changeFeedCategoryViewModel.loadFeedSource(feedSource)
                showChangeCategorySheet = true
            },
            onOpenWebsite = { url -> browserManager.openUrlWithFavoriteBrowser(url, context) },
            onDeleteAllFeedsInCategoryByIdClick = { categoryId ->
                homeViewModel.deleteAllFeedsInCategory(categoryId)
            },
            onReorderPinnedFeedSources = { feedSources ->
                homeViewModel.reorderPinnedFeedSources(feedSources)
            },
            onReorderCategories = { categoryWrappers ->
                homeViewModel.reorderCategories(categoryWrappers)
            },
            onReorderFeedSources = { feedSources ->
                homeViewModel.reorderFeedSources(feedSources)
            },
        )
    }

    val shareBehavior = remember(context, strings) {
        ShareBehavior(
            onShareClick = { titleAndUrl ->
                context.openShareSheet(
                    title = titleAndUrl.title,
                    url = titleAndUrl.url,
                )
            },
            shareLinkTitle = strings.menuShare,
            shareCommentsTitle = strings.menuShareComments,
        )
    }

    val useDockedDrawer = currentWindowAdaptiveInfo()
        .windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    AdaptiveHomeView(
        snackbarHostState = snackbarHostState,
        onSettingsButtonClicked = onSettingsButtonClicked,
        onSearchClick = onSearchClick,
        displayState = homeDisplayState,
        feedListActions = feedListActions,
        feedManagementActions = feedManagementActions,
        useDockedDrawer = useDockedDrawer,
        shareBehavior = shareBehavior,
        onBackupClick = homeViewModel::enqueueBackup,
        onFeedSuggestionsClick = onFeedSuggestionsClick,
        onImportExportClick = onImportExportClick,
        onEmptyStateClick = {
            showNoFeedsBottomSheet = true
        },
        onNavigateToNextFeed = onNavigateToNextFeed,
        viewMenuState = viewMenuState,
        onFeedOrderChange = homeViewModel::updateFeedOrder,
        onFocusClick = onFocusClick,
    )

    if (showChangeCategorySheet) {
        EditCategorySheet(
            sheetState = changeCategorySheetState,
            categoryState = categoriesState,
            onCategorySelected = { categoryId ->
                changeFeedCategoryViewModel.onCategorySelected(categoryId)
            },
            onAddCategory = { categoryName ->
                changeFeedCategoryViewModel.addNewCategory(categoryName)
            },
            onDeleteCategory = { categoryId ->
                changeFeedCategoryViewModel.deleteCategory(categoryId.value)
            },
            onEditCategory = { categoryId, newName ->
                changeFeedCategoryViewModel.editCategory(categoryId, newName)
            },
            validateCategoryName = changeFeedCategoryViewModel::validateCategoryName,
            onDismiss = {
                changeFeedCategoryViewModel.saveCategory()
            },
        )
    }

    if (showNoFeedsBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        NoFeedsBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                showNoFeedsBottomSheet = false
            },
            onAddFeedClick = {
                showNoFeedsBottomSheet = false
                onAddFeedClick()
            },
            onImportExportClick = {
                showNoFeedsBottomSheet = false
                onImportExportClick()
            },
            onAccountsClick = {
                showNoFeedsBottomSheet = false
                onAccountsClick()
            },
            onFeedSuggestionsClick = {
                showNoFeedsBottomSheet = false
                onFeedSuggestionsClick()
            },
        )
    }
}

private fun openUrl(
    urlInfo: FeedItemUrlInfo,
    navigateToReaderMode: (FeedItemUrlInfo) -> Unit,
    browserManager: BrowserManager,
    context: Context,
) {
    val openMode = urlInfo.resolveArticleOpenMode(browserManager.getArticleOpenMode())
    when {
        openMode.isReaderMode() -> navigateToReaderMode(urlInfo)
        openMode == ArticleOpenMode.INTERNAL_BROWSER -> browserManager.openWithInAppBrowser(urlInfo.url, context)
        else -> browserManager.openUrlWithFavoriteBrowser(urlInfo.url, context)
    }
}

fun NextFeedPreviewState.asDisplayState(): NextFeedDisplayState = when (this) {
    is NextFeedPreviewDisabledState -> NextFeedDisplayState.NextFeedDisplayDisabledState
    is NextFeedPreviewEnabledState -> NextFeedDisplayState.NextFeedDisplayEnabledState(this.title)
}
