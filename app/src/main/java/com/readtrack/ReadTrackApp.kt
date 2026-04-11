package com.readtrack

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ReadTrackApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        Log.d("ReadTrackApp", "Application started")
    }
}
