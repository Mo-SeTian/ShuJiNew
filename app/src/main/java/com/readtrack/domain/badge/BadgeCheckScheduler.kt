package com.readtrack.domain.badge

import android.util.Log
import com.readtrack.domain.repository.BadgeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 徽章核算节流器。
 *
 * 数据变更（插入/删除阅读记录、状态变更）会频繁触发徽章判定，而每次判定都要
 * 全量加载书籍与阅读记录。这里把短时间内的多次触发合并为一次：第一个触发启动
 * 一个带 1s 去抖的核算，期间后续触发直接丢弃，避免突发的连续写入反复全量扫描。
 */
@Singleton
class BadgeCheckScheduler @Inject constructor(
    private val badgeRepository: BadgeRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private var scheduled = false

    fun schedule() {
        scope.launch {
            val shouldRun = mutex.withLock {
                if (scheduled) {
                    false
                } else {
                    scheduled = true
                    true
                }
            }
            if (!shouldRun) return@launch

            try {
                delay(DEBOUNCE_MS)
                runCatching { badgeRepository.checkAndAward() }
                    .onFailure { Log.w("BadgeCheckScheduler", "徽章核算失败", it) }
            } finally {
                mutex.withLock { scheduled = false }
            }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 1_000L
    }
}
