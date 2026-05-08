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
import com.umeng.umcrash.UMCrash
import com.umeng.umcrash.UMCrashCallback
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

    override fun attachBaseContext(base: android.content.Context?) {
        super.attachBaseContext(base)
        UMConfigure.preInit(this, "__UMENG_APP_KEY__", "official")
    }

    override fun onCreate() {
        super.onCreate()

        initUmeng()
        registerActivityLifecycleCallbacks(UmengSessionTracker())
    }

    private fun initUmeng() {
        try {
            UMConfigure.init(
                this,
                "__UMENG_APP_KEY__",
                "official",
                UMConfigure.DEVICE_TYPE_PHONE,
                ""
            )
            UMConfigure.setLogEnabled(true)
            MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO)

            UMCrash.registerUMCrashCallback(UMCrashCallback {
                android.util.Log.e("ReadTrack", "闪退回调触发")
                ""
            })

            android.util.Log.i("ReadTrack", "友盟统计初始化完成（含闪退上报）")
        } catch (e: Exception) {
            android.util.Log.e("ReadTrack", "友盟统计初始化失败", e)
        }
    }

    private var activityRefCount = 0

    private inner class UmengSessionTracker : ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            MobclickAgent.onResume(activity)
        }

        override fun onActivityPaused(activity: Activity) {
            MobclickAgent.onPause(activity)
        }

        override fun onActivityStarted(activity: Activity) {
            activityRefCount++
        }

        override fun onActivityStopped(activity: Activity) {
            activityRefCount--
            if (activityRefCount <= 0) {
                MobclickAgent.onKillProcess(activity)
                android.util.Log.i("ReadTrack", "App 进入后台，session 已保存")
            }
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
