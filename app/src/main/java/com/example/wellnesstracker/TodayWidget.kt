package com.example.wellnesstracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class TodayWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = Prefs(context)
        val habits = prefs.getHabits()
        
        // Calculate completion for today
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val done = habits.count { it.completedDates.contains(today) }
        val percent = if (habits.isEmpty()) 0 else (done * 100 / habits.size)

        for (id in appWidgetIds) {
            val rv = RemoteViews(context.packageName, R.layout.widget_today)
            rv.setTextViewText(R.id.widgetPercent, "$percent%")
            val pi = PendingIntent.getActivity(context, 0, Intent(context, HomeActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            rv.setOnClickPendingIntent(R.id.widgetRoot, pi)
            appWidgetManager.updateAppWidget(id, rv)
        }
    }
}
