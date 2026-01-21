package com.example.light

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
            addCard(container, "Увага", "Оберіть чергу в меню", isOutage = false, isWarning = true)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val data = LightParser.getSchedule(queue)

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                container.removeAllViews() // Очищаємо екран

                // 1. ДОДАЄМО ПОТОЧНУ ДАТУ ЗАМІСТЬ СЛОВА "ГРАФІК"
                val today = LocalDate.now()
                // Формат: "Середа, 21 січня"
                val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("uk", "UA"))
                val dateText = today.format(formatter).replaceFirstChar { it.uppercase() }

                addMainDateHeader(container, dateText)

                if (data.scheduleText.isEmpty() || data.scheduleText.startsWith("Помилка")) {
                    addSectionTitle(container, "Статус")
                    addCard(container, "Помилка", "Не вдалося завантажити дані", false, true)
                } else {
                    processAndDisplaySchedule(container, data.scheduleText)
                }
            }
        }
    }

    private fun processAndDisplaySchedule(container: LinearLayout, text: String) {
        val now = LocalTime.now()

        // 1. Парсимо інтервали
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

        // 2. Блок "Зараз"
        addSectionTitle(container, "Зараз")

        var isLightsOutNow = false
        var currentOutage: OutageInterval? = null

        for (outage in outages) {
            if ((now == outage.start || now.isAfter(outage.start)) && now.isBefore(outage.end)) {
                isLightsOutNow = true
                currentOutage = outage
                break
            }
        }

        if (isLightsOutNow && currentOutage != null) {
            addCard(container, "Триває відключення", currentOutage.rawText, isOutage = true, isWarning = false)
        } else {
            addCard(container, "Електроенергія присутня", "Світло є", isOutage = false, isWarning = false)
        }

        // 3. Блок "Наступні"
        val futureOutages = outages.filter { it.start.isAfter(now) }

        addSectionTitle(container, "Наступні відключення") // Текст заголовка

        if (futureOutages.isNotEmpty()) {
            for (outage in futureOutages) {
                addCard(container, "Заплановано", outage.rawText, isOutage = true, isWarning = false)
            }
        } else {
            addCard(container, "Чудово", "Подальші відключення відсутні", isOutage = false, isWarning = true)
        }
    }

    // --- Функції малювання ---

    // ВЕЛИКА ДАТА ЗВЕРХУ (Замість "Графік")
    private fun addMainDateHeader(container: LinearLayout, text: String) {
        val textView = TextView(requireContext())
        textView.text = text
        textView.textSize = 22f // Великий розмір
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        textView.typeface = Typeface.DEFAULT_BOLD
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 16, 0, 32) // Відступи
        textView.layoutParams = params

        container.addView(textView)
    }

    // Заголовки розділів ("Зараз", "Наступні")
    private fun addSectionTitle(container: LinearLayout, text: String) {
        val textView = TextView(requireContext())
        textView.text = text
        textView.textSize = 16f
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary)) // Сірий колір
        textView.typeface = Typeface.DEFAULT_BOLD

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(12, 24, 0, 12)
        textView.layoutParams = params

        container.addView(textView)
    }

    private fun addCard(container: LinearLayout, title: String, time: String, isOutage: Boolean, isWarning: Boolean) {
        val cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_schedule_card, container, false)

        val tvTitle = cardView.findViewById<TextView>(R.id.tvCardTitle)
        val tvTime = cardView.findViewById<TextView>(R.id.tvCardTime)
        val tvStatus = cardView.findViewById<TextView>(R.id.tvCardStatus)

        tvTitle.text = title
        tvTime.text = time

        if (isOutage) {
            tvStatus.text = "Світла не буде"
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red))
            tvTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red))
        } else if (isWarning) {
            tvStatus.text = "Інформація"
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            tvTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        } else {
            tvStatus.text = "Світло є"
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_green))
            tvTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_green))
        }

        container.addView(cardView)
    }
}