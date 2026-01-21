package com.example.kyro

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Clase que se encarga de crear y hablar con un modelo de IA (En este caso Gemini 2-5 flash)
 * para generar preguntas tipo test basadas en un texto de apuntes y devolverlas en un JSON
 * para ser convertidas mas tarde en un tipo test didactico para el usuario
 */
class GeminiService {

    // Extrayendo la API_KEY de local.properties
    private val apiKey = BuildConfig.API_KEY

    // Creando el modelo Generativo de IA con la API_KEY y definiendo su modelo
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey
    )

    suspend fun generarTestDeApuntes(textoApuntes: String): List<PreguntaGenerada> {
        return withContext(Dispatchers.IO) {
            try {
                // Limitamos el texto para no saturar el token limit si el apunte es enorme
                val textoSeguro = textoApuntes.take(10000)

                val prompt = """
                    Eres un profesor experto. Genera preguntas tipo test basadas en este texto.
                    
                    TEXTO:
                    "$textoSeguro"
                    
                    REGLAS OBLIGATORIAS:
                    1. Responde ÚNICAMENTE con un JSON Array válido.
                    2. Cada pregunta debe tener EXACTAMENTE 4 opciones.
                    3. El campo "respuestaCorrecta" debe ser la letra: "A", "B", "C" o "D".
                    4. Formato JSON esperado:
                    [
                      {
                        "pregunta": "¿Pregunta?",
                        "opciones": ["Opción A", "Opción B", "Opción C", "Opción D"],
                        "respuestaCorrecta": "A",
                        "explicacion": "Breve explicación de por qué es la correcta."
                      }
                    ]
                """.trimIndent() // Para eliminar espacios en blanco innecesarios

                val response = generativeModel.generateContent(prompt)

                // Obtención segura del texto: tomamos la primera version de la respuesta de la IA (puede haber varias)
                // Entramos al contenido que puede estar dividido por partes (texto, imagenes, etc...)
                // Si no hay texto, devuelve una lista vacía
                val parte = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()
                val textoRespuesta = if (parte is com.google.ai.client.generativeai.type.TextPart) {
                    parte.text
                } else {
                    "" // Si la respuesta no es de texto, devuelve una cadena vacía
                }

                // Para ver la respuesta primal por la terminal (logcat)
                Log.d("KyroAI_RAW", "Respuesta cruda:\n$textoRespuesta")

                // Antes de intentar parsear a preguntas tipo test, verificamos que no sea una cadena vacía
                if (textoRespuesta.isBlank()) {
                    return@withContext emptyList()
                }

                // Limpieza de Markdown (```json ... ```)
                // Basicamente es por si la IA devuelve algo que no debe: Aqui tienes tu json: / Claro!, estas son tus preguntas:
                val jsonLimpio = textoRespuesta
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                val gson = Gson() // Creando una instancia Gson
                val tipoLista = object : TypeToken<List<PreguntaGenerada>>() {}.type

                // Convertimos cada objeto del JSON a un objeto Kotlin con la herramienta Gson
                return@withContext gson.fromJson(jsonLimpio, tipoLista)

            } catch (e: Exception) {
                Log.e("KyroAI", "❌ Error al conectar o parsear: ${e.message}")
                return@withContext emptyList()
            }
        }
    }
}