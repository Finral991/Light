package com.example.light

import org.jsoup.Jsoup

object LightParser {

    // Повертає текст графіку або помилку
    fun getScheduleForQueue(queue: String): String {
        try {
            val url = "https://hoe.com.ua/page/pogodinni-vidkljuchennja"
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(60000) // 60 секунд тайм-аут
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .get()

            val fullText = doc.body().text()
            // Розбиваємо текст на шматки, де є слово "черга" або "підчерга"
            val parts = fullText.split(Regex("(?i)(під)?черга"))

            val sb = StringBuilder()

            for (part in parts) {
                val line = part.trim()
                // Шукаємо точний збіг. Наприклад, якщо queue="4.1", шукаємо "4.1"
                // Використовуємо startsWith, щоб знайти "4.1." або "4.1 "
                if (line.startsWith(queue)) {
                    // Беремо все до крапки з комою
                    val info = line.substringBefore(";")
                    sb.append("• ").append(info).append("\n\n")
                }
            }

            return if (sb.isNotEmpty()) sb.toString() else ""

        } catch (e: Exception) {
            e.printStackTrace()
            return "Помилка: ${e.message}"
        }
    }
}