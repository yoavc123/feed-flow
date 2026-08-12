package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.ReadingHistoryItem
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExtrasSettingsViewModelTest : KoinTestBase() {

    private val viewModel: ExtrasSettingsViewModel by inject()
    private val databaseHelper: DatabaseHelper by inject()

    @Test
    fun `state is loaded from settings repository on init`() = runTest {
        viewModel.state.test {
            val initialState = awaitItem()
            // Default value from SettingsRepository is false
            assertFalse(initialState.isReduceMotionEnabled)
        }
    }

    @Test
    fun `updateReduceMotionEnabled updates state`() = runTest {
        viewModel.state.test {
            awaitItem()

            viewModel.updateReduceMotionEnabled(true)
            assertTrue(awaitItem().isReduceMotionEnabled)

            viewModel.updateReduceMotionEnabled(false)
            assertFalse(awaitItem().isReduceMotionEnabled)
        }
    }

    @Test
    fun `calm insights can be disabled locally`() = runTest {
        viewModel.updateCalmInsightsEnabled(false)

        assertFalse(viewModel.state.value.areCalmInsightsEnabled)
    }

    @Test
    fun `clear local history removes history and progress`() = runTest {
        databaseHelper.upsertReadingHistory(
            ReadingHistoryItem(
                feedItemId = "history",
                url = "https://example.com",
                title = "History",
                summary = null,
                author = null,
                feedSourceId = "source",
                feedSourceTitle = "Source",
                articleText = null,
                openedAtMillis = 1,
            ),
        )
        databaseHelper.upsertReadingProgress("history", 0.5f)

        viewModel.clearLocalReadingHistory()
        advanceUntilIdle()

        assertTrue(databaseHelper.getReadingHistory().isEmpty())
        assertTrue(databaseHelper.getReadingProgress("history") == null)
    }
}
