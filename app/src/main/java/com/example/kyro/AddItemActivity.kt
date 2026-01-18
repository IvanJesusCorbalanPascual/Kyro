package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.view.View
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

// Esta clase gestiona la logica para anadir o editar un nuevo elemento sea Tarea o Examen
class AddItemActivity : AppCompatActivity() {

    // Variables para almacenar la fecha y hora seleccionadas
    private var selectedDate: String = ""
    private var selectedTime: String = ""

    // Funcion que se ejecuta al crear la actividad
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)

        // Obtiene el tipo de elemento a anadir Tarea o Examen
        val type = intent.getStringExtra("type") ?: "Tarea"

        // --- Toolbar ---
        // Configura la barra de herramientas superior
        val toolbar: MaterialToolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.title = if (type.equals("Examen", ignoreCase = true)) "Anadir Examen" else "Anadir Tarea"
        // Configura el boton para volver atras
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // --- Vistas ---
        // Inicializa las vistas de la interfaz de usuario
        val etAsignatura: EditText = findViewById(R.id.etAsignatura)
        val etDescripcion: EditText = findViewById(R.id.etDescripcion)
        val calendarView: CalendarView = findViewById(R.id.calendarView)
        val timePicker: TimePicker = findViewById(R.id.timePicker)
        val spinnerNotificacion1: Spinner = findViewById(R.id.spinnerNotificacion1)
        val spinnerNotificacion2: Spinner = findViewById(R.id.spinnerNotificacion2)
        val btnGuardar: Button = findViewById(R.id.btnGuardar)

        // --- Spinner Adapter ---
        // Configura las opciones para los spinners de notificacion
        val notificationOptions = listOf("No notificar", "En el momento del evento", "5 minutos antes", "10 minutos antes", "30 minutos antes", "1 hora antes", "1 dia antes")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, notificationOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNotificacion1.adapter = adapter
        spinnerNotificacion2.adapter = adapter

        // Obtiene el ID del evento si se esta editando
        val eventId = intent.getLongExtra("id", -1)
        val isEditMode = eventId != -1L
        val navigateToCalendar = intent.getBooleanExtra("NAVIGATE_TO_CALENDAR", false)

        // Inicializa la fecha y hora seleccionadas con la fecha y hora actuales
        val calendar = Calendar.getInstance()
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.US)
        selectedDate = sdfDate.format(calendar.time)
        selectedTime = sdfTime.format(calendar.time)

        // Listener para cuando cambia la fecha en el CalendarView
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            selectedDate = sdfDate.format(calendar.time)
        }

        // Listener para cuando cambia la hora en el TimePicker
        timePicker.setOnTimeChangedListener { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            selectedTime = sdfTime.format(calendar.time)
        }

        // Si esta en modo edicion rellena los campos con los datos del evento
        if (isEditMode) {
            // Modo Edicion
            toolbar.title = if (type.equals("Examen", ignoreCase = true)) "Editar Examen" else "Editar Tarea"
            val title = intent.getStringExtra("title")
            val description = intent.getStringExtra("description")
            val date = intent.getStringExtra("date")
            val time = if (type.equals("Examen", ignoreCase = true)) intent.getStringExtra("hora_examen") else intent.getStringExtra("hora_entrega")
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
                    // Maneja el error de parseo de fecha
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
                    // Maneja el error de parseo de hora
                }
            }

            // Establece la seleccion de los spinners de notificacion
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

        // Listener para el boton de guardar
        btnGuardar.setOnClickListener {
            val asignatura = etAsignatura.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()
            val notificacion1 = spinnerNotificacion1.selectedItem.toString()
            val notificacion2 = spinnerNotificacion2.selectedItem.toString()

            // Comprueba que los campos no esten vacios
            if (asignatura.isEmpty() || descripcion.isEmpty() || selectedDate.isEmpty()) {
                Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Lanza una corrutina para realizar la operacion en la base de datos
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val idUsuarioActual = SupabaseClient.client.auth.currentUserOrNull()?.id
                    if (idUsuarioActual == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(applicationContext, "Error: Sesion no valida. Por favor, inicie sesion de nuevo.", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }

                    if (isEditMode) {
                        // Actualiza el evento usando el método de actualización directa
                        val tableName = if (type.equals("Examen", ignoreCase = true)) "examenes" else "tareas"
                        SupabaseClient.client.postgrest[tableName].update({
                            set("nombre_asignatura", asignatura)
                            set("descripcion", descripcion)
                            if (type.equals("Examen", ignoreCase = true)) {
                                set("fecha_examen", selectedDate)
                                set("hora_examen", selectedTime)
                            } else {
                                set("fecha_entrega", selectedDate)
                                set("hora_entrega", selectedTime)
                            }
                            set("notificacion1", notificacion1)
                            set("notificacion2", notificacion2)
                        }) { filter {
                            eq("id", eventId)
                            eq("id_usuario", idUsuarioActual)
                        } }
                    } else {
                        // La logica para insertar un nuevo elemento no cambia
                        val tableName = if (type.equals("Examen", ignoreCase = true)) "examenes" else "tareas"
                        if (type.equals("Examen", ignoreCase = true)) {
                            val nuevoExamen = Examen(
                                id_usuario = idUsuarioActual,
                                nombre_asignatura = asignatura,
                                descripcion = descripcion,
                                fecha_examen = selectedDate,
                                hora_examen = selectedTime,
                                notificacion1 = notificacion1,
                                notificacion2 = notificacion2
                            )
                            SupabaseClient.client.postgrest[tableName].insert(nuevoExamen)
                        } else {
                            val nuevaTarea = Tarea(
                                id_usuario = idUsuarioActual,
                                nombre_asignatura = asignatura,
                                descripcion = descripcion,
                                fecha_entrega = selectedDate,
                                hora_entrega = selectedTime,
                                notificacion1 = notificacion1,
                                notificacion2 = notificacion2
                            )
                            SupabaseClient.client.postgrest[tableName].insert(nuevaTarea)
                        }
                    }

                    // Muestra un mensaje de exito y cierra la actividad
                    withContext(Dispatchers.Main) {
                        val message = if (isEditMode) "$type actualizado" else "$type guardado"
                        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()

                        if (navigateToCalendar && isEditMode) {
                            val intent = Intent(this@AddItemActivity, CalendarioActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        finish() // Cierra AddItemActivity
                    }
                } catch (e: Exception) {
                    // Muestra un mensaje de error
                    withContext(Dispatchers.Main) {
                        val errorMessage = when (e) {
                            is NullPointerException -> "Error: Sesion no valida. Por favor, inicie sesion de nuevo."
                            else -> e.message ?: "Ocurrio un error desconocido"
                        }
                        Toast.makeText(applicationContext, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
