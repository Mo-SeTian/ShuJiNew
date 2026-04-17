package com.readtrack.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import org.conscrypt.Conscrypt
import java.security.Security
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 网络模块 — 提供 OkHttpClient，使用 Conscrypt TLS 修复指纹
 * 豆瓣/部分网站会拦截 Java/HTTP 协议栈的 TLS 指纹，
 * OkHttp + Conscrypt 可发出与 Chrome 一致的 TLS 握手
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // 注册 Conscrypt 为最高优先级 Provider（让 OkHttp 的 TLS 使用 Chrome 同款握手）
        Security.insertProviderAt(Conscrypt.newProvider(), 1)

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
