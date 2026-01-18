package com.example.kyro

import kotlinx.serialization.SerialName
import java.io.Serializable

// Representa las asignaturas
@kotlinx.serialization.Serializable
data class Asignatura(
    val id: Long = 0,
    val created_at: String = "",
    val contenido: String = "",
    val titulo: String = "",
    val preguntas_json: String? = null,

    // Para que coincida con su columna de Supabase
    @SerialName("user_id")
    val user_id: String? = null
)

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
data class Archivo(
    val id: Long = 0,
    @SerialName("user_id")
    val userId: String,
    @SerialName("asignatura_id")
    val asignaturaId: Long,
    @SerialName("tarea_id")
    val tareaId: Long? = null,
    @SerialName("examen_id")
    val examenId: Long? = null,
    val nombre: String,
    val url: String,
    val created_at: String = ""
)

// Clase para manejar las preguntas generadas por la IA
data class PreguntaGenerada(
    val pregunta: String,
    val opciones: List<String>,
    val respuestaCorrecta: String, // La IA devolverá "A", "B", "C" o "D"
    val explicacion: String
) : Serializable

// Clase para manejar los ejercicios de cada asignatura
@kotlinx.serialization.Serializable
data class EjercicioIA(
    val id: Long = 0,
    val asignatura_id: Long, // Vincula con la asignatura padre
    val nombre: String,  // Ej: "Test Generado 1"
    val preguntas_json: String // Aquí va el array de preguntas
)