package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class TemarioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temario)

        // 1. Configurar botones de "Estudiar"
        setupStudyButtons()

        // 2. Configurar menú inferior
        setupBottomNavigation()
    }

    private fun setupStudyButtons() {
        // Botón Estudiar 1 (Java) -> Abre TemarioSeleccionadoActivity
        val btnEstudiar1 = findViewById<TextView>(R.id.btnEstudiar1)
        btnEstudiar1?.setOnClickListener {
            try {
                val intent = Intent(this, TemarioSeleccionadoActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("TemarioActivity", "Error al abrir TemarioSeleccionadoActivity", e)
                Toast.makeText(this, "Error al abrir el temario", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón Estudiar 2 (Base de Datos)
        val btnEstudiar2 = findViewById<TextView>(R.id.btnEstudiar2)
        btnEstudiar2?.setOnClickListener {
            Toast.makeText(this, "Abriendo Base de Datos...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Verificación de seguridad por si el ID del layout no es correcto
        if (bottomNav == null) {
            Log.e("TemarioActivity", "BottomNavigationView no encontrado en el layout")
            return
        }

        // Marcar "Temario" como seleccionado
        bottomNav.selectedItemId = R.id.nav_temario

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // Navegación a Calendario
                R.id.nav_calendario -> {
                    try {
                        val intent = Intent(this, CalendarioActivity::class.java)
                        // FLAGS: Reordenan el historial para que no se acumulen actividades
                        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        startActivity(intent)
                        overridePendingTransition(0, 0) // Quita animación brusca
                        // finish() // Comentado: A veces es mejor no matar la actividad actual si quieres volver atrás rápido
                        true
                    } catch (e: Exception) {
                        Log.e("TemarioActivity", "Error al abrir CalendarioActivity", e)
                        Toast.makeText(this, "Error al abrir Calendario", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                // Ya estamos en Temario
                R.id.nav_temario -> true

                // TODO: Aquí puedes añadir los casos para Inicio, IA y Ajustes

                else -> false
            }
        }
    }
}
