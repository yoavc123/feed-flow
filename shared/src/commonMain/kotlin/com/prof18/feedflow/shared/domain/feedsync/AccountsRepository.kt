package com.prof18.feedflow.shared.domain.feedsync

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.feedbin.domain.FeedbinRepository
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class AccountsRepository(
    private val dropboxSettings: DropboxSettings,
    private val googleDriveSettings: GoogleDriveSettings,
    private val appConfig: AppConfig,
    private val gReaderRepository: GReaderRepository,
    private val networkSettings: NetworkSettings,
    private val feedbinRepository: FeedbinRepository,
    private val databaseHelper: DatabaseHelper,
) {
    private val currentAccountMutableState = MutableStateFlow(SyncAccounts.LOCAL)
    val currentAccountState = currentAccountMutableState.asStateFlow()

    init {
        restoreAccounts()
    }

    fun getValidAccounts(): List<SyncAccounts> = buildList {
        if (appConfig.isDropboxSyncEnabled) {
            add(SyncAccounts.DROPBOX)
        }
        if (appConfig.isGoogleDriveSyncEnabled) {
            add(SyncAccounts.GOOGLE_DRIVE)
        }
        add(SyncAccounts.FRESH_RSS)
        add(SyncAccounts.MINIFLUX)
        add(SyncAccounts.FEEDBIN)
        add(SyncAccounts.BAZQUX)
    }

    fun setDropboxAccount() {
        clearOtherSyncCredentials(except = SyncAccounts.DROPBOX)
        currentAccountMutableState.value = SyncAccounts.DROPBOX
    }

    fun setGoogleDriveAccount() {
        clearOtherSyncCredentials(except = SyncAccounts.GOOGLE_DRIVE)
        currentAccountMutableState.value = SyncAccounts.GOOGLE_DRIVE
    }

    fun setFreshRssAccount() {
        clearOtherSyncCredentials(except = SyncAccounts.FRESH_RSS)
        networkSettings.setSyncAccountType(SyncAccounts.FRESH_RSS)
        networkSettings.clearLastSyncDate()
        currentAccountMutableState.value = SyncAccounts.FRESH_RSS
    }

    fun setMinifluxAccount() {
        clearOtherSyncCredentials(except = SyncAccounts.MINIFLUX)
        networkSettings.setSyncAccountType(SyncAccounts.MINIFLUX)
        networkSettings.clearLastSyncDate()
        currentAccountMutableState.value = SyncAccounts.MINIFLUX
    }

    fun setFeedbinAccount() {
        clearOtherSyncCredentials(except = SyncAccounts.FEEDBIN)
        networkSettings.setSyncAccountType(SyncAccounts.FEEDBIN)
        networkSettings.clearLastSyncDate()
        currentAccountMutableState.value = SyncAccounts.FEEDBIN
    }

    fun setBazquxAccount() {
        clearOtherSyncCredentials(except = SyncAccounts.BAZQUX)
        networkSettings.setSyncAccountType(SyncAccounts.BAZQUX)
        networkSettings.clearLastSyncDate()
        currentAccountMutableState.value = SyncAccounts.BAZQUX
    }

    suspend fun clearAccount() {
        databaseHelper.deleteAllReadStatusPendingActions()
        currentAccountMutableState.value = SyncAccounts.LOCAL
    }

    suspend fun clearAllAccounts() {
        databaseHelper.deleteAllReadStatusPendingActions()
        clearOtherSyncCredentials(except = SyncAccounts.LOCAL)
        currentAccountMutableState.value = SyncAccounts.LOCAL
    }

    private fun clearOtherSyncCredentials(except: SyncAccounts) {
        if (except != SyncAccounts.DROPBOX) {
            dropboxSettings.clearDropboxData()
        }
        if (except != SyncAccounts.GOOGLE_DRIVE) {
            googleDriveSettings.clearAll()
        }
        if (except != SyncAccounts.FRESH_RSS &&
            except != SyncAccounts.MINIFLUX &&
            except != SyncAccounts.BAZQUX &&
            except != SyncAccounts.FEEDBIN
        ) {
            networkSettings.deleteAll()
        }
    }

    fun getCurrentSyncAccount(): SyncAccounts {
        val dropboxSettings = dropboxSettings.getDropboxData()
        if (dropboxSettings != null) {
            return SyncAccounts.DROPBOX
        }
        if (googleDriveSettings.isGoogleDriveLinked()) {
            return SyncAccounts.GOOGLE_DRIVE
        }
        if (gReaderRepository.isAccountSet()) {
            return networkSettings.getSyncAccountType() ?: SyncAccounts.LOCAL
        }
        if (feedbinRepository.isAccountSet()) {
            return SyncAccounts.FEEDBIN
        }
        return SyncAccounts.LOCAL
    }

    fun isSyncEnabled(): Boolean {
        val currentSyncAccount = getCurrentSyncAccount()
        return currentSyncAccount == SyncAccounts.DROPBOX ||
            currentSyncAccount == SyncAccounts.GOOGLE_DRIVE
    }

    private fun restoreAccounts() {
        currentAccountMutableState.update {
            getCurrentSyncAccount()
        }
    }
}
