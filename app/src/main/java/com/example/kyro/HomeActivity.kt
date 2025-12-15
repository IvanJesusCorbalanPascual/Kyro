package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setupBottomNavigation()
        setupQuickActions()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Marcando "Inicio" como seleccionado por defecto
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true

                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendarioActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_ai_chat -> {
                    Toast.makeText(this, "Próximamente: Chat IA", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.nav_syllabus -> {
                    startActivity(Intent(this, TemarioActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_settings -> {
                    startActivity(Intent(this, AjustesActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                else -> false
            }
        }
    }

    private fun setupQuickActions() {
        val btnQuickAI = findViewById<MaterialCardView>(R.id.btnQuickAI)
        btnQuickAI.setOnClickListener {
            Toast.makeText(this, "Abriendo Kyro IA...", Toast.LENGTH_SHORT).show()
        }

        val btnQuickSyllabus = findViewById<MaterialCardView>(R.id.btnQuickSyllabus)
        btnQuickSyllabus.setOnClickListener {
            startActivity(Intent(this, TemarioActivity::class.java))
        }


    }
}