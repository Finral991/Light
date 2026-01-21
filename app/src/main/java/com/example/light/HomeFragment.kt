package com.example.light

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Знаходимо елементи на екрані
        val container = view.findViewById<LinearLayout>(R.id.cardsContainer)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val tvHeaderDate = view.findViewById<TextView>(R.id.tvHeaderDate)

        // Отримуємо збережену чергу
        val queue = Prefs.getQueue(requireContext())

        // Якщо черга не обрана - пишемо підказку
        if (queue == null) {
            tvHeaderDate.text = "Черга не обрана"
            // Додаємо картку-підказку
            addCard(container, "Увага", "Перейдіть в налаштування", isOutage = false, isWarning = true)
            progressBar.visibility = View.GONE
            return
        }

        // Запускаємо завантаження (в іншому потоці)
        lifecycleScope.launch(Dispatchers.IO) {

            // Викликаємо наш парсер
            val data = LightParser.getSchedule(queue)

            // Повертаємося в головний потік, щоб малювати інтерфейс
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                tvHeaderDate.text = data.date // Ставимо дату (наприклад "Графік на 21.01")

                // Очищаємо контейнер від старих карток (лишаємо тільки заголовок)
                // childCount > 1 означає, що ми пропускаємо перші елементи (Заголовок дати)
                // Але надійніше видалити всі View, крім першого (TextView дати)
                val viewsToRemove = mutableListOf<View>()
                for (i in 0 until container.childCount) {
                    val v = container.getChildAt(i)
                    if (v.id != R.id.tvHeaderDate && v.id != R.id.progressBar) {
                        viewsToRemove.add(v)
                    }
                }
                viewsToRemove.forEach { container.removeView(it) }

                // Аналізуємо текст і малюємо картки
                if (data.scheduleText.isEmpty() || data.scheduleText.startsWith("Помилка")) {
                    addCard(container, "Статус", "Даних немає або помилка", isOutage = false, isWarning = true)
                } else {
                    parseAndCreateCards(container, data.scheduleText)
                }
            }
        }
    }

    // Функція, яка ріже текст "з 00:00 до 04:00, з 08:00 до 12:00" на окремі картки
    private fun parseAndCreateCards(container: LinearLayout, text: String) {
        // Очищаємо текст від зайвих слів "з " та переносів рядків
        val cleanText = text.replace("з ", "").replace("\n", "")

        // Розбиваємо по комі (бо зазвичай графік йде через кому)
        val timeIntervals = cleanText.split(",")

        var hasOutages = false

        for (interval in timeIntervals) {
            val time = interval.trim()
            if (time.contains("до")) {
                // Це інтервал відключення!
                hasOutages = true
                addCard(container, "Відключення", time, isOutage = true, isWarning = false)
            }
        }

        // Якщо ми пройшли весь текст і не знайшли слово "до", значить відключень немає
        if (!hasOutages) {
            addCard(container, "Чудово", "Світло є весь день", isOutage = false, isWarning = false)
        }
    }

    // Функція, яка бере XML картки і додає її на екран
    private fun addCard(container: LinearLayout, title: String, time: String, isOutage: Boolean, isWarning: Boolean) {
        // "Надуваємо" (Inflate) XML файл картки
        val cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_schedule_card, container, false)

        val tvTitle = cardView.findViewById<TextView>(R.id.tvCardTitle)
        val tvTime = cardView.findViewById<TextView>(R.id.tvCardTime)
        val tvStatus = cardView.findViewById<TextView>(R.id.tvCardStatus)

        tvTitle.text = title
        tvTime.text = time

        if (isOutage) {
            tvStatus.text = "Електроенергія відсутня"
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red))
            // Можна змінити колір часу на червоний для акценту
            tvTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red))
        } else if (isWarning) {
            tvStatus.text = "Увага"
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        } else {
            tvStatus.text = "Світло є"
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_green))
            tvTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_green))
        }

        // Додаємо готову картку в список
        container.addView(cardView)
    }
}