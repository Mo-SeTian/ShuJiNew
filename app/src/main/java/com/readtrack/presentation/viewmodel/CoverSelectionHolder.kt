package com.readtrack.presentation.viewmodel

/**
 * 封面选择临时存储
 * 用于在 AddBookScreen 和 CoverPickerScreen 之间传递封面数据
 */
object CoverSelectionHolder {
    var selectedCoverUri: String? = null
        private set
    
    fun setCover(uri: String?) {
        selectedCoverUri = uri
    }
    
    fun getAndClear(): String? {
        val result = selectedCoverUri
        selectedCoverUri = null
        return result
    }
}
