package com.example.tuvguhall

import com.google.firebase.messaging.FirebaseMessaging
import android.content.Intent
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AlertDialog

class ScheduleActivity : AppCompatActivity() {

    private lateinit var spinnerRoom: Spinner
    private lateinit var buttonPickDate: Button
    private lateinit var textSelectedDate: TextView
    private lateinit var listViewSlots: ListView
    private lateinit var textUserInfo: TextView
    private lateinit var buttonLogout: Button
    private lateinit var dbRef: DatabaseReference
    private var selectedDate: String = ""
    private var selectedRoom: String = "125"
    private var isGuestMode = false

    private val timeSlots = listOf(
        "08:30-10:00", "10:10-11:40", "13:00-14:30",
        "14:40-16:10", "16:20-17:30"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        spinnerRoom = findViewById(R.id.spinnerRoom)
        buttonPickDate = findViewById(R.id.buttonPickDate)
        textSelectedDate = findViewById(R.id.textSelectedDate)
        listViewSlots = findViewById(R.id.listViewSlots)
        textUserInfo = findViewById(R.id.textUserInfo)
        buttonLogout = findViewById(R.id.buttonLogout)

        dbRef = FirebaseDatabase.getInstance().getReference("Schedule")

        val buttonJournal = findViewById<Button>(R.id.buttonJournal)

        buttonJournal.setOnClickListener {
            val intent = Intent(this, JournalActivity::class.java)
            startActivityForResult(intent, 1001)
        }
        val rootLayout = findViewById<LinearLayout>(R.id.layoutRoot)

        val settingsButton = Button(this).apply {
            text = "Настройки"
            setOnClickListener {
                val intent = Intent(this@ScheduleActivity, SettingsActivity::class.java)
                startActivityForResult(intent, 1001)
            }
        }

        rootLayout.addView(settingsButton)

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM", "FCM Token: $token")
                Toast.makeText(this, "Токен получен в логах", Toast.LENGTH_SHORT).show()
            }
        }
        // загрузка данных пользователя
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid
            val email = user.email ?: "Без email"

            FirebaseDatabase.getInstance().getReference("Users").child(uid).child("role")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val role = snapshot.value.toString()
                        textUserInfo.text = "Вы вошли как: $role ($email)"
                    }

                    override fun onCancelled(error: DatabaseError) {
                        textUserInfo.text = "Не удалось загрузить данные пользователя"
                    }
                })
        } else {
            // Гость
            isGuestMode = true
            textUserInfo.text = "Вы не авторизованы (только просмотр)"
            buttonLogout.text = "Войти"
        }

        if (user != null) {
            val uid = user.uid
            val email = user.email ?: "Без email"

            FirebaseDatabase.getInstance().getReference("Users").child(uid).child("role")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val role = snapshot.value.toString()
                        textUserInfo.text = "Вы вошли как: $role ($email)"
                    }

                    override fun onCancelled(error: DatabaseError) {
                        textUserInfo.text = "Не удалось загрузить данные пользователя"
                    }
                })
        }

        // обработка выхода
        buttonLogout.setOnClickListener {
            if (isGuestMode) {
                // Гость → переход к AuthActivity
                val intent = Intent(this, AuthActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(this, "Выход выполнен", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, AuthActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        // Обработка выбора аудитории
        spinnerRoom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedRoom = when (parent?.getItemAtPosition(position).toString()) {
                    "Аудитория 125" -> "125"
                    "Актовый зал" -> "hall"
                    else -> "125" // значение по умолчанию
                }
                if (selectedDate.isNotEmpty()) {
                    loadSchedule()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Обработка выбора даты
        buttonPickDate.setOnClickListener {
            showDatePicker()
        }

        listViewSlots.setOnItemClickListener { _, _, position, _ ->
            if (isGuestMode) {
                Toast.makeText(this, "Войдите, чтобы забронировать слот", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }
            val slotText = listViewSlots.getItemAtPosition(position).toString()

            if (slotText.contains("ПРОШЛО")) {
                Toast.makeText(this, "Этот слот уже в прошлом", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }

            if (selectedDate.isNotEmpty()) {
                val slot = slotText.substringBefore(" -") // извлекаем только время
                bookSlotIfAvailable(slot)
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        // Если уже была выбрана дата — установить её как стартовую
        if (selectedDate.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(selectedDate)
                date?.let {
                    calendar.time = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, day ->
                val picked = Calendar.getInstance()
                picked.set(year, month, day)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                selectedDate = sdf.format(picked.time)
                textSelectedDate.text = "Выбранная дата: $selectedDate"
                loadSchedule()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.datePicker.minDate = Calendar.getInstance().timeInMillis
        datePicker.show()
    }

    private fun loadSchedule() {
        val path = "$selectedRoom/$selectedDate"
        dbRef.child(path).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val now = Calendar.getInstance()
                val isToday = selectedDate == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
                val slotStatusList = timeSlots.map { slot ->

                    // Проверка на прошедшее время
                    val slotStartTime = slot.substringBefore("-")
                    val slotDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .parse("$selectedDate $slotStartTime")

                    if (isToday && slotDateTime != null && slotDateTime.before(now.time)) {
                        return@map "$slot - ⛔ ПРОШЛО"
                    }

                    // Проверка на бронь
                    val booking = snapshot.child(slot)
                    if (booking.exists()) {
                        val bookedById = booking.child("bookedBy").value.toString()
                        val bookedByEmail = booking.child("email").value.toString()

                        if (bookedById == currentUserId) {
                            "$slot - 🟦 МОЁ БРОНИРОВАНИЕ ($bookedByEmail)"
                        } else {
                            "$slot - ЗАНЯТО ($bookedByEmail)"
                        }
                    } else {
                        "$slot - СВОБОДНО"
                    }
                }
                val adapter = SlotAdapter(this@ScheduleActivity, slotStatusList)
                listViewSlots.adapter = adapter
                listViewSlots.setOnItemLongClickListener { _, _, position, _ ->
                    val slotStatus = (listViewSlots.adapter.getItem(position) as String)

                    if (slotStatus.contains("МОЁ БРОНИРОВАНИЕ")) {
                        AlertDialog.Builder(this@ScheduleActivity)
                            .setTitle("Отменить бронь?")
                            .setMessage("Вы уверены, что хотите отменить бронирование на ${timeSlots[position]}?")
                            .setPositiveButton("Да") { _, _ ->
                                val user = FirebaseAuth.getInstance().currentUser
                                if (user != null) {
                                    val bookingRef = FirebaseDatabase.getInstance().reference
                                        .child("Schedule")
                                        .child(selectedRoom)
                                        .child(selectedDate)
                                        .child(timeSlots[position])

                                    bookingRef.removeValue().addOnSuccessListener {
                                        Toast.makeText(this@ScheduleActivity, "Бронь отменена", Toast.LENGTH_SHORT).show()
                                        loadSchedule()
                                    }.addOnFailureListener {
                                        Toast.makeText(this@ScheduleActivity, "Ошибка при отмене", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .setNegativeButton("Отмена", null)
                            .show()
                        true
                    } else {
                        false
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ScheduleActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun bookSlotIfAvailable(slot: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Авторизуйтесь для брони", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = user.uid
        val email = user.email ?: "Нет email"

        // Получаем роль пользователя
        FirebaseDatabase.getInstance().getReference("Users").child(uid).child("role")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val role = snapshot.value.toString()

                    val bookingRef = dbRef.child("$selectedRoom/$selectedDate/$slot")
                    bookingRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                Toast.makeText(this@ScheduleActivity, "Слот уже занят", Toast.LENGTH_SHORT).show()
                            } else {
                                val booking = mapOf(
                                    "bookedBy" to uid,
                                    "email" to email,
                                    "role" to role
                                )
                                bookingRef.setValue(booking)
                                Toast.makeText(this@ScheduleActivity, "Бронь успешна", Toast.LENGTH_SHORT).show()
                                loadSchedule()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == RESULT_OK) {
            recreate() // перезапускаем активити, обновляется роль и всё остальное
        }
    }
}