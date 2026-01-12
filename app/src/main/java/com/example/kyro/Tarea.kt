package com.example.kyro

import kotlinx.serialization.Serializable

@Serializable
data class Tarea(
    val id: Long? = null,
    val id_usuario: String,
    val nombre_asignatura: String,
    val descripcion: String,
    val fecha_entrega: String,
    val completada: Boolean = false,
    val notificacion1: String? = null,
    val notificacion2: String? = null
)
