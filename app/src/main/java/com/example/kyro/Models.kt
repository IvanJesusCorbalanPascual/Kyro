package com.example.kyro

import java.io.Serializable

// Representa  las asignaturas
data class Asignatura(
    val id: String = "",
    val nombre: String = "",
    // Color por defecto de las asignaturas
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

// Repredenta los ejercicios generados por la IA
data class Ejercicio(
    val id: String = "",
    // ID del tema al que pertenece el ejercicio
    val temaId: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    // Guarda la respuesta correcta por si fuera necesario
    val respuestaCorrecta: String = ""
) : Serializable

// Clase para poder enviar datos, contiene una etiqueta para no chocar con Serializable
@kotlinx.serialization.Serializable
data class ApunteUsuario(
    val contenido: String
)