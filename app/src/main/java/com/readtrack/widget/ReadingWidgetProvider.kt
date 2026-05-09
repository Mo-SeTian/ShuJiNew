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
        updateAllWidgets(context)
    }

    override fun onEnabled(context: Context) {
    }

    override fun onDisabled(context: Context) {
        job.cancel()
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PREV_PAGE, ACTION_NEXT_PAGE -> {
                val appWidgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val prefs = context.applicationContext.getSharedPreferences(
                        WidgetUpdateHelper.PREFS_NAME, Context.MODE_PRIVATE
                    )
                    val currentPage = prefs.getInt("${WidgetUpdateHelper.PAGE_KEY_PREFIX}$appWidgetId", 0)
                    val newPage = if (intent.action == ACTION_NEXT_PAGE) currentPage + 1 else currentPage - 1
                    prefs.edit().putInt("${WidgetUpdateHelper.PAGE_KEY_PREFIX}$appWidgetId", newPage).apply()
                    updateAllWidgets(context)
                }
            }
            else -> {
                super.onReceive(context, intent)
                if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
                    updateAllWidgets(context)
                }
            }
        }
    }

    private fun updateAllWidgets(context: Context) {
        val helper = getWidgetUpdateHelper(context)
        scope.launch {
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
        const val ACTION_PREV_PAGE = "com.readtrack.widget.PREV_PAGE"
        const val ACTION_NEXT_PAGE = "com.readtrack.widget.NEXT_PAGE"
        const val EXTRA_WIDGET_ID = "extra_widget_id"
    }
}
