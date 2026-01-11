package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_main) // Opcional si tienes layout
        /*
        // ---------------------------------------------------------
        // 🧪 ZONA DE PRUEBAS: GEMINI AI
        // ---------------------------------------------------------
        lifecycleScope.launch {
            try {
                Log.d("KyroAI", "🔵 Iniciando prueba de conexión con Gemini...")

                val servicioIA = GeminiService()

                // Texto de prueba (simulando unos apuntes de Historia)
                val apuntesPrueba = "La Revolución Industrial marcó un punto de inflexión en la historia, modificando e influenciando todos los aspectos de la vida cotidiana de una u otra manera. La producción tanto agrícola como de la naciente industria se multiplicó a la vez que disminuía el tiempo de producción."

                Log.d("KyroAI", "📤 Enviando texto a la IA...")
                val preguntas = servicioIA.generarTestDeApuntes(apuntesPrueba)

                if (preguntas.isNotEmpty()) {
                    Log.d("KyroAI", "✅ ¡ÉXITO! Se generaron ${preguntas.size} preguntas:")
                    preguntas.forEachIndexed { index, p ->
                        Log.d("KyroAI", "   [$index] P: ${p.pregunta}")
                        Log.d("KyroAI", "       R: ${p.respuestaCorrecta}")
                    }
                } else {
                    Log.e("KyroAI", "❌ La lista llegó vacía (revisa la API Key o la conexión)")
                }

            } catch (e: Exception) {
                Log.e("KyroAI", "❌ Error CRÍTICO durante la prueba: ${e.message}")
                e.printStackTrace()
            }
        }
        // ---------------------------------------------------------
        */

        // 👇 CÓDIGO ORIGINAL (COMENTADO TEMPORALMENTE) 👇
        // Mantenemos esto apagado para que la app no se cierre mientras la IA piensa

         // Al iniciar la aplicación por primera vez, te llevara a la presentación de la app,
        val intent = Intent(this, Primal1Activity::class.java)

        // Inicia la aplicacion
        startActivity(intent)

        // Cierra esta pantalla para que no moleste
        finish()

    }
}