package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.PreferencesManager
import com.readtrack.data.local.StatsUnit
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.ProgressType
import com.readtrack.domain.repository.ReadingRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MonthlyStatsUiState(
    val months: List<HeatmapMonth> = emptyList(),
    val isChapterBased: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class MonthlyStatsViewModel @Inject constructor(
    private val recordRepository: ReadingRecordRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonthlyStatsUiState())
    val uiState: StateFlow<MonthlyStatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                recordRepository.getAllRecords().catch { emit(emptyList()) },
                preferencesManager.statsUnit
            ) { records, unit ->
                val isChapter = unit == StatsUnit.CHAPTER
                val filtered = if (isChapter) {
                    records.filter { it.recordType == RecordType.NORMAL && it.bookSnapshot?.progressType == ProgressType.CHAPTER }
                } else {
                    records.filter { it.recordType == RecordType.NORMAL && it.bookSnapshot?.progressType != ProgressType.CHAPTER }
                }
                Pair(buildHeatmapMonths(filtered), isChapter)
            }.collect { (months, isChapter) ->
                _uiState.value = MonthlyStatsUiState(
                    months = months,
                    isChapterBased = isChapter,
                    isLoading = false
                )
            }
        }
    }
}
