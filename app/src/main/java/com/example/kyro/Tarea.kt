package com.example.kyro

import kotlinx.serialization.Serializable

// Representa la estructura de datos para una tarea
@Serializable
data class Tarea(
    // El identificador unico de la tarea, puede ser nulo si aun no se ha guardado en la base de datos
    val id: Long? = null,
    // El identificador del usuario al que pertenece la tarea
    val id_usuario: String,
    // El identificador de la asignatura a la que pertenece la tarea
    val asignatura_id: Long,
    // El nombre de la tarea
    val nombre_tarea: String,
    // Una descripcion detallada de la tarea
    val descripcion: String,
    // La fecha de entrega de la tarea
    val fecha_entrega: String,
    // La hora de entrega de la tarea, puede ser nula
    val hora_entrega: String? = null,
    // Indica si la tarea ha sido completada o no
    val completada: Boolean = false,
    // La primera opcion de notificacion para la tarea
    val notificacion1: String? = null,
    // La segunda opcion de notificacion para la tarea
    val notificacion2: String? = null
)
