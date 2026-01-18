package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.widget.CalendarView

class AddItemActivity : AppCompatActivity() {

    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private lateinit var calendar: Calendar

    private lateinit var datePicker: DatePicker
    private lateinit var calendarView: CalendarView
    private lateinit var cvCalendarContainer: CardView

    private lateinit var spinnerAsignaturas: Spinner
    private val asignaturasList = mutableListOf<Asignatura>()
    private lateinit var asignaturasAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)

        calendar = Calendar.getInstance()

        val type = intent.getStringExtra("type") ?: "Tarea"

        val toolbar: MaterialToolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.title = if (type.equals("Examen", ignoreCase = true)) "Añadir Examen" else "Añadir Tarea"
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        spinnerAsignaturas = findViewById(R.id.spinnerAsignaturas)
        val etNombre: EditText = findViewById(R.id.etNombre)
        val etDescripcion: EditText = findViewById(R.id.etDescripcion)
        datePicker = findViewById(R.id.datePicker)
        calendarView = findViewById(R.id.calendarView)
        cvCalendarContainer = findViewById(R.id.cvCalendarContainer)
        val timePicker: TimePicker = findViewById(R.id.timePicker)
        val spinnerNotificacion1: Spinner = findViewById(R.id.spinnerNotificacion1)
        val spinnerNotificacion2: Spinner = findViewById(R.id.spinnerNotificacion2)
        val btnGuardar: Button = findViewById(R.id.btnGuardar)
        val btnToggleCalendar: ImageButton = findViewById(R.id.btnToggleCalendar)

        setupAsignaturasSpinner()
        loadAsignaturas()

        val notificationOptions = listOf("No notificar", "En el momento del evento", "5 minutos antes", "10 minutos antes", "30 minutos antes", "1 hora antes", "1 día antes")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, notificationOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNotificacion1.adapter = adapter
        spinnerNotificacion2.adapter = adapter

        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfTime = SimpleDateFormat("HH:mm:00", Locale.US)

        val selectedDateStr = intent.getStringExtra("selectedDate")
        if (selectedDateStr != null) {
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDateStr)
                if (date != null) {
                    calendar.time = date
                }
            } catch (e: Exception) {
                // Handle error
            }
        }

        selectedDate = sdfDate.format(calendar.time)
        selectedTime = sdfTime.format(calendar.time)

        datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)) { _, year, monthOfYear, dayOfMonth ->
            calendar.set(year, monthOfYear, dayOfMonth)
            selectedDate = sdfDate.format(calendar.time)
            calendarView.date = calendar.timeInMillis
        }

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            selectedDate = sdfDate.format(calendar.time)
            datePicker.updateDate(year, month, dayOfMonth)
        }

        timePicker.setOnTimeChangedListener { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            selectedTime = sdfTime.format(calendar.time)
        }

        btnToggleCalendar.setOnClickListener {
            cvCalendarContainer.visibility = if (cvCalendarContainer.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        val eventId = intent.getLongExtra("id", -1)
        val isEditMode = eventId != -1L

        if (isEditMode) {
            toolbar.title = if (type.equals("Examen", ignoreCase = true)) "Editar Examen" else "Editar Tarea"
            val nombre = intent.getStringExtra("nombre")
            val descripcion = intent.getStringExtra("descripcion")
            val dateStr = intent.getStringExtra("date")
            val timeStr = if (type.equals("Examen", ignoreCase = true)) intent.getStringExtra("hora_examen") else intent.getStringExtra("hora_entrega")
            val notif1 = intent.getStringExtra("notif1")
            val notif2 = intent.getStringExtra("notif2")

            etNombre.setText(nombre)
            etDescripcion.setText(descripcion)

            dateStr?.let {
                try {
                    val date = sdfDate.parse(it)
                    if (date != null) {
                        val cal = Calendar.getInstance()
                        cal.time = date
                        datePicker.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                        calendarView.date = cal.timeInMillis
                    }
                } catch (e: Exception) { /* Handle parse exception */ }
            }

            timeStr?.let {
                try {
                    val parsedTime = SimpleDateFormat("HH:mm:ss", Locale.US).parse(it)
                    if(parsedTime != null) {
                        val timeCal = Calendar.getInstance().apply { time = parsedTime }
                        timePicker.hour = timeCal.get(Calendar.HOUR_OF_DAY)
                        timePicker.minute = timeCal.get(Calendar.MINUTE)
                    }
                } catch (e: Exception) { /* Handle parse exception */ }
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
            val selectedAsignaturaPosition = spinnerAsignaturas.selectedItemPosition
            if (selectedAsignaturaPosition == 0) { // Asumiendo que la posición 0 es "Elige una asignatura"
              showKyroToast("Por favor, selecciona una asignatura")
                return@setOnClickListener
            }
            val selectedAsignatura = asignaturasList[selectedAsignaturaPosition - 1] // Ajuste de índice
            val nombre = etNombre.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()
            val notificacion1 = spinnerNotificacion1.selectedItem.toString()
            val notificacion2 = spinnerNotificacion2.selectedItem.toString()

            if (nombre.isEmpty()) {
               showKyroToast("Por favor, rellena todos los campos")
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val idUsuarioActual = SupabaseClient.client.auth.currentUserOrNull()?.id
                    if (idUsuarioActual == null) {
                        withContext(Dispatchers.Main) {
                           showKyroToast("Error: Sesion no valida. Por favor, inicie sesion de nuevo.")
                        }
                        return@launch
                    }

                    if (isEditMode) {
                        val tableName = if (type.equals("Examen", ignoreCase = true)) "examenes" else "tareas"
                        SupabaseClient.client.postgrest[tableName].update({
                            set("asignatura_id", selectedAsignatura.id)
                            if (type.equals("Examen", ignoreCase = true)) {
                                set("nombre_examen", nombre)
                                set("descripcion", descripcion)
                                set("fecha_examen", selectedDate)
                                set("hora_examen", selectedTime)
                            } else {
                                set("nombre_tarea", nombre)
                                set("descripcion", descripcion)
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
                        val tableName = if (type.equals("Examen", ignoreCase = true)) "examenes" else "tareas"
                        if (type.equals("Examen", ignoreCase = true)) {
                            val nuevoExamen = Examen(
                                id_usuario = idUsuarioActual,
                                asignatura_id = selectedAsignatura.id,
                                nombre_examen = nombre,
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
                                asignatura_id = selectedAsignatura.id,
                                nombre_tarea = nombre,
                                descripcion = descripcion,
                                fecha_entrega = selectedDate,
                                hora_entrega = selectedTime,
                                notificacion1 = notificacion1,
                                notificacion2 = notificacion2
                            )
                            SupabaseClient.client.postgrest[tableName].insert(nuevaTarea)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        val message = if (isEditMode) "$type actualizado" else "$type guardado"
                       showKyroToast(message)
                        finish()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showKyroToast("Error: ${e.message}")
                    }
                }
            }
        }
    }

    private fun setupAsignaturasSpinner() {
        asignaturasAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf("Elige una asignatura"))
        asignaturasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAsignaturas.adapter = asignaturasAdapter
    }

    private fun loadAsignaturas() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val idUsuarioActual = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (idUsuarioActual != null) {
                    val asignaturas = SupabaseClient.client.from("asignaturas").select { filter { eq("user_id", idUsuarioActual) } }.decodeList<Asignatura>()
                    withContext(Dispatchers.Main) {
                        asignaturasList.clear()
                        asignaturasList.addAll(asignaturas)
                        val asignaturaNombres = mutableListOf("Elige una asignatura")
                        asignaturaNombres.addAll(asignaturas.map { it.titulo })
                        asignaturasAdapter.clear()
                        asignaturasAdapter.addAll(asignaturaNombres)
                        asignaturasAdapter.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                   showKyroToast("Error al cargar asignaturas: ${e.message}")
                }
            }
        }
    }
}