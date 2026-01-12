package com.example.kyro

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast

// ARCHIVO DE EXTENSIONES (Metodos extra que se pueden usar en cualquier parte del codigo)
// Metodo "showKyroToast" para poder crear toast personalizados de Kyro
fun Context.showKyroToast(message: String) {
    // 1. Inflamos el diseño que acabamos de crear
    val inflater = LayoutInflater.from(this)
    val layout = inflater.inflate(R.layout.layout_custom_toast, null)

    // 2. Ponemos el texto que tú quieras
    val text: TextView = layout.findViewById(R.id.toast_text)
    text.text = message

    // 3. Creamos el Toast y le pegamos nuestro diseño
    val toast = Toast(applicationContext)
    toast.duration = Toast.LENGTH_SHORT
    toast.view = layout
    toast.setGravity(Gravity.BOTTOM, 0, 250) // Para que no tape la barra de navegación
    toast.show()
}