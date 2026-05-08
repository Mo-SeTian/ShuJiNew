package com.readtrack

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ReadTrackApp : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        try {
            initUmeng()
        } catch (e: Exception) {
            android.util.Log.e("ReadTrack", "友盟初始化失败", e)
        }

        try {
            registerActivityLifecycleCallbacks(UmengSessionTracker())
        } catch (e: Exception) {
            android.util.Log.e("ReadTrack", "友盟会话追踪注册失败", e)
        }

        Thread.setDefaultUncaughtExceptionHandler(CrashHandler())
    }

    private fun initUmeng() {
        UMConfigure.preInit(this, "__UMENG_APP_KEY__", "official")
        UMConfigure.init(
            this,
            "__UMENG_APP_KEY__",
            "official",
            UMConfigure.DEVICE_TYPE_PHONE,
            ""
        )
        UMConfigure.setLogEnabled(true)
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO)

        android.util.Log.i("ReadTrack", "友盟统计初始化完成（渠道=official, 自动采集版本/留存）")
    }

    private inner class CrashHandler : Thread.UncaughtExceptionHandler {
        private val previous = Thread.getDefaultUncaughtExceptionHandler()

        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            try {
                android.util.Log.e("ReadTrack", "未捕获异常", throwable)
                MobclickAgent.reportError(this@ReadTrackApp, throwable)
                // 给 1 秒时间让 SDK 异步发送错误报告
                Thread.sleep(1000)
            } catch (_: Exception) {
            } finally {
                previous?.uncaughtException(thread, throwable)
            }
        }
    }

    private var activityRefCount = 0

    private inner class UmengSessionTracker : ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            try {
                MobclickAgent.onResume(activity)
            } catch (_: Exception) {}
        }

        override fun onActivityPaused(activity: Activity) {
            try {
                MobclickAgent.onPause(activity)
            } catch (_: Exception) {}
        }

        override fun onActivityStarted(activity: Activity) {
            activityRefCount++
        }

        override fun onActivityStopped(activity: Activity) {
            activityRefCount--
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
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
