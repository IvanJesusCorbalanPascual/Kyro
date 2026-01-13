package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class EventDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        // --- Configuración de la Toolbar ---
        val toolbar: MaterialToolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // --- Obtener datos del Intent ---
        val id = intent.getLongExtra("id", -1)
        val type = intent.getStringExtra("type")
        val title = intent.getStringExtra("title")
        val date = intent.getStringExtra("date")
        val description = intent.getStringExtra("description")
        val notif1 = intent.getStringExtra("notif1")
        val notif2 = intent.getStringExtra("notif2")

        // --- Rellenar la vista con los datos ---
        findViewById<TextView>(R.id.tvDetailTitle).text = title
        findViewById<TextView>(R.id.tvDetailDate).text = date
        findViewById<TextView>(R.id.tvDetailDescription).text = description
        findViewById<TextView>(R.id.tvDetailNotif1).text = "Recordatorio 1: ${notif1 ?: "No establecido"}"
        findViewById<TextView>(R.id.tvDetailNotif2).text = "Recordatorio 2: ${notif2 ?: "No establecido"}"

        // --- Lógica del botón de Editar ---
        val btnEdit: Button = findViewById(R.id.btnEditEvent)
        btnEdit.setOnClickListener {
            val editIntent = if (type == "tarea") {
                Intent(this, AddTaskActivity::class.java)
            } else {
                Intent(this, AddExamActivity::class.java)
            }

            editIntent.apply {
                putExtra("id", id)
                putExtra("type", type)
                putExtra("title", title)
                putExtra("description", description)
                putExtra("date", date)
                putExtra("notif1", notif1)
                putExtra("notif2", notif2)
                // Añade esta línea para redirigir a CalendarioActivity
                putExtra("NAVIGATE_TO_CALENDAR", true)
            }
            startActivity(editIntent)
            finish() // Cierra la pantalla de detalles
        }

        // --- Configuración de la Navegación Inferior ---
        NavigationHelper.setupBottomNavigation(this, R.id.nav_calendar)
    }
}
