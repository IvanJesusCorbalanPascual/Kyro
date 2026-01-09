package com.example.kyro

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiService {

    // ⚠️ IMPORTANTE: En un proyecto real, esto iría en local.properties o BuildConfig.
    // Para el prototipo del TFG, pégala aquí pero NO subas esto a un GitHub público.
    private val apiKey = "AIzaSyCfsceVB_dw_IN4IFBGfdcHS_pPgxMyeHQ"

    // Creando el modelo de ia "Flash" (Rápido y eficiente) con nuestra apiKey
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    // Recibe el texto de los apuntes y devuelve una lista de preguntas
    suspend fun generarTestDeApuntes(textoApuntes: String): List<PreguntaGenerada> {
        return withContext(Dispatchers.IO) {
            try {
                // Le damos instrucciones muy precisas para que actúe como un profesor experto y para que devuelva las preguntas en el formato deseado
                val prompt = """
                    Eres un profesor experto y tutor de repaso.
                    Analiza el siguiente texto de estudio y genera 5 preguntas tipo test para un examen.
                    
                    TEXTO A ANALIZAR:
                    "$textoApuntes"
                    
                    REGLAS OBLIGATORIAS DE RESPUESTA:
                    1. Responde ÚNICAMENTE con un JSON Array. No saludes, no uses Markdown (```json).
                    2. Cada objeto del array debe tener exactamente esta estructura:
                       {
                         "pregunta": "texto de la pregunta",
                         "opciones": ["Opción A", "Opción B", "Opción C"],
                         "respuestaCorrecta": "Opción B",
                         "explicacion": "Breve explicación de por qué es la correcta (Scaffolding)"
                       }
                    3. La "explicacion" debe ser educativa.
                    4. Genera preguntas variadas sobre los conceptos clave del texto.
                """.trimIndent()

                // Llamada a la api
                val response = generativeModel.generateContent(prompt)

                // --- Limpieza y Parseo ---
                // A veces la IA devuelve bloques de código Markdown (```json ... ```). Los limpiamos.
                var jsonLimpio = response.text?.trim() ?: ""
                jsonLimpio = jsonLimpio.replace("```json", "").replace("```", "")

                // Convertimos el String JSON a objetos Kotlin usando Gson
                val gson = Gson()
                val tipoLista = object : TypeToken<List<PreguntaGenerada>>() {}.type
                val listaPreguntas: List<PreguntaGenerada> = gson.fromJson(jsonLimpio, tipoLista)

                return@withContext listaPreguntas

            } catch (e: Exception) {
                e.printStackTrace()
                // Si falla, devolvemos lista vacía o manejamos el error
                return@withContext emptyList()
            }
        }
    }
}