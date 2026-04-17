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
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // 将 Conscrypt 注册为最高优先级 Security Provider
        Security.insertProviderAt(Conscrypt.newProvider(), 1)

        // 显式使用 Conscrypt Provider 的 SSLContext（BoringSSL 实现）
        // 让 OkHttp 发出的 TLS 握手指纹与 Chrome 一致
        val sslContext = SSLContext.getInstance("TLSv1.2", "Conscrypt")

        // 获取系统默认的 TrustManager（包含正确的证书验证逻辑）
        val trustManager = try {
            val factory = javax.net.ssl.TrustManagerFactory.getInstance(
                javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm()
            )
            factory.init(null as java.security.KeyStore?)
            factory.trustManagers.filterIsInstance<X509TrustManager>().first()
        } catch (_: Exception) {
            // 回退：使用 Conscrypt 内置的 TrustManager
            val fallbackFactory = javax.net.ssl.TrustManagerFactory.getInstance(
                javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm()
            )
            fallbackFactory.init(java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType()))
            fallbackFactory.trustManagers.filterIsInstance<X509TrustManager>().first()
        }

        sslContext.init(null, arrayOf(trustManager), null)

        val sslSocketFactory = sslContext.socketFactory

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .sslSocketFactory(sslSocketFactory, trustManager)
            .build()
    }
}
