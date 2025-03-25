package com.example.tuvguhall

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Если пользователь авторизован — перейти в главное расписание
        if (auth.currentUser != null) {
            // TODO: показать расписание
            setContentView(R.layout.activity_main)
        } else {
            // Перейти на экран авторизации
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish() // Закрываем MainActivity, чтобы пользователь не мог вернуться назад
        }
    }
}