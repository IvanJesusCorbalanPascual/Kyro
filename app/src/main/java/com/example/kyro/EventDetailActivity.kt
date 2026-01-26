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

// Esta clase muestra los detalles de un evento (tarea o examen)
class EventDetailActivity : AppCompatActivity() {

    // Funcion que se ejecuta al crear la actividad
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        // --- Configuracion de la Toolbar ---
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
        val asignaturaNombre = intent.getStringExtra("asignatura_nombre")
        val date = intent.getStringExtra("date")
        val time = if (type == "examen") intent.getStringExtra("hora_examen") else intent.getStringExtra("hora_entrega")
        val description = intent.getStringExtra("description")
        val notif1 = intent.getStringExtra("notif1")
        val notif2 = intent.getStringExtra("notif2")
        val completada = intent.getBooleanExtra("completada", false)

        // --- Rellenar la vista con los datos ---
        findViewById<TextView>(R.id.tvDetailTitle).text = title
        findViewById<TextView>(R.id.tvDetailAsignatura).text = asignaturaNombre
        findViewById<TextView>(R.id.tvDetailDate).text = if (time != null) "$date a las $time" else date
        findViewById<TextView>(R.id.tvDetailDescription).text = description

        val noEstablecido = getString(R.string.event_detail_no_establecido)
        findViewById<TextView>(R.id.tvDetailNotif1).text = getString(R.string.event_detail_recordatorio_1, notif1 ?: noEstablecido)
        findViewById<TextView>(R.id.tvDetailNotif2).text = getString(R.string.event_detail_recordatorio_2, notif2 ?: noEstablecido)

        // --- Logica del boton de Editar ---
        val btnEdit: Button = findViewById(R.id.btnEditEvent)
        btnEdit.setOnClickListener {
            val editIntent = Intent(this, AddItemActivity::class.java)

            editIntent.apply {
                putExtra("id", id)
                putExtra("type", type)
                putExtra("title", title)
                putExtra("description", description)
                putExtra("date", date)
                putExtra("asignatura_nombre", asignaturaNombre)
                if (type == "examen") {
                    putExtra("hora_examen", time)
                } else {
                    putExtra("hora_entrega", time)
                }
                putExtra("notif1", notif1)
                putExtra("notif2", notif2)
                // Anade esta linea para redirigir a CalendarioActivity
                putExtra("NAVIGATE_TO_CALENDAR", true)
            }
            startActivity(editIntent)
            finish() // Cierra la pantalla de detalles
        }

        // Logica del boton Eliminar
        val btnDelete: Button = findViewById(R.id.btnDeleteEvent)
        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)

                .setTitle(getString(R.string.dialog_confirmar_eliminar_titulo))
                .setMessage(getString(R.string.dialog_confirmar_eliminar_msg))
                .setPositiveButton(getString(R.string.btn_eliminar)) { _, _ ->
                    deleteEvent(id, type ?: "")
                }
                .setNegativeButton(getString(R.string.btn_cancelar), null)
                .show()
        }

        // Logica del boton para marcar como incompleto
        val btnMarkAsIncomplete: Button = findViewById(R.id.btnMarkAsIncomplete)
        if (type == "tarea" && completada) {
            btnMarkAsIncomplete.visibility = View.VISIBLE
            btnMarkAsIncomplete.setOnClickListener {
                markAsIncomplete(id)
            }
        }

        // --- Configuracion de la Navegacion Inferior ---
        NavigationHelper.setupBottomNavigation(this, R.id.nav_calendar)
    }

    // Funcion para eliminar un evento
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

    // Funcion para marcar una tarea como incompleta
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
                // Manejar el error
            }
        }
    }
}