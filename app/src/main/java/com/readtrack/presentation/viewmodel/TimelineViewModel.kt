package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.ReadingRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class TimelineItem(
    val record: ReadingRecordEntity,
    val book: BookEntity,
    val dateLabel: String,   // 如 "今天"、"昨天"、"3月15日"
    val timeLabel: String    // 如 "14:30"
)

data class TimelineDayGroup(
    val dateLabel: String,
    val dateKey: String,     // 用于排序的日期字符串 yyyy-MM-dd
    val items: List<TimelineItem>
)

data class TimelineUiState(
    val groups: List<TimelineDayGroup> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val recordRepository: ReadingRecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    init {
        loadTimeline()
    }

    private fun loadTimeline() {
        viewModelScope.launch {
            combine(
                bookRepository.getAllBooks().catch { emit(emptyList()) },
                recordRepository.getAllRecords().catch { emit(emptyList()) }
            ) { books, records ->
                val bookMap = books.associateBy { it.id }
                val calendar = Calendar.getInstance()
                val today = clearTime(calendar.timeInMillis)
                val yesterday = today - 24 * 3600 * 1000

                records
                    .filter { bookMap.containsKey(it.bookId) }
                    .sortedByDescending { it.date }
                    .map { record ->
                        val book = bookMap[record.bookId]!!
                        calendar.timeInMillis = record.date
                        val recordDate = clearTime(record.date)
                        val dateLabel = when (recordDate) {
                            today -> "今天"
                            yesterday -> "昨天"
                            else -> SimpleDateFormat("M月d日", Locale.CHINESE).format(Date(record.date))
                        }
                        val timeLabel = SimpleDateFormat("HH:mm", Locale.CHINESE).format(Date(record.date))
                        TimelineItem(
                            record = record,
                            book = book,
                            dateLabel = dateLabel,
                            timeLabel = timeLabel
                        )
                    }
                    .groupBy { item ->
                        SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE).format(Date(clearTime(item.record.date)))
                    }
                    .map { (_, items) ->
                        val firstItem = items.first()
                        TimelineDayGroup(
                            dateLabel = firstItem.dateLabel,
                            dateKey = firstItem.record.date.let { SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE).format(Date(clearTime(it))) },
                            items = items.sortedByDescending { it.record.date }
                        )
                    }
                    .sortedByDescending { it.dateKey }
            }
                .flowOn(Dispatchers.Default)
                .catch { e ->
                    _uiState.value = TimelineUiState(isLoading = false, errorMessage = e.message)
                }
                .collect { groups ->
                    _uiState.value = TimelineUiState(groups = groups, isLoading = false)
                }
        }
    }

    private fun clearTime(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
