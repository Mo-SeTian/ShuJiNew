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

/**
 * 阅读时间线中的单个记录项。
 * book 使用写入时冻结的 BookSnapshot，删除图书后仍可完整显示。
 */
data class TimelineItem(
    val record: ReadingRecordEntity,
    val bookSnapshot: BookSnapshot?,  // 快照：删除图书后仍可显示书名封面
    val dateLabel: String,   // 如 "今天"、"昨天"、"3月15日"
    val timeLabel: String    // 如 "14:30"
)

data class TimelineDayGroup(
    val dateLabel: String,
    val dateKey: String,     // 用于排序的日期字符串 yyyy-MM-dd
    val items: List<TimelineItem>
)

/** 时间范围筛选选项 */
sealed class TimelineTimeRange(val label: String, val days: Long?) {
    /** 本周（7天） */
    data object Week : TimelineTimeRange("本周", 7)
    /** 一个月（30天） */
    data object Month : TimelineTimeRange("一个月", 30)
    /** 三个月（90天） */
    data object ThreeMonths : TimelineTimeRange("三个月", 90)
    /** 半年（180天） */
    data object HalfYear : TimelineTimeRange("半年", 180)
    /** 全部时间 */
    data object All : TimelineTimeRange("全部", null)
    /** 自定义范围 */
    data class Custom(val startMs: Long, val endMs: Long) : TimelineTimeRange("自定义", null)
}

data class TimelineUiState(
    val groups: List<TimelineDayGroup> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedRange: TimelineTimeRange = TimelineTimeRange.Week
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

    /** 切换时间范围筛选 */
    fun selectRange(range: TimelineTimeRange) {
        _uiState.value = _uiState.value.copy(selectedRange = range, isLoading = true)
        loadTimeline()
    }

    private fun loadTimeline() {
        viewModelScope.launch {
            val selectedRange = _uiState.value.selectedRange
            // 计算时间边界
            val now = System.currentTimeMillis()
            val startMs: Long? = when (selectedRange) {
                is TimelineTimeRange.Custom -> selectedRange.startMs
                is TimelineTimeRange.All -> null
                else -> {
                    val days = selectedRange.days ?: return@launch
                    now - days * 24 * 3600 * 1000
                }
            }
            val endMs: Long? = when (selectedRange) {
                is TimelineTimeRange.Custom -> selectedRange.endMs
                else -> null
            }

            combine(
                // 关联 live books 用于补充 bookSnapshot 为 null 的旧数据
                bookRepository.getAllBooks().catch { emit(emptyList()) },
                recordRepository.getAllRecords().catch { emit(emptyList()) }
            ) { books, records ->
                val liveBookMap = books.associateBy { it.id }
                val calendar = Calendar.getInstance()
                val today = clearTime(calendar.timeInMillis)
                val yesterday = today - 24 * 3600 * 1000

                // 时间范围过滤
                records
                    .filter { record ->
                        (startMs == null || record.date >= startMs) &&
                        (endMs == null || record.date <= endMs)
                    }
                    .sortedByDescending { it.date }
                    .map { record ->
                        // 优先用写入时冻结的快照；旧数据（snapshot=null）用 live book 补全
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
                        TimelineItem(
                            record = record,
                            bookSnapshot = snapshot,
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
