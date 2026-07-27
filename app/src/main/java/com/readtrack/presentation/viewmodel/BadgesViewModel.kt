package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BadgeEntity
import com.readtrack.domain.model.BadgeCatalog
import com.readtrack.domain.model.BadgeCategory
import com.readtrack.domain.repository.BadgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BadgeUiEntry(
    val badge: com.readtrack.domain.model.Badge,
    val earnedAt: Long?,
    val currentProgress: Int = 0,
    val progressPercent: Float = 0f,
    val progressLabel: String = ""
)

data class BadgesUiState(
    val loading: Boolean = true,
    val earnedCount: Int = 0,
    val totalCount: Int = BadgeCatalog.ALL.size,
    val groups: Map<BadgeCategory, List<BadgeUiEntry>> = emptyMap()
)

@HiltViewModel
class BadgesViewModel @Inject constructor(
    private val badgeRepository: BadgeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BadgesUiState())
    val uiState: StateFlow<BadgesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // 持续监听 earned badge 变更，每次从 checkAndAward 获取最新进度
            badgeRepository.observeEarnedBadges().collect { earned ->
                val (_, progress) = runCatching { badgeRepository.checkAndAward() }
                    .getOrDefault(Pair(emptyList(), emptyMap()))
                _uiState.value = buildState(earned, progress)
            }
        }
    }

    private fun buildState(earned: List<BadgeEntity>, progress: Map<String, Int>): BadgesUiState {
        val earnedMap = earned.associateBy { it.id }
        val entries = BadgeCatalog.ALL.map { badge ->
            val cur = progress[badge.id] ?: 0
            val pct = if (badge.threshold > 0) (cur.toFloat() / badge.threshold).coerceIn(0f, 1f) else 0f
            val label = if (badge.threshold > 0) "${cur} / ${badge.threshold} ${badge.unit}" else ""
            BadgeUiEntry(
                badge = badge,
                earnedAt = earnedMap[badge.id]?.earnedAt,
                currentProgress = cur,
                progressPercent = pct,
                progressLabel = label
            )
        }
        val grouped = entries
            .groupBy { it.badge.category }
            .mapValues { (_, list) ->
                list.sortedWith(compareByDescending<BadgeUiEntry> { it.earnedAt != null }
                    .thenBy { it.badge.threshold })
            }
        return BadgesUiState(
            loading = false,
            earnedCount = earned.size,
            totalCount = BadgeCatalog.ALL.size,
            groups = grouped
        )
    }
}
