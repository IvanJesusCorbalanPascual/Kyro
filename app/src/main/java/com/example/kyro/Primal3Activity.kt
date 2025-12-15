package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Primal3Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pantalla_primal_3)

        val btnNext = findViewById<Button>(R.id.btnNext)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)

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