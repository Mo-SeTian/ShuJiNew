package com.readtrack.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * 封面选择临时存储
 * 使用 Compose State 包装，使其可被重组追踪
 */
object CoverSelectionHolder {
    private val _coverState = mutableStateOf<String?>(null)
    
    val coverState = _coverState
    
    fun setCover(uri: String?) {
        _coverState.value = uri
    }
    
    fun consume(): String? {
        val result = _coverState.value
        _coverState.value = null
        return result
    }
}
