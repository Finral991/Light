package com.example.light

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ScheduleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val queue = Prefs.getQueue(applicationContext) ?: return Result.success()
        val data = LightParser.getSchedule(queue)

        // Зберігаємо, якщо є хоч якісь дані (на сьогодні або на завтра)
        if (data.todaySchedule.isNotEmpty() || data.tomorrowSchedule.isNotEmpty()) {
            // Тут ми можемо зберегти об'єднаний текст або додумати логіку збереження
            // Поки що для простоти збережемо графік на сьогодні, щоб віджет/сповіщення працювали
            if (data.todaySchedule.isNotEmpty()) {
                Prefs.saveSchedule(applicationContext, data.todaySchedule)
            }
        }

        return Result.success()
    }
}