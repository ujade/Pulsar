package com.ds.pulsar.widget
import android.content.Context

object HrWidgetPrefs {
    private const val PREFS = "pulsar_widget_prefs"
    private const val KEY_BPM = "last_bpm"
    private const val KEY_STATUS = "last_status"

    fun save(context: Context, bpm: Int, status: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_BPM, bpm)
            .putString(KEY_STATUS, status)
            .apply()
    }

    fun getLastBpm(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_BPM, 0)

    fun getStatus(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_STATUS, HrWidget.STATUS_DISCONNECTED)
            ?: HrWidget.STATUS_DISCONNECTED
}
