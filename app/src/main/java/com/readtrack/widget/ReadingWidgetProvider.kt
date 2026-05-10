package com.readtrack.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReadingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAllWidgets(context)
    }

    override fun onDisabled(context: Context) {
        widgetScope.cancel()
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val helper = getWidgetUpdateHelper(context)
        widgetScope.launch {
            withContext(NonCancellable) {
                helper.clearWidgetSelections(appWidgetIds)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            updateAllWidgets(context)
        }
    }

    private fun updateAllWidgets(context: Context) {
        val helper = getWidgetUpdateHelper(context)
        widgetScope.launch {
            helper.updateWidgets(context)
        }
    }

    private fun getWidgetUpdateHelper(context: Context): WidgetUpdateHelper {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        return entryPoint.widgetUpdateHelper()
    }

    companion object {
        // 进程级单例 scope，避免 BroadcastReceiver 多实例导致 scope 泄漏
        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}
