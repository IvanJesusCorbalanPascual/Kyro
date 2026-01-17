package com.example.kyro

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Clase que se encarga de recrear un Quiz tipo test con las preguntas generadas por la IA
 */

class QuizActivity : AppCompatActivity() {

    // Variables de la vista
    private lateinit var tvContador: TextView
    private lateinit var tvPregunta: TextView
    private lateinit var btnOpcion1: Button
    private lateinit var btnOpcion2: Button
    private lateinit var btnOpcion3: Button
    private lateinit var btnOpcion4: Button

    // Variables lógicas
    private var listaPreguntas: ArrayList<PreguntaGenerada> = ArrayList()
    private var posicionActual = 0
    private var aciertos = 0
    private var botonesBloqueados = false // Para evitar doble click rápido

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        // Vinculando vistas
        tvContador = findViewById(R.id.tvContador)
        tvPregunta = findViewById(R.id.tvPregunta)
        btnOpcion1 = findViewById(R.id.btnOpcion1)
        btnOpcion2 = findViewById(R.id.btnOpcion2)
        btnOpcion3 = findViewById(R.id.btnOpcion3)
        btnOpcion4 = findViewById(R.id.btnOpcion4)

        // Recibe los datos de TemarioActivity y los almacena en el array de listaPreguntas
        @Suppress("DEPRECATION")
        listaPreguntas = intent.getSerializableExtra("EXTRA_PREGUNTAS") as? ArrayList<PreguntaGenerada> ?: ArrayList()

        // Manejo de errores
        if (listaPreguntas.isEmpty()) {
            showKyroToast("Error: No llegaron preguntas")
            finish()
            return
        }

        // Configurar clicks de botones
        // Mapeando los botones a las letras A, B, C, D
        btnOpcion1.setOnClickListener { verificarRespuesta("A", btnOpcion1) }
        btnOpcion2.setOnClickListener { verificarRespuesta("B", btnOpcion2) }
        btnOpcion3.setOnClickListener { verificarRespuesta("C", btnOpcion3) }
        btnOpcion4.setOnClickListener { verificarRespuesta("D", btnOpcion4) }

        // Mostrando la primera pregunta
        mostrarPregunta()
    }

    private fun mostrarPregunta() {
        // Desbloqueamos botones para la nueva ronda
        botonesBloqueados = false

        // Obtenemos la pregunta actual
        val preguntaActual = listaPreguntas[posicionActual]

        // Actualizamos textos
        tvContador.text = "Pregunta ${posicionActual + 1} de ${listaPreguntas.size}"
        tvPregunta.text = preguntaActual.pregunta

        // Asignamos las opciones a los botones
        // Aseguramos que haya suficientes opciones (la IA a veces manda 3 o 5)
        val botones = listOf(btnOpcion1, btnOpcion2, btnOpcion3, btnOpcion4)

        // Reseteamos el estilo visual (Color azul original)
        restaurarEstiloBotones(botones)

        // Este bucle se encarga de mostrar las opciones y ocultar las que no sean necesarias
        for (i in botones.indices) {
            if (i < preguntaActual.opciones.size) {
                botones[i].text = preguntaActual.opciones[i]
                botones[i].visibility = android.view.View.VISIBLE
            } else {
                botones[i].visibility = android.view.View.GONE
            }
        }
    }

    private fun verificarRespuesta(letraSeleccionada: String, botonPulsado: Button) {
        if (botonesBloqueados) return // Si ya pulsó, no hacemos nada
        botonesBloqueados = true

        val preguntaActual = listaPreguntas[posicionActual]

        // Comparamos ignorando mayúsculas/minúsculas ("A" vs "a")
        val esCorrecta = letraSeleccionada.equals(preguntaActual.respuestaCorrecta, ignoreCase = true)

        if (esCorrecta) {
            aciertos++
            showKyroToast("¡Correcto!")
            // Pintar verde
            pintarBoton(botonPulsado, R.color.verde_completado) // Necesitas definir este color o usar uno de sistema
        } else {
            showKyroToast("Fallaste...")
            // Pintar rojo el pulsado
            pintarBoton(botonPulsado, R.color.rojo_expirado)

            // Pintar verde el que ERA correcto para que el usuario lo sepa
            iluminarRespuestaCorrecta(preguntaActual.respuestaCorrecta)

            // Mostrando la explicación de la IA en un SnackBar largo
            if (preguntaActual.explicacion.isNotEmpty()) {
                // Busca la vista raíz para anclar el mensaje a la pantalla actual
                val vistaRaiz = findViewById<android.view.View>(android.R.id.content)

                com.google.android.material.snackbar.Snackbar.make(
                    vistaRaiz,
                    preguntaActual.explicacion,
                    8000 // Duración en milisegundos (8 segundos)
                ).apply {
                    setAction("OK") { dismiss() } // Botón para cerrarlo antes
                    setTextMaxLines(5) // Permite hasta 5 líneas de texto
                    show()
                }
            }
        }

        // Esperar 1.5 segundos y pasar a la siguiente
        Handler(Looper.getMainLooper()).postDelayed({
            posicionActual++
            if (posicionActual < listaPreguntas.size) {
                mostrarPregunta()
            } else {
                finalizarQuiz()
            }
        }, 1500)
    }

    private fun iluminarRespuestaCorrecta(letraCorrecta: String) {
        when (letraCorrecta.uppercase()) {
            "A" -> pintarBoton(btnOpcion1, R.color.verde_completado)
            "B" -> pintarBoton(btnOpcion2, R.color.verde_completado)
            "C" -> pintarBoton(btnOpcion3, R.color.verde_completado)
            "D" -> pintarBoton(btnOpcion4, R.color.verde_completado)
        }
    }

    private fun finalizarQuiz() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("¡Entrenamiento completado!")
        builder.setMessage("Has acertado $aciertos de ${listaPreguntas.size} preguntas.")
        builder.setCancelable(false) // Obliga a pulsar el botón

        builder.setPositiveButton("Volver al temario") { _, _ ->
            finish() // Cierra esta pantalla y vuelve a la anterior
        }

        builder.show()
    }

    // Funciones auxiliares de diseño

    private fun restaurarEstiloBotones(botones: List<Button>) {
        // Recorre los botones y les quita los colores de la anterior ronda (Verde / Rojo) y les vuelve a poner el azul Kyro
        for (btn in botones) {
            btn.setBackgroundColor(R.color.b900) // Color Kyro oscuro
            btn.isEnabled = true
        }
    }

    // Pinta el boton dependiendo de si es acierto o error
    private fun pintarBoton(boton: Button, colorResId: Int) {
        val colorReal = if (colorResId == R.color.verde_completado) Color.parseColor("#4CAF50")
        else if (colorResId == R.color.rojo_expirado) Color.parseColor("#F44336")
        else ContextCompat.getColor(this, colorResId)

        boton.setBackgroundColor(colorReal)
    }
}