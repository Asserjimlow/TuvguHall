package com.example.tuvguhall

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import android.os.Build
import android.widget.Toast
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class AuthActivity : AppCompatActivity() {

    private lateinit var roleEditText: EditText
    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signInButton: Button
    private lateinit var registerButton: Button
    private lateinit var forgotPasswordTextView: TextView
    private lateinit var guestButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        auth = FirebaseAuth.getInstance()

        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        signInButton = findViewById(R.id.buttonSignIn)
        registerButton = findViewById(R.id.buttonRegister)
        forgotPasswordTextView = findViewById(R.id.textForgotPassword)
        roleEditText = findViewById(R.id.editTextRole)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        signInButton.setOnClickListener {
            signInUser()
        }

        registerButton.setOnClickListener {
            registerUser()
        }
        guestButton = findViewById(R.id.buttonGuest)

        guestButton.setOnClickListener {
            val intent = Intent(this, ScheduleActivity::class.java)
            startActivity(intent)
            finish()
        }

        forgotPasswordTextView.setOnClickListener {
            resetPassword()
        }
    }

    private fun signInUser() {
        val email = emailEditText.text.toString()
        val password = passwordEditText.text.toString()

        if (email.isNotEmpty() && password.isNotEmpty()) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Успешный вход!", Toast.LENGTH_SHORT).show()
                        // Переход к следующему экрану
                        val intent = Intent(this@AuthActivity, ScheduleActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Ошибка входа: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerUser() {
        val email = emailEditText.text.toString()
        val password = passwordEditText.text.toString()
        val role = roleEditText.text.toString()

        if (email.isNotEmpty() && password.isNotEmpty() && role.isNotEmpty()) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        val userMap = mapOf(
                            "email" to email,
                            "role" to role
                        )

                        // Сохраняем в Firebase Realtime Database
                        val dbRef = FirebaseDatabase.getInstance().getReference("Users")
                        userId?.let {
                            dbRef.child(it).setValue(userMap)
                        }

                        Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                        // Можно отправить на экран расписания
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            val userMap = mapOf(
                                "email" to email,
                                "role" to role
                            )
                            val dbRef = FirebaseDatabase.getInstance().getReference("Users")
                            userId?.let {
                                dbRef.child(it).setValue(userMap)
                            }

                            Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@AuthActivity, ScheduleActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        Toast.makeText(this, "Ошибка регистрации: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetPassword() {
        val email = emailEditText.text.toString()

        if (email.isNotEmpty()) {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Письмо отправлено!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Ошибка: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Уведомления разрешены", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Уведомления отключены", Toast.LENGTH_SHORT).show()
            }
        }
    }
}