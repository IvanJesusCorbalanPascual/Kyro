package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class CalendarioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Cargamos el layout correcto (activity_calendario_tareas)
        try {
            setContentView(R.layout.activity_calendario_tareas)
        } catch (e: Exception) {
            Log.e("CalendarioActivity", "Error inflando el layout: ${e.message}")
            return // Salimos para evitar más errores si falla la vista
        }

        // 2. Configuramos la barra inferior
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        // Buscamos la barra por el ID 'bottomNavigation'
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        if (bottomNav == null) {
            Log.e("CalendarioActivity", "¡ERROR! No se encontró BottomNavigationView con ID 'bottomNavigation' en activity_calendario_tareas.xml")
            return
        }

        // Marcamos el botón de calendario como seleccionado
        bottomNav.selectedItemId = R.id.nav_calendar

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // Si pulsan Calendario, no hacemos nada (ya estamos aquí)
                R.id.nav_calendar -> true

                // Si pulsan Temario, volvemos
                R.id.nav_syllabus -> {
                    val intent = Intent(this, TemarioActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish() // Cerramos calendario
                    true
                }

                // Agrega aquí tus otros botones (Inicio, IA, Ajustes) si los tienes

                else -> false
            }
        }
    }
}
