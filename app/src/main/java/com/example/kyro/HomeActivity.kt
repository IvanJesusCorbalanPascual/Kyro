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
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Data class unificada para Tareas y Examenes
data class Event(
    val name: String,
    val date: String
)

class HomeActivity : AppCompatActivity() {

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
    }

    private fun updateNextEvent() {
        val tvEventName: TextView = findViewById(R.id.tvEventName)
        val tvEventTime: TextView = findViewById(R.id.tvEventTime)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch

                val today = LocalDate.now()

                val upcomingTasks = SupabaseClient.client.postgrest["tareas"]
                    .select { filter { eq("id_usuario", userId); eq("completada", false) } }
                    .decodeList<Tarea>()
                    .filter { LocalDate.parse(it.fecha_entrega).isAfter(today.minusDays(1)) }
                    .map { Event(it.nombre_asignatura, it.fecha_entrega) }

                val upcomingExams = SupabaseClient.client.postgrest["examenes"]
                    .select { filter { eq("id_usuario", userId); eq("completada", false) } }
                    .decodeList<Examen>()
                    .filter { LocalDate.parse(it.fecha_examen).isAfter(today.minusDays(1)) }
                    .map { Event(it.nombre_asignatura, it.fecha_examen) }

                val allUpcomingEvents = (upcomingTasks + upcomingExams).sortedBy { it.date }

                val nextEvent = allUpcomingEvents.firstOrNull()

                withContext(Dispatchers.Main) {
                    if (nextEvent != null) {
                        tvEventName.text = nextEvent.name

                        val eventDate = LocalDate.parse(nextEvent.date)
                        val daysUntil = ChronoUnit.DAYS.between(today, eventDate)
                        val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM")

                        val timeText = "${eventDate.format(formatter)} • Faltan $daysUntil días"
                        tvEventTime.text = timeText
                    } else {
                        tvEventName.text = "No hay eventos próximos"
                        tvEventTime.text = ""
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

    // Configuracion de los botones de las tarjetas
    private fun setupQuickActions() {
        val btnQuickAI = findViewById<MaterialCardView>(R.id.btnQuickAI)
        btnQuickAI.setOnClickListener {
            //Toast.makeText(this, "Abriendo Kyro IA...", Toast.LENGTH_SHORT).show()
            showKyroToast("Abriendo Kyro IA...")
        }

        // Botón para ir a temario
        val btnQuickSyllabus = findViewById<MaterialCardView>(R.id.btnQuickSyllabus)
        btnQuickSyllabus.setOnClickListener {
            startActivity(Intent(this, TemarioActivity::class.java))
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
            builder.setMessage("Para que Kyro te pueda ayudar a concentrarte y evitar distracciones con redes sociales, necesita permiso para detectar que apps usas. " +
                    "\n\nBusca 'Kyro' en la siguiente lista  y actívalo si estas de acuerdo.")

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