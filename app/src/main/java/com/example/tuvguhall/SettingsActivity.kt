package com.example.tuvguhall

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import androidx.appcompat.app.AppCompatDelegate

class SettingsActivity : AppCompatActivity() {

    private lateinit var textUserEmail: TextView
    private lateinit var textUserRole: TextView
    private lateinit var logoutButton: Button
    private lateinit var roleSpinner: Spinner
    private lateinit var saveRoleButton: Button
    private lateinit var darkModeSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        textUserEmail = findViewById(R.id.textUserEmail)
        textUserRole = findViewById(R.id.textUserRole)
        logoutButton = findViewById(R.id.buttonLogoutSettings)
        roleSpinner = findViewById(R.id.roleSpinner)
        saveRoleButton = findViewById(R.id.saveRoleButton)
        roleSpinner = findViewById(R.id.roleSpinner)
        saveRoleButton = findViewById(R.id.saveRoleButton)
        darkModeSwitch = findViewById(R.id.darkModeSwitch)

        val roles = resources.getStringArray(R.array.roles_array)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roleSpinner.adapter = adapter

        // Загружаем настройку из SharedPreferences
        val sharedPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("dark_mode", false)
        darkModeSwitch.isChecked = isDarkMode

// Применяем тему при запуске (если вдруг переключение было раньше)
        applyTheme(isDarkMode)

// Обработка переключателя
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPrefs.edit()
            editor.putBoolean("dark_mode", isChecked)
            editor.apply()

            applyTheme(isChecked)
        }
        // Установим текущую роль в спиннер (если пользователь авторизован)
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid
            FirebaseDatabase.getInstance().getReference("Users").child(uid).child("role")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentRole = snapshot.value?.toString()
                        val index = roles.indexOf(currentRole)
                        if (index >= 0) {
                            roleSpinner.setSelection(index)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

// Обработка кнопки "Сохранить"
        saveRoleButton.setOnClickListener {
            val newRole = roleSpinner.selectedItem.toString()
            if (user != null) {
                val uid = user.uid
                FirebaseDatabase.getInstance().getReference("Users").child(uid).child("role")
                    .setValue(newRole)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Роль обновлена на: $newRole", Toast.LENGTH_SHORT).show()
                        textUserRole.text = "Роль: $newRole"
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Ошибка обновления роли", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        val aboutButton = Button(this).apply {
            text = "О приложении"
            setOnClickListener {
                startActivity(Intent(this@SettingsActivity, AboutActivity::class.java))
            }
        }
        val layout = findViewById<LinearLayout>(R.id.layoutRoot) // если используешь корневой layout
        layout.addView(aboutButton)
        if (user != null) {
            textUserEmail.text = "Email: ${user.email}"

            val uid = user.uid
            val ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("role")
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val role = snapshot.value?.toString() ?: "Не указана"
                    textUserRole.text = "Роль: $role"
                }

                override fun onCancelled(error: DatabaseError) {
                    textUserRole.text = "Ошибка загрузки роли"
                }
            })
        } else {
            textUserEmail.text = "Вы гость"
            textUserRole.text = "Роль: -"
        }

        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            setResult(RESULT_OK)
            finish()
        }
    }
    override fun onBackPressed() {
        setResult(RESULT_OK)
        super.onBackPressed()
    }
    private fun applyTheme(isDarkMode: Boolean) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}