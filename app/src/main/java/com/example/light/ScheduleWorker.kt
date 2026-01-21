package com.example.light

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ScheduleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Отримуємо збережену чергу
        val queue = Prefs.getQueue(applicationContext) ?: return Result.success()

        // ВИПРАВЛЕННЯ: Використовуємо нову функцію getSchedule
        val data = LightParser.getSchedule(queue)

        // Перевіряємо, чи отримали ми нормальні дані (не помилку і не пустий текст)
        if (data.scheduleText.isNotEmpty() && !data.scheduleText.startsWith("Помилка")) {
            // Зберігаємо тільки текст графіку (дату поки що в Prefs не зберігаємо, але можна додати пізніше)
            Prefs.saveSchedule(applicationContext, data.scheduleText)
        }

        return Result.success()
    }
}