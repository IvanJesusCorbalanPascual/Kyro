package com.example.kyro

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class CalendarioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendario_tareas)

        val botonAnadirTarea: MaterialButton = findViewById(R.id.btnAddTask)
        val botonAnadirExamen: MaterialButton = findViewById(R.id.btnAddExam)

        // Listener para el botón de añadir tarea
        botonAnadirTarea.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            startActivity(intent)
        }

        // Listener para el botón de añadir examen
        botonAnadirExamen.setOnClickListener {
            val intent = Intent(this, AddExamActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Llama al Helper y le dice que ilumine Agenda
        NavigationHelper.setupBottomNavigation(this, R.id.nav_calendar)
    }
}
