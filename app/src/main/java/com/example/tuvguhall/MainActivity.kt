package com.example.tuvguhall

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        // Применение темы (тёмная/светлая)
        val isDarkMode = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            .getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )


        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Если пользователь авторизован — переходим в расписание
        if (auth.currentUser != null) {
            val intent = Intent(this, ScheduleActivity::class.java)
            startActivity(intent)
        } else {
            // Иначе — на экран входа
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
        }

        finish() // Закрываем MainActivity, чтобы не возвращаться к нему
    }
}