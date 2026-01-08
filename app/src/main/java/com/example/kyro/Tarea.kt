package com.example.kyro

import kotlinx.serialization.Serializable

@Serializable // Muy importante: permite que Supabase convierta esto a JSON
data class Tarea(
    val id_usuario: String,         // El ID del usuario logueado para saber de quién es la tarea
    val nombre_asignatura: String,
    val descripcion: String,
    val fecha_entrega: String,      // Formato "YYYY-MM-DD" para ser compatible con Supabase
    val completada: Boolean = false // Valor por defecto
)
