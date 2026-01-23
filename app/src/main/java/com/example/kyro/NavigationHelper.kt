package com.example.kyro

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView

object NavigationHelper {

    fun setupBottomNavigation(activity: Activity, currentItemId: Int) {
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNavigation) ?: return

        // Anulamos el listener para evitar que se dispare al seleccionar el ítem mediante código
        bottomNav.setOnItemSelectedListener(null)
        // Marcamos el ítem de la actividad actual como seleccionado
        bottomNav.selectedItemId = currentItemId

        bottomNav.setOnItemSelectedListener { item ->
            // Si el ítem seleccionado es el actual, no hacemos nada
            if (item.itemId == currentItemId) {
                return@setOnItemSelectedListener true
            }

            val targetActivity: Class<*>? = when (item.itemId) {
                R.id.nav_home -> HomeActivity::class.java
                R.id.nav_calendar -> CalendarioActivity::class.java
                R.id.nav_asignatura -> AsignaturaActivity::class.java
                R.id.nav_settings -> AjustesActivity::class.java
                R.id.nav_kyro_ai -> KyroAiActivity::class.java
                else -> null
            }

            if (targetActivity != null) {
                startActivityWithAnimation(activity, targetActivity)
                // Devolvemos false para que la nueva actividad sea la que gestione el cambio visual
                return@setOnItemSelectedListener false
            }

            // Si no hay actividad de destino (como en el Toast), no cambiamos la selección
            return@setOnItemSelectedListener false
        }
    }

    private fun startActivityWithAnimation(activity: Activity, targetActivity: Class<*>) {
        val intent = Intent(activity, targetActivity)
        // Usamos esta bandera para traer una actividad existente al frente en lugar de crear una nueva
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        activity.startActivity(intent)
        // Eliminamos la animación de transición para un efecto de "pestaña" más fluido
        activity.overridePendingTransition(0, 0)
    }
}