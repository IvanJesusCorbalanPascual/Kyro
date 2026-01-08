package com.example.kyro

import kotlinx.serialization.Serializable

@Serializable // Muy importante
data class Examen(
    val id_usuario: String,         // El ID del usuario logueado
    val nombre_asignatura: String,
    val temario: String,
    val fecha_examen: String,       // Formato "YYYY-MM-DD"
    val nota: Double? = null        // Puede ser nulo si aún no se ha calificado
)
