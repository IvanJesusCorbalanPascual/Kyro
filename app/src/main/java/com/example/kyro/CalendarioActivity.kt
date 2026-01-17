package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class CalendarioActivity : AppCompatActivity() {

    private lateinit var calendarComposeView: ComposeView
    private lateinit var tasksAndExamsContainer: LinearLayout
    private lateinit var btnToggleTasks: MaterialButton
    private lateinit var btnAddExam: MaterialButton
    private lateinit var btnAddTask: MaterialButton
    private var selectedDate: LocalDate = LocalDate.now()
    private var allEvents: List<Event> = listOf()
    private var showAllTasks = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendario_tareas)

        calendarComposeView = findViewById(R.id.calendarComposeView)
        tasksAndExamsContainer = findViewById(R.id.tasksAndExamsContainer)
        btnToggleTasks = findViewById(R.id.btnToggleTasks)
        btnAddExam = findViewById(R.id.btnAddExam)
        btnAddTask = findViewById(R.id.btnAddTask)


        NavigationHelper.setupBottomNavigation(this, R.id.nav_calendar)

        btnToggleTasks.setOnClickListener { 
            showAllTasks = !showAllTasks
            displayEvents(allEvents)
            if (showAllTasks) {
                btnToggleTasks.text = getString(R.string.calendar_btn_ver_dia)
            } else {
                btnToggleTasks.text = getString(R.string.calendar_btn_view_all)
            }
        }

        btnAddExam.setOnClickListener {
            val intent = Intent(this, AddExamActivity::class.java)
            startActivity(intent)
        }

        btnAddTask.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
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
                tareas.forEach { event -> event.id?.let { events.add(Event(it, "tarea", event.nombre_asignatura, event.descripcion, event.fecha_entrega, event.hora_entrega, event.completada, event.notificacion1, event.notificacion2)) } }
                examenes.forEach { event -> event.id?.let { events.add(Event(it, "examen", event.nombre_asignatura, event.descripcion, event.fecha_examen, null, event.completada, event.notificacion1, event.notificacion2)) } }
                allEvents = events

                withContext(Dispatchers.Main) {
                    calendarComposeView.setContent {
                        Calendar(events = allEvents, onDateSelected = {
                            selectedDate = it
                            displayEvents(allEvents)
                        })
                    }
                    displayEvents(allEvents)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun displayEvents(events: List<Event>) {
        tasksAndExamsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        val eventsToDisplay = if (showAllTasks) {
            events.sortedBy { it.date }
        } else {
            events.filter { it.date == selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE) }.sortedBy { it.date }
        }

        var lastDate: String? = null
        for (event in eventsToDisplay) {
            val eventView = inflater.inflate(R.layout.list_item_event_with_date, tasksAndExamsContainer, false)

            val cardView: CardView = eventView.findViewById(R.id.cardView)
            val icon: ImageView = eventView.findViewById(R.id.ivEventIcon)
            val title: TextView = eventView.findViewById(R.id.tvEventTitle)
            val description: TextView = eventView.findViewById(R.id.tvEventDescription)
            val completeButton: MaterialButton = eventView.findViewById(R.id.btnComplete)
            val dateHeader: TextView = eventView.findViewById(R.id.tvEventDateHeader)
            val eventType: TextView = eventView.findViewById(R.id.tvEventType)

            if (showAllTasks) {
                if (lastDate == null || lastDate != event.date) {
                    dateHeader.visibility = View.VISIBLE
                    dateHeader.text = LocalDate.parse(event.date).format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                    lastDate = event.date
                } else {
                    dateHeader.visibility = View.GONE
                }
            } else {
                dateHeader.visibility = View.GONE
            }

            title.text = event.title
            description.text = event.description

            if (event.type == "tarea") {
                eventType.text = getString(R.string.tipo_tarea)
                eventType.setTextColor(ContextCompat.getColor(this, R.color.b500))
                icon.setImageResource(R.drawable.ic_task)

                var isExpired = false
                if (event.time != null) {
                    try {
                        val dateTimeString = "${event.date} ${event.time}"
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        val expirationDateTime = LocalDateTime.parse(dateTimeString, formatter)
                        isExpired = LocalDateTime.now().isAfter(expirationDateTime)
                    } catch (e: java.time.format.DateTimeParseException) {
                        isExpired = false
                    }
                }

                if (event.completada) {
                    cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.verde_completado))
                    completeButton.isEnabled = false
                    completeButton.text = "✓"
                    completeButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.verde_completado)
                    completeButton.setTextColor(ContextCompat.getColor(this, R.color.white))
                } else if (isExpired) {
                    cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.rojo_expirado))
                    completeButton.isEnabled = false
                    completeButton.text = "X"
                    completeButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.rojo_expirado)
                    completeButton.setTextColor(ContextCompat.getColor(this, R.color.white))
                } else {
                    cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))
                    completeButton.isEnabled = true
                    completeButton.text = getString(R.string.btn_completar)
                    completeButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.b500)
                    completeButton.setTextColor(ContextCompat.getColor(this, R.color.white))
                }

                completeButton.setOnClickListener {
                    if (!event.completada && !isExpired) {
                        markAsCompleted(event)
                    }
                }
            } else { // "examen"
                eventType.text = getString(R.string.tipo_examen)
                eventType.setTextColor(ContextCompat.getColor(this, R.color.purple_500))
                icon.setImageResource(R.drawable.ic_book)
                completeButton.visibility = View.GONE

                val examDate = LocalDate.parse(event.date)
                val isPastExam = LocalDate.now().isAfter(examDate)

                if (isPastExam) {
                    cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.verde_completado))
                } else if (event.completada) {
                    cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.verde_completado))
                }
            }

            cardView.setOnClickListener {
                val intent = Intent(this, EventDetailActivity::class.java).apply {
                    putExtra("id", event.id)
                    putExtra("type", event.type)
                    putExtra("title", event.title)
                    putExtra("description", event.description)
                    putExtra("date", event.date)
                    putExtra("hora_entrega", event.time)
                    putExtra("notif1", event.notificacion1)
                    putExtra("notif2", event.notificacion2)
                    putExtra("completada", event.completada) // <-- AÑADIDO
                }
                startActivity(intent)
            }

            tasksAndExamsContainer.addView(eventView)
        }
    }

    private fun markAsCompleted(event: Event) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val tableName = if (event.type == "tarea") "tareas" else "examenes"
                SupabaseClient.client.postgrest[tableName]
                    .update({ set("completada", true) }) {
                        filter { eq("id", event.id) }
                    }

                withContext(Dispatchers.Main) {
                    loadTasksAndExams()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    data class Event(
        val id: Long,
        val type: String,
        val title: String,
        val description: String,
        val date: String,
        val time: String?,
        val completada: Boolean,
        val notificacion1: String?,
        val notificacion2: String?
    )
}

@Composable
fun Calendar(events: List<CalendarioActivity.Event>, onDateSelected: (LocalDate) -> Unit) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val eventMap = events.groupBy { it.date }

    Column(modifier = Modifier.padding(8.dp)) { // Reduced padding
        // Header with month navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Més anterior")
            }
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp, // Reduced font size
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Més següent")
            }
        }
        Spacer(modifier = Modifier.height(8.dp)) // Reduced spacer

        // Days of the week header
        Row(modifier = Modifier.fillMaxWidth()) {
            val daysOfWeek = DayOfWeek.values()
            for (day in daysOfWeek) {
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp // Reduced font size
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp)) // Reduced spacer

        // Days of the month grid
        val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek
        val daysInMonth = currentMonth.lengthOfMonth()
        val emptyDays = (1 until firstDayOfMonth.value).map { null }
        val days = (1..daysInMonth).toList()
        val allDays = emptyDays + days
        val chunkedDays = allDays.chunked(7)

        for (week in chunkedDays) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (day in week) {
                    DayCell(day, currentMonth, selectedDate, eventMap) { newDate ->
                        selectedDate = newDate
                        onDateSelected(newDate)
                    }
                }
                // Fill remaining space in the last row
                if (week.size < 7) {
                    for (i in 0 until (7 - week.size)) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp)) // Reduced spacer
        }
    }
}

@Composable
fun RowScope.DayCell(
    day: Int?,
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    eventMap: Map<String, List<CalendarioActivity.Event>>,
    onDateSelected: (LocalDate) -> Unit
) {
    if (day != null) {
        val date = currentMonth.atDay(day)
        val isSelected = date == selectedDate
        val isToday = date == LocalDate.now()
        val eventsOnDate = eventMap[date.format(DateTimeFormatter.ISO_LOCAL_DATE)] ?: emptyList()

        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .padding(2.dp) // Reduced padding
                .clip(CircleShape)
                .background(if (isSelected) Color.LightGray else Color.Transparent)
                .clickable { onDateSelected(date) },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = day.toString(),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp, // Reduced font size
                    color = when {
                        isToday -> Color.Blue
                        date.month == currentMonth.month -> Color.Black
                        else -> Color.Gray
                    }
                )
                if (eventsOnDate.isNotEmpty()) {
                    Row {
                        eventsOnDate.take(3).forEach { event ->
                            val dotColor = when {
                                event.completada -> Color.Green
                                event.type == "examen" -> Color.Blue
                                event.type == "tarea" -> {
                                    val isExpired = if (event.time != null) {
                                        try {
                                            val dateTimeString = "${event.date} ${event.time}"
                                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                            val expirationDateTime = LocalDateTime.parse(dateTimeString, formatter)
                                            LocalDateTime.now().isAfter(expirationDateTime)
                                        } catch (e: Exception) {
                                            LocalDate.parse(event.date).isBefore(LocalDate.now())
                                        }
                                    } else {
                                        LocalDate.parse(event.date).isBefore(LocalDate.now())
                                    }

                                    if (isExpired) Color.Red else Color.Gray
                                }
                                else -> Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 1.dp)
                                    .size(4.dp) // Reduced size
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
    }
}
