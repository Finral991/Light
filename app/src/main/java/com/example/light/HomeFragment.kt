package com.example.light

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeFragment : Fragment(R.layout.fragment_home) {

    data class OutageInterval(
        val start: LocalTime,
        val end: LocalTime,
        val rawText: String
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val container = view.findViewById<LinearLayout>(R.id.cardsContainer)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        val queue = Prefs.getQueue(requireContext())

        if (queue == null) {
            progressBar.visibility = View.GONE
            addSectionTitle(container, "Налаштування")
            addCard(container, "Увага", "Оберіть чергу в меню", null, isWarning = true)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val data = LightParser.getSchedule(queue)

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                container.removeAllViews()

                // ЗАГОЛОВОК ДАТИ СЬОГОДНІ
                val todayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("uk", "UA"))
                val todayTitle = LocalDate.now().format(todayFormatter).replaceFirstChar { it.uppercase() }
                addMainDateHeader(container, todayTitle)

                if (data.todaySchedule.isEmpty() && data.tomorrowSchedule.isEmpty()) {
                    addSectionTitle(container, "Статус")
                    addCard(container, "Інформація", "Графік відсутній або помилка", null, true)
                } else {
                    // СЬОГОДНІ
                    processTodaySchedule(container, data.todaySchedule)

                    // ЗАВТРА
                    if (data.tomorrowSchedule.isNotEmpty()) {
                        addSpacer(container)
                        addMainDateHeader(container, "Завтра, ${data.tomorrowDate}")
                        processTomorrowSchedule(container, data.tomorrowSchedule)
                    }
                }
            }
        }
    }

    private fun processTodaySchedule(container: LinearLayout, text: String) {
        val now = LocalTime.now()
        val outages = parseIntervals(text)

        // === БЛОК "ЗАРАЗ" ===
        addSectionTitle(container, "Зараз")

        var isLightsOutNow = false
        var currentOutage: OutageInterval? = null
        var nextOutageStart: LocalTime? = null

        // 1. Шукаємо, чи є зараз відключення
        for (outage in outages) {
            if ((now == outage.start || now.isAfter(outage.start)) && now.isBefore(outage.end)) {
                isLightsOutNow = true
                currentOutage = outage
            }
            // Шукаємо найближче майбутнє відключення для "зеленої" картки
            if (outage.start.isAfter(now)) {
                if (nextOutageStart == null || outage.start.isBefore(nextOutageStart)) {
                    nextOutageStart = outage.start
                }
            }
        }

        if (isLightsOutNow && currentOutage != null) {
            // ЧЕРВОНА КАРТКА (Світла немає)
            addCard(
                container,
                "Триває відключення",
                currentOutage.rawText,
                // Передаємо час початку і кінця для розрахунку прогресу
                progressTimes = Pair(currentOutage.start, currentOutage.end),
                isWarning = false,
                isRed = true
            )
        } else {
            // ЗЕЛЕНА КАРТКА (Світло є)
            // Треба зрозуміти, коли почалось "світло" (кінець попереднього відключення або 00:00)
            // і коли закінчиться (початок наступного відключення або 24:00)

            val greenStart = findGreenStart(now, outages)
            val greenEnd = nextOutageStart ?: LocalTime.of(23, 59)

            val timeText = if (nextOutageStart != null) "до $nextOutageStart" else "до кінця доби"

            addCard(
                container,
                "Електроенергія присутня",
                timeText,
                progressTimes = Pair(greenStart, greenEnd),
                isWarning = false,
                isRed = false
            )
        }

        // === БЛОК "НАСТУПНІ" ===
        val futureOutages = outages.filter { it.start.isAfter(now) }

        if (futureOutages.isNotEmpty()) {
            addSectionTitle(container, "Наступні відключення")
            for (outage in futureOutages) {
                addCard(container, "Заплановано", outage.rawText, null, false, isRed = true)
            }
        } else if (isLightsOutNow) {
            // Якщо зараз темно, але далі відключень немає
            addSectionTitle(container, "Наступні відключення")
            addCard(container, "Чудово", "Більше відключень не планується", null, true)
        }
    }

    private fun findGreenStart(now: LocalTime, outages: List<OutageInterval>): LocalTime {
        // Шукаємо, коли закінчилось останнє відключення перед "зараз"
        var lastEnd = LocalTime.of(0, 0)
        for (outage in outages) {
            if (outage.end.isBefore(now) && outage.end.isAfter(lastEnd)) {
                lastEnd = outage.end
            }
        }
        return lastEnd
    }

    private fun processTomorrowSchedule(container: LinearLayout, text: String) {
        val outages = parseIntervals(text)
        addSectionTitle(container, "Розклад на весь день")
        for (outage in outages) {
            addCard(container, "Заплановано", outage.rawText, null, false, isRed = true)
        }
    }

    // --- ГОЛОВНА ФУНКЦІЯ МАЛЮВАННЯ КАРТКИ ---
    private fun addCard(
        container: LinearLayout,
        title: String,
        timeText: String,
        progressTimes: Pair<LocalTime, LocalTime>?, // Час початку і кінця для прогресу
        isWarning: Boolean,
        isRed: Boolean = false
    ) {
        val cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_schedule_card, container, false)

        val tvTitle = cardView.findViewById<TextView>(R.id.tvCardTitle)
        val tvTime = cardView.findViewById<TextView>(R.id.tvCardTime)
        val tvStatus = cardView.findViewById<TextView>(R.id.tvCardStatus)
        val tvTimerRemaining = cardView.findViewById<TextView>(R.id.tvTimerRemaining)
        val progressBar = cardView.findViewById<ProgressBar>(R.id.progressBarCard)

        tvTitle.text = title
        tvTime.text = timeText

        // Налаштування кольорів
        val colorRes = if (isRed) R.color.status_red else if (isWarning) R.color.text_secondary else R.color.status_green
        val color = ContextCompat.getColor(requireContext(), colorRes)

        tvStatus.setTextColor(color)
        tvTime.setTextColor(color)

        // Налаштування статус-тексту
        if (isWarning) {
            tvStatus.text = "Інформація"
        } else if (isRed) {
            tvStatus.text = "Світла немає"
        } else {
            tvStatus.text = "Світло є"
        }

        // --- ЛОГІКА ПРОГРЕСУ ТА ТАЙМЕРА ---
        if (progressTimes != null) {
            progressBar.visibility = View.VISIBLE
            tvTimerRemaining.visibility = View.VISIBLE

            val start = progressTimes.first
            val end = progressTimes.second
            val now = LocalTime.now()

            // Рахуємо хвилини
            val totalMinutes = Duration.between(start, end).toMinutes().toFloat()
            val passedMinutes = Duration.between(start, now).toMinutes().toFloat()
            val remainingMinutes = totalMinutes - passedMinutes

            // Захист від ділення на нуль
            val progress = if (totalMinutes > 0) ((passedMinutes / totalMinutes) * 100).toInt() else 0

            progressBar.progress = progress
            // Фарбуємо прогрес бар в колір статусу (червоний або зелений)
            progressBar.progressTintList = ColorStateList.valueOf(color)

            // Формуємо текст таймера (наприклад "1 год 5 хв")
            val hoursLeft = (remainingMinutes / 60).toInt()
            val minsLeft = (remainingMinutes % 60).toInt()

            val statusPrefix = if(isRed) "Увімкнення через: " else "Вимкнення через: "

            if (hoursLeft > 0) {
                tvTimerRemaining.text = "$statusPrefix${hoursLeft} год ${minsLeft} хв"
            } else {
                tvTimerRemaining.text = "$statusPrefix${minsLeft} хв"
            }

            // Приховуємо звичайний текстовий статус, бо у нас є таймер
            tvStatus.visibility = View.GONE

        } else {
            // Для майбутніх карток прогрес не потрібен
            progressBar.visibility = View.GONE
            tvTimerRemaining.visibility = View.GONE
            tvStatus.visibility = View.VISIBLE
        }

        container.addView(cardView)
    }

    // --- Парсинг і UI допоміжні функції (без змін) ---
    private fun parseIntervals(text: String): List<OutageInterval> {
        val cleanText = text.replace("з ", "").replace("\n", "")
        val rawIntervals = cleanText.split(",")
        val outages = mutableListOf<OutageInterval>()
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        for (raw in rawIntervals) {
            val parts = raw.trim().split(" до ")
            if (parts.size == 2) {
                try {
                    val start = LocalTime.parse(parts[0].trim(), timeFormatter)
                    val endString = if (parts[1].trim() == "24:00") "23:59" else parts[1].trim()
                    val end = LocalTime.parse(endString, timeFormatter)
                    outages.add(OutageInterval(start, end, "${parts[0]} - ${parts[1]}"))
                } catch (e: Exception) { continue }
            }
        }
        return outages
    }

    private fun addMainDateHeader(container: LinearLayout, text: String) {
        val textView = TextView(requireContext())
        textView.text = text
        textView.textSize = 22f
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        textView.typeface = Typeface.DEFAULT_BOLD
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 16, 0, 16)
        textView.layoutParams = params
        container.addView(textView)
    }

    private fun addSectionTitle(container: LinearLayout, text: String) {
        val textView = TextView(requireContext())
        textView.text = text
        textView.textSize = 14f
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(12, 16, 0, 8)
        textView.layoutParams = params
        container.addView(textView)
    }

    private fun addSpacer(container: LinearLayout) {
        val view = View(requireContext())
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2)
        params.setMargins(24, 40, 24, 20)
        view.layoutParams = params
        view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_dark))
        container.addView(view)
    }
}