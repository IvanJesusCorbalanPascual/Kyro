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

        // Recibe los datos de la pantalla anterior
        val tituloRecibido = intent.getStringExtra("EXTRA_TITULO") ?: "Sin Título"
        val contenidoRecibido = intent.getStringExtra("EXTRA_CONTENIDO") ?: ""
        val idRecibido = intent.getLongExtra("EXTRA_ID", -1)

        // Sustituye el texto por defecto por el real de la BD
        tvTituloTema.text = tituloRecibido

        // Lógica al pulsar el botón modificar
        btnModificar.setOnClickListener {
            val intent = Intent(this, ModificarTemarioActivity::class.java)

            // Le pasa también el contenido a la pantalla de modificar
            intent.putExtra("EXTRA_TITULO", tituloRecibido)
            intent.putExtra("EXTRA_CONTENIDO", contenidoRecibido)
            intent.putExtra("EXTRA_ID", idRecibido)

            startActivity(intent)
        }
        NavigationHelper.setupBottomNavigation(this, R.id.nav_syllabus)
    }
}