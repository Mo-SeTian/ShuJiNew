package com.readtrack

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.readtrack.worker.ReadingReminderWorker
import com.readtrack.worker.ReadingReportWorker
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

        initNotificationChannel()

        Thread.setDefaultUncaughtExceptionHandler(CrashHandler())
    }

    private fun initNotificationChannel() {
        val channel = NotificationChannel(
            ReadingReminderWorker.CHANNEL_ID,
            "阅读提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "每日阅读打卡提醒"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val reportChannel = NotificationChannel(
            ReadingReportWorker.CHANNEL_ID,
            "阅读报告",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "每周/每月阅读报告"
        }
        manager.createNotificationChannel(reportChannel)
    }

    private fun initUmeng() {
        val appKey = BuildConfig.UMENG_APP_KEY.ifEmpty { return }
        UMConfigure.preInit(this, appKey, "official")
        UMConfigure.init(
            this,
            appKey,
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
            } catch (e: Exception) {
                android.util.Log.w("ReadTrack", "上报未捕获异常失败", e)
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
            } catch (e: Exception) {
                android.util.Log.w("ReadTrack", "友盟 onResume 失败", e)
            }
        }

        override fun onActivityPaused(activity: Activity) {
            try {
                MobclickAgent.onPause(activity)
            } catch (e: Exception) {
                android.util.Log.w("ReadTrack", "友盟 onPause 失败", e)
            }
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
