package com.example.kyro

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiService {

    // Extrayendo la API_KEY de local.properties
    private val apiKey = BuildConfig.API_KEY

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = apiKey
    )

    suspend fun generarTestDeApuntes(textoApuntes: String): List<PreguntaGenerada> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Eres un profesor experto y tutor de repaso.
                    Analiza el siguiente texto de estudio y genera 5 preguntas tipo test.
                    
                    TEXTO:
                    "$textoApuntes"
                    
                    REGLAS:
                    1. Responde SOLO con un JSON Array (sin Markdown).
                    2. Formato EXACTO:
                    [
                      {
                        "pregunta": "texto",
                        "opciones": ["A", "B", "C"],
                        "respuestaCorrecta": "B",
                        "explicacion": "Explicación educativa"
                      }
                    ]
                """.trimIndent()

                val response = generativeModel.generateContent(prompt) // Recogiendo la respuesta

                // ✅ FORMA CORRECTA DE LEER LA RESPUESTA
                // La forma correcta en la versión 0.9.0 es acceder a la propiedad 'text' dentro del part
                // O si esa te da problemas, usa esta cadena segura:
                val parte = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()
                // Forzamos a que sea texto plano
                val textoRespuesta = if (parte is com.google.ai.client.generativeai.type.TextPart) {
                    parte.text
                } else {
                    ""
                }

                Log.d("KyroAI_RAW", "Respuesta cruda:\n$textoRespuesta")

                if (textoRespuesta.isBlank()) {
                    Log.e("KyroAI", "❌ Respuesta vacía de Gemini")
                    return@withContext emptyList()
                }

                val jsonLimpio = textoRespuesta
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                val gson = Gson()
                val tipoLista = object : TypeToken<List<PreguntaGenerada>>() {}.type

                gson.fromJson<List<PreguntaGenerada>>(jsonLimpio, tipoLista)

            } catch (e: Exception) {
                Log.e("KyroAI", "❌ Error Gemini", e)
                emptyList()
            }
        }
    }
}
