package com.example.light
import android.content.Context

object Prefs {
    private const val PREFS_NAME = "LightAppPrefs"
    private const val KEY_QUEUE = "user_queue"
    private const val KEY_SCHEDULE = "cached_schedule_text"

    fun saveQueue(context: Context, queue: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_QUEUE, queue).apply()
    }

    fun getQueue(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_QUEUE, null)
    }

    fun saveSchedule(context: Context, text: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_SCHEDULE, text).apply()
    }

    fun getSchedule(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SCHEDULE, "") ?: ""
    }
}