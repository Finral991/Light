package com.example.light
import org.jsoup.Jsoup
import java.util.regex.Pattern

object LightParser {

    data class ParsedData(
        val scheduleText: String,
        val date: String // Наприклад "20.01.2026"
    )

    fun getSchedule(queue: String): ParsedData {
        try {
            val url = "https://hoe.com.ua/page/pogodinni-vidkljuchennja"
            val doc = Jsoup.connect(url).timeout(60000).ignoreContentType(true).ignoreHttpErrors(true).get()
            val fullText = doc.body().text()

            // 1. Шукаємо дату. Зазвичай це формат XX.XX.XXXX
            val dateMatcher = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4}").matcher(fullText)
            var foundDate = "Графік"
            if (dateMatcher.find()) {
                foundDate = "Графік на ${dateMatcher.group()}"
            }

            // 2. Шукаємо чергу
            val parts = fullText.split(Regex("(?i)(під)?черга"))
            val sb = StringBuilder()

            for (part in parts) {
                val line = part.trim()
                if (line.startsWith(queue)) {
                    val info = line.substringBefore(";")
                    // Прибираємо зайві "4.1 - " з тексту
                    val cleanInfo = info.replace("$queue", "").replace("-", "").trim()
                    sb.append(cleanInfo).append("\n")
                }
            }

            val result = if (sb.isNotEmpty()) sb.toString() else ""
            return ParsedData(result, foundDate)

        } catch (e: Exception) {
            return ParsedData("Помилка", "")
        }
    }
}