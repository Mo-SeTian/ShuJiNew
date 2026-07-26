package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BadgeEntity
import com.readtrack.domain.model.Badge
import com.readtrack.domain.model.BadgeCatalog
import com.readtrack.domain.model.BadgeCategory
import com.readtrack.domain.repository.BadgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BadgeUiEntry(
    val badge: Badge,
    val earnedAt: Long? // null = 未获得
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

    val uiState: StateFlow<BadgesUiState> = badgeRepository.observeEarnedBadges()
        .map { earned -> buildState(earned) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BadgesUiState())

    init {
        // 页面打开时补一次判定，确保历史数据也能解锁
        viewModelScope.launch {
            runCatching { badgeRepository.checkAndAward() }
        }
    }

    private fun buildState(earned: List<BadgeEntity>): BadgesUiState {
        val earnedMap = earned.associateBy { it.id }
        val entries = BadgeCatalog.ALL.map { badge ->
            BadgeUiEntry(badge = badge, earnedAt = earnedMap[badge.id]?.earnedAt)
        }
        // 类别内：已获得优先，其次按 threshold 升序
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
