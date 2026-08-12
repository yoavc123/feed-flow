package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.domain.TimeProvider
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceSettings
import com.prof18.feedflow.core.model.FlowPace
import com.prof18.feedflow.core.model.RateLimit
import com.prof18.feedflow.core.model.SourcePresentation
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.VoiceStatus
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feedcategories.FeedCategoryRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.model.FeedEditedState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditFeedViewModel internal constructor(
    private val categoryUseCase: FeedCategoryRepository,
    private val accountsRepository: AccountsRepository,
    private val feedSourcesRepository: FeedSourcesRepository,
    private val databaseHelper: DatabaseHelper,
    private val feedStateRepository: FeedStateRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {
    val categoriesState = categoryUseCase.categoriesState
    private var originalFeedSource: FeedSource? = null

    private val feedUrlMutableState = MutableStateFlow("")
    val feedUrlState = feedUrlMutableState.asStateFlow()

    private val feedNameMutableState = MutableStateFlow("")
    val feedNameState = feedNameMutableState.asStateFlow()

    private val feedSourceSettingsMutableState = MutableStateFlow(FeedSourceSettings())
    val feedSourceSettingsState = feedSourceSettingsMutableState.asStateFlow()

    private val feedEditedMutableState: MutableSharedFlow<FeedEditedState> = MutableSharedFlow()
    val feedEditedState = feedEditedMutableState.asSharedFlow()

    private val feedDeletedMutableState: MutableSharedFlow<Unit> = MutableSharedFlow()
    val feedDeletedState = feedDeletedMutableState.asSharedFlow()

    private val showNotificationToggleMutableState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showNotificationToggleState = showNotificationToggleMutableState.asStateFlow()

    init {
        viewModelScope.launch {
            showNotificationToggleMutableState.update { databaseHelper.areNotificationsEnabled() }
        }
    }

    fun canEditUrl(): Boolean =
        accountsRepository.getCurrentSyncAccount() != SyncAccounts.FRESH_RSS &&
            accountsRepository.getCurrentSyncAccount() != SyncAccounts.MINIFLUX &&
            accountsRepository.getCurrentSyncAccount() != SyncAccounts.FEEDBIN &&
            accountsRepository.getCurrentSyncAccount() != SyncAccounts.BAZQUX

    fun updateFeedUrlTextFieldValue(feedUrlTextFieldValue: String) {
        feedUrlMutableState.update { feedUrlTextFieldValue }
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun updateFeedNameTextFieldValue(feedNameTextFieldValue: String) {
        feedNameMutableState.update { feedNameTextFieldValue }
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun updateArticleOpenMode(articleOpenMode: ArticleOpenMode) {
        feedSourceSettingsMutableState.update { oldValue ->
            oldValue.copy(articleOpenMode = articleOpenMode)
        }
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun updateIsHiddenFromTimeline(isHidden: Boolean) {
        feedSourceSettingsMutableState.update { oldValue ->
            oldValue.copy(isHiddenFromTimeline = isHidden)
        }
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun updateIsPinned(isPinned: Boolean) {
        feedSourceSettingsMutableState.update { oldValue ->
            oldValue.copy(isPinned = isPinned)
        }
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun updateIsNotificationEnabled(isNotificationEnabled: Boolean) {
        feedSourceSettingsMutableState.update { oldValue ->
            oldValue.copy(isNotificationEnabled = isNotificationEnabled)
        }
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun updateIsHideImagesEnabled(isHideImagesEnabled: Boolean) {
        feedSourceSettingsMutableState.update { oldValue ->
            oldValue.copy(isHideImagesEnabled = isHideImagesEnabled)
        }
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun updateSourceFlowPace(flowPace: FlowPace?) {
        feedSourceSettingsMutableState.update { it.copy(flowPace = flowPace) }
        markEditing()
    }

    fun updateMutedForHours(hours: Long?) {
        feedSourceSettingsMutableState.update {
            it.copy(mutedUntilMillis = hours?.let { value -> timeProvider.nowMillis() + value * MILLIS_PER_HOUR })
        }
        markEditing()
    }

    fun updateVoiceStatus(voiceStatus: VoiceStatus) {
        feedSourceSettingsMutableState.update { it.copy(voiceStatus = voiceStatus) }
        markEditing()
    }

    fun updateSourcePresentation(sourcePresentation: SourcePresentation) {
        feedSourceSettingsMutableState.update { it.copy(sourcePresentation = sourcePresentation) }
        markEditing()
    }

    fun updateRateLimit(rateLimit: RateLimit) {
        feedSourceSettingsMutableState.update { it.copy(rateLimit = rateLimit) }
        markEditing()
    }

    private fun markEditing() {
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun loadFeedToEdit(feedSource: FeedSource) {
        originalFeedSource = feedSource

        viewModelScope.launch {
            feedUrlMutableState.update { feedSource.url }
            feedNameMutableState.update { feedSource.title }
            feedSourceSettingsMutableState.update {
                FeedSourceSettings(
                    articleOpenMode = feedSource.articleOpenMode,
                    isHiddenFromTimeline = feedSource.isHiddenFromTimeline,
                    isPinned = feedSource.isPinned,
                    isNotificationEnabled = feedSource.isNotificationEnabled,
                    isHideImagesEnabled = feedSource.isHideImagesEnabled,
                    flowPace = feedSource.flowPace,
                    mutedUntilMillis = feedSource.mutedUntilMillis,
                    voiceStatus = feedSource.voiceStatus,
                    sourcePresentation = feedSource.sourcePresentation,
                    rateLimit = feedSource.rateLimit,
                )
            }

            val categoryName = feedSource.category?.title?.let { CategoryName(it) }
            categoryUseCase.setInitialSelection(categoryName)
            categoryUseCase.initCategories()
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            categoryUseCase.deleteCategory(categoryId)
        }
    }

    fun onCategorySelected(categoryId: CategoryId) {
        categoryUseCase.onCategorySelected(categoryId)
    }

    fun addNewCategory(categoryName: CategoryName) {
        viewModelScope.launch {
            categoryUseCase.addNewCategory(categoryName)
        }
    }

    fun editCategory(categoryId: CategoryId, newName: CategoryName) {
        viewModelScope.launch {
            categoryUseCase.updateCategoryName(categoryId, newName)
        }
    }

    fun validateCategoryName(categoryId: CategoryId?, newName: CategoryName): CategoryNameValidationResult =
        categoryUseCase.validateCategoryName(categoryId, newName)

    fun editFeed() {
        viewModelScope.launch {
            if (feedUrlState.value.isEmpty() || feedNameState.value.isEmpty()) {
                feedEditedMutableState.emit(FeedEditedState.Error.InvalidTitleLink)
                return@launch
            }

            feedEditedMutableState.emit(FeedEditedState.Loading)

            val selectedCategory = categoryUseCase.getSelectedCategory()
            val newFeedSource = originalFeedSource?.copy(
                url = feedUrlState.value,
                title = feedNameState.value,
                category = selectedCategory,
                articleOpenMode = feedSourceSettingsState.value.articleOpenMode,
                isHiddenFromTimeline = feedSourceSettingsState.value.isHiddenFromTimeline,
                isPinned = feedSourceSettingsState.value.isPinned,
                isNotificationEnabled = feedSourceSettingsState.value.isNotificationEnabled,
                isHideImagesEnabled = feedSourceSettingsState.value.isHideImagesEnabled,
                flowPace = feedSourceSettingsState.value.flowPace,
                mutedUntilMillis = feedSourceSettingsState.value.mutedUntilMillis,
                voiceStatus = feedSourceSettingsState.value.voiceStatus,
                sourcePresentation = feedSourceSettingsState.value.sourcePresentation,
                rateLimit = feedSourceSettingsState.value.rateLimit,
            )
            var state: FeedEditedState = FeedEditedState.FeedEdited(feedNameState.value)
            if (newFeedSource != null && newFeedSource != originalFeedSource) {
                state = feedSourcesRepository.editFeedSource(
                    newFeedSource = newFeedSource,
                    originalFeedSource = originalFeedSource,
                )
            }
            if (newFeedSource != null && state is FeedEditedState.FeedEdited) {
                databaseHelper.updateFeedSourceCalmSettings(
                    feedSourceId = newFeedSource.id,
                    flowPace = newFeedSource.flowPace,
                    mutedUntilMillis = newFeedSource.mutedUntilMillis,
                    voiceStatus = newFeedSource.voiceStatus,
                    sourcePresentation = newFeedSource.sourcePresentation,
                    rateLimit = newFeedSource.rateLimit,
                )
                feedStateRepository.getFeeds()
            }
            feedEditedMutableState.emit(state)
        }
    }

    fun deleteFeed() {
        val feedToDelete = originalFeedSource ?: return
        viewModelScope.launch {
            feedSourcesRepository.deleteFeed(feedToDelete)
            feedStateRepository.getFeeds()
            feedDeletedMutableState.emit(Unit)
        }
    }
}

private const val MILLIS_PER_HOUR = 60L * 60L * 1_000L
