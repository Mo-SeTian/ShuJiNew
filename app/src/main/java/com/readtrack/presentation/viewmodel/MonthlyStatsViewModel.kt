package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.ProgressType
import com.readtrack.domain.repository.ReadingRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L

data class MonthlyStatsUiState(
    val months: List<HeatmapMonth> = emptyList(),
    val isChapterBased: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class MonthlyStatsViewModel @Inject constructor(
    private val recordRepository: ReadingRecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonthlyStatsUiState())
    val uiState: StateFlow<MonthlyStatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            recordRepository.getAllRecords().catch { emit(emptyList()) }
                .collect { records ->
                    _uiState.value = MonthlyStatsUiState(
                        months = computeHeatmapData(records),
                        isLoading = false
                    )
                }
        }
    }

    private fun computeHeatmapData(records: List<ReadingRecordEntity>): List<HeatmapMonth> {
        val normalRecords = records.filter { it.recordType == RecordType.NORMAL }
        if (normalRecords.isEmpty()) return emptyList()

        val calendar = Calendar.getInstance()

        // 按天聚合
        val dailyMap = linkedMapOf<Long, HeatmapDay>()
        normalRecords.forEach { record ->
            calendar.timeInMillis = record.date
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val dow = calendar.get(Calendar.DAY_OF_WEEK)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val dayStart = calendar.timeInMillis

            val isChapter = record.bookSnapshot?.progressType == ProgressType.CHAPTER
            val amount = if (isChapter) (record.chaptersRead ?: 0).toDouble() else record.pagesRead
            val existing = dailyMap[dayStart]
            if (existing != null) {
                dailyMap[dayStart] = existing.copy(
                    chaptersRead = existing.chaptersRead + if (isChapter) amount else 0.0,
                    pagesRead = existing.pagesRead + if (!isChapter) amount else 0.0
                )
            } else {
                dailyMap[dayStart] = HeatmapDay(
                    dateMs = dayStart,
                    year = year,
                    month = month,
                    dayOfMonth = day,
                    dayOfWeek = dow,
                    chaptersRead = if (isChapter) amount else 0.0,
                    pagesRead = if (!isChapter) amount else 0.0
                )
            }
        }

        // 填充缺失日
        val earliest = dailyMap.keys.min()
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        var currentDay = earliest
        while (currentDay <= todayStart) {
            if (!dailyMap.containsKey(currentDay)) {
                calendar.timeInMillis = currentDay
                dailyMap[currentDay] = HeatmapDay(
                    dateMs = currentDay,
                    year = calendar.get(Calendar.YEAR),
                    month = calendar.get(Calendar.MONTH),
                    dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
                    dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
                    chaptersRead = 0.0,
                    pagesRead = 0.0
                )
            }
            currentDay += ONE_DAY_MILLIS
        }

        // 按月分组
        val sortedDays = dailyMap.values.sortedBy { it.dateMs }
        return sortedDays.groupBy { it.year to it.month }.map { (key, days) ->
            val (year, month) = key
            HeatmapMonth(
                year = year,
                month = month,
                label = "${year}年${month + 1}月",
                days = days,
                totalValue = days.sumOf { it.pagesRead + it.chaptersRead }
            )
        }
    }
}
