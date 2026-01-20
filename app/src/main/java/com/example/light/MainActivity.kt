package com.example.light

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var cardTimer: MaterialCardView
    private lateinit var tvTimer: TextView
    private lateinit var tvStatusMessage: TextView
    private lateinit var tvQueueInfo: TextView
    private lateinit var tvRawData: TextView
    private lateinit var btnSetup: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivitiesIfAvailable(this.application)
        setContentView(R.layout.activity_main)

        initViews()
        setupMenu()
        setupWorker()
        updateUI()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        cardTimer = findViewById(R.id.cardTimer)
        tvTimer = findViewById(R.id.tvTimer)
        tvStatusMessage = findViewById(R.id.tvStatusMessage)
        tvQueueInfo = findViewById(R.id.tvQueueInfo)
        tvRawData = findViewById(R.id.tvRawData)
        btnSetup = findViewById(R.id.btnSetup)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        btnSetup.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
    }

    private fun setupMenu() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            val queue = when (menuItem.itemId) {
                R.id.q_1_1 -> "1.1"
                R.id.q_1_2 -> "1.2"
                R.id.q_2_1 -> "2.1"
                R.id.q_2_2 -> "2.2"
                R.id.q_3_1 -> "3.1"
                R.id.q_3_2 -> "3.2"
                R.id.q_4_1 -> "4.1"
                R.id.q_4_2 -> "4.2"
                R.id.q_5_1 -> "5.1"
                R.id.q_5_2 -> "5.2"
                R.id.q_6_1 -> "6.1"
                R.id.q_6_2 -> "6.2"
                else -> null
            }

            if (queue != null) {
                saveQueue(queue)
            } else if (menuItem.itemId == R.id.menu_refresh) {
                fetchData()
            }

            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun saveQueue(queue: String) {
        Prefs.saveQueue(this, queue)
        updateUI()
        fetchData()
    }

    private fun updateUI() {
        val queue = Prefs.getQueue(this)
        val schedule = Prefs.getSchedule(this)

        if (queue == null) {
            cardTimer.visibility = View.GONE
            btnSetup.visibility = View.VISIBLE
            tvRawData.text = "Оберіть чергу в меню зліва"
        } else {
            cardTimer.visibility = View.VISIBLE
            btnSetup.visibility = View.GONE
            tvQueueInfo.text = "Підчерга $queue"
            tvRawData.text = schedule

            val state = TimeHelper.analyze(schedule)
            tvTimer.text = state.timerText
            tvStatusMessage.text = state.statusText

            if (state.isLightOn) {
                // Зелений (світло є)
                cardTimer.setCardBackgroundColor(getColor(android.R.color.holo_green_light))
            } else {
                // Червоний (світла немає)
                cardTimer.setCardBackgroundColor(getColor(android.R.color.holo_red_light))
            }
        }
    }

    private fun fetchData() {
        val queue = Prefs.getQueue(this) ?: return
        tvStatusMessage.text = "Оновлення..."

        lifecycleScope.launch(Dispatchers.IO) {
            // Використовуємо наш окремий парсер
            val result = LightParser.getScheduleForQueue(queue)

            withContext(Dispatchers.Main) {
                if (result.isNotEmpty() && !result.startsWith("Помилка")) {
                    Prefs.saveSchedule(this@MainActivity, result)
                } else if (result.startsWith("Помилка")) {
                    tvRawData.text = result // Показуємо помилку внизу
                }
                updateUI()
            }
        }
    }

    private fun setupWorker() {
        val workRequest = PeriodicWorkRequestBuilder<ScheduleWorker>(3, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "LightUpdateWork", ExistingPeriodicWorkPolicy.KEEP, workRequest
        )
    }
}