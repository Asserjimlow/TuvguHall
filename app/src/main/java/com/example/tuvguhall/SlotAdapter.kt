package com.example.tuvguhall

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class SlotAdapter(context: Context, private val slots: List<String>) :
    ArrayAdapter<String>(context, 0, slots) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val slot = slots[position]
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_slot, parent, false)
        val textView = view.findViewById<TextView>(R.id.textSlot)

        textView.text = slot

        // Цветовая подсветка
        when {
            slot.contains("СВОБОДНО") -> {
                view.setBackgroundColor(0xFF4CAF50.toInt()) // зелёный
            }
            slot.contains("МОЁ БРОНИРОВАНИЕ") -> {
                view.setBackgroundColor(0xFF2196F3.toInt()) // синий
            }
            slot.contains("ЗАНЯТО") -> {
                view.setBackgroundColor(0xFFF44336.toInt()) // красный
            }
        }

        return view
    }
}