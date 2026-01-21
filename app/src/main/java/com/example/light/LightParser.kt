package com.example.light

import org.jsoup.Jsoup
import java.util.regex.Pattern

object LightParser {

    data class ParsedData(
        val scheduleText: String,
        val date: String
    )

    fun getSchedule(queue: String): ParsedData {
        try {
            val url = "https://hoe.com.ua/page/pogodinni-vidkljuchennja"

            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "uk-UA")
                .timeout(60000)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .get()

            val fullText = doc.body().text()

            // 1. Шукаємо дату
            val dateMatcher = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4}").matcher(fullText)
            var foundDate = ""
            if (dateMatcher.find()) {
                foundDate = dateMatcher.group() // Просто дата "21.01.2026"
            }

            // 2. Шукаємо чергу
            // Розбиваємо текст на частини по слову "черга"
            val parts = fullText.split(Regex("(?i)(під)?черга"))
            val sb = StringBuilder()

            // Regex для пошуку часу: "00:00 до 00:00" (з урахуванням можливих пробілів)
            val timePattern = Regex("\\d{2}:\\d{2}\\s*до\\s*\\d{2}:\\d{2}")

            for (part in parts) {
                val line = part.trim()

                // Якщо рядок починається з нашої черги (наприклад "4.1")
                if (line.startsWith(queue)) {
                    // ЗАМІСТЬ ОБРІЗАННЯ ПО ";" МИ ШУКАЄМО ВСІ ЗБІГИ ЧАСУ В ЦЬОМУ РЯДКУ
                    val matches = timePattern.findAll(line)

                    // Збираємо всі знайдені години через кому
                    val allTimes = matches.map { it.value }.joinToString(", ")

                    if (allTimes.isNotEmpty()) {
                        sb.append(allTimes).append("\n")
                    }
                }
            }

            val result = if (sb.isNotEmpty()) sb.toString() else ""

            return ParsedData(result, foundDate)

        } catch (e: Exception) {
            e.printStackTrace()
            return ParsedData("Помилка з'єднання", "")
        }
    }
}