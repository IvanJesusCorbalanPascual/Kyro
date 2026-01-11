package com.example.kyro

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
// Clase que se encarga de cambiar de pantalla e iluminar el botón correcto

object NavigationHelper {

    fun setupBottomNavigation(activity: Activity, selectedItemId: Int) {
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNavigation) ?: return

        // Limpiando el listener para evitar errores
        bottomNav.setOnItemSelectedListener(null)

        // Marcamos el botón que corresponde a ESTA pantalla
        bottomNav.selectedItemId = selectedItemId

        // Configuramos el listener
        bottomNav.setOnItemSelectedListener { item ->
            // Si pulsamos el mismo botón donde ya estamos, no hacemos nada y mantenemos la selección
            if (item.itemId == selectedItemId) {
                if (item.itemId == R.id.nav_syllabus && activity !is TemarioActivity) {
                    startActivityWithAnimation(activity, TemarioActivity::class.java)
                    return@setOnItemSelectedListener true
                }
                return@setOnItemSelectedListener true
            }

            when (item.itemId) {
                R.id.nav_home -> {
                    startActivityWithAnimation(activity, HomeActivity::class.java)
                    false // Devolvemos false para que el cambio visual lo gestione la nueva Activity
                }
                R.id.nav_calendar -> {
                    startActivityWithAnimation(activity, CalendarioActivity::class.java)
                    false
                }
                R.id.nav_ai_chat -> {
                    Toast.makeText(activity, "Próximamente: Chat IA", Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.nav_syllabus -> {
                    startActivityWithAnimation(activity, TemarioActivity::class.java)
                    false
                }
                R.id.nav_settings -> {
                    startActivityWithAnimation(activity, AjustesActivity::class.java)
                    false
                }
                else -> false
            }
        }
    }

    private fun startActivityWithAnimation(activity: Activity, targetActivity: Class<*>) {
        val intent = Intent(activity, targetActivity)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        activity.startActivity(intent)
        // Eliminamos la transición para que el cambio sea instantáneo (efecto tab)
        activity.overridePendingTransition(0, 0)
    }
}