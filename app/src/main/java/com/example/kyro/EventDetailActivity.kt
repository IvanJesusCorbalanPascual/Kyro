package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

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
        val time = intent.getStringExtra("hora_entrega")
        val description = intent.getStringExtra("description")
        val notif1 = intent.getStringExtra("notif1")
        val notif2 = intent.getStringExtra("notif2")
        val completada = intent.getBooleanExtra("completada", false)

        // --- Rellenar la vista con los datos ---
        findViewById<TextView>(R.id.tvDetailTitle).text = title
        findViewById<TextView>(R.id.tvDetailDate).text = if (time != null) "$date a las $time" else date
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
                putExtra("hora_entrega", time)
                putExtra("notif1", notif1)
                putExtra("notif2", notif2)
                // Añade esta línea para redirigir a CalendarioActivity
                putExtra("NAVIGATE_TO_CALENDAR", true)
            }
            startActivity(editIntent)
            finish() // Cierra la pantalla de detalles
        }

        // Lógica del botón Eliminar
        val btnDelete: Button = findViewById(R.id.btnDeleteEvent)
        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Confirmar Eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar este evento?")
                .setPositiveButton("Eliminar") { _, _ ->
                    deleteEvent(id, type ?: "")
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        val btnMarkAsIncomplete: Button = findViewById(R.id.btnMarkAsIncomplete)
        if (type == "tarea" && completada) {
            btnMarkAsIncomplete.visibility = View.VISIBLE
            btnMarkAsIncomplete.setOnClickListener {
                markAsIncomplete(id)
            }
        }

        // --- Configuración de la Navegación Inferior ---
        NavigationHelper.setupBottomNavigation(this, R.id.nav_calendar)
    }

    private fun deleteEvent(id: Long, type: String) {
        lifecycleScope.launch {
            try {
                val tableName = if (type == "tarea") "tareas" else "examenes"
                SupabaseClient.client.postgrest[tableName].delete {
                    filter {
                        eq("id", id)
                    }
                }
                // Evento eliminado, volver a CalendarioActivity
                val intent = Intent(this@EventDetailActivity, CalendarioActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                // Manejar el error
            }
        }
    }

    private fun markAsIncomplete(id: Long) {
        lifecycleScope.launch {
            try {
                SupabaseClient.client.postgrest["tareas"].update(
                    { set("completada", false) },
                    { filter { eq("id", id) } }
                )
                // Vuelve a CalendarioActivity
                val intent = Intent(this@EventDetailActivity, CalendarioActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
