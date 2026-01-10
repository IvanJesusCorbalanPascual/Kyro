package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class TemarioSeleccionadoActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temario_seleccionado)

        // Vincula las vistas
        val tvTituloTema = findViewById<TextView>(R.id.tvNombreAsignatura)
        // Al pulsar "Modificar", desplaza a la siguiente pantalla
        val btnModificar = findViewById<TextView>(R.id.btnModificar)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Recibe los datos de la pantalla anterior
        val contenidoRecibido = intent.getStringExtra("EXTRA_CONTENIDO") ?: "Sin Contenido"
        val idRecibido = intent.getLongExtra("EXTRA_ID", -1)

        // Sustituye el texto por defecto por el real de la BD
        tvTituloTema.text = contenidoRecibido

        // Lógica al pulsar el botón modificar
        btnModificar.setOnClickListener {
            val intent = Intent(this, ModificarTemarioActivity::class.java)

            // Le pasa también el contenido a la pantalla de modificar
            intent.putExtra("EXTRA_CONTENIDO", contenidoRecibido)
            intent.putExtra("EXTRA_ID", idRecibido)

            startActivity(intent)
        }

        // Configura el menú inferior de navegación
        setupBottomNavigation(bottomNav)
    }

    // Configuración de la barra de navegación
    private fun setupBottomNavigation(bottomNav: BottomNavigationView) {

        // Mantiene seleccionado temario en la barra de navegación
        bottomNav.selectedItemId = R.id.nav_syllabus

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_syllabus -> true
                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendarioActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, AjustesActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}