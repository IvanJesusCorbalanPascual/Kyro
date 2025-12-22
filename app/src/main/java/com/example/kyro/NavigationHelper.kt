package com.example.kyro

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
// Clase que se encarga de cambiar de pantalla e iluminar el botón correcto

object NavigationHelper {

    /**
     * @param activity La actividad actual (this)
     * @param selectedItemId El ID del botón que debe estar iluminado en esta pantalla
     */
    fun setupBottomNavigation(activity: Activity, selectedItemId: Int) {

        // Guardando la barra de navegación en la variable bottomBar
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Si no existe la barra en el XML de esta pantalla, salimos para evitar crasheos
        if (bottomNav == null) return

        // Marcamos el botón correcto como seleccionado
        bottomNav.selectedItemId = selectedItemId

        // Definimos qué pasa al pulsar los botones
        bottomNav.setOnItemSelectedListener { item ->
            // Si pulsamos el botón de la pantalla en la que YA estamos, no hacemos nada
            if (item.itemId == selectedItemId) {
                return@setOnItemSelectedListener true
            }

            when (item.itemId) {
                R.id.nav_home -> {
                    startActivityWithAnimation(activity, HomeActivity::class.java)
                    true
                }
                R.id.nav_calendar -> {
                    startActivityWithAnimation(activity, CalendarioActivity::class.java)
                    true
                }
                R.id.nav_ai_chat -> {
                    Toast.makeText(activity, "Próximamente: Chat IA", Toast.LENGTH_SHORT).show()
                    false // False para que no se marque el botón
                }
                R.id.nav_syllabus -> {
                    startActivityWithAnimation(activity, TemarioActivity::class.java)
                    true
                }
                R.id.nav_settings -> {
                    startActivityWithAnimation(activity, AjustesActivity::class.java)
                    true
                }
                else -> false
            }
        }
    }

    // Función privada para hacer el cambio de pantalla sin animación (efecto "fijo")
    private fun startActivityWithAnimation(activity: Activity, targetActivity: Class<*>) {
        val intent = Intent(activity, targetActivity)
        // Opcional: Esto evita que se acumulen infinitas pantallas si pulsas mucho
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        activity.startActivity(intent)
        activity.overridePendingTransition(0, 0)
    }
}