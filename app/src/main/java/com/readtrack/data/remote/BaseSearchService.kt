package com.readtrack.data.remote

import okhttp3.Request

/**
 * 搜索服务基类
 * 提供通用的 User-Agent 和 Header 构建逻辑
 */
abstract class BaseSearchService {

    companion object {
        protected val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        protected val DEFAULT_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        protected val DEFAULT_ACCEPT_LANGUAGE = "zh-CN,zh;q=0.9,en;q=0.8"
    }

    /**
     * 构建通用请求头
     */
    protected fun buildCommonHeaders(
        builder: Request.Builder,
        referer: String = "",
        userAgent: String = DEFAULT_USER_AGENT
    ) {
        builder
            .header("User-Agent", userAgent)
            .header("Accept", DEFAULT_ACCEPT)
            .header("Accept-Language", DEFAULT_ACCEPT_LANGUAGE)
            .header("DNT", "1")
            .header("Upgrade-Insecure-Requests", "1")
        if (referer.isNotBlank()) {
            builder.header("Referer", referer)
        }
    }

    /**
     * 构建 Bing 图片搜索专用的额外请求头
     */
    protected fun buildBingImageHeaders(builder: Request.Builder) {
        builder
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .header("Sec-Fetch-User", "?1")
    }
}