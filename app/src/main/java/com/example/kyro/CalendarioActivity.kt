package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalendarioActivity : AppCompatActivity() {

    private lateinit var tasksAndExamsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendario_tareas)

        tasksAndExamsContainer = findViewById(R.id.tasksAndExamsContainer)

        val botonAnadirTarea: MaterialButton = findViewById(R.id.btnAddTask)
        val botonAnadirExamen: MaterialButton = findViewById(R.id.btnAddExam)

        botonAnadirTarea.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            startActivity(intent)
        }

        botonAnadirExamen.setOnClickListener {
            val intent = Intent(this, AddExamActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        NavigationHelper.setupBottomNavigation(this, R.id.nav_calendar)
        loadTasksAndExams()
    }

    private fun loadTasksAndExams() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch

                val tareas = SupabaseClient.client.postgrest["tareas"]
                    .select { filter { eq("id_usuario", userId) } }
                    .decodeList<Tarea>()

                val examenes = SupabaseClient.client.postgrest["examenes"]
                    .select { filter { eq("id_usuario", userId) } }
                    .decodeList<Examen>()

                val events = mutableListOf<Event>()
                tareas.forEach { event -> event.id?.let { events.add(Event(it, "tarea", event.nombre_asignatura, event.descripcion, event.fecha_entrega, event.notificacion1, event.notificacion2)) } }
                examenes.forEach { event -> event.id?.let { events.add(Event(it, "examen", event.nombre_asignatura, event.descripcion, event.fecha_examen, event.notificacion1, event.notificacion2)) } }

                events.sortBy { it.date }

                withContext(Dispatchers.Main) {
                    displayEvents(events)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun displayEvents(events: List<Event>) {
        tasksAndExamsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        for (event in events) {
            val eventView = inflater.inflate(R.layout.list_item_event, tasksAndExamsContainer, false)

            val icon: ImageView = eventView.findViewById(R.id.ivEventIcon)
            val title: TextView = eventView.findViewById(R.id.tvEventTitle)
            val description: TextView = eventView.findViewById(R.id.tvEventDescription)
            val date: TextView = eventView.findViewById(R.id.tvEventDate)

            title.text = event.title
            description.text = event.description
            date.text = event.date

            if (event.type == "tarea") {
                icon.setImageResource(R.drawable.ic_task)
            } else {
                icon.setImageResource(R.drawable.ic_book)
            }

            eventView.setOnClickListener {
                val intent = Intent(this, EventDetailActivity::class.java).apply {
                    putExtra("id", event.id)
                    putExtra("type", event.type)
                    putExtra("title", event.title)
                    putExtra("description", event.description)
                    putExtra("date", event.date)
                    putExtra("notif1", event.notificacion1)
                    putExtra("notif2", event.notificacion2)
                }
                startActivity(intent)
            }

            tasksAndExamsContainer.addView(eventView)
        }
    }

    data class Event(
        val id: Long,
        val type: String,
        val title: String,
        val description: String,
        val date: String,
        val notificacion1: String?,
        val notificacion2: String?
    )
}
