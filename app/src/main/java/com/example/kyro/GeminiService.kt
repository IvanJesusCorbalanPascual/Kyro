package com.example.kyro

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Clase que se encarga de crear y hablar con un modelo de IA (En este caso Gemini 2-5 flash)
 *
 * TEST: generar una cantidad de preguntas tipo test definida por el usuasrio, así como la dificultad de estas,
 * basadas en un texto de apuntes y devolverlas en un JSON
 * para ser convertidas mas tarde en un tipo test didactico para el usuario
 *
 * ASISTENTE KYRO AI: Resumir, Explicar o Esquematizar un texto pasado por el usuario
 */
class GeminiService {

    private val apiKey = BuildConfig.API_KEY
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey
    )

    /** Aqui es donde ocurre la Magia, este metodo recoge la informacion pasada por el usuario,
     * así como los parametros de dificultad y cantidad de preguntas, y genera unos ejercicios tipo test
     * en forma de JSON para que pueda ser convertido en un tipo test didactico para el usuario*/

    suspend fun generarTestDeApuntes(
        textoApuntes: String,
        dificultad: String,
        cantidad: Int
    ): List<PreguntaGenerada> {
        return withContext(Dispatchers.IO) {
            try {
                // Limitamos el texto por seguridad (Token limit)
                val textoSeguro = textoApuntes.take(20000)

                // CONSTRUIMOS EL PROMPT AQUÍ, USANDO LOS PARÁMETROS
                val prompt = """
                    TU ROL:
                    Eres un profesor veterano, bueno, inteligente y experto creando test.
                    Llevas muchos años en el mundo de la enseñanza, sabes lo que necesitas los alumnos y 
                    eres capaz de crear estos test de manera tan eficiente que los estudiantes pueden aprender de ellos.
                    Ademas te gusta optimizar tanto tus preguntas como tus respuestas basándote en la dificultad que se te exige:
                    - Facil: Preguntas sencillas, faciles de entender, que no sean demasiado largas, el estudiante no tiene mucho conocimiento sobre el tema
                    - Medio: Preguntas normales, lonfitud normal, ni muy faciles ni demasiado complicadas, el estudiante tiene una idea sobre el tema y le gustaria que lo apliquen
                    - Dificil: Preguntas para un estudiante que quiere ponerse a prueba, sabe mucho del tema y quiere preguntas que no cualquiera sabría responder, que sean un poco enrevesadas,
                      puedes hacer tanto las preguntas como las respeustas largas, pero tampoco superemos las 30 palabras.
                    
                    TU TAREA:
                    Genera un examen tipo test de $cantidad preguntas.
                    Nivel de dificultad: $dificultad.
                    Basado estrictamente en el tema del siguiente contenido el cual ha sido escrito por un estudiante 
                    (OJO, no cogas literalmente este texto y hagas preguntas de el, sino que cogas el contenido, lo analices en profundidad
                    y luego haz preguntas variadas sobre él):
                    
                    --- INICIO CONTENIDO ---
                    "$textoSeguro"
                    --- FIN CONTENIDO ---
                    
                    REGLAS OBLIGATORIAS DE FORMATO:
                    1. Responde ÚNICAMENTE con un JSON Array válido. Sin markdown, sin explicaciones previas.
                    2. Cada pregunta debe tener EXACTAMENTE 4 opciones.
                    3. El campo "respuestaCorrecta" debe ser la letra: "A", "B", "C" o "D".
                    4. NUNCA incluyas "a)", "b)", "c)" o "d)" en la respuesta, solo el texto de la respuesta.
                    5. Estructura JSON exacta:
                    [
                      {
                        "pregunta": "¿Enunciado de la pregunta?",
                        "opciones": ["Opción A", "Opción B", "Opción C", "Opción D"],
                        "respuestaCorrecta": "A",
                        "explicacion": "Breve explicación de la respuesta."
                      }
                    ]
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)

                val parte = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()
                val textoRespuesta = if (parte is com.google.ai.client.generativeai.type.TextPart) {
                    parte.text
                } else {
                    ""
                }

                Log.d("KyroAI_RAW", "Respuesta cruda:\n$textoRespuesta")

                if (textoRespuesta.isBlank()) return@withContext emptyList()

                // Limpieza robusta del JSON
                val jsonLimpio = textoRespuesta
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                val gson = Gson()
                val tipoLista = object : TypeToken<List<PreguntaGenerada>>() {}.type
                return@withContext gson.fromJson(jsonLimpio, tipoLista)

            } catch (e: Exception) {
                Log.e("KyroAI", "❌ Error al conectar o parsear: ${e.message}")
                return@withContext emptyList()
            }
        }
    }

    // Para la vista del Asistente KyroAI
    suspend fun consultarAsistente(texto: String, tipo: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val promptInstruccion = when (tipo) {
                    "RESUMIR" -> "Eres un experto tomando notas. Haz un resumen conciso y esquemático con los puntos clave del siguiente texto:"
                    "EXPLICAR" -> "Eres un profesor didáctico. Explica el siguiente concepto de forma sencilla y clara para un estudiante, usando analogías si ayuda:"
                    "ESQUEMA" -> "Genera un esquema estructurado (con guiones y subguiones) que organice lógicamente el siguiente contenido:"
                    else -> "Ayuda con este texto:"
                }

                val promptFinal = "$promptInstruccion\n\nTEXTO:\n\"$texto\""

                val response = generativeModel.generateContent(promptFinal)

                // Devuelve solo el texto limpio
                return@withContext response.text ?: "No pude generar una respuesta."
            } catch (e: Exception) {
                return@withContext "Error de conexión: ${e.message}"
            }
        }
    }
}