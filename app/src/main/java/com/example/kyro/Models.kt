package com.example.kyro

import kotlinx.serialization.SerialName
import java.io.Serializable

// Representa las asignaturas
data class Asignatura(
    val id: String = "",
    val nombre: String = "",
    val color: String = "#FF5252"
) : Serializable

// Representa un tema dentro de una asignatura
data class Tema(
    val id: String = "",
    val asignaturaId: String = "",
    val titulo: String = "",
    val contenido: String = "",
    val preguntasGeneradas: Boolean = false
) : Serializable

// Representa los ejercicios generados por la IA
data class Ejercicio(
    val id: String = "",
    val temaId: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val respuestaCorrecta: String = ""
) : Serializable

// Clase para apuntes de usuario (Supabase)
@kotlinx.serialization.Serializable
data class ApunteUsuario(
    val id: Long = 0,
    val created_at: String = "",
    val contenido: String = "",
    val titulo: String = "",
    val preguntas_json: String? = null,

    // Para que coincida con su columna de Supabase
    @SerialName("user_id")
    val user_id: String? = null
)

// Clase para manejar las preguntas generadas por la IA
data class PreguntaGenerada(
    val pregunta: String,
    val opciones: List<String>,
    val respuestaCorrecta: String, // La IA devolverá "A", "B", "C" o "D"
    val explicacion: String
) : Serializable

// Clase para manejar los ejercicios de cada temario
@kotlinx.serialization.Serializable
data class EjercicioIA(
    val id: Long = 0,
    val apunte_id: Long, // Vincula con el temario padre
    val nombre: String,  // Ej: "Test Generado 1"
    val preguntas_json: String // Aquí va el array de preguntas
)