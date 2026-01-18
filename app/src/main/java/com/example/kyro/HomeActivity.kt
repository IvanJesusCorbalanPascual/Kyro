package com.example.kyro

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Data class unificada para Tareas y Examenes
data class Event(
    val id: Long,
    val type: String,
    val name: String,
    val description: String,
    val date: String,
    val time: String?,
    val completada: Boolean,
    val notificacion1: String?,
    val notificacion2: String?,
    val asignaturaId: Long
) : Serializable

class HomeActivity : AppCompatActivity() {

    private var nextEvent: Event? = null

    // Identifica la respuesta del usuario al pedir notificaciones
    private val CODIGO_PETICION_NOTIFICACIONES = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Configura los botones de rapido acceso de la pantalla principal
        setupQuickActions()
    }

    // Se ejecuta siempre que el usuario ve esta pantalla
    override fun onResume() {
        super.onResume()
        // Llama al Helper y dile que ilumine "nav_home"
        NavigationHelper.setupBottomNavigation(this, R.id.nav_home)

        // Comprueba y pide los  en un dialogo para el modo de estudio Focus si no los tiene
        verificarYPedirPermisosFocus()

        // Comprueba si ya tiene permiso, si lo tiene activa el servicio en segundo plano
        if (comprobarPermisoDeUso()) {
            val intentService = Intent(this, MonitorService::class.java)
            startService(intentService)
        }
        updateTaskProgress()
        updateNextEvent()
        loadUserProfile()
        updateDeliveredTasksProgress()
    }

    private fun loadUserProfile() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
                val userProfile = SupabaseClient.client.from("profiles").select { filter { eq("id", userId) } }.decodeSingleOrNull<UserProfile>()

                withContext(Dispatchers.Main) {
                    userProfile?.let {
                        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)
                        tvGreeting.text = "Hola, ${it.username}! 👋"
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun updateNextEvent() {
        val tvEventName: TextView = findViewById(R.id.tvEventName)
        val tvEventTime: TextView = findViewById(R.id.tvEventTime)
        val cardNextEvent: MaterialCardView = findViewById(R.id.cardNextEvent)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch

                val today = LocalDate.now()

                val upcomingTasks = SupabaseClient.client.postgrest["tareas"]
                    .select { filter { eq("id_usuario", userId); eq("completada", false) } }
                    .decodeList<Tarea>()
                    .filter { LocalDate.parse(it.fecha_entrega).isAfter(today.minusDays(1)) }
                    .map { Event(it.id!!, "tarea", it.nombre_tarea, it.descripcion, it.fecha_entrega, it.hora_entrega, it.completada, it.notificacion1, it.notificacion2, it.asignatura_id) }

                val upcomingExams = SupabaseClient.client.postgrest["examenes"]
                    .select { filter { eq("id_usuario", userId); eq("completada", false) } }
                    .decodeList<Examen>()
                    .filter { LocalDate.parse(it.fecha_examen).isAfter(today.minusDays(1)) }
                    .map { Event(it.id!!, "examen", it.nombre_examen, it.descripcion, it.fecha_examen, it.hora_examen, it.completada, it.notificacion1, it.notificacion2, it.asignatura_id) }

                val allUpcomingEvents = (upcomingTasks + upcomingExams).sortedBy { it.date }

                nextEvent = allUpcomingEvents.firstOrNull()

                withContext(Dispatchers.Main) {
                    if (nextEvent != null) {
                        tvEventName.text = nextEvent!!.name

                        val eventDate = LocalDate.parse(nextEvent!!.date)
                        val daysUntil = ChronoUnit.DAYS.between(today, eventDate)
                        val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM")

                        val timeText = "${eventDate.format(formatter)} • Faltan $daysUntil días"
                        tvEventTime.text = timeText

                        cardNextEvent.setOnClickListener {
                            val intent = Intent(this@HomeActivity, EventDetailActivity::class.java).apply {
                                putExtra("id", nextEvent!!.id)
                                putExtra("type", nextEvent!!.type)
                                putExtra("title", nextEvent!!.name)
                                putExtra("description", nextEvent!!.description)
                                putExtra("date", nextEvent!!.date)
                                if (nextEvent!!.type == "examen") {
                                    putExtra("hora_examen", nextEvent!!.time)
                                } else {
                                    putExtra("hora_entrega", nextEvent!!.time)
                                }
                                putExtra("notif1", nextEvent!!.notificacion1)
                                putExtra("notif2", nextEvent!!.notificacion2)
                                putExtra("completada", nextEvent!!.completada)
                                putExtra("asignatura_id", nextEvent!!.asignaturaId)
                            }
                            startActivity(intent)
                        }

                    } else {
                        tvEventName.text = "No hay eventos próximos"
                        tvEventTime.text = ""
                        cardNextEvent.setOnClickListener(null)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvEventName.text = "Error al cargar evento"
                    tvEventTime.text = e.message // Muestra el error para depurar
                }
            }
        }
    }

    private fun updateTaskProgress() {
        val tvTasksPercentage: TextView = findViewById(R.id.tvTasksPercentage)
        val pbTasks: ProgressBar = findViewById(R.id.pbTasks)
        val tvTasksCompleted: TextView = findViewById(R.id.tvTasksCompleted)

        val tvAllTasksPercentage: TextView = findViewById(R.id.tvAllTasksPercentage)
        val pbAllTasks: ProgressBar = findViewById(R.id.pbAllTasks)
        val tvAllTasksCompleted: TextView = findViewById(R.id.tvAllTasksCompleted)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (userId == null) {
                    // Handle user not logged in
                    return@launch
                }

                val today = LocalDate.now()
                val threeDaysLater = today.plusDays(2)

                val allUpcomingTasks = SupabaseClient.client.postgrest["tareas"]
                    .select { filter { eq("id_usuario", userId) } }
                    .decodeList<Tarea>()
                    .filter { LocalDate.parse(it.fecha_entrega).isAfter(today.minusDays(1)) }

                // Tareas en los próximos 3 días
                val tasksInNext3Days = allUpcomingTasks.filter { 
                    val taskDate = LocalDate.parse(it.fecha_entrega)
                    !taskDate.isBefore(today) && !taskDate.isAfter(threeDaysLater)
                }

                val totalTasks3Days = tasksInNext3Days.size
                val completedTasks3Days = tasksInNext3Days.count { it.completada }

                val percentage3Days = if (totalTasks3Days > 0) {
                    (completedTasks3Days * 100) / totalTasks3Days
                } else {
                    0
                }

                withContext(Dispatchers.Main) {
                    tvTasksPercentage.text = "$percentage3Days%"
                    pbTasks.progress = percentage3Days
                    if (totalTasks3Days > 0) {
                        tvTasksCompleted.text = "Has completado $completedTasks3Days de $totalTasks3Days tareas"
                    } else {
                        tvTasksCompleted.text = "No tienes tareas en los próximos 3 días"
                    }
                }

                // Todas las tareas próximas
                val totalAllTasks = allUpcomingTasks.size
                val completedAllTasks = allUpcomingTasks.count { it.completada }

                val percentageAll = if (totalAllTasks > 0) {
                    (completedAllTasks * 100) / totalAllTasks
                } else {
                    0
                }

                withContext(Dispatchers.Main) {
                    tvAllTasksPercentage.text = "$percentageAll%"
                    pbAllTasks.progress = percentageAll
                    if (totalAllTasks > 0) {
                        tvAllTasksCompleted.text = "Has completado $completedAllTasks de $totalAllTasks tareas"
                    } else {
                        tvAllTasksCompleted.text = "No tienes tareas próximas"
                    }
                }

            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun updateDeliveredTasksProgress() {
        val tvPercentage: TextView = findViewById(R.id.tvDeliveredTasksPercentage)
        val pb: ProgressBar = findViewById(R.id.pbDeliveredTasks)
        val tvCompleted: TextView = findViewById(R.id.tvDeliveredTasksCompleted)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
                val today = LocalDate.now()

                val allTasks = SupabaseClient.client.from("tareas").select {
                    filter {
                        eq("id_usuario", userId)
                    }
                }.decodeList<Tarea>()

                val relevantTasks = allTasks.filter { it.completada || LocalDate.parse(it.fecha_entrega).isBefore(today) }
                val deliveredCount = relevantTasks.count { it.completada }
                val totalRelevantTasks = relevantTasks.size

                val percentage = if (totalRelevantTasks > 0) {
                    (deliveredCount * 100) / totalRelevantTasks
                } else {
                    0
                }

                withContext(Dispatchers.Main) {
                    tvPercentage.visibility = View.VISIBLE
                    pb.visibility = View.VISIBLE
                    tvPercentage.text = "$percentage%"
                    pb.progress = percentage
                    if (totalRelevantTasks > 0) {
                        tvCompleted.text = "$deliveredCount de $totalRelevantTasks tareas finalizadas"
                    } else {
                        tvCompleted.text = "No hay tareas entregadas o caducadas."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvCompleted.text = "Error al cargar las tareas."
                    tvPercentage.visibility = View.GONE
                    pb.visibility = View.GONE
                }
            }
        }
    }


    // Configuracion de los botones de las tarjetas
    private fun setupQuickActions() {
        val btnQuickAI = findViewById<MaterialCardView>(R.id.btnQuickAI)
        btnQuickAI.setOnClickListener {
            //Toast.makeText(this, "Abriendo Kyro IA...", Toast.LENGTH_SHORT).show()
            showKyroToast("Abriendo Kyro IA...")
        }

        // Botón para ir a asignatura
        val btnQuickAsignatura = findViewById<MaterialCardView>(R.id.btnQuickAsignaturas)
        btnQuickAsignatura.setOnClickListener {
            startActivity(Intent(this, AsignaturaActivity::class.java))
        }
    }

    // Logica del modo Focus, pregunta al sistema si tiene permisos para ver el historial de uso de apps
    private fun comprobarPermisoDeUso(): Boolean {
        // Obtiene el gestor de operaciones de apps del sistema
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        // Comprueba el estado del permiso de uso de apps
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            // ID del proceso de Kyro
            Process.myUid(),
            // Nombre del paquete
            packageName
        )
        // Devuelve true si esta permitido, si no devuelve false
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // Muestra el diálogo para pedir los permisos y reedirige al usuario si no los tiene
    private fun verificarYPedirPermisosFocus() {

        // Primero pide permiso de notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {

                // Pido el permiso directamente en un pop-up
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    CODIGO_PETICION_NOTIFICACIONES
                )
                // [IMPORTANTE] Hacemos return aquí para que NO salga el diálogo de "Uso" inmediatamente.
                // Esperamos a que el usuario acepte las notificaciones y vuelva a entrar.
                return
            }
        }

        // Si ya tiene notificaciones, comprueba los datos de uso
        if (!comprobarPermisoDeUso()) {
            // Crea una alerta visual para avisar al usuario de porque necesita la app dichos permisos, obligatorio por Google Play
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Activar Modo Focus")
            builder.setMessage("Para que Kyro te pueda ayudar a concentrarte y evitar distracciones, necesita permiso para detectar qué apps usas.\n\nBusca 'Kyro' en la siguiente lista y activa el permiso si estás de acuerdo.")

            // Lleva al usuario a la configuración de Android si selecciona esta opción
            builder.setPositiveButton("Ir a Ajustes") { dialog, _ ->
                // Abre la lista de "Acceso a datos de uso" del sistema
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                // Cierra el dialogo
                dialog.dismiss()
            }

            // En caso de ser negativo, cierra el diálogo
            builder.setNegativeButton("Más tarde") { dialog, _ ->
                dialog.dismiss()
            }

            // Es obligatorio elegir una opción
            builder.setCancelable(false)

            // Muestra el diálogo
            builder.show()
        }
    }
}