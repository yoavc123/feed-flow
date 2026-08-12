package com.prof18.feedflow.android.editfeed

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.categoryselection.EditCategorySheet
import com.prof18.feedflow.android.feed.ArticleOpenModeSelector
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedSourceSettings
import com.prof18.feedflow.core.model.FlowPace
import com.prof18.feedflow.core.model.RateLimit
import com.prof18.feedflow.core.model.SourcePresentation
import com.prof18.feedflow.core.model.VoiceStatus
import com.prof18.feedflow.shared.domain.model.FeedEditedState
import com.prof18.feedflow.shared.presentation.EditFeedViewModel
import com.prof18.feedflow.shared.presentation.preview.categoriesExpandedState
import com.prof18.feedflow.shared.ui.feed.editfeed.EditFeedContent
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.settings.SettingSelectorItem
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Composable
internal fun EditScreen(
    viewModel: EditFeedViewModel,
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
) {
    val feedUrl by viewModel.feedUrlState.collectAsStateWithLifecycle()
    val feedName by viewModel.feedNameState.collectAsStateWithLifecycle()
    val feedSourceSettings by viewModel.feedSourceSettingsState.collectAsStateWithLifecycle()
    var showLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val categoriesState by viewModel.categoriesState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val strings = LocalFeedFlowStrings.current

    val showNotificationToggle by viewModel.showNotificationToggleState.collectAsStateWithLifecycle()

    val latestNavigateBack by rememberUpdatedState(navigateBack)

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCategorySheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.feedEditedState.collect { feedAddedState ->
            when (feedAddedState) {
                is FeedEditedState.Error -> {
                    showError = true
                    showLoading = false
                    errorMessage = when (feedAddedState) {
                        FeedEditedState.Error.InvalidUrl -> strings.invalidRssUrl
                        FeedEditedState.Error.InvalidTitleLink -> strings.missingTitleAndLink
                        FeedEditedState.Error.GenericError -> strings.editFeedGenericError
                    }
                }

                is FeedEditedState.FeedEdited -> {
                    showLoading = false
                    val message = strings.feedEditedMessage(feedAddedState.feedName)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT)
                        .show()
                    latestNavigateBack()
                }

                FeedEditedState.Idle -> {
                    showLoading = false
                    showError = false
                    errorMessage = ""
                }

                FeedEditedState.Loading -> {
                    showLoading = true
                }
            }
        }
    }

    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.feedDeletedState.collect {
            showDeleteDialog = false
            latestNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(LocalFeedFlowStrings.current.editFeed)
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navigateBack()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        EditFeedContent(
            feedUrl = feedUrl,
            feedName = feedName,
            showError = showError,
            errorMessage = errorMessage,
            showLoading = showLoading,
            categoriesState = categoriesState,
            canEditUrl = viewModel.canEditUrl(),
            feedSourceSettings = feedSourceSettings,
            onFeedUrlUpdated = { url ->
                viewModel.updateFeedUrlTextFieldValue(url)
            },
            onFeedNameUpdated = { name ->
                viewModel.updateFeedNameTextFieldValue(name)
            },
            articleOpenModeSelector = { selectorModifier ->
                Column(modifier = selectorModifier) {
                    ArticleOpenModeSelector(
                        currentMode = feedSourceSettings.articleOpenMode,
                        onModeSelected = { articleOpenMode ->
                            viewModel.updateArticleOpenMode(articleOpenMode)
                        },
                        allowDefault = true,
                    )
                    SourceFlowPaceSelector(
                        selectedPace = feedSourceSettings.flowPace,
                        onPaceSelected = viewModel::updateSourceFlowPace,
                    )
                    SourceOptionSelector(
                        title = strings.sourceVoiceStatus,
                        selected = feedSourceSettings.voiceStatus,
                        options = persistentListOf(
                            VoiceStatus.AUTOMATIC to strings.sourceVoiceAutomatic,
                            VoiceStatus.VOICE to strings.sourceVoiceYes,
                            VoiceStatus.NOT_VOICE to strings.sourceVoiceNo,
                        ),
                        onSelected = viewModel::updateVoiceStatus,
                    )
                    SourceOptionSelector(
                        title = strings.sourcePresentation,
                        selected = feedSourceSettings.sourcePresentation,
                        options = persistentListOf(
                            SourcePresentation.STANDARD to strings.sourcePresentationStandard,
                            SourcePresentation.WEB_COMIC to strings.sourcePresentationWebcomic,
                        ),
                        onSelected = viewModel::updateSourcePresentation,
                    )
                    SourceOptionSelector(
                        title = strings.sourceRateLimit,
                        selected = feedSourceSettings.rateLimit,
                        options = persistentListOf(
                            RateLimit.NONE to strings.sourceRateLimitNone,
                            RateLimit.ONE_PER_HOUR to strings.sourceRateLimitHourly,
                            RateLimit.THREE_PER_DAY to strings.sourceRateLimitThreeDaily,
                            RateLimit.ONE_PER_DAY to strings.sourceRateLimitDaily,
                        ),
                        onSelected = viewModel::updateRateLimit,
                    )
                    SourceMuteSelector(
                        isMuted = feedSourceSettings.mutedUntilMillis != null,
                        onMuteForHours = viewModel::updateMutedForHours,
                    )
                }
            },
            onHiddenToggled = { hidden ->
                viewModel.updateIsHiddenFromTimeline(hidden)
            },
            onHideImagesToggled = { hideImages ->
                viewModel.updateIsHideImagesEnabled(hideImages)
            },
            onPinnedToggled = { pinned ->
                viewModel.updateIsPinned(pinned)
            },
            showNotificationToggle = showNotificationToggle,
            onNotificationToggleChanged = { isNotificationEnabled ->
                viewModel.updateIsNotificationEnabled(isNotificationEnabled)
            },
            editFeed = {
                viewModel.editFeed()
            },
            onCategorySelectorClick = {
                showCategorySheet = true
            },
            showDeleteDialog = showDeleteDialog,
            onShowDeleteDialog = { showDeleteDialog = true },
            onDismissDeleteDialog = { showDeleteDialog = false },
            onConfirmDelete = { viewModel.deleteFeed() },
            contentPadding = innerPadding,
        )
    }

    if (showCategorySheet) {
        EditCategorySheet(
            sheetState = sheetState,
            categoryState = categoriesState,
            onCategorySelected = { categoryId ->
                viewModel.onCategorySelected(categoryId)
            },
            onAddCategory = { categoryName ->
                viewModel.addNewCategory(categoryName)
            },
            onDeleteCategory = { categoryId ->
                viewModel.deleteCategory(categoryId.value)
            },
            onEditCategory = { categoryId, newName ->
                viewModel.editCategory(categoryId, newName)
            },
            validateCategoryName = viewModel::validateCategoryName,
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        showCategorySheet = false
                    }
                }
            },
        )
    }
}

@Composable
private fun SourceFlowPaceSelector(
    selectedPace: FlowPace?,
    onPaceSelected: (FlowPace?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val strings = LocalFeedFlowStrings.current

    Box {
        SettingSelectorItem(
            title = strings.sourceFlowPace,
            currentValueLabel = selectedPace?.label() ?: strings.sourceFlowPaceInherit,
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
        ) {
            DropdownMenuItem(
                text = { Text(strings.sourceFlowPaceInherit) },
                onClick = {
                    onPaceSelected(null)
                    expanded = false
                },
            )
            FlowPace.entries.forEach { pace ->
                DropdownMenuItem(
                    text = { Text(pace.label()) },
                    onClick = {
                        onPaceSelected(pace)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun <T> SourceOptionSelector(
    title: String,
    selected: T,
    options: ImmutableList<Pair<T, String>>,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        SettingSelectorItem(
            title = title,
            currentValueLabel = options.first { it.first == selected }.second,
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
        ) {
            options.forEach { (option, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SourceMuteSelector(
    isMuted: Boolean,
    onMuteForHours: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val strings = LocalFeedFlowStrings.current
    val options = persistentListOf(
        null to strings.sourceMuteNone,
        1L to strings.sourceMuteOneHour,
        HOURS_PER_DAY to strings.sourceMuteOneDay,
        HOURS_PER_WEEK to strings.sourceMuteOneWeek,
    )
    Box {
        SettingSelectorItem(
            title = strings.sourceMuteUntil,
            currentValueLabel = if (isMuted) strings.sourceMuteActive else strings.sourceMuteNone,
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
        ) {
            options.forEach { (hours, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onMuteForHours(hours)
                        expanded = false
                    },
                )
            }
        }
    }
}

private const val HOURS_PER_DAY = 24L
private const val HOURS_PER_WEEK = 168L

@Composable
private fun FlowPace.label(): String = when (this) {
    FlowPace.FLASH -> LocalFeedFlowStrings.current.flowPaceFlash
    FlowPace.DAILY -> LocalFeedFlowStrings.current.flowPaceDaily
    FlowPace.STANDARD -> LocalFeedFlowStrings.current.flowPaceStandard
    FlowPace.SLOW -> LocalFeedFlowStrings.current.flowPaceSlow
    FlowPace.TIMELESS -> LocalFeedFlowStrings.current.flowPaceTimeless
}

@PreviewPhone
@Composable
private fun EditScreenPreview() {
    FeedFlowTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(LocalFeedFlowStrings.current.editFeed)
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {},
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            EditFeedContent(
                feedUrl = "https://www.ablog.com/feed",
                feedName = "Feed Name",
                showError = false,
                showLoading = false,
                errorMessage = "",
                canEditUrl = true,
                categoriesState = categoriesExpandedState,
                feedSourceSettings = FeedSourceSettings(),
                onFeedUrlUpdated = {},
                onFeedNameUpdated = {},
                articleOpenModeSelector = { selectorModifier ->
                    ArticleOpenModeSelector(
                        modifier = selectorModifier,
                        currentMode = ArticleOpenMode.DEFAULT,
                        onModeSelected = {},
                        allowDefault = true,
                    )
                },
                onHiddenToggled = {},
                onHideImagesToggled = {},
                onPinnedToggled = {},
                showNotificationToggle = true,
                onNotificationToggleChanged = {},
                editFeed = { },
                onCategorySelectorClick = {},
                showDeleteDialog = true,
                onShowDeleteDialog = {},
                onDismissDeleteDialog = {},
                onConfirmDelete = {},
                contentPadding = innerPadding,
            )
        }
    }
}
