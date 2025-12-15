package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class Primal1Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pantalla_primal_1)

        val btnNext = findViewById<ImageButton>(R.id.btnNext)

        btnNext.setOnClickListener { // Avanza de pantalla
            val intent = Intent(this, Primal2Activity::class.java)
            startActivity(intent)

            // Animación de transición suave
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}