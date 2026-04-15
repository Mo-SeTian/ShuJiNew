package com.readtrack.util

import android.os.SystemClock
import android.util.Log

object PerformanceTrace {
    private const val TAG = "ReadTrackPerf"

    inline fun <T> measure(label: String, block: () -> T): T {
        val start = SystemClock.elapsedRealtimeNanos()
        return try {
            block()
        } finally {
            val durationMs = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
            Log.d(TAG, "$label took ${"%.2f".format(durationMs)} ms")
        }
    }

    fun mark(label: String) {
        Log.d(TAG, label)
    }
}