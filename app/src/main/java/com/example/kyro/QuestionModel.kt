package com.example.kyro

data class PreguntaGenerada(
    val pregunta: String,
    val opciones: List<String>, // ["A) ...", "B) ...", "C) ..."]
    val respuestaCorrecta: String, // El texto de la correcta
    val explicacion: String // Feedback para el alumno
) 