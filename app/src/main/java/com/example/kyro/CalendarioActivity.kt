package com.example.kyro

import android.content.Intent // ¡No olvides importar Intent!
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class CalendarioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendario_tareas)

        NavigationHelper.setupBottomNavigation(this, R.id.nav_calendar)

        val botonAnadirTarea: MaterialButton = findViewById(R.id.btnAddTask)
        val botonAnadirExamen: MaterialButton = findViewById(R.id.btnAddExam)

        // Listener para el botón de añadir tarea
        botonAnadirTarea.setOnClickListener {
            // ¡Ahora abrimos la nueva pantalla!
            val intent = Intent(this, AddTaskActivity::class.java)
            startActivity(intent)
        }

        // Listener para el botón de añadir examen
        botonAnadirExamen.setOnClickListener {
            Toast.makeText(this, "Funcionalidad para añadir examen pendiente", Toast.LENGTH_SHORT).show()

            // TODO: Crear AddExamActivity y su layout (igual que con Tareas)
            // y luego descomentar esto:
            // val intent = Intent(this, AddExamActivity::class.java)
            // startActivity(intent)
        }
    }
}
