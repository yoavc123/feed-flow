package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.presentation.model.ExtrasSettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExtrasSettingsViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
    private val databaseHelper: DatabaseHelper,
) : ViewModel() {

    private val stateMutableFlow = MutableStateFlow(ExtrasSettingsState())
    val state: StateFlow<ExtrasSettingsState> = stateMutableFlow.asStateFlow()

    init {
        viewModelScope.launch { loadSettings() }
    }

    private fun loadSettings() {
        val isReduceMotionEnabled = settingsRepository.getReduceMotionEnabled()
        val areCalmInsightsEnabled = settingsRepository.getCalmInsightsEnabled()

        stateMutableFlow.update {
            ExtrasSettingsState(
                isReduceMotionEnabled = isReduceMotionEnabled,
                areCalmInsightsEnabled = areCalmInsightsEnabled,
            )
        }
    }

    fun updateReduceMotionEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReduceMotionEnabled(value)
            stateMutableFlow.update {
                it.copy(isReduceMotionEnabled = value)
            }
        }
    }

    fun updateCalmInsightsEnabled(value: Boolean) {
        settingsRepository.setCalmInsightsEnabled(value)
        stateMutableFlow.update { it.copy(areCalmInsightsEnabled = value) }
    }

    fun clearLocalReadingHistory() {
        viewModelScope.launch { databaseHelper.clearLocalReadingHistory() }
    }
}
