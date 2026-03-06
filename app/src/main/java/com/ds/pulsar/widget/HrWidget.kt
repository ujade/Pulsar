package com.ds.pulsar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ds.pulsar.MainActivity
import com.ds.pulsar.R

class HrWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val bpm = HrWidgetPrefs.getLastBpm(context)
        val status = HrWidgetPrefs.getStatus(context)
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id, bpm, status)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_HR_UPDATE) {
            val bpm = intent.getIntExtra(EXTRA_BPM, 0)
            val status = intent.getStringExtra(EXTRA_STATUS) ?: STATUS_CONNECTED

            HrWidgetPrefs.save(context, bpm, status)

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, HrWidget::class.java)
            )
            for (id in ids) {
                updateWidget(context, manager, id, bpm, status)
            }
        }
    }

    companion object {
        const val ACTION_HR_UPDATE = "net.dsvi.pulsar.ACTION_HR_UPDATE"
        const val EXTRA_BPM = "extra_bpm"
        const val EXTRA_STATUS = "extra_status"

        const val STATUS_CONNECTED = "CONNECTED"
        const val STATUS_DISCONNECTED = "DISCONNECTED"
        const val STATUS_SCANNING = "SCANNING…"
        fun sendHrUpdate(context: Context, bpm: Int, status: String = STATUS_CONNECTED) {
            val intent = Intent(ACTION_HR_UPDATE).apply {
                component = ComponentName(context, HrWidget::class.java)
                putExtra(EXTRA_BPM, bpm)
                putExtra(EXTRA_STATUS, status)
            }
            context.sendBroadcast(intent)
        }

        fun sendDisconnected(context: Context) {
            sendHrUpdate(context, bpm = 0, status = STATUS_DISCONNECTED)
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            bpm: Int,
            status: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_hr)

            val bpmText = if (bpm > 0) bpm.toString() else "––"
            views.setTextViewText(R.id.widget_bpm, bpmText)

            val zoneColor = zoneColor(bpm)
            views.setInt(R.id.widget_bpm, "setTextColor", zoneColor)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            manager.updateAppWidget(widgetId, views)
        }

        private fun zoneColor(bpm: Int): Int {
            return when {
                bpm <= 0  -> 0xFF667788.toInt()
                bpm < 100 -> 0xFF00E5FF.toInt()
                bpm < 130 -> 0xFF00FF9D.toInt()
                bpm < 160 -> 0xFFFFCC00.toInt()
                else      -> 0xFFFF4D4D.toInt()
            }
        }
    }
}
