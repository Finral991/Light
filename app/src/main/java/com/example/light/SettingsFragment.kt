package com.example.light

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dropdownQueue = view.findViewById<AutoCompleteTextView>(R.id.dropdownQueue)
        val dropdownSubQueue = view.findViewById<AutoCompleteTextView>(R.id.dropdownSubQueue)
        val btnSave = view.findViewById<Button>(R.id.btnSave)

        // 1. Створюємо списки даних
        val queues = listOf("1", "2", "3", "4", "5", "6")
        val subQueues = listOf("1", "2", "3", "4") // Підчерги зазвичай до 4-х

        // 2. Підключаємо адаптери (це те, що з'єднує список з інтерфейсом)
        val adapterQueue = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, queues)
        val adapterSubQueue = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, subQueues)

        dropdownQueue.setAdapter(adapterQueue)
        dropdownSubQueue.setAdapter(adapterSubQueue)

        // 3. Відображаємо поточні налаштування (якщо є)
        val savedFull = Prefs.getQueue(requireContext()) // напр. "4.1"
        if (savedFull != null && savedFull.contains(".")) {
            val parts = savedFull.split(".")
            dropdownQueue.setText(parts[0], false)
            dropdownSubQueue.setText(parts[1], false)
        }

        // 4. Збереження
        btnSave.setOnClickListener {
            val q = dropdownQueue.text.toString()
            val sq = dropdownSubQueue.text.toString()

            if (q.isNotEmpty() && sq.isNotEmpty()) {
                val fullQueue = "$q.$sq"
                Prefs.saveQueue(requireContext(), fullQueue)
                Toast.makeText(context, "Збережено: Черга $fullQueue", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Будь ласка, оберіть значення зі списку", Toast.LENGTH_SHORT).show()
            }
        }
    }
}