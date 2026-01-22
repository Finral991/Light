package com.example.light

import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object LightParser {

    data class ParsedData(
        val todaySchedule: String,
        val tomorrowSchedule: String,
        val todayDate: String,
        val tomorrowDate: String
    )

    fun getSchedule(queue: String): ParsedData {
        try {
            val url = "https://hoe.com.ua/page/pogodinni-vidkljuchennja"

            // 1. Качаємо сайт
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(30000)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .get()

            val fullText = doc.body().text()

            // 2. Готуємо дати (тільки день і місяць, наприклад "22.01")
            // Це надійніше, бо рік іноді пишуть, а іноді ні
            val formatter = DateTimeFormatter.ofPattern("dd.MM")
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)

            val todayShort = today.format(formatter)       // "21.01"
            val tomorrowShort = tomorrow.format(formatter) // "22.01"

            // 3. Визначаємо, про який день йдеться
            // Просто перевіряємо, чи згадується завтрашня дата в тексті
            val hasTomorrowDate = fullText.contains(tomorrowShort)

            var scheduleToday = ""
            var scheduleTomorrow = ""

            // Шукаємо години для вашої черги
            val foundSchedule = extractScheduleForQueue(fullText, queue)

            // ЛОГІКА РОЗПОДІЛУ:
            if (hasTomorrowDate) {
                // Якщо в тексті є "22.01", вважаємо це графіком на завтра
                scheduleTomorrow = foundSchedule
            } else {
                // Інакше вважаємо це графіком на сьогодні
                scheduleToday = foundSchedule
            }

            // Якщо нічого не знайшли, повертаємо спеціальне повідомлення для діагностики
            if (scheduleToday.isEmpty() && scheduleTomorrow.isEmpty()) {
                val debugMsg = if (hasTomorrowDate) "Знайшов дату $tomorrowShort, але не чергу $queue" else "Не знайшов дату $tomorrowShort"
                if (hasTomorrowDate) scheduleTomorrow = debugMsg else scheduleToday = debugMsg
            }

            return ParsedData(
                todaySchedule = scheduleToday,
                tomorrowSchedule = scheduleTomorrow,
                todayDate = today.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                tomorrowDate = tomorrow.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            )

        } catch (e: Exception) {
            e.printStackTrace()
            return ParsedData("Помилка: ${e.message}", "", "", "")
        }
    }

    private fun extractScheduleForQueue(text: String, queue: String): String {
        val sb = StringBuilder()

        // Розбиваємо текст на шматки по слову "черга"
        val parts = text.split(Regex("(?i)(під)?черга"))

        // Шукаємо час у форматі "00:00 до 00:00"
        val timePattern = Regex("\\d{2}:\\d{2}\\s*до\\s*\\d{2}:\\d{2}")

        for (part in parts) {
            val line = part.trim()

            // Перевіряємо, чи починається рядок з номера черги.
            // Додаємо крапку в перевірку, бо часто пишуть "4.1."
            if (line.startsWith(queue) || line.startsWith("$queue.")) {

                val matches = timePattern.findAll(line)
                val allTimes = matches.map { it.value }.joinToString(", ")

                if (allTimes.isNotEmpty()) {
                    sb.append(allTimes).append("\n")
                }
            }
        }
        return sb.toString()
    }
}