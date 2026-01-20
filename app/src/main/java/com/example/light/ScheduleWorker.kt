package com.example.light

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ScheduleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val queue = Prefs.getQueue(applicationContext) ?: return Result.success()

        // Використовуємо наш новий LightParser
        val result = LightParser.getScheduleForQueue(queue)

        if (result.isNotEmpty() && !result.startsWith("Помилка")) {
            Prefs.saveSchedule(applicationContext, result)
        }

        return Result.success()
    }
}