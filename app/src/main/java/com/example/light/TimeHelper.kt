package com.example.light

import java.time.LocalTime
import java.time.temporal.ChronoUnit

object TimeHelper {

    data class State(
        val isLightOn: Boolean,
        val timerText: String,
        val statusText: String
    )

    fun analyze(scheduleText: String): State {
        if (scheduleText.isEmpty() || scheduleText.startsWith("Помилка"))
            return State(true, "--:--", "Немає даних")

        // Якщо немає слова "до", значить немає годин відключень
        if (!scheduleText.contains("до", ignoreCase = true)) {
            return State(true, "Світло є", "За графіком")
        }

        val now = LocalTime.now()
        // Шукаємо: 00:00 до 04:00
        val regex = Regex("(\\d{2}:\\d{2})\\s*до\\s*(\\d{2}:\\d{2})")
        val matches = regex.findAll(scheduleText)

        var isCurrentlyOff = false
        var nextEvent: LocalTime? = null

        // 1. Чи темно зараз?
        for (match in matches) {
            try {
                val start = LocalTime.parse(match.groupValues[1])
                val end = LocalTime.parse(match.groupValues[2])

                // Обробка переходу через північ (наприклад 22:00 до 02:00)
                if (start.isAfter(end)) {
                    if (now.isAfter(start) || now.isBefore(end)) {
                        isCurrentlyOff = true
                        nextEvent = end
                        break
                    }
                } else {
                    if (now.isAfter(start) && now.isBefore(end)) {
                        isCurrentlyOff = true
                        nextEvent = end
                        break
                    }
                }
            } catch (e: Exception) { continue }
        }

        // 2. Якщо світло є, коли вимкнуть?
        if (!isCurrentlyOff) {
            var minMinutes = Long.MAX_VALUE
            for (match in matches) {
                try {
                    val start = LocalTime.parse(match.groupValues[1])
                    // Лише майбутні події
                    if (start.isAfter(now)) {
                        val diff = ChronoUnit.MINUTES.between(now, start)
                        if (diff < minMinutes) {
                            minMinutes = diff
                            nextEvent = start
                        }
                    }
                } catch (e: Exception) { continue }
            }
        }

        val timerStr = if (nextEvent != null) {
            val hours = ChronoUnit.HOURS.between(now, nextEvent)
            val minutes = ChronoUnit.MINUTES.between(now, nextEvent) % 60
            String.format("%dг %02dхв", hours, minutes)
        } else {
            "Без змін"
        }

        val statusStr = if (isCurrentlyOff) "До увімкнення:" else "До відключення:"
        return State(!isCurrentlyOff, timerStr, statusStr)
    }
}