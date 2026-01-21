package com.example.light

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etQueue = view.findViewById<EditText>(R.id.etQueue)
        val etSubQueue = view.findViewById<EditText>(R.id.etSubQueue)
        val btnSave = view.findViewById<Button>(R.id.btnSave)

        // Завантажуємо старі дані
        // Примітка: треба буде оновити Prefs.kt, щоб він умів зберігати split-дані
        val savedFull = Prefs.getQueue(requireContext()) // Наприклад "4.1"
        if (savedFull != null && savedFull.contains(".")) {
            val parts = savedFull.split(".")
            etQueue.setText(parts[0])
            etSubQueue.setText(parts[1])
        }

        btnSave.setOnClickListener {
            val q = etQueue.text.toString()
            val sq = etSubQueue.text.toString()

            if (q.isNotEmpty() && sq.isNotEmpty()) {
                val fullQueue = "$q.$sq"
                Prefs.saveQueue(requireContext(), fullQueue)
                Toast.makeText(context, "Збережено: $fullQueue", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Заповніть обидва поля", Toast.LENGTH_SHORT).show()
            }
        }
    }
}