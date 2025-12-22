package com.example.kyro

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile( // Modelo similar al de la bd para guardar la informacion de los usuarios
    val id: String, // El ID que nos da Supabase Auth
    @SerialName("username") val username: String, // Mapeamos con 'etName'
    @SerialName("xp_total") val xpTotal: Int = 0,
    @SerialName("study_streak") val studyStreak: Int = 0
)