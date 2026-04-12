package com.readtrack.presentation.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 封面选择临时存储
 * 使用 StateFlow 实现可观察的状态变化
 */
object CoverSelectionHolder {
    private val _coverState = MutableStateFlow<String?>(null)
    
    val coverState: StateFlow<String?> = _coverState.asStateFlow()
    
    fun setCover(uri: String?) {
        _coverState.value = uri
    }
    
    fun consume(): String? {
        val result = _coverState.value
        _coverState.value = null
        return result
    }
}
