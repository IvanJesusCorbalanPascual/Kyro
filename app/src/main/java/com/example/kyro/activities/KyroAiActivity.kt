package com.example.kyro.activities

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kyro.GeminiService
import com.example.kyro.NavigationHelper
import com.example.kyro.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Clase que maneja la vista central de la aplicacion KyroAI: Esta pantalla es un asistente de IA que ayuda al usuario a entender
 * mejor los contenidos de las asignaturas, de momento solo tiene 3 posibilidades (resumir, explicar y esquematizar) pero en el futuro
 * se podrían agregar mas opciones mas interesantes
 */
class KyroAiActivity : AppCompatActivity() {

    // Vistas
    private lateinit var etInput: TextInputEditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var btnProcesar: MaterialButton
    private lateinit var tvResultado: TextView
    private lateinit var cardResultado: MaterialCardView
    private lateinit var tvLabelResultado: TextView
    private lateinit var progressBar: ProgressBar

    // Servicio Gemini
    private val geminiService = GeminiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kyro_ai)

        initViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        NavigationHelper.setupBottomNavigation(this, R.id.nav_kyro_ai)
    }

    private fun initViews() {
        etInput = findViewById(R.id.etInputKyro)
        chipGroup = findViewById(R.id.chipGroupAcciones)
        btnProcesar = findViewById(R.id.btnProcesarIA)
        tvResultado = findViewById(R.id.tvResultadoIA)
        cardResultado = findViewById(R.id.cardResultado)
        tvLabelResultado = findViewById(R.id.tvLabelResultado)
        progressBar = findViewById(R.id.progressBarAI)
    }

    private fun setupListeners() {
        btnProcesar.setOnClickListener {
            val textoUsuario = etInput.text.toString().trim()

            if (textoUsuario.isEmpty()) {
                showKyroToast(getString(R.string.kyro_ai_toast_vacio))
                return@setOnClickListener
            }

            // Ocultar teclado para ver el resultado mejor
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etInput.windowToken, 0)

            // Detectar qué chip está seleccionado para saber qué pedirle a Gemini
            val tipoAccion = when (chipGroup.checkedChipId) {
                R.id.chipResumir -> "RESUMIR"
                R.id.chipExplicar -> "EXPLICAR"
                R.id.chipEsquema -> "ESQUEMA"
                else -> "RESUMIR"
            }

            procesarConsultaIA(textoUsuario, tipoAccion)
        }
    }

    private fun procesarConsultaIA(texto: String, accion: String) {
        // UI de Carga (ocultar resultado previo, mostrar barra de carga)
        progressBar.visibility = View.VISIBLE
        btnProcesar.isEnabled = false
        cardResultado.visibility = View.GONE
        tvLabelResultado.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Llamada a Gemini usando la función nueva que creamos en el servicio
                val respuesta = geminiService.consultarAsistente(texto, accion)

                // Mostrar resultado
                tvResultado.text = respuesta
                cardResultado.visibility = View.VISIBLE
                tvLabelResultado.visibility = View.VISIBLE

            } catch (e: Exception) {
                showKyroToast(getString(R.string.kyro_ai_error_conexion))
            } finally {
                // Restaurar estado original
                progressBar.visibility = View.GONE
                btnProcesar.isEnabled = true
            }
        }
    }

    private fun showKyroToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}