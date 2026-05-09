package com.readtrack.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ReadingWidgetProvider : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val helper = getWidgetUpdateHelper(context)
        scope.launch {
            helper.updateWidgets(context)
        }
    }

    override fun onEnabled(context: Context) {
        // 第一个小组件被添加时
    }

    override fun onDisabled(context: Context) {
        // 最后一个小组件被移除时
        job.cancel()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val helper = getWidgetUpdateHelper(context)
            scope.launch {
                helper.updateWidgets(context)
            }
        }
    }

    private fun getWidgetUpdateHelper(context: Context): WidgetUpdateHelper {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        return entryPoint.widgetUpdateHelper()
    }
}
