package com.example.light

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var tvPageTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvPageTitle = findViewById(R.id.tvPageTitle)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Завантажуємо стартовий екран
        loadFragment(HomeFragment())
        tvPageTitle.text = "Графік"

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    tvPageTitle.text = "Графік"
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    tvPageTitle.text = "Налаштування"
                }
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}