package com.example.kyro.activities

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.kyro.PreguntaGenerada
import com.example.kyro.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator

class QuizActivity : AppCompatActivity() {

    // Vistas de texto y progreso
    private lateinit var tvContador: TextView
    private lateinit var tvPregunta: TextView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var btnSubmit: MaterialButton
    private lateinit var btnClose: View

    // Layouts de las opciones (las tarjetas)
    private lateinit var layoutA: LinearLayout
    private lateinit var layoutB: LinearLayout
    private lateinit var layoutC: LinearLayout
    private lateinit var layoutD: LinearLayout

    // Textos de las opciones
    private lateinit var tvA: TextView
    private lateinit var tvB: TextView
    private lateinit var tvC: TextView
    private lateinit var tvD: TextView

    // Variables lógicas
    private var listaPreguntas: ArrayList<PreguntaGenerada> = ArrayList()
    private var posicionActual = 0
    private var aciertos = 0
    private var opcionSeleccionada: String? = null // Guarda "A", "B", "C" o "D"
    private var revisandoRespuesta = false // Indica si estamos viendo el resultado (verde/rojo)

    private lateinit var tvExplanation: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        initViews()

        // Recibir datos
        @Suppress("DEPRECATION")
        listaPreguntas = intent.getSerializableExtra("EXTRA_PREGUNTAS") as? ArrayList<PreguntaGenerada> ?: ArrayList()

        if (listaPreguntas.isEmpty()) {
            finish()
            return
        }

        setupListeners()
        mostrarPregunta()
    }

    private fun initViews() {
        tvContador = findViewById(R.id.tvContador)
        tvPregunta = findViewById(R.id.tvPregunta)
        progressBar = findViewById(R.id.progressBarQuiz)
        btnSubmit = findViewById(R.id.btnComprobar)
        btnClose = findViewById(R.id.btnCloseQuiz)
        tvExplanation = findViewById(R.id.tvExplanation)

        layoutA = findViewById(R.id.layoutOpcionA)
        layoutB = findViewById(R.id.layoutOpcionB)
        layoutC = findViewById(R.id.layoutOpcionC)
        layoutD = findViewById(R.id.layoutOpcionD)

        tvA = findViewById(R.id.tvOpcionA)
        tvB = findViewById(R.id.tvOpcionB)
        tvC = findViewById(R.id.tvOpcionC)
        tvD = findViewById(R.id.tvOpcionD)
    }

    private fun setupListeners() {
        // Al hacer clic en una tarjeta, se selecciona
        layoutA.setOnClickListener { marcarOpcion("A") }
        layoutB.setOnClickListener { marcarOpcion("B") }
        layoutC.setOnClickListener { marcarOpcion("C") }
        layoutD.setOnClickListener { marcarOpcion("D") }

        btnClose.setOnClickListener { finish() }

        btnSubmit.setOnClickListener  {
            if (revisandoRespuesta) {
                // Si ya comprobamos, vamos a la siguiente pregunta
                posicionActual++
                if (posicionActual < listaPreguntas.size) {
                    mostrarPregunta()
                } else {
                    finalizarQuiz()
                }
            } else {
                // Si no hemos comprobado, verificamos la selección
                if (opcionSeleccionada == null) {
                    showKyroToast(getString(R.string.quiz_msg_seleccionar))
                } else {
                    verificarRespuesta()
                }
            }
        }
    }

    private fun marcarOpcion(letra: String) {
        if (revisandoRespuesta) return // Evita cambiar la selección si ya se mostró si era correcta

        opcionSeleccionada = letra

        // Al poner 'true', el XML 'bg_answer_card' cambia al color azul instantáneamente
        layoutA.isSelected = (letra == "A")
        layoutB.isSelected = (letra == "B")
        layoutC.isSelected = (letra == "C")
        layoutD.isSelected = (letra == "D")

        // Activando el botón de comprobar solo cuando haya una opcion seleccionada
        btnSubmit.isEnabled = true
        btnSubmit.alpha = 1.0f
    }

    private fun mostrarPregunta() {
        revisandoRespuesta = false
        opcionSeleccionada = null
        btnSubmit.text = getString(R.string.quiz_btn_comprobar)

        tvExplanation.visibility = View.GONE

        val preguntaActual = listaPreguntas[posicionActual]

        // Actualizar Progreso
        tvContador.text = getString(R.string.quiz_contador, posicionActual + 1, listaPreguntas.size)
        val progreso = ((posicionActual + 1).toFloat() / listaPreguntas.size * 100).toInt()
        progressBar.setProgress(progreso, true)

        // Limpiar estilos y asignar textos
        resetEstilosTarjetas()
        tvPregunta.text = preguntaActual.pregunta
        tvA.text = preguntaActual.opciones.getOrNull(0) ?: ""
        tvB.text = preguntaActual.opciones.getOrNull(1) ?: ""
        tvC.text = preguntaActual.opciones.getOrNull(2) ?: ""
        tvD.text = preguntaActual.opciones.getOrNull(3) ?: ""
    }

    private fun verificarRespuesta() {
        revisandoRespuesta = true
        val preguntaActual = listaPreguntas[posicionActual]
        val esCorrecta = opcionSeleccionada.equals(preguntaActual.respuestaCorrecta, ignoreCase = true)

        if (esCorrecta) {
            aciertos++
            pintarResultado(opcionSeleccionada!!, true)
            showKyroToast(getString(R.string.quiz_msg_correcto))
        } else {
            pintarResultado(opcionSeleccionada!!, false) // El que marcó el usuario en rojo
            pintarResultado(preguntaActual.respuestaCorrecta, true) // La correcta en verde

            // Mostrando explicación
            if (preguntaActual.explicacion.isNotEmpty()) {
                tvExplanation.text = getString(R.string.quiz_explicacion_prefix, preguntaActual.explicacion)
                tvExplanation.visibility = View.VISIBLE
            }
        }

        btnSubmit.text = if (posicionActual + 1 < listaPreguntas.size)
            getString(R.string.quiz_btn_siguiente)
        else
            getString(R.string.quiz_btn_finalizar)
    }


    private fun pintarResultado(letra: String, correcta: Boolean) {
        val layout = when (letra.uppercase()) {
            "A" -> layoutA
            "B" -> layoutB
            "C" -> layoutC
            "D" -> layoutD
            else -> null
        }

        // Determinamos qué drawable (fondo redondeado) usar
        val drawableResId = if (correcta) {
            R.drawable.bg_answer_correct // El fondo verde redondeado
        } else {
            R.drawable.bg_answer_incorrect // El fondo rojo redondeado
        }

        // Al pintar el resultado, quitamos la selección azul primero
        layout?.isSelected = false

        // Fondo Redondeado
        layout?.setBackgroundResource(drawableResId)

        // Si el fondo es oscuro, forzamos el texto a blanco para que se lea bien
        if (layout != null) {
            setOptionsTextColor(layout, Color.WHITE)
        }
    }

    // Función auxiliar para cambiar el color del texto dentro de un layout de opción
    private fun setOptionsTextColor(layout: LinearLayout, color: Int) {
        for (i in 0 until layout.childCount) {
            val view = layout.getChildAt(i)
            if (view is TextView) {
                view.setTextColor(color)
            }
        }
    }

    private fun resetEstilosTarjetas() {
        // Lista de los contenedores (los LinearLayouts)
        val layouts = listOf(layoutA, layoutB, layoutC, layoutD)

        for (layout in layouts) {
            // Quitamos selección y ponemos fondo blanco
            layout.isSelected = false
            layout.setBackgroundResource(R.drawable.bg_blanco_redondeado)

            // Bucle que recorre todos los elementos dentro de la tarjeta (Para el texto y las letras A, B, C y D)
            for (i in 0 until layout.childCount) {
                val child = layout.getChildAt(i)
                if (child is TextView) {
                    child.setTextColor(ContextCompat.getColor(this, R.color.b500))
                }
            }
        }
    }

    private fun finalizarQuiz() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.quiz_dialog_titulo))
        builder.setMessage(getString(R.string.quiz_dialog_msg, aciertos, listaPreguntas.size))
        builder.setCancelable(false)
        builder.setPositiveButton(getString(R.string.quiz_btn_volver)) { _, _ -> finish() }
        builder.show()
    }

    private fun showKyroToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}