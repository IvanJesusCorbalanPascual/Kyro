package com.example.kyro.entities

import kotlinx.serialization.Serializable

// Representa la estructura de datos para un examen
@Serializable
data class Examen(
    // El identificador unico del examen, puede ser nulo si aun no se ha guardado en la base de datos
    val id: Long? = null,
    // El identificador del usuario al que pertenece el examen
    val id_usuario: String,
    // El identificador de la asignatura a la que pertenece el examen
    val asignatura_id: Long,
    // El nombre del examen
    val nombre_examen: String,
    // Una descripcion detallada del examen
    val descripcion: String,
    // La fecha en que se realizara el examen
    val fecha_examen: String,
    // La hora en que se realizara el examen, puede ser nula
    val hora_examen: String? = null,
    // Indica si el examen ha sido completado o no
    val completada: Boolean = false,
    // La primera opcion de notificacion para el examen
    val notificacion1: String? = null,
    // La segunda opcion de notificacion para el examen
    val notificacion2: String? = null
)
