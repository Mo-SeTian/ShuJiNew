package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.domain.model.BookSnapshot
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

data class ReadingHistoryItem(
    val record: ReadingRecordEntity,
    val bookSnapshot: BookSnapshot?,
    val currentTitle: String = bookSnapshot?.title ?: "已删除图书",
    val dateLabel: String,
    val timeLabel: String
)

data class ReadingHistoryDayGroup(
    val dateLabel: String,
    val dateKey: String,
    val items: List<ReadingHistoryItem>
)

sealed class ReadingHistoryTimeRange(val label: String, val days: Long?) {
    data object Week : ReadingHistoryTimeRange("本周", 7)
    data object Month : ReadingHistoryTimeRange("一个月", 30)
    data object ThreeMonths : ReadingHistoryTimeRange("三个月", 90)
    data object HalfYear : ReadingHistoryTimeRange("半年", 180)
    data object All : ReadingHistoryTimeRange("全部", null)
    data class Custom(val startMs: Long, val endMs: Long) : ReadingHistoryTimeRange("自定义", null)
}

data class ReadingHistoryUiState(
    val groups: List<ReadingHistoryDayGroup> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedRange: ReadingHistoryTimeRange = ReadingHistoryTimeRange.Week
)

@HiltViewModel
class ReadingHistoryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val recordRepository: ReadingRecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadingHistoryUiState())
    val uiState: StateFlow<ReadingHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun selectRange(range: ReadingHistoryTimeRange) {
        _uiState.value = _uiState.value.copy(selectedRange = range, isLoading = true)
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val selectedRange = _uiState.value.selectedRange
            val now = System.currentTimeMillis()
            val startMs: Long? = when (selectedRange) {
                is ReadingHistoryTimeRange.Custom -> selectedRange.startMs
                is ReadingHistoryTimeRange.All -> null
                else -> {
                    val days = selectedRange.days ?: return@launch
                    now - days * 24 * 3600 * 1000
                }
            }
            val endMs: Long? = when (selectedRange) {
                is ReadingHistoryTimeRange.Custom -> selectedRange.endMs
                else -> null
            }

            combine(
                bookRepository.getAllBooks().catch { emit(emptyList()) },
                recordRepository.getAllRecords().catch { emit(emptyList()) }
            ) { books, records ->
                val liveBookMap = books.associateBy { it.id }
                val calendar = Calendar.getInstance()
                val today = clearTime(calendar.timeInMillis)
                val yesterday = today - 24 * 3600 * 1000

                records
                    .filter { record ->
                        (startMs == null || record.date >= startMs) &&
                        (endMs == null || record.date <= endMs)
                    }
                    .sortedByDescending { it.date }
                    .map { record ->
                        val snapshot: BookSnapshot? = record.bookSnapshot
                            ?: record.bookId?.let { liveBookMap[it] }?.let { book ->
                                BookSnapshot.from(book, book.status)
                            }

                        calendar.timeInMillis = record.date
                        val recordDate = clearTime(record.date)
                        val dateLabel = when (recordDate) {
                            today -> "今天"
                            yesterday -> "昨天"
                            else -> SimpleDateFormat("M月d日", Locale.CHINESE).format(Date(record.date))
                        }
                        val timeLabel = SimpleDateFormat("HH:mm", Locale.CHINESE).format(Date(record.date))
                        val currentTitle = record.bookId?.let { liveBookMap[it]?.title }
                            ?: snapshot?.title
                            ?: "已删除图书"
                        ReadingHistoryItem(
                            record = record,
                            bookSnapshot = snapshot,
                            currentTitle = currentTitle,
                            dateLabel = dateLabel,
                            timeLabel = timeLabel
                        )
                    }
                    .groupBy { item ->
                        SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE).format(Date(clearTime(item.record.date)))
                    }
                    .map { (_, items) ->
                        val firstItem = items.first()
                        ReadingHistoryDayGroup(
                            dateLabel = firstItem.dateLabel,
                            dateKey = firstItem.record.date.let { SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE).format(Date(clearTime(it))) },
                            items = items.sortedByDescending { it.record.date }
                        )
                    }
                    .sortedByDescending { it.dateKey }
            }
                .flowOn(Dispatchers.Default)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
                }
                .collect { groups ->
                    _uiState.value = _uiState.value.copy(groups = groups, isLoading = false)
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