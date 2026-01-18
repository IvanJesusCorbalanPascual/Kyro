package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AsignaturaSeleccionadaActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asignatura_seleccionada)

        val tvTituloAsignatura = findViewById<TextView>(R.id.tvNombreAsignatura)
        val btnModificar = findViewById<TextView>(R.id.btnModificar)

        // Vincula la tarjeta de ejercicios (Asegúrate de que en el XML tenga este ID: cardEjercicios)
        val cardEjercicios = findViewById<MaterialCardView>(R.id.cardInfoAsignatura)

        // Recibe los datos
        val tituloRecibido = intent.getStringExtra("EXTRA_TITULO") ?: "Sin Título"
        val contenidoRecibido = intent.getStringExtra("EXTRA_CONTENIDO") ?: ""
        val preguntasJson = intent.getStringExtra("EXTRA_JSON_PREGUNTAS")
        val idRecibido = intent.getLongExtra("EXTRA_ID", -1)

        tvTituloAsignatura.text = tituloRecibido

        // Al pulsar la tarjeta azul de ejercicios
        cardEjercicios.setOnClickListener {
            // Verificamos si hay preguntas guardadas en la "mochila" (JSON)
            if (!preguntasJson.isNullOrEmpty()) {
                try {
                    // Usamos Gson para convertir el TEXTO JSON a OBJETOS Kotlin
                    val gson = Gson()
                    val tipoLista = object : TypeToken<List<PreguntaGenerada>>() {}.type
                    val listaPreguntas: List<PreguntaGenerada> = gson.fromJson(preguntasJson, tipoLista)

                    // Preparamos el contenido en el intent antes de enviarlo a la siguiente pantalla (QuizActivity)
                    val intent = Intent(this, QuizActivity::class.java)

                    // Empaquetamos la lista como ArrayList para que viaje bien
                    intent.putExtra("EXTRA_PREGUNTAS", ArrayList(listaPreguntas))
                    startActivity(intent)

                } catch (e: Exception) {
                    showKyroToast("Error al cargar el test guardado")
                    e.printStackTrace()
                }
            } else {
                // Si no hay test guardado (porque es un tema antiguo o falló la IA)
                showKyroToast("Este tema no tiene ejercicios generados aún.")
            }
        }

        // --- BOTÓN MODIFICAR ---
        btnModificar.setOnClickListener {
            val intent = Intent(this, ModificarAsignaturaActivity::class.java)
            intent.putExtra("EXTRA_TITULO", tituloRecibido)
            intent.putExtra("EXTRA_CONTENIDO", contenidoRecibido)
            intent.putExtra("EXTRA_ID", idRecibido)
            startActivity(intent)
        }

        NavigationHelper.setupBottomNavigation(this, R.id.nav_asignatura)
    }
}