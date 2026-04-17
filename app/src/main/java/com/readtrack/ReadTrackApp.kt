package com.readtrack

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.hilt.android.HiltAndroidApp
import org.conscrypt.Conscrypt
import java.security.Security

@HiltAndroidApp
class ReadTrackApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        // 将 Conscrypt（Chrome/BoringSSL）注册为默认 TLS/SSL Provider
        // 修复豆瓣等网站对 Java HttpURLConnection 默认 JSSE 指纹的拦截（HTTP 403）
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
        Log.d("ReadTrackApp", "Application started with Conscrypt TLS provider")
    }
    
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 使用25%的可用内存作为缓存
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // 使用2%的可用磁盘空间
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .crossfade(200)
            .respectCacheHeaders(false)
            .build()
    }
}
