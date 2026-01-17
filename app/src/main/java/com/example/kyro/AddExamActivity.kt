package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExamActivity : AppCompatActivity() {

    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_exam)

        // --- Toolbar ---
        val toolbar: MaterialToolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // --- Vistas ---
        val etAsignatura: EditText = findViewById(R.id.etAsignaturaExamen)
        val etDescripcion: EditText = findViewById(R.id.etDescripcionExamen)
        val calendarView: CalendarView = findViewById(R.id.calendarView)
        val spinnerNotificacion1: Spinner = findViewById(R.id.spinnerNotificacion1)
        val spinnerNotificacion2: Spinner = findViewById(R.id.spinnerNotificacion2)
        val btnGuardar: Button = findViewById(R.id.btnGuardarExamen)

        // --- Spinner Adapter ---
        val notificationOptions = listOf("No notificar", "En el momento del evento", "5 minutos antes", "10 minutos antes", "30 minutos antes", "1 hora antes", "1 día antes")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, notificationOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNotificacion1.adapter = adapter
        spinnerNotificacion2.adapter = adapter

        val eventId = intent.getLongExtra("id", -1)
        val isEditMode = eventId != -1L
        val navigateToCalendar = intent.getBooleanExtra("NAVIGATE_TO_CALENDAR", false)

        // Initialize selectedDate with today's date
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        selectedDate = sdf.format(calendar.time)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            selectedDate = sdf.format(calendar.time)
        }

        if (isEditMode) {
            // Modo Edición
            toolbar.title = "Editar Examen"
            val title = intent.getStringExtra("title")
            val description = intent.getStringExtra("description")
            val date = intent.getStringExtra("date")
            val notif1 = intent.getStringExtra("notif1")
            val notif2 = intent.getStringExtra("notif2")

            etAsignatura.setText(title)
            etDescripcion.setText(description)
            date?.let {
                try {
                    val dateCalendar = Calendar.getInstance()
                    sdf.parse(it)?.let { parsedDate ->
                        dateCalendar.time = parsedDate
                        calendarView.date = dateCalendar.timeInMillis
                        selectedDate = it
                    }
                } catch (e: Exception) {
                    // Handle date parsing error
                }
            }

            val notif1Position = adapter.getPosition(notif1)
            if (notif1Position >= 0) {
                spinnerNotificacion1.setSelection(notif1Position)
            }

            val notif2Position = adapter.getPosition(notif2)
            if (notif2Position >= 0) {
                spinnerNotificacion2.setSelection(notif2Position)
            }
        }

        btnGuardar.setOnClickListener {
            val asignatura = etAsignatura.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()
            val notificacion1 = spinnerNotificacion1.selectedItem.toString()
            val notificacion2 = spinnerNotificacion2.selectedItem.toString()

            if (asignatura.isEmpty() || descripcion.isEmpty() || selectedDate.isEmpty()) {
                Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val idUsuarioActual = SupabaseClient.client.auth.currentUserOrNull()?.id
            if (idUsuarioActual == null) {
                Toast.makeText(this, "Error: sesión no válida.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    if (isEditMode) {
                        // Actualizar
                        SupabaseClient.client.postgrest["examenes"].update({
                            set("nombre_asignatura", asignatura)
                            set("descripcion", descripcion)
                            set("fecha_examen", selectedDate)
                            set("notificacion1", notificacion1)
                            set("notificacion2", notificacion2)
                        }) { filter { eq("id", eventId) } }
                    } else {
                        // Insertar
                        val nuevoExamen = Examen(
                            id_usuario = idUsuarioActual,
                            nombre_asignatura = asignatura,
                            descripcion = descripcion,
                            fecha_examen = selectedDate,
                            notificacion1 = notificacion1,
                            notificacion2 = notificacion2
                        )
                        SupabaseClient.client.postgrest["examenes"].insert(nuevoExamen)
                    }

                    withContext(Dispatchers.Main) {
                        val message = if (isEditMode) "¡Examen actualizado!" else "¡Examen guardado!"
                        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()

                        if (navigateToCalendar && isEditMode) {
                            val intent = Intent(this@AddExamActivity, CalendarioActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        finish() // Cierra AddExamActivity
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
