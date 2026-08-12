package com.prof18.feedflow.shared.domain.feedsync

import app.cash.turbine.test
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AccountsRepositoryTest : KoinTestBase() {
    private val accountsRepository: AccountsRepository by inject()
    private val dropboxSettings: DropboxSettings by inject()
    private val googleDriveSettings: GoogleDriveSettings by inject()

    @Test
    fun `Android exposes only supported sync accounts`() {
        assertEquals(
            listOf(
                SyncAccounts.DROPBOX,
                SyncAccounts.GOOGLE_DRIVE,
                SyncAccounts.FRESH_RSS,
                SyncAccounts.MINIFLUX,
                SyncAccounts.FEEDBIN,
                SyncAccounts.BAZQUX,
            ),
            accountsRepository.getValidAccounts(),
        )
    }

    @Test
    fun `selecting Dropbox clears Google Drive and updates account state`() = runTest(testDispatcher) {
        googleDriveSettings.setGoogleDriveLinked(true)

        accountsRepository.currentAccountState.test {
            assertEquals(SyncAccounts.GOOGLE_DRIVE, awaitItem())
            accountsRepository.setDropboxAccount()
            assertEquals(SyncAccounts.DROPBOX, awaitItem())
        }

        assertFalse(googleDriveSettings.isGoogleDriveLinked())
    }

    @Test
    fun `selecting Google Drive clears Dropbox`() = runTest(testDispatcher) {
        dropboxSettings.setDropboxData("linked")

        accountsRepository.setGoogleDriveAccount()

        assertEquals(null, dropboxSettings.getDropboxData())
        assertEquals(SyncAccounts.GOOGLE_DRIVE, accountsRepository.currentAccountState.value)
    }
}
