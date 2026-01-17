package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.Spinner
import android.widget.TimePicker
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

class AddTaskActivity : AppCompatActivity() {

    private var selectedDate: String = ""
    private var selectedTime: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        // --- Toolbar ---
        val toolbar: MaterialToolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // --- Vistas ---
        val etAsignatura: EditText = findViewById(R.id.etAsignaturaTarea)
        val etDescripcion: EditText = findViewById(R.id.etDescripcionTarea)
        val calendarView: CalendarView = findViewById(R.id.calendarView)
        val timePicker: TimePicker = findViewById(R.id.timePicker)
        val spinnerNotificacion1: Spinner = findViewById(R.id.spinnerNotificacion1)
        val spinnerNotificacion2: Spinner = findViewById(R.id.spinnerNotificacion2)
        val btnGuardar: Button = findViewById(R.id.btnGuardarTarea)

        // --- Spinner Adapter ---
        val notificationOptions = listOf("No notificar", "En el momento del evento", "5 minutos antes", "10 minutos antes", "30 minutos antes", "1 hora antes", "1 día antes")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, notificationOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNotificacion1.adapter = adapter
        spinnerNotificacion2.adapter = adapter

        val eventId = intent.getLongExtra("id", -1)
        val isEditMode = eventId != -1L
        val navigateToCalendar = intent.getBooleanExtra("NAVIGATE_TO_CALENDAR", false)

        // Initialize selectedDate and selectedTime with today's date and time
        val calendar = Calendar.getInstance()
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfTime = SimpleDateFormat("HH:mm", Locale.US)
        selectedDate = sdfDate.format(calendar.time)
        selectedTime = sdfTime.format(calendar.time)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            selectedDate = sdfDate.format(calendar.time)
        }

        timePicker.setOnTimeChangedListener { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            selectedTime = sdfTime.format(calendar.time)
        }

        if (isEditMode) {
            // Modo Edición
            toolbar.title = "Editar Tarea"
            val title = intent.getStringExtra("title")
            val description = intent.getStringExtra("description")
            val date = intent.getStringExtra("date")
            val time = intent.getStringExtra("hora_entrega")
            val notif1 = intent.getStringExtra("notif1")
            val notif2 = intent.getStringExtra("notif2")

            etAsignatura.setText(title)
            etDescripcion.setText(description)
            date?.let {
                try {
                    val dateCalendar = Calendar.getInstance()
                    sdfDate.parse(it)?.let { parsedDate ->
                        dateCalendar.time = parsedDate
                        calendarView.date = dateCalendar.timeInMillis
                        selectedDate = it
                    }
                } catch (e: Exception) {
                    // Handle date parsing error
                }
            }
            time?.let {
                try {
                    val timeCalendar = Calendar.getInstance()
                    sdfTime.parse(it)?.let { parsedTime ->
                        timeCalendar.time = parsedTime
                        timePicker.hour = timeCalendar.get(Calendar.HOUR_OF_DAY)
                        timePicker.minute = timeCalendar.get(Calendar.MINUTE)
                        selectedTime = it
                    }
                } catch (e: Exception) {
                    // Handle time parsing error
                }
            }

            if (notif1 != null) {
                val notif1Position = adapter.getPosition(notif1)
                if (notif1Position >= 0) {
                    spinnerNotificacion1.setSelection(notif1Position)
                }
            } else {
                spinnerNotificacion1.setSelection(0) // No notificar
            }

            if (notif2 != null) {
                val notif2Position = adapter.getPosition(notif2)
                if (notif2Position >= 0) {
                    spinnerNotificacion2.setSelection(notif2Position)
                }
            } else {
                spinnerNotificacion2.setSelection(0) // No notificar
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
                        SupabaseClient.client.postgrest["tareas"].update({
                            set("nombre_asignatura", asignatura)
                            set("descripcion", descripcion)
                            set("fecha_entrega", selectedDate)
                            set("hora_entrega", selectedTime)
                            set("notificacion1", notificacion1)
                            set("notificacion2", notificacion2)
                        }) { filter { eq("id", eventId) } }
                    } else {
                        // Insertar
                        val nuevaTarea = Tarea(
                            id_usuario = idUsuarioActual,
                            nombre_asignatura = asignatura,
                            descripcion = descripcion,
                            fecha_entrega = selectedDate,
                            hora_entrega = selectedTime,
                            notificacion1 = notificacion1,
                            notificacion2 = notificacion2
                        )
                        SupabaseClient.client.postgrest["tareas"].insert(nuevaTarea)
                    }

                    withContext(Dispatchers.Main) {
                        val message = if (isEditMode) "¡Tarea actualizada!" else "¡Tarea guardada!"
                        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()

                        if (navigateToCalendar && isEditMode) {
                            val intent = Intent(this@AddTaskActivity, CalendarioActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        finish() // Cierra AddTaskActivity
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
