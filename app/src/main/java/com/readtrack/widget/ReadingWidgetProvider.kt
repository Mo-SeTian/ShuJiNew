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
        val pendingResult = goAsync()
        widgetScope.launch {
            try {
                getWidgetUpdateHelper(context).clearWidgetSelections(appWidgetIds)
            } finally {
                pendingResult.finish()
            }
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
        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
