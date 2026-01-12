package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Primal3Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pantalla_primal_3)

        val btnNext = findViewById<Button>(R.id.btnNext)
        val layoutDots = findViewById<LinearLayout>(R.id.layoutDots)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)

        val punto1 = layoutDots.getChildAt(0)
        val punto2 = layoutDots.getChildAt(1)
        val punto3 = layoutDots.getChildAt(2)

        // Abrimos el fichero de preferencias (como una libreta pequeña)
        val sharedPref = getSharedPreferences("PreferenciasApp", MODE_PRIVATE)

        // Escribimos que el tutorial ya se ha completado
        with (sharedPref.edit()) {
            putBoolean("intro_completada", true) // La clave es "intro_completada"
            apply() // Guardamos los cambios
        }

        // Lógica de Puntos
        punto1.isSelected = false
        punto2.isSelected = false
        punto3.isSelected = true

        // El boton de "Empezar Ahora" lleva al usuario al registro
        btnNext.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }

        // El boton de "Ir a Login" lleva al usuario al login
        tvGoToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}