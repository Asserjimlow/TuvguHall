package com.example.tuvguhall

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class JournalActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var dbRef: DatabaseReference
    private val journalItems = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journal)

        listView = findViewById(R.id.journalListView)
        dbRef = FirebaseDatabase.getInstance().getReference("Schedule")

        loadJournal()
        val exportButton = findViewById<Button>(R.id.buttonExport)

        exportButton.setOnClickListener {
            exportJournalToCSV()
        }
    }

    private fun loadJournal() {
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                journalItems.clear()

                for (roomSnapshot in snapshot.children) {
                    val room = roomSnapshot.key ?: continue

                    for (dateSnapshot in roomSnapshot.children) {
                        val date = dateSnapshot.key ?: continue

                        for (slotSnapshot in dateSnapshot.children) {
                            val time = slotSnapshot.key ?: continue
                            val email = slotSnapshot.child("email").value?.toString() ?: "Без email"
                            val role = slotSnapshot.child("role").value?.toString() ?: "Без роли"

                            val entry = "📅 $date — $room — $time — $email ($role)"
                            journalItems.add(entry)
                        }
                    }
                }

                journalItems.sort() // сортировка по дате
                listView.adapter = ArrayAdapter(this@JournalActivity, android.R.layout.simple_list_item_1, journalItems)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@JournalActivity, "Ошибка загрузки журнала", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun exportJournalToCSV() {
        if (journalItems.isEmpty()) {
            Toast.makeText(this, "Журнал пуст, нечего экспортировать", Toast.LENGTH_SHORT).show()
            return
        }

        val csvHeader = "Дата,Аудитория,Время,Email,Роль\n"
        val csvBuilder = StringBuilder(csvHeader)

        for (entry in journalItems) {
            // Пример строки: 📅 2025-03-25 — 125 — 08:30-10:00 — ivanov@mail.ru (Преподаватель)
            val cleanEntry = entry.replace("📅 ", "")
            val parts = cleanEntry.split(" — ")

            if (parts.size >= 4) {
                val date = parts[0]
                val room = parts[1]
                val time = parts[2]
                val email = parts[3].substringBefore(" (")
                val role = parts[3].substringAfter("(", "Нет").removeSuffix(")")

                csvBuilder.append("$date,$room,$time,$email,$role\n")
            }
        }

        try {
            val fileName = "journal_export.csv"
            val downloads = getExternalFilesDir(null) // internal-access to Downloads-like dir
            val file = java.io.File(downloads, fileName)
            file.writeText(csvBuilder.toString())

            Toast.makeText(this, "Файл сохранён: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка экспорта: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}