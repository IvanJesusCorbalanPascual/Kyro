package com.example.kyro.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kyro.entities.EjercicioIA
import com.example.kyro.adapters.EjerciciosAdapter
import com.example.kyro.entities.Examen
import com.example.kyro.NavigationHelper
import com.example.kyro.entities.PreguntaGenerada
import com.example.kyro.R
import com.example.kyro.SupabaseClient
import com.example.kyro.entities.Tarea
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Maneja la funcionalidad de la vista de asignatura seleccionada.
 * Muestra todos los elementos de la Asignatura: Exámenes, Tareas y Ejercicios.
 */
class AsignaturaSeleccionadaActivity : AppCompatActivity() {

    private var asignaturaId: Long = -1
    private lateinit var allEvents: List<Event>

    // Contenedores existentes
    private lateinit var tasksContainer: LinearLayout
    private lateinit var examsContainer: LinearLayout
    private lateinit var containerTareas: LinearLayout
    private lateinit var containerExamenes: LinearLayout

    // Para ejercicios
    private lateinit var containerEjercicios: LinearLayout
    private lateinit var rvEjercicios: RecyclerView
    private lateinit var adaptadorEjercicios: EjerciciosAdapter
    private lateinit var tvSinEjercicios: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asignatura_seleccionada)

        // Inicializar Vistas
        val tvTituloAsignatura = findViewById<TextView>(R.id.tvNombreAsignatura)
        val btnModificar = findViewById<TextView>(R.id.btnModificar)
        val btnGenerarMas = findViewById<MaterialButton>(R.id.btnGenerarMasEjercicios)

        tasksContainer = findViewById(R.id.tasksContainer)
        examsContainer = findViewById(R.id.examsContainer)
        containerTareas = findViewById(R.id.containerTareas)
        containerExamenes = findViewById(R.id.containerExamenes)

        // Vistas de Ejercicios
        containerEjercicios = findViewById(R.id.containerEjercicios)
        rvEjercicios = findViewById(R.id.rvEjercicios)
        tvSinEjercicios = findViewById(R.id.tvSinEjercicios)

        // Configurar RecyclerView de Ejercicios
        rvEjercicios.layoutManager = LinearLayoutManager(this)
        adaptadorEjercicios = EjerciciosAdapter(
            emptyList(),
            onClick = { ejercicio ->
                abrirQuiz(ejercicio)
            },
            onDelete = { ejercicio ->
                confirmarBorrado(ejercicio) // Llamamos a la función de confirmar
            }
        )
        rvEjercicios.adapter = adaptadorEjercicios

        // Recibir Datos del Intent
        val tituloRecibido = intent.getStringExtra("EXTRA_TITULO") ?: "Sin Título"
        val contenidoRecibido = intent.getStringExtra("EXTRA_CONTENIDO") ?: ""
        asignaturaId = intent.getLongExtra("EXTRA_ID", -1)

        tvTituloAsignatura.text = tituloRecibido

        // Botón generar ejercicios con IA
        btnGenerarMas.setOnClickListener {
            val intent = Intent(this, GenerarTestActivity::class.java)
            intent.putExtra("ASIGNATURA_ID", asignaturaId)
            intent.putExtra("CONTENIDO_BASE", contenidoRecibido)
            startActivity(intent)
        }

        // Botón Modificar
        btnModificar.setOnClickListener {
            val intent = Intent(this, ModificarAsignaturaActivity::class.java)
            intent.putExtra("EXTRA_TITULO", tituloRecibido)
            intent.putExtra("EXTRA_CONTENIDO", contenidoRecibido)
            intent.putExtra("EXTRA_ID", asignaturaId)
            startActivity(intent)
        }

        NavigationHelper.setupBottomNavigation(this, R.id.nav_asignatura)
    }

    override fun onResume() {
        super.onResume()
        loadTasksAndExams()
        cargarEjerciciosDeLaAsignatura()
    }

    // --- LÓGICA DE EJERCICIOS ---

    private fun cargarEjerciciosDeLaAsignatura() {
        if (asignaturaId == -1L) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Descargar ejercicios de Supabase
                val listaEjercicios = SupabaseClient.client
                    .from("ejercicios")
                    .select {
                        filter {
                            eq("asignatura_id", asignaturaId)
                        }
                    }
                    .decodeList<EjercicioIA>()

                withContext(Dispatchers.Main) {

                    // El contenedor siempre está visible para que el botón de GENERAR MAS EJERCICIOS se vea.
                    containerEjercicios.visibility = View.VISIBLE

                    if (listaEjercicios.isNotEmpty()) {
                        // Si hay ejercicios: mostramos lista, ocultamos mensaje "vacío"
                        rvEjercicios.visibility = View.VISIBLE
                        tvSinEjercicios.visibility = View.GONE
                        adaptadorEjercicios.actualizarLista(listaEjercicios)
                    } else {
                        // Si NO hay ejercicios: ocultamos lista, mostramos mensaje "vacío"
                        rvEjercicios.visibility = View.GONE
                        tvSinEjercicios.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun abrirQuiz(ejercicio: EjercicioIA) {
        try {
            val gson = Gson()
            val tipoLista = object : TypeToken<List<PreguntaGenerada>>() {}.type
            val preguntas: List<PreguntaGenerada> = gson.fromJson(ejercicio.preguntas_json, tipoLista)

            if (preguntas.isNotEmpty()) {
                val intent = Intent(this, QuizActivity::class.java)
                intent.putExtra("EXTRA_PREGUNTAS", ArrayList(preguntas))
                startActivity(intent)
            } else {
                showKyroToast(getString(R.string.error_ejercicio_vacio))
            }
        } catch (e: Exception) {
            showKyroToast(getString(R.string.error_abrir_test))
            e.printStackTrace()
        }
    }

    // --- LÓGICA DE TAREAS Y EXÁMENES ---

    private fun loadTasksAndExams() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch

                val tareas = SupabaseClient.client.from("tareas").select {
                    filter {
                        eq("id_usuario", userId)
                        eq("asignatura_id", asignaturaId)
                    }
                }.decodeList<Tarea>()

                val examenes = SupabaseClient.client.from("examenes").select {
                    filter {
                        eq("id_usuario", userId)
                        eq("asignatura_id", asignaturaId)
                    }
                }.decodeList<Examen>()

                val events = mutableListOf<Event>()

                // Conversión segura de IDs a Long para evitar errores de tipo
                tareas.forEach { event ->
                    event.id?.let { idVal ->
                        events.add(Event(idVal.toString().toLong(), "tarea", event.nombre_tarea, event.descripcion, event.fecha_entrega, event.hora_entrega, event.completada, event.notificacion1, event.notificacion2, ""))
                    }
                }
                examenes.forEach { event ->
                    event.id?.let { idVal ->
                        events.add(Event(idVal.toString().toLong(), "examen", event.nombre_examen, event.descripcion, event.fecha_examen, event.hora_examen, event.completada, event.notificacion1, event.notificacion2, ""))
                    }
                }

                allEvents = events

                withContext(Dispatchers.Main) {
                    displayEvents(allEvents)
                }
            } catch (e: Exception) {
                // Error silencioso
            }
        }
    }

    private fun confirmarBorrado(ejercicio: EjercicioIA) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_eliminar_titulo))
            .setMessage(getString(R.string.dialog_eliminar_mensaje, ejercicio.nombre))
            .setPositiveButton(getString(R.string.btn_eliminar)) { _, _ ->
                borrarEjercicioEnSupabase(ejercicio)
            }
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .show()
    }

    private fun borrarEjercicioEnSupabase(ejercicio: EjercicioIA) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Borrar de Supabase
                SupabaseClient.client.from("ejercicios").delete {
                    filter {
                        eq("id", ejercicio.id)
                    }
                }

                // Actualizar la lista en el hilo principal
                withContext(Dispatchers.Main) {
                    showKyroToast(getString(R.string.toast_test_eliminado))
                    cargarEjerciciosDeLaAsignatura() // Recargamos la lista
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showKyroToast(getString(R.string.error_eliminar_generico, e.message))
                }
            }
        }
    }

    private fun displayEvents(events: List<Event>) {
        tasksContainer.removeAllViews()
        examsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        val upcomingTasks = events.filter { it.type == "tarea" && !it.completada && !isExpired(it) }.sortedBy { it.date }
        val upcomingExams = events.filter { it.type == "examen" && !it.completada && !isPastExam(it) }.sortedBy { it.date }

        if (upcomingTasks.isEmpty()) {
            containerTareas.visibility = View.GONE
        } else {
            containerTareas.visibility = View.VISIBLE
            for (event in upcomingTasks) {
                val eventView = inflater.inflate(R.layout.list_item_event_with_date, tasksContainer, false)
                setupEventView(eventView, event)
                tasksContainer.addView(eventView)
            }
        }

        if (upcomingExams.isEmpty()) {
            containerExamenes.visibility = View.GONE
        } else {
            containerExamenes.visibility = View.VISIBLE
            for (event in upcomingExams) {
                val eventView = inflater.inflate(R.layout.list_item_event_with_date, examsContainer, false)
                setupEventView(eventView, event)
                examsContainer.addView(eventView)
            }
        }
    }

    private fun isExpired(event: Event): Boolean {
        if (event.time == null) return false
        return try {
            val dateTimeString = "${event.date} ${event.time}"
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val expirationDateTime = LocalDateTime.parse(dateTimeString, formatter)
            LocalDateTime.now().isAfter(expirationDateTime)
        } catch (e: DateTimeParseException) {
            false
        }
    }

    private fun isPastExam(event: Event): Boolean {
        val examDate = LocalDate.parse(event.date)
        return LocalDate.now().isAfter(examDate)
    }

    private fun setupEventView(eventView: View, event: Event) {
        val cardView: CardView = eventView.findViewById(R.id.cardView)
        val icon: ImageView = eventView.findViewById(R.id.ivEventIcon)
        val title: TextView = eventView.findViewById(R.id.tvEventTitle)
        val description: TextView = eventView.findViewById(R.id.tvEventDescription)
        val completeButton: MaterialButton = eventView.findViewById(R.id.btnComplete)
        val dateHeader: TextView = eventView.findViewById(R.id.tvEventDateHeader)
        val eventType: TextView = eventView.findViewById(R.id.tvEventType)

        dateHeader.visibility = View.GONE
        title.text = event.title
        description.text = event.description

        val timeText = if (event.time != null) {
            try {
                val time = LocalTime.parse(event.time, DateTimeFormatter.ofPattern("HH:mm:ss"))
                " - ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            } catch (e: Exception) {
                ""
            }
        } else {
            ""
        }

        if (event.type == "tarea") {
            eventType.text = "${getString(R.string.tipo_tarea)}$timeText"
            eventType.setTextColor(ContextCompat.getColor(this, R.color.b500))
            icon.setImageResource(R.drawable.ic_task)

            if (event.completada) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.verde_completado))
                completeButton.isEnabled = false
                completeButton.text = "✓"
                completeButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.verde_completado)
                completeButton.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else if (isExpired(event)) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.rojo_expirado))
                completeButton.isEnabled = false
                completeButton.text = "X"
                completeButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.rojo_expirado)
                completeButton.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.tarjeta_fondo))
                completeButton.isEnabled = true
                completeButton.text = getString(R.string.btn_completar)
                completeButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.b500)
                completeButton.setTextColor(ContextCompat.getColor(this, R.color.white))
            }

            completeButton.setOnClickListener {
                if (!event.completada && !isExpired(event)) {
                    markAsCompleted(event)
                }
            }
        } else {
            eventType.text = "${getString(R.string.tipo_examen)}$timeText"
            eventType.setTextColor(ContextCompat.getColor(this, R.color.b400))
            icon.setImageResource(R.drawable.ic_book)
            completeButton.visibility = View.GONE

            if (isPastExam(event)) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.verde_completado))
            } else if (event.completada) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.verde_completado))
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.tarjeta_fondo))
            }
        }

        cardView.setOnClickListener {
            val intent = Intent(this, EventDetailActivity::class.java).apply {
                putExtra("id", event.id)
                putExtra("type", event.type)
                putExtra("title", event.title)
                putExtra("description", event.description)
                putExtra("date", event.date)
                putExtra("asignatura_nombre", event.asignaturaNombre)
                if (event.type == "examen") {
                    putExtra("hora_examen", event.time)
                } else {
                    putExtra("hora_entrega", event.time)
                }
                putExtra("notif1", event.notificacion1)
                putExtra("notif2", event.notificacion2)
                putExtra("completada", event.completada)
            }
            startActivity(intent)
        }
    }

    private fun markAsCompleted(event: Event) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val tableName = if (event.type == "tarea") "tareas" else "examenes"
                SupabaseClient.client.from(tableName)
                    .update({ set("completada", true) }) {
                        filter { eq("id", event.id) }
                    }
                withContext(Dispatchers.Main) {
                    loadTasksAndExams()
                }
            } catch (e: Exception) {
                // Maneja el error
            }
        }
    }

    private fun showKyroToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Definición de Event interna para evitar conflictos de clases si no usas Models.kt
    data class Event(
        val id: Long,
        val type: String,
        val title: String,
        val description: String,
        val date: String,
        val time: String?,
        val completada: Boolean,
        val notificacion1: String?,
        val notificacion2: String?,
        val asignaturaNombre: String
    )
}